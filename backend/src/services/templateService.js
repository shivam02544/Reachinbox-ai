const templateRepository = require('../repositories/templateRepository');

const createTemplate = async (userId, data) => {
    return await templateRepository.createTemplate(userId, data);
};

const getTemplates = async (userId, query) => {
    const { page, limit, search, sortBy, sortOrder } = query;
    const { templates, total } = await templateRepository.findTemplatesByUserId(userId, {
        page: parseInt(page) || 1,
        limit: Math.min(parseInt(limit) || 20, 100),
        search,
        sortBy,
        sortOrder
    });

    const totalPages = Math.ceil(total / (parseInt(limit) || 20));

    return {
        templates,
        pagination: {
            total,
            page: parseInt(page) || 1,
            limit: parseInt(limit) || 20,
            totalPages,
            hasNextPage: (parseInt(page) || 1) < totalPages,
            hasPrevPage: (parseInt(page) || 1) > 1
        }
    };
};

const getTemplate = async (id, userId) => {
    const template = await templateRepository.findTemplateById(id, userId);
    if (!template) {
        const error = new Error('Template not found');
        error.statusCode = 404;
        error.code = 'NOT_FOUND';
        throw error;
    }
    return template;
};

const updateTemplate = async (id, userId, data) => {
    const template = await templateRepository.findTemplateById(id, userId);
    if (!template) {
        const error = new Error('Template not found');
        error.statusCode = 404;
        error.code = 'NOT_FOUND';
        throw error;
    }
    return await templateRepository.updateTemplate(id, userId, data);
};

const deleteTemplate = async (id, userId) => {
    const template = await templateRepository.findTemplateById(id, userId);
    if (!template) {
        const error = new Error('Template not found');
        error.statusCode = 404;
        error.code = 'NOT_FOUND';
        throw error;
    }
    await templateRepository.deleteTemplate(id, userId);
};

module.exports = {
    createTemplate,
    getTemplates,
    getTemplate,
    updateTemplate,
    deleteTemplate
};
