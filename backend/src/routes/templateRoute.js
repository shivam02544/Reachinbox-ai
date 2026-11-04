const express = require('express');
const templateController = require('../controllers/templateController');
const authMiddleware = require('../middleware/authMiddleware');
const { validate } = require('../validators/authValidator');
const { createTemplateSchema, updateTemplateSchema, templateQuerySchema, templateIdParamSchema } = require('../validators/templateValidator');

const router = express.Router();

router.use(authMiddleware);

/**
 * @route   POST /api/v1/templates
 */
router.post('/', validate(createTemplateSchema), templateController.createTemplate);

/**
 * @route   GET /api/v1/templates
 */
router.get('/', validate(templateQuerySchema, 'query'), templateController.getTemplates);

/**
 * @route   GET /api/v1/templates/:id
 */
router.get('/:id', validate(templateIdParamSchema, 'params'), templateController.getTemplate);

/**
 * @route   PATCH /api/v1/templates/:id
 */
router.patch('/:id', validate(templateIdParamSchema, 'params'), validate(updateTemplateSchema), templateController.updateTemplate);

/**
 * @route   DELETE /api/v1/templates/:id
 */
router.delete('/:id', validate(templateIdParamSchema, 'params'), templateController.deleteTemplate);

module.exports = router;
