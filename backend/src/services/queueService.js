const { Queue } = require('bullmq');
const { redisConnection } = require('../config/redis');

const EMAIL_QUEUE_NAME = 'email-delivery';
const CAMPAIGN_QUEUE_NAME = 'campaign-delivery';

const emailQueue = new Queue(EMAIL_QUEUE_NAME, {
    connection: redisConnection,
    defaultJobOptions: {
        attempts: 3,
        backoff: {
            type: 'exponential',
            delay: 5000,
        },
        removeOnComplete: true,
        removeOnFail: false,
    }
});

const campaignQueue = new Queue(CAMPAIGN_QUEUE_NAME, {
    connection: redisConnection,
    defaultJobOptions: {
        attempts: 3,
        backoff: {
            type: 'exponential',
            delay: 10000,
        },
        removeOnComplete: true,
        removeOnFail: false,
    }
});

/**
 * Schedules an email delivery job in BullMQ.
 * @param {string} scheduledEmailId The UUID of the ScheduledEmail record
 * @param {Date} scheduledAt When the email should be sent
 */
const scheduleEmailJob = async (scheduledEmailId, scheduledAt) => {
    const delay = Math.max(0, scheduledAt.getTime() - Date.now());

    await emailQueue.add(
        'send-email',
        { scheduledEmailId },
        {
            delay,
            jobId: scheduledEmailId
        }
    );
};

/**
 * Removes/cancels a scheduled job from the queue.
 */
const cancelEmailJob = async (scheduledEmailId) => {
    const job = await emailQueue.getJob(scheduledEmailId);
    if (job) {
        await job.remove();
    }
};

/**
 * Enqueues a campaign dispatcher job.
 */
const enqueueCampaignDispatcher = async (campaignId) => {
    await campaignQueue.add(
        'dispatch-campaign',
        { campaignId },
        {
            jobId: `dispatch-${campaignId}` // Idempotent per campaign
        }
    );
};

/**
 * Enqueues an individual recipient delivery job.
 */
const enqueueRecipientDelivery = async (campaignId, recipientId) => {
    await campaignQueue.add(
        'deliver-recipient',
        { campaignId, recipientId },
        {
            jobId: `deliver-${recipientId}` // Idempotent per recipient
        }
    );
};

const closeQueues = async () => {
    await Promise.all([
        emailQueue.close(),
        campaignQueue.close()
    ]);
};

module.exports = {
    scheduleEmailJob,
    cancelEmailJob,
    enqueueCampaignDispatcher,
    enqueueRecipientDelivery,
    closeQueues,
    EMAIL_QUEUE_NAME,
    CAMPAIGN_QUEUE_NAME
};
