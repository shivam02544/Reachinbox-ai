require('dotenv').config();
const { Worker } = require('bullmq');
const { redisConnection } = require('../config/redis');
const { CAMPAIGN_QUEUE_NAME, enqueueRecipientDelivery, enqueueCampaignDispatcher } = require('../services/queueService');
const prisma = require('../utils/prisma');
const smtpService = require('../services/smtpService');
const campaignRepository = require('../repositories/campaignRepository');
const campaignService = require('../services/campaignService');

const BATCH_SIZE = 100;

const worker = new Worker(CAMPAIGN_QUEUE_NAME, async (job) => {
    if (job.name === 'dispatch-campaign') {
        await handleDispatch(job);
    } else if (job.name === 'deliver-recipient') {
        await handleDelivery(job);
    }
}, {
    connection: redisConnection,
    concurrency: 10
});

async function handleDispatch(job) {
    const { campaignId } = job.data;
    console.log(`Dispatching campaign ${campaignId}`);

    const campaign = await prisma.campaign.findUnique({
        where: { id: campaignId }
    });

    if (!campaign || campaign.status !== 'RUNNING') {
        console.log(`Campaign ${campaignId} is not RUNNING. Current status: ${campaign ? campaign.status : 'NOT_FOUND'}`);
        return;
    }

    // 1. Get a batch of pending recipients
    const recipients = await campaignRepository.findPendingRecipients(campaignId, BATCH_SIZE);

    if (recipients.length === 0) {
        // Check if all are done
        await campaignService.checkCompletion(campaignId, campaign.userId);
        return;
    }

    // 2. Enqueue delivery jobs and update status to QUEUED
    for (const recipient of recipients) {
        try {
            await prisma.$transaction(async (tx) => {
                await tx.campaignRecipient.update({
                    where: { id: recipient.id },
                    data: { status: 'QUEUED' }
                });
                await enqueueRecipientDelivery(campaignId, recipient.id);
            });
        } catch (err) {
            console.error(`Failed to enqueue delivery for recipient ${recipient.id}:`, err.message);
        }
    }

    // 3. Enqueue next dispatch if there might be more
    if (recipients.length === BATCH_SIZE) {
        await enqueueCampaignDispatcher(campaignId);
    }
}

async function handleDelivery(job) {
    const { campaignId, recipientId } = job.data;
    console.log(`Delivering recipient ${recipientId} for campaign ${campaignId}`);

    // 1. Verify campaign status and recipient status
    const result = await prisma.$transaction(async (tx) => {
        const campaign = await tx.campaign.findUnique({
            where: { id: campaignId },
            include: {
                senderAccount: {
                    include: { credential: true }
                }
            }
        });

        if (!campaign || campaign.status !== 'RUNNING') {
            // If paused, move back to PENDING so it can be picked up later
            if (campaign && campaign.status === 'PAUSED') {
                await tx.campaignRecipient.update({
                    where: { id: recipientId },
                    data: { status: 'PENDING' }
                });
            } else if (campaign && campaign.status === 'CANCELLED') {
                await tx.campaignRecipient.update({
                    where: { id: recipientId },
                    data: { status: 'CANCELLED' }
                });
            }
            return null;
        }

        const recipient = await tx.campaignRecipient.findUnique({
            where: { id: recipientId }
        });

        if (!recipient || recipient.status !== 'QUEUED') {
            return null;
        }

        // Update to PROCESSING
        const updatedRecipient = await tx.campaignRecipient.update({
            where: { id: recipientId },
            data: { status: 'PROCESSING', attempts: { increment: 1 } }
        });

        return { campaign, recipient: updatedRecipient };
    });

    if (!result) return;

    const { campaign, recipient } = result;

    try {
        // 2. Ensure Email record exists (Idempotency)
        // Idempotency key: campaignId + recipientId
        const idempotencyKey = recipientId; // recipientId is unique and stable for this send

        let email = await prisma.email.findFirst({
            where: { userId: campaign.userId, idempotencyKey }
        });

        // Strict Idempotency Check: Skip if already terminal
        if (email && (email.status === 'SENT' || email.status === 'FAILED')) {
            console.log(`Email for recipient ${recipientId} already terminal (${email.status}). Skipping.`);

            // Sync recipient status if they got out of sync
            if (recipient.status !== email.status) {
                await prisma.campaignRecipient.update({
                    where: { id: recipientId },
                    data: { status: email.status, sentAt: email.status === 'SENT' ? new Date() : null }
                });
            }
            return;
        }

        if (!email) {
            email = await prisma.email.create({
                data: {
                    userId: campaign.userId,
                    senderAccountId: campaign.senderAccountId,
                    recipient: recipient.recipient,
                    subject: campaign.subject,
                    htmlBody: campaign.bodyHtml,
                    textBody: campaign.bodyText,
                    status: 'PROCESSING',
                    idempotencyKey
                }
            });

            await prisma.campaignRecipient.update({
                where: { id: recipientId },
                data: { emailId: email.id }
            });
        }

        // 3. Send via SMTP
        await smtpService.sendEmail(campaign.senderAccount, {
            recipient: recipient.recipient,
            subject: campaign.subject,
            htmlBody: campaign.bodyHtml,
            textBody: campaign.bodyText
        });

        // 4. Update success
        await prisma.$transaction([
            prisma.campaignRecipient.update({
                where: { id: recipientId },
                data: { status: 'SENT', sentAt: new Date() }
            }),
            prisma.email.update({
                where: { id: email.id },
                data: { status: 'SENT' }
            }),
            prisma.activity.create({
                data: {
                    userId: campaign.userId,
                    emailId: email.id,
                    type: 'EMAIL_SENT',
                    message: `Campaign email sent to ${recipient.recipient}`
                }
            })
        ]);

        // Check for completion after success
        await campaignService.checkCompletion(campaignId, campaign.userId);

    } catch (error) {
        console.error(`Delivery failed for recipient ${recipientId}:`, error.message);

        // Update failure
        await prisma.$transaction([
            prisma.campaignRecipient.update({
                where: { id: recipientId },
                data: { status: 'FAILED', failedAt: new Date(), errorMessage: error.message }
            }),
            // Also update Email if it exists
            ...(recipient.emailId ? [prisma.email.update({
                where: { id: recipient.emailId },
                data: { status: 'FAILED' }
            })] : [])
        ]);

        // Check for completion after failure
        await campaignService.checkCompletion(campaignId, campaign.userId);

        throw error;
    }
}

worker.on('completed', (job) => {
    console.log(`Campaign job ${job.id} has completed!`);
});

worker.on('failed', (job, err) => {
    console.error(`Campaign job ${job.id} has failed with ${err.message}`);
});

console.log('Campaign worker started');

module.exports = worker;
