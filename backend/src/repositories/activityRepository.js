const prisma = require('../utils/prisma');

const findActivitiesByUserId = async (userId, skip = 0, take = 20) => {
    return await prisma.activity.findMany({
        where: { userId },
        orderBy: { createdAt: 'desc' },
        skip,
        take,
        include: {
            email: {
                select: {
                    id: true,
                    subject: true,
                    recipient: true,
                    status: true
                }
            }
        }
    });
};

const countActivitiesByUserId = async (userId) => {
    return await prisma.activity.count({
        where: { userId }
    });
};

module.exports = {
    findActivitiesByUserId,
    countActivitiesByUserId
};
