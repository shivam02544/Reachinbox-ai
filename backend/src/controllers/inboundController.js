const inboundMailService = require('../services/inboundMailService');

exports.handleWebhook = async (req, res, next) => {
    try {
        const inboundEmail = await inboundMailService.processInboundMail(req.body);

        res.status(200).json({
            success: true,
            data: { id: inboundEmail.id }
        });
    } catch (error) {
        next(error);
    }
};
