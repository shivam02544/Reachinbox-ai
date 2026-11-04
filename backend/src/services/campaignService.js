const campaignRepository = require('../repositories/campaignRepository');
const senderRepository = require('../repositories/senderRepository');

const queueService = require('./queueService');
const prisma = require('../utils/prisma');

const createCampaign = async (userId, campaignData) => {
    const { senderAccountId } = campaignData;

    // Verify sender ownership
    const sender = await senderRepository.findSenderById(senderAccountId, userId);
    if (!sender) {
        const error = new Error('Sender account not found or access denied');
        error.statusCode = 404;
        error.code = 'SENDER_NOT_FOUND';
        throw error;
    }

    return await campaignRepository.createCampaign(userId, campaignData);
};

const getCampaigns = async (userId, page = 1, limit = 20) => {
    const finalLimit = Math.min(limit, 100);
    const skip = (page - 1) * finalLimit;
    const [campaigns, total] = await Promise.all([
        campaignRepository.findCampaignsByUserId(userId, skip, finalLimit),
        prisma.campaign.count({ where: { userId } })
    ]);

    // Enhance campaigns with stats in a single batch query (Fix N+1)
    const campaignIds = campaigns.map(c => c.id);
    const statsMap = await campaignRepository.getBatchCampaignStats(campaignIds);

    const enhancedCampaigns = campaigns.map(c => ({
        ...c,
        stats: statsMap[c.id]
    }));

    return {
        campaigns: enhancedCampaigns,
        pagination: {
            total,
            page,
            limit: finalLimit,
            totalPages: Math.ceil(total / finalLimit)
        }
    };
};

const getCampaignDetails = async (id, userId, recipientPage = 1, recipientLimit = 50, recipientSearch = '') => {
    const campaign = await campaignRepository.findCampaignById(id, userId);
    if (!campaign) {
        const error = new Error('Campaign not found');
        error.statusCode = 404;
        error.code = 'NOT_FOUND';
        throw error;
    }

    const recipientSkip = (recipientPage - 1) * recipientLimit;
    const [stats, recipients] = await Promise.all([
        campaignRepository.getCampaignStats(id, userId),
        campaignRepository.findRecipientsByCampaignId(id, recipientSkip, recipientLimit, recipientSearch)
    ]);

    return {
        ...campaign,
        stats,
        recipients
    };
};

const updateCampaign = async (id, userId, updateData) => {
    const campaign = await campaignRepository.findCampaignById(id, userId);
    if (!campaign) {
        const error = new Error('Campaign not found');
        error.statusCode = 404;
        error.code = 'NOT_FOUND';
        throw error;
    }

    if (campaign.status !== 'DRAFT') {
        const error = new Error('Only DRAFT campaigns can be updated');
        error.statusCode = 409;
        error.code = 'INVALID_STATUS';
        throw error;
    }

    if (updateData.senderAccountId) {
        const sender = await senderRepository.findSenderById(updateData.senderAccountId, userId);
        if (!sender) {
            const error = new Error('Sender account not found or access denied');
            error.statusCode = 404;
            error.code = 'SENDER_NOT_FOUND';
            throw error;
        }
    }

    return await campaignRepository.updateCampaign(id, userId, updateData);
};

const deleteCampaign = async (id, userId) => {
    const campaign = await campaignRepository.findCampaignById(id, userId);
    if (!campaign) {
        const error = new Error('Campaign not found');
        error.statusCode = 404;
        error.code = 'NOT_FOUND';
        throw error;
    }

    if (campaign.status !== 'DRAFT') {
        const error = new Error('Only DRAFT campaigns can be deleted');
        error.statusCode = 409;
        error.code = 'INVALID_STATUS';
        throw error;
    }

    return await campaignRepository.deleteCampaign(id, userId);
};

const addRecipients = async (id, userId, recipients) => {
    const campaign = await campaignRepository.findCampaignById(id, userId);
    if (!campaign) {
        const error = new Error('Campaign not found');
        error.statusCode = 404;
        error.code = 'NOT_FOUND';
        throw error;
    }

    if (campaign.status !== 'DRAFT') {
        const error = new Error('Only DRAFT campaigns can receive recipients');
        error.statusCode = 409;
        error.code = 'INVALID_STATUS';
        throw error;
    }

    // Filter duplicates in the array
    const uniqueRecipients = [...new Set(recipients)];

    // Valid emails filter can be done here or in validator
    const validRecipients = uniqueRecipients.filter(email =>
        /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
    );

    const result = await campaignRepository.addRecipients(id, validRecipients);

    return {
        added: result.count,
        skipped: uniqueRecipients.length - result.count,
        invalid: recipients.length - uniqueRecipients.length
    };
};

const removeRecipient = async (campaignId, recipientId, userId) => {
    const campaign = await campaignRepository.findCampaignById(campaignId, userId);
    if (!campaign) {
        const error = new Error('Campaign not found');
        error.statusCode = 404;
        error.code = 'NOT_FOUND';
        throw error;
    }

    if (campaign.status !== 'DRAFT') {
        const error = new Error('Only DRAFT campaigns can modify recipients');
        error.statusCode = 409;
        error.code = 'INVALID_STATUS';
        throw error;
    }

    return await campaignRepository.deleteRecipient(campaignId, recipientId, userId);
};

