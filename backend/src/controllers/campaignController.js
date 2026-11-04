const campaignService = require('../services/campaignService');

const createCampaign = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const campaign = await campaignService.createCampaign(userId, req.body);
        res.status(201).json({
            success: true,
            data: campaign
        });
    } catch (error) {
        next(error);
    }
};

const getCampaigns = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;
        const result = await campaignService.getCampaigns(userId, page, limit);
        res.status(200).json({
            success: true,
            data: result
        });
    } catch (error) {
        next(error);
    }
};

const getCampaignDetails = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const recipientPage = parseInt(req.query.recipientPage) || 1;
        const recipientLimit = Math.min(Math.max(parseInt(req.query.recipientLimit) || 50, 1), 100);
        const recipientSearch = typeof req.query.recipientSearch === 'string' ? req.query.recipientSearch.trim().slice(0, 254) : '';

        const campaign = await campaignService.getCampaignDetails(
            req.params.id,
            userId,
            recipientPage,
            recipientLimit,
            recipientSearch
        );
        res.status(200).json({
            success: true,
            data: campaign
        });
    } catch (error) {
        next(error);
    }
};

const updateCampaign = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const campaign = await campaignService.updateCampaign(req.params.id, userId, req.body);
        res.status(200).json({
            success: true,
            data: campaign
        });
    } catch (error) {
        next(error);
    }
};

const deleteCampaign = async (req, res, next) => {
    try {
        const userId = req.user.id;
        await campaignService.deleteCampaign(req.params.id, userId);
        res.status(200).json({
            success: true,
            message: 'Campaign deleted successfully'
        });
    } catch (error) {
        next(error);
    }
};

const addRecipients = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const result = await campaignService.addRecipients(req.params.id, userId, req.body.recipients);
        res.status(200).json({
            success: true,
            data: result
        });
    } catch (error) {
        next(error);
    }
};

const removeRecipient = async (req, res, next) => {
    try {
        const userId = req.user.id;
        await campaignService.removeRecipient(req.params.campaignId, req.params.recipientId, userId);
        res.status(200).json({
            success: true,
            message: 'Recipient removed successfully'
        });
    } catch (error) {
        next(error);
    }
};

const markAsReady = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const campaign = await campaignService.markAsReady(req.params.id, userId);
        res.status(200).json({
            success: true,
            data: campaign
        });
    } catch (error) {
        next(error);
    }
};

const startCampaign = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const campaign = await campaignService.startCampaign(req.params.id, userId);
        res.status(200).json({
            success: true,
            data: campaign
        });
    } catch (error) {
        next(error);
    }
};

const pauseCampaign = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const campaign = await campaignService.pauseCampaign(req.params.id, userId);
        res.status(200).json({
            success: true,
            data: campaign
        });
    } catch (error) {
        next(error);
    }
};

const cancelCampaign = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const campaign = await campaignService.cancelCampaign(req.params.id, userId);
        res.status(200).json({
            success: true,
            data: campaign
        });
    } catch (error) {
        next(error);
    }
};

const retryFailedRecipients = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const result = await campaignService.retryFailedRecipients(req.params.id, userId);
        res.status(200).json({
            success: true,
            data: result
        });
    } catch (error) {
        next(error);
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
    retryFailedRecipients
};
