const prisma = require('../utils/prisma');

const createCampaign = async (userId, data) => {
    return await prisma.campaign.create({
        data: {
            ...data,
            userId
        },
        include: {
            senderAccount: {
                select: {
                    id: true,
                    displayName: true,
                    email: true
                }
            }
        }
    });
};

const findCampaignsByUserId = async (userId, skip = 0, take = 20) => {
    return await prisma.campaign.findMany({
        where: { userId },
        skip,
        take,
        orderBy: { createdAt: 'desc' },
        include: {
            _count: {
                select: { recipients: true }
            }
        }
    });
};

const findCampaignById = async (id, userId) => {
    return await prisma.campaign.findFirst({
        where: { id, userId },
        include: {
            senderAccount: {
                select: {
                    id: true,
                    displayName: true,
                    email: true,
                    status: true
                }
            }
        }
    });
};

const findRecipientsByCampaignId = async (campaignId, skip = 0, take = 20, recipientSearch = '') => {
    const search = recipientSearch.trim();
    return await prisma.campaignRecipient.findMany({
        where: {
            campaignId,
            ...(search ? { recipient: { contains: search, mode: 'insensitive' } } : {})
        },
        skip,
        take,
        orderBy: { createdAt: 'asc' }
    });
};

const getBatchCampaignStats = async (campaignIds) => {
    const counts = await prisma.campaignRecipient.groupBy({
        by: ['campaignId', 'status'],
        where: {
            campaignId: { in: campaignIds }
        },
        _count: {
            id: true
        }
    });

    const statsMap = {};
    campaignIds.forEach(id => {
        statsMap[id] = {
            total: 0,
            PENDING: 0,
            QUEUED: 0,
            PROCESSING: 0,
            SENT: 0,
            FAILED: 0,
            CANCELLED: 0
        };
    });

    counts.forEach(c => {
        if (statsMap[c.campaignId]) {
            statsMap[c.campaignId][c.status] = c._count.id;
            statsMap[c.campaignId].total += c._count.id;
        }
    });

    return statsMap;
};

const getCampaignStats = async (id, userId) => {
    const counts = await prisma.campaignRecipient.groupBy({
        by: ['status'],
        where: {
            campaignId: id,
            campaign: { userId }
        },
        _count: {
            id: true
        }
    });

    const stats = {
        total: 0,
        PENDING: 0,
        QUEUED: 0,
        PROCESSING: 0,
        SENT: 0,
        FAILED: 0,
        CANCELLED: 0
    };

    counts.forEach(c => {
        stats[c.status] = c._count.id;
        stats.total += c._count.id;
    });

    return stats;
};

const updateCampaign = async (id, userId, data) => {
    return await prisma.campaign.update({
        where: { id, userId },
        data
    });
};

const deleteCampaign = async (id, userId) => {
    return await prisma.campaign.delete({
        where: { id, userId, status: 'DRAFT' }
    });
};

const addRecipients = async (campaignId, recipients) => {
    // recipients is array of strings (emails)
    const data = recipients.map(email => ({
        campaignId,
        recipient: email
    }));

    return await prisma.campaignRecipient.createMany({
        data,
        skipDuplicates: true
    });
};

const deleteRecipient = async (campaignId, recipientId, userId) => {
    return await prisma.campaignRecipient.delete({
        where: {
            id: recipientId,
            campaignId,
            campaign: { userId }
        }
    });
};

const findRecipientByEmail = async (campaignId, email) => {
    return await prisma.campaignRecipient.findUnique({
        where: {
            campaignId_recipient: {
                campaignId,
                recipient: email
            }
        }
    });
};

const findPendingRecipients = async (campaignId, take = 100) => {
    return await prisma.campaignRecipient.findMany({
        where: {
            campaignId,
            status: 'PENDING'
        },
        take
    });
};

const updateRecipientStatus = async (recipientId, status, data = {}) => {
    return await prisma.campaignRecipient.update({
        where: { id: recipientId },
        data: {
            status,
            ...data
        }
    });
};

const updateRecipientStatuses = async (campaignId, fromStatus, toStatus) => {
    return await prisma.campaignRecipient.updateMany({
        where: {
            campaignId,
            status: fromStatus
        },
        data: {
            status: toStatus
        }
    });
};

module.exports = {
    createCampaign,
    findCampaignsByUserId,
    findCampaignById,
    findRecipientsByCampaignId,
    getBatchCampaignStats,
    getCampaignStats,
    updateCampaign,
    deleteCampaign,
    addRecipients,
    deleteRecipient,
    findRecipientByEmail,
    findPendingRecipients,
    updateRecipientStatus,
    updateRecipientStatuses
};
