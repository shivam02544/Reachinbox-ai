const emailService = require('../services/emailService');

const sendEmail = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const email = await emailService.sendEmailImmediate(userId, req.body);

        res.status(201).json({
            success: true,
            data: { email }
        });
    } catch (error) {
        next(error);
    }
};

const getEmails = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const data = await emailService.getEmails(userId, req.query);

        res.status(200).json({
            success: true,
            data: data
        });
    } catch (error) {
        next(error);
    }
};

const getEmailDetails = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const email = await emailService.getEmailDetails(req.params.id, userId);

        res.status(200).json({
            success: true,
            data: { email }
        });
    } catch (error) {
        next(error);
    }
};

const getEmailThread = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const thread = await emailService.getEmailThread(req.params.id, userId);

        res.status(200).json({
            success: true,
            data: thread
        });
    } catch (error) {
        next(error);
    }
};

const saveDraft = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const draft = await emailService.saveDraft(userId, req.body);
        res.status(201).json({
            success: true,
            data: { draft }
        });
    } catch (error) {
        next(error);
    }
};

const getDrafts = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;
        const data = await emailService.getDrafts(userId, page, limit);
        res.status(200).json({
            success: true,
            data
        });
    } catch (error) {
        next(error);
    }
};

const updateDraft = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const draft = await emailService.updateDraft(req.params.id, userId, req.body);
        res.status(200).json({
            success: true,
            data: { draft }
        });
    } catch (error) {
        next(error);
    }
};

const deleteDraft = async (req, res, next) => {
    try {
        const userId = req.user.id;
        await emailService.deleteDraft(req.params.id, userId);
        res.status(200).json({
            success: true,
            message: 'Draft deleted successfully'
        });
    } catch (error) {
        next(error);
    }
};

const sendDraft = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const { idempotencyKey } = req.body;
        const email = await emailService.sendDraft(req.params.id, userId, idempotencyKey);
        res.status(200).json({
            success: true,
            data: { email }
        });
    } catch (error) {
        next(error);
    }
};

const scheduleDraft = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const { scheduledAt } = req.body;
        const scheduledEmail = await emailService.scheduleDraft(req.params.id, userId, scheduledAt);
        res.status(200).json({
            success: true,
            data: { scheduledEmail }
        });
    } catch (error) {
        next(error);
    }
};

module.exports = {
    sendEmail,
    getEmails,
    getEmailDetails,
    getEmailThread,
    saveDraft,
    getDrafts,
    updateDraft,
    deleteDraft,
    sendDraft,
    scheduleDraft
};
