const emailService = require('../services/emailService');

const scheduleEmail = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const result = await emailService.scheduleEmail(userId, req.body);

        res.status(201).json({
            success: true,
            data: result
        });
    } catch (error) {
        next(error);
    }
};

const getScheduledEmails = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;

        const data = await emailService.getScheduledEmails(userId, page, limit);

        res.status(200).json({
            success: true,
            data: data
        });
    } catch (error) {
        next(error);
    }
};

const cancelScheduledEmail = async (req, res, next) => {
    try {
        const userId = req.user.id;
        await emailService.cancelScheduledEmail(userId, req.params.id);

        res.status(200).json({
            success: true,
            message: 'Scheduled email cancelled successfully'
        });
    } catch (error) {
        next(error);
    }
};

module.exports = {
    scheduleEmail,
    getScheduledEmails,
    cancelScheduledEmail
};
