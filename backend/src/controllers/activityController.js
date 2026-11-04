const activityService = require('../services/activityService');

const getActivities = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;

        const data = await activityService.getActivities(userId, page, limit);

        res.status(200).json({
            success: true,
            data: data
        });
    } catch (error) {
        next(error);
    }
};

module.exports = {
    getActivities
};
