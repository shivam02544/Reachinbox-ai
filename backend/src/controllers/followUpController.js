const followUpGenerationService = require('../services/followUpGenerationService');

exports.generateFollowUp = async (req, res, next) => {
    try {
        const followUp = await followUpGenerationService.generateFollowUp(req.user.id, req.body);

        res.status(200).json({
            success: true,
            data: followUp
        });
    } catch (error) {
        next(error);
    }
};
