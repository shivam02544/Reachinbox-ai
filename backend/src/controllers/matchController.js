const matchService = require('../services/matchService');

exports.generateMatch = async (req, res, next) => {
    try {
        const { jobRequirementId } = req.body;

        const match = await matchService.generateMatch(req.user.id, jobRequirementId);

        res.status(200).json({
            success: true,
            data: match
        });
    } catch (error) {
        next(error);
    }
};

exports.getMatch = async (req, res, next) => {
    try {
        const { jobRequirementId } = req.params;

        const match = await matchService.getMatch(req.user.id, jobRequirementId);

        if (!match) {
            return res.status(404).json({
                success: false,
                message: 'Match analysis not found for this job requirement.'
            });
        }

        res.status(200).json({
            success: true,
            data: match
        });
    } catch (error) {
        next(error);
    }
};
