require('dotenv').config();
const { Worker } = require('bullmq');
const { redisConnection } = require('../config/redis');
const { EMAIL_QUEUE_NAME } = require('../services/queueService');
const prisma = require('../utils/prisma');
const smtpService = require('../services/smtpService');

const worker = new Worker(EMAIL_QUEUE_NAME, async (job) => {
    const { scheduledEmailId } = job.data;
    console.log(`Processing job ${job.id} for ScheduledEmail ${scheduledEmailId}`);

    try {
        // 1. Claim processing using atomic status update
        const scheduledEmail = await prisma.$transaction(async (tx) => {
            const record = await tx.scheduledEmail.findUnique({
                where: { id: scheduledEmailId },
                include: {
                    email: {
                        include: {
                            senderAccount: {
                                include: { credential: true }
                            }
                        }
                    }
                }
            });

            if (!record || record.status !== 'SCHEDULED') {
                console.log(`Job ${job.id} skipped. Status is ${record ? record.status : 'NOT_FOUND'}`);
                return null;
            }

            // Update to PROCESSING
            return await tx.scheduledEmail.update({
                where: { id: scheduledEmailId },
                data: { status: 'PROCESSING' },
                include: {
                    email: {
                        include: {
                            senderAccount: {
                                include: { credential: true }
                            }
                        }
                    }
                }
            });
        });

        if (!scheduledEmail) return;

        // 2. Prepare and send email
        const { email } = scheduledEmail;
        const { senderAccount } = email;

        try {
            await smtpService.sendEmail(senderAccount, {
                recipient: email.recipient,
                subject: email.subject,
                htmlBody: email.htmlBody,
                textBody: email.textBody
            });

            // 3. Update success statuses
            await prisma.$transaction([
                prisma.scheduledEmail.update({
                    where: { id: scheduledEmailId },
                    data: { status: 'SENT' }
                }),
                prisma.email.update({
                    where: { id: email.id },
                    data: { status: 'SENT' }
                }),
                prisma.activity.create({
                    data: {
                        userId: email.userId,
                        emailId: email.id,
                        type: 'EMAIL_SENT',
                        message: `Scheduled email sent to ${email.recipient}`
                    }
                })
            ]);

            console.log(`Job ${job.id} completed successfully`);

        } catch (sendError) {
            console.error(`SMTP Send failed for job ${job.id}:`, sendError.message);

            // 4. Update failure statuses
            await prisma.$transaction([
                prisma.scheduledEmail.update({
                    where: { id: scheduledEmailId },
                    data: { status: 'FAILED' }
                }),
                prisma.email.update({
                    where: { id: email.id },
                    data: { status: 'FAILED' }
                }),
                prisma.activity.create({
                    data: {
                        userId: email.userId,
                        emailId: email.id,
                        type: 'EMAIL_FAILED',
                        message: `Failed to send scheduled email to ${email.recipient}: ${sendError.message}`
                    }
                })
            ]);

            throw sendError; // BullMQ will handle retry based on queue config
        }

    } catch (error) {
        console.error(`Worker error for job ${job.id}:`, error.message);
        throw error;
    }
}, {
    connection: redisConnection,
    concurrency: 5
});

worker.on('completed', (job) => {
    console.log(`Job ${job.id} has completed!`);
});

worker.on('failed', (job, err) => {
    console.error(`Job ${job.id} has failed with ${err.message}`);
});

console.log('Email worker started');

const gracefulShutdown = async () => {
    console.log('Stopping worker...');
    await worker.close();
    await prisma.$disconnect();
    process.exit(0);
};

process.on('SIGTERM', gracefulShutdown);
process.on('SIGINT', gracefulShutdown);

module.exports = worker;
