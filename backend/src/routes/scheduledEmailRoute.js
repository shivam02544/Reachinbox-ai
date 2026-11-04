const express = require('express');
const scheduledEmailController = require('../controllers/scheduledEmailController');
const authMiddleware = require('../middleware/authMiddleware');
const { validate } = require('../validators/authValidator');
const { scheduleEmailSchema, scheduledEmailIdParamSchema } = require('../validators/scheduledEmailValidator');

const router = express.Router();

router.use(authMiddleware);

/**
 * @route   POST /api/v1/scheduled-emails
 */
router.post('/', validate(scheduleEmailSchema), scheduledEmailController.scheduleEmail);

/**
 * @route   GET /api/v1/scheduled-emails
 */
router.get('/', scheduledEmailController.getScheduledEmails);

/**
 * @route   DELETE /api/v1/scheduled-emails/:id
 */
router.delete('/:id', validate(scheduledEmailIdParamSchema, 'params'), scheduledEmailController.cancelScheduledEmail);

module.exports = router;
