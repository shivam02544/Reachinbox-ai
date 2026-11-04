const prisma = require('../utils/prisma');

const createTemplate = async (userId, data) => {
    return await prisma.template.create({
        data: {
            ...data,
            userId
        }
    });
};

const findTemplatesByUserId = async (userId, { page = 1, limit = 20, search, sortBy = 'updatedAt', sortOrder = 'desc' }) => {
    const skip = (page - 1) * limit;
    const where = {
        userId,
        OR: search ? [
            { name: { contains: search, mode: 'insensitive' } },
            { subject: { contains: search, mode: 'insensitive' } }
        ] : undefined
    };

    const [templates, total] = await Promise.all([
        prisma.template.findMany({
            where,
            skip,
            take: limit,
            orderBy: { [sortBy]: sortOrder }
        }),
        prisma.template.count({ where })
    ]);

    return { templates, total };
};

const findTemplateById = async (id, userId) => {
    return await prisma.template.findFirst({
        where: { id, userId }
    });
};

const updateTemplate = async (id, userId, data) => {
    return await prisma.template.update({
        where: { id, userId },
        data
    });
};

const deleteTemplate = async (id, userId) => {
    return await prisma.template.delete({
        where: { id, userId }
    });
};

module.exports = {
    createTemplate,
    findTemplatesByUserId,
    findTemplateById,
    updateTemplate,
    deleteTemplate
};
