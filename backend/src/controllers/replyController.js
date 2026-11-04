const replyIntelligenceService = require('../services/replyIntelligenceService');

exports.analyzeReply = async (req, res, next) => {
    try {
        const { inboundEmailId } = req.params;
        const analysis = await replyIntelligenceService.analyzeReply(req.user.id, inboundEmailId);

        res.status(200).json({
            success: true,
            data: analysis
        });
    } catch (error) {
        next(error);
    }
};
