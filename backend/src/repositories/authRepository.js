const prisma = require('../utils/prisma');

const findUserByEmail = async (email) => {
    return await prisma.user.findUnique({
        where: { email }
    });
};

const findUserById = async (id) => {
    return await prisma.user.findUnique({
        where: { id }
    });
};

const createUser = async (data) => {
    return await prisma.user.create({
        data
    });
};

const createRefreshToken = async (data) => {
    return await prisma.refreshToken.create({
        data
    });
};

const findRefreshTokenByHash = async (tokenHash) => {
    return await prisma.refreshToken.findUnique({
        where: { tokenHash },
        include: { user: true }
    });
};

const updateRefreshToken = async (id, data) => {
    return await prisma.refreshToken.update({
        where: { id },
        data
    });
};

const revokeRefreshTokensForUser = async (userId) => {
    return await prisma.refreshToken.updateMany({
        where: {
            userId,
            revokedAt: null
        },
        data: {
            revokedAt: new Date()
        }
    });
};

module.exports = {
    findUserByEmail,
    findUserById,
    createUser,
    createRefreshToken,
    findRefreshTokenByHash,
    updateRefreshToken,
    revokeRefreshTokensForUser
};
