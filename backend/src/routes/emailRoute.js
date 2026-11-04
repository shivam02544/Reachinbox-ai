const express = require('express');
const emailController = require('../controllers/emailController');
const authMiddleware = require('../middleware/authMiddleware');
const { validate } = require('../validators/authValidator');
const {
    sendEmailSchema,
    draftSchema,
    updateDraftSchema,
    getEmailsQuerySchema,
    emailIdParamSchema
} = require('../validators/emailValidator');
const { z } = require('zod');

const router = express.Router();

router.use(authMiddleware);

/**
 * @route   POST /api/v1/emails
 */
router.post('/', validate(sendEmailSchema), emailController.sendEmail);

/**
 * @route   GET /api/v1/emails
 */
router.get('/', validate(getEmailsQuerySchema, 'query'), emailController.getEmails);

/**
 * Draft Endpoints
 * registered before /:id to avoid conflicts
 */
router.post('/drafts', validate(draftSchema), emailController.saveDraft);
router.get('/drafts', emailController.getDrafts);
router.get('/drafts/:id', validate(emailIdParamSchema, 'params'), emailController.getEmailDetails); // Re-use details
router.patch('/drafts/:id', validate(emailIdParamSchema, 'params'), validate(updateDraftSchema), emailController.updateDraft);
router.delete('/drafts/:id', validate(emailIdParamSchema, 'params'), emailController.deleteDraft);

router.post('/drafts/:id/send', validate(emailIdParamSchema, 'params'), validate(z.object({
    idempotencyKey: z.string().uuid()
})), emailController.sendDraft);

router.post('/drafts/:id/schedule', validate(emailIdParamSchema, 'params'), validate(z.object({
    scheduledAt: z.string().datetime()
})), emailController.scheduleDraft);

/**
 * @route   GET /api/v1/emails/:id/thread
 */
router.get('/:id/thread', validate(emailIdParamSchema, 'params'), emailController.getEmailThread);

/**
 * @route   GET /api/v1/emails/:id
 */
router.get('/:id', validate(emailIdParamSchema, 'params'), emailController.getEmailDetails);

module.exports = router;
