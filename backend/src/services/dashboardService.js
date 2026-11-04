const dashboardRepository = require('../repositories/dashboardRepository');

const getDashboardData = async (userId) => {
    const [emailStats, senderCount, recentActivities] = await Promise.all([
        dashboardRepository.getEmailStats(userId),
        dashboardRepository.getSenderCount(userId),
        dashboardRepository.getRecentActivities(userId)
    ]);

    return {
        stats: {
            totalEmails: emailStats.totalEmails,
            drafts: emailStats.DRAFT,
            scheduled: emailStats.SCHEDULED,
            sent: emailStats.SENT,
            failed: emailStats.FAILED,
            totalSenders: senderCount
        },
        recentActivity: recentActivities
    };
};

module.exports = {
    getDashboardData
};
