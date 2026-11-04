const prisma = require('../utils/prisma');

const createEmail = async (data) => {
    return await prisma.email.create({
        data,
        include: {
            senderAccount: true
        }
    });
};

const findEmailByIdempotencyKey = async (userId, idempotencyKey) => {
    return await prisma.email.findUnique({
        where: {
            userId_idempotencyKey: {
                userId,
                idempotencyKey
            }
        },
        include: {
            senderAccount: true
        }
    });
};

const updateEmailStatus = async (id, status) => {
    return await prisma.email.update({
        where: { id },
        data: { status }
    });
};

const findEmailById = async (id, userId) => {
    return await prisma.email.findFirst({
        where: { id, userId },
        include: {
            senderAccount: {
                select: {
                    id: true,
                    displayName: true,
                    email: true,
                    provider: true
                }
            },
            scheduledEmails: {
                orderBy: { createdAt: 'desc' },
                take: 1
            }
        }
    });
};

const findEmailsByUserId = async (userId, filters = {}, skip = 0, take = 20, sort = { createdAt: 'desc' }) => {
    const where = {
        userId,
        ...buildWhereClause(filters)
    };

    return await prisma.email.findMany({
        where,
        orderBy: sort,
        skip,
        take,
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

const countEmailsByUserId = async (userId, filters = {}) => {
    const where = {
        userId,
        ...buildWhereClause(filters)
    };

    return await prisma.email.count({
        where
    });
};

const buildWhereClause = (filters) => {
    const where = {};

    if (filters.status) {
        where.status = filters.status;
    }

    if (filters.senderAccountId) {
        where.senderAccountId = filters.senderAccountId;
    }

    if (filters.search) {
        where.OR = [
            { subject: { contains: filters.search, mode: 'insensitive' } },
            { recipient: { contains: filters.search, mode: 'insensitive' } },
            { senderAccount: { email: { contains: filters.search, mode: 'insensitive' } } }
        ];
    }

    if (filters.from || filters.to) {
        where.createdAt = {};
        if (filters.from) where.createdAt.gte = new Date(filters.from);
        if (filters.to) where.createdAt.lte = new Date(filters.to);
    }

    return where;
};

const findDraftsByUserId = async (userId, skip = 0, take = 20) => {
    return await prisma.email.findMany({
        where: { userId, status: 'DRAFT' },
        orderBy: { updatedAt: 'desc' },
        skip,
        take,
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

const countDraftsByUserId = async (userId) => {
    return await prisma.email.count({
        where: { userId, status: 'DRAFT' }
    });
};

const updateDraft = async (id, userId, data) => {
    return await prisma.email.update({
        where: { id, userId, status: 'DRAFT' },
        data
    });
};

const deleteDraft = async (id, userId) => {
    return await prisma.email.delete({
        where: { id, userId, status: 'DRAFT' }
    });
};

const createScheduledEmail = async (data) => {
    return await prisma.scheduledEmail.create({
        data
    });
};

const updateScheduledEmailStatus = async (id, status) => {
    return await prisma.scheduledEmail.update({
        where: { id },
        data: { status }
    });
};

const findScheduledEmailById = async (id, userId) => {
    return await prisma.scheduledEmail.findFirst({
        where: {
            id,
            email: { userId }
        },
        include: {
            email: {
                include: {
                    senderAccount: {
                        include: { credential: true }
                    }
                }
            }
        }
    });
};

const findScheduledEmailsByUserId = async (userId, skip = 0, take = 20) => {
    return await prisma.scheduledEmail.findMany({
        where: {
            email: { userId }
        },
        orderBy: { scheduledAt: 'desc' },
        skip,
        take,
        include: {
            email: {
                include: {
                    senderAccount: {
                        select: {
                            id: true,
                            displayName: true,
                            email: true
                        }
                    }
                }
            }
        }
    });
};

const countScheduledEmailsByUserId = async (userId) => {
    return await prisma.scheduledEmail.count({
        where: {
            email: { userId }
        }
    });
};

module.exports = {
    createEmail,
    findEmailByIdempotencyKey,
    updateEmailStatus,
    findEmailById,
    findEmailsByUserId,
    countEmailsByUserId,
    findDraftsByUserId,
    countDraftsByUserId,
    updateDraft,
    deleteDraft,
    createScheduledEmail,
    updateScheduledEmailStatus,
    findScheduledEmailById,
    findScheduledEmailsByUserId,
    countScheduledEmailsByUserId
};
