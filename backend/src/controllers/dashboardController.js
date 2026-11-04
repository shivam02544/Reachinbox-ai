const dashboardService = require('../services/dashboardService');

const getDashboard = async (req, res, next) => {
    try {
        // userId is attached to req.user by authMiddleware
        const userId = req.user.id;
        const data = await dashboardService.getDashboardData(userId);

        res.status(200).json({
            success: true,
            data: data
        });
    } catch (error) {
        next(error);
    }
};

module.exports = {
    getDashboard
};
