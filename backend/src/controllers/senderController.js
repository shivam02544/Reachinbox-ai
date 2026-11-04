const senderService = require('../services/senderService');

const createSender = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const sender = await senderService.addSender(userId, req.body);
        res.status(201).json({
            success: true,
            data: sender
        });
    } catch (error) {
        next(error);
    }
};

const getSenders = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const senders = await senderService.getSenders(userId);
        res.status(200).json({
            success: true,
            data: senders
        });
    } catch (error) {
        next(error);
    }
};

const getSender = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const sender = await senderService.getSender(req.params.id, userId);
        res.status(200).json({
            success: true,
            data: sender
        });
    } catch (error) {
        next(error);
    }
};

const verifySender = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const result = await senderService.verifySender(req.params.id, userId);
        res.status(200).json({
            success: true,
            data: result
        });
    } catch (error) {
        next(error);
    }
};

const deleteSender = async (req, res, next) => {
    try {
        const userId = req.user.id;
        await senderService.deleteSender(req.params.id, userId);
        res.status(200).json({
            success: true,
            message: 'Sender deleted successfully'
        });
    } catch (error) {
        next(error);
    }
};

module.exports = {
    createSender,
    getSenders,
    getSender,
    verifySender,
    deleteSender
};
