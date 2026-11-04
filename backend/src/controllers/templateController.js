const templateService = require('../services/templateService');

const createTemplate = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const template = await templateService.createTemplate(userId, req.body);
        res.status(201).json({
            success: true,
            data: template
        });
    } catch (error) {
        next(error);
    }
};

const getTemplates = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const result = await templateService.getTemplates(userId, req.query);
        res.status(200).json({
            success: true,
            data: result
        });
    } catch (error) {
        next(error);
    }
};

const getTemplate = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const template = await templateService.getTemplate(req.params.id, userId);
        res.status(200).json({
            success: true,
            data: template
        });
    } catch (error) {
        next(error);
    }
};

const updateTemplate = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const template = await templateService.updateTemplate(req.params.id, userId, req.body);
        res.status(200).json({
            success: true,
            data: template
        });
    } catch (error) {
        next(error);
    }
};

const deleteTemplate = async (req, res, next) => {
    try {
        const userId = req.user.id;
        await templateService.deleteTemplate(req.params.id, userId);
        res.status(200).json({
            success: true,
            message: 'Template deleted successfully'
        });
    } catch (error) {
        next(error);
    }
};

module.exports = {
    createTemplate,
    getTemplates,
    getTemplate,
    updateTemplate,
    deleteTemplate
};
