const prisma = require('../utils/prisma');

const createSender = async (userId, data, credentialData) => {
    return await prisma.senderAccount.create({
        data: {
            ...data,
            userId,
            credential: {
                create: credentialData
            }
        },
        include: {
            credential: true
        }
    });
};

const findSendersByUserId = async (userId) => {
    return await prisma.senderAccount.findMany({
        where: { userId },
        orderBy: { createdAt: 'desc' }
    });
};

const findSenderById = async (id, userId) => {
    return await prisma.senderAccount.findFirst({
        where: { id, userId },
        include: { credential: true }
    });
};

const updateSender = async (id, userId, data) => {
    return await prisma.senderAccount.update({
        where: { id, userId },
        data
    });
};

const deleteSender = async (id, userId) => {
    // Check if emails reference this sender before deletion
    // In our schema Email has onDelete: Cascade for SenderAccount,
    // but maybe we want to restrict deletion if there are SENT emails for audit.
    // For now, let's follow the requirement: verify ownership.
    return await prisma.senderAccount.delete({
        where: { id, userId }
    });
};

const updateSenderStatus = async (id, status) => {
    return await prisma.senderAccount.update({
        where: { id },
        data: { status }
    });
};

module.exports = {
    createSender,
    findSendersByUserId,
    findSenderById,
    updateSender,
    deleteSender,
    updateSenderStatus
};
