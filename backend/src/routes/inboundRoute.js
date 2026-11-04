const express = require('express');
const router = express.Router();
const inboundController = require('../controllers/inboundController');

// Webhook endpoint (unauthenticated for provider inbound integration)
router.post('/webhook', inboundController.handleWebhook);

module.exports = router;
