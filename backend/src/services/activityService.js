const activityRepository = require('../repositories/activityRepository');

const getActivities = async (userId, page = 1, limit = 20) => {
    const skip = (page - 1) * limit;
    const [activities, total] = await Promise.all([
        activityRepository.findActivitiesByUserId(userId, skip, limit),
        activityRepository.countActivitiesByUserId(userId)
    ]);

    return {
        activities,
        pagination: {
            total,
            page,
            limit,
            totalPages: Math.ceil(total / limit)
        }
    };
};

module.exports = {
    getActivities
};
