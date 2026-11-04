const express = require('express');
const senderController = require('../controllers/senderController');
const authMiddleware = require('../middleware/authMiddleware');
const { validate } = require('../validators/authValidator'); // Reuse validate wrapper
const { createSenderSchema, senderIdParamSchema } = require('../validators/senderValidator');

const router = express.Router();

router.use(authMiddleware);

/**
 * @route   POST /api/v1/senders
 */
router.post('/', validate(createSenderSchema), senderController.createSender);

/**
 * @route   GET /api/v1/senders
 */
router.get('/', senderController.getSenders);

/**
 * @route   GET /api/v1/senders/:id
 */
router.get('/:id', validate(senderIdParamSchema, 'params'), senderController.getSender);

/**
 * @route   POST /api/v1/senders/:id/verify
 */
router.post('/:id/verify', validate(senderIdParamSchema, 'params'), senderController.verifySender);

/**
 * @route   DELETE /api/v1/senders/:id
 */
router.delete('/:id', validate(senderIdParamSchema, 'params'), senderController.deleteSender);

module.exports = router;
