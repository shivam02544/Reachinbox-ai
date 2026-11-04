const prisma = require('../utils/prisma');

const getEmailStats = async (userId) => {
    const stats = await prisma.email.groupBy({
        by: ['status'],
        where: { userId },
        _count: {
            id: true
        }
    });

    // Transform to a more accessible object
    const result = {
        totalEmails: 0,
        DRAFT: 0,
        SCHEDULED: 0,
        SENT: 0,
        FAILED: 0
    };

    stats.forEach(stat => {
        result[stat.status] = stat._count.id;
        result.totalEmails += stat._count.id;
    });

    return result;
};

const getSenderCount = async (userId) => {
    return await prisma.senderAccount.count({
        where: { userId }
    });
};

const getRecentActivities = async (userId, limit = 10) => {
    return await prisma.activity.findMany({
        where: { userId },
        orderBy: { createdAt: 'desc' },
        take: limit,
        select: {
            id: true,
            type: true,
            message: true,
            createdAt: true,
            emailId: true
        }
    });
};

module.exports = {
    getEmailStats,
    getSenderCount,
    getRecentActivities
};
