const outreachGenerationService = require('../services/outreachGenerationService');

exports.generateOutreach = async (req, res, next) => {
    try {
        const outreach = await outreachGenerationService.generateOutreach(req.user.id, req.body);

        res.status(200).json({
            success: true,
            data: outreach
        });
    } catch (error) {
        next(error);
    }
};
