const express = require('express');
const campaignController = require('../controllers/campaignController');
const authMiddleware = require('../middleware/authMiddleware');
const { validate } = require('../validators/authValidator');
const {
    createCampaignSchema,
    updateCampaignSchema,
    addRecipientsSchema,
    campaignIdParamSchema,
    campaignRecipientParamsSchema
} = require('../validators/campaignValidator');

const router = express.Router();

router.use(authMiddleware);

/**
 * @route   POST /api/v1/campaigns
 */
router.post('/', validate(createCampaignSchema), campaignController.createCampaign);

/**
 * @route   GET /api/v1/campaigns
 */
router.get('/', campaignController.getCampaigns);

/**
 * @route   GET /api/v1/campaigns/:id
 */
router.get('/:id', validate(campaignIdParamSchema, 'params'), campaignController.getCampaignDetails);

/**
 * @route   PATCH /api/v1/campaigns/:id
 */
router.patch('/:id', validate(campaignIdParamSchema, 'params'), validate(updateCampaignSchema), campaignController.updateCampaign);

/**
 * @route   DELETE /api/v1/campaigns/:id
 */
router.delete('/:id', validate(campaignIdParamSchema, 'params'), campaignController.deleteCampaign);

/**
 * @route   POST /api/v1/campaigns/:id/recipients
 */
router.post('/:id/recipients', validate(campaignIdParamSchema, 'params'), validate(addRecipientsSchema), campaignController.addRecipients);

/**
 * @route   DELETE /api/v1/campaigns/:campaignId/recipients/:recipientId
 */
router.delete('/:campaignId/recipients/:recipientId', validate(campaignRecipientParamsSchema, 'params'), campaignController.removeRecipient);

/**
 * @route   POST /api/v1/campaigns/:id/ready
 */
router.post('/:id/ready', validate(campaignIdParamSchema, 'params'), campaignController.markAsReady);

/**
 * @route   POST /api/v1/campaigns/:id/start
 */
router.post('/:id/start', validate(campaignIdParamSchema, 'params'), campaignController.startCampaign);

/**
 * @route   POST /api/v1/campaigns/:id/resume
 */
router.post('/:id/resume', validate(campaignIdParamSchema, 'params'), campaignController.startCampaign);

/**
 * @route   POST /api/v1/campaigns/:id/pause
 */
router.post('/:id/pause', validate(campaignIdParamSchema, 'params'), campaignController.pauseCampaign);

/**
 * @route   POST /api/v1/campaigns/:id/cancel
 */
router.post('/:id/cancel', validate(campaignIdParamSchema, 'params'), campaignController.cancelCampaign);

/**
 * @route   POST /api/v1/campaigns/:id/retry-failed
 */
router.post('/:id/retry-failed', validate(campaignIdParamSchema, 'params'), campaignController.retryFailedRecipients);

module.exports = router;