const markAsReady = async (id, userId) => {
    const campaign = await campaignRepository.findCampaignById(id, userId);
    if (!campaign) {
        const error = new Error('Campaign not found');
        error.statusCode = 404;
        error.code = 'NOT_FOUND';
        throw error;
    }

    if (campaign.status !== 'DRAFT') {
        const error = new Error('Campaign is already beyond DRAFT status');
        error.statusCode = 409;
        error.code = 'INVALID_STATUS';
        throw error;
    }

    // Validation
    if (!campaign.subject || (!campaign.bodyHtml && !campaign.bodyText)) {
        const error = new Error('Campaign must have a subject and body');
        error.statusCode = 400;
        error.code = 'INCOMPLETE_CONTENT';
        throw error;
    }

    if (!campaign.senderAccount || campaign.senderAccount.status !== 'CONNECTED') {
        const error = new Error('Campaign must have a CONNECTED sender account');
        error.statusCode = 400;
        error.code = 'INVALID_SENDER';
        throw error;
    }

    const stats = await campaignRepository.getCampaignStats(id, userId);
    if (stats.total === 0) {
        const error = new Error('Campaign must have at least one recipient');
        error.statusCode = 400;
        error.code = 'NO_RECIPIENTS';
        throw error;
    }

    return await campaignRepository.updateCampaign(id, userId, { status: 'READY' });
};

const startCampaign = async (id, userId) => {
    const campaign = await campaignRepository.findCampaignById(id, userId);
    if (!campaign) {
        const error = new Error('Campaign not found');
        error.statusCode = 404;
        throw error;
    }

    if (campaign.status !== 'READY' && campaign.status !== 'PAUSED') {
        const error = new Error(`Cannot start campaign in status: ${campaign.status}`);
        error.statusCode = 409;
        throw error;
    }

    // 1. Update status to RUNNING
    // We do this in a specific order to ensure consistency
    // If enqueuing fails, we don't want the campaign to stay in RUNNING
    try {
        await queueService.enqueueCampaignDispatcher(id);
        const updatedCampaign = await campaignRepository.updateCampaign(id, userId, { status: 'RUNNING' });

        // 2. Log activity
        await prisma.activity.create({
            data: {
                userId,
                type: 'CAMPAIGN_STARTED',
                message: `Campaign "${campaign.name}" has started.`
            }
        });

        return updatedCampaign;
    } catch (queueError) {
        console.error('Failed to enqueue campaign dispatcher:', queueError.message);
        throw new Error('Failed to start campaign: queue service unavailable');
    }
};

const pauseCampaign = async (id, userId) => {
    const campaign = await campaignRepository.findCampaignById(id, userId);
    if (!campaign || campaign.status !== 'RUNNING') {
        const error = new Error('Campaign not found or not running');
        error.statusCode = 404;
        throw error;
    }

    const updated = await campaignRepository.updateCampaign(id, userId, { status: 'PAUSED' });

    // Note: Workers check status before each recipient delivery, so it will pause gracefully.

    return updated;
};

const cancelCampaign = async (id, userId) => {
    const campaign = await campaignRepository.findCampaignById(id, userId);
    if (!campaign) {
        const error = new Error('Campaign not found');
        error.statusCode = 404;
        throw error;
    }

    const terminalStates = ['COMPLETED', 'CANCELLED', 'FAILED'];
    if (terminalStates.includes(campaign.status)) {
        const error = new Error('Campaign is already in a terminal state');
        error.statusCode = 409;
        throw error;
    }

    const updated = await prisma.$transaction(async (tx) => {
        const campaign = await tx.campaign.update({
            where: { id, userId },
            data: { status: 'CANCELLED' }
        });

        // Cancel all non-terminal recipients (PENDING, QUEUED, PROCESSING)
        await tx.campaignRecipient.updateMany({
            where: {
                campaignId: id,
                status: { in: ['PENDING', 'QUEUED', 'PROCESSING'] }
            },
            data: { status: 'CANCELLED' }
        });

        return campaign;
    });

    return updated;
};

const retryFailedRecipients = async (id, userId) => {
    const campaign = await campaignRepository.findCampaignById(id, userId);
    if (!campaign) {
        const error = new Error('Campaign not found');
        error.statusCode = 404;
        throw error;
    }

    const eligibleStates = ['READY', 'PAUSED', 'FAILED'];
    if (!eligibleStates.includes(campaign.status)) {
        const error = new Error(`Cannot retry recipients for campaign in ${campaign.status} status`);
        error.statusCode = 409;
        throw error;
    }

    const result = await prisma.campaignRecipient.updateMany({
        where: {
            campaignId: id,
            status: 'FAILED'
        },
        data: {
            status: 'PENDING',
            attempts: 0,
            errorMessage: null
        }
    });

    return {
        retried: result.count
    };
};

const checkCompletion = async (id, userId) => {
    const campaign = await campaignRepository.findCampaignById(id, userId);
    if (!campaign || campaign.status === 'COMPLETED' || campaign.status === 'CANCELLED') {
        return;
    }

    const stats = await campaignRepository.getCampaignStats(id, userId);
    const activeStates = ['PENDING', 'QUEUED', 'PROCESSING'];
    const activeCount = activeStates.reduce((acc, state) => acc + (stats[state] || 0), 0);

    if (activeCount === 0 && stats.total > 0) {
        await campaignRepository.updateCampaign(id, userId, { status: 'COMPLETED' });
        console.log(`Campaign ${id} marked as COMPLETED`);

        await prisma.activity.create({
            data: {
                userId,
                type: 'CAMPAIGN_COMPLETED',
                message: `Campaign "${campaign.name}" has completed.`
            }
        });
    }
};

module.exports = {
    createCampaign,
    getCampaigns,
    getCampaignDetails,
    updateCampaign,
    deleteCampaign,
    addRecipients,
    removeRecipient,
    markAsReady,
    startCampaign,
    pauseCampaign,
    cancelCampaign,
    retryFailedRecipients,
    checkCompletion
};
