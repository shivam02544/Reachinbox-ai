const express = require('express');
const router = express.Router();
const replyController = require('../controllers/replyController');
const authMiddleware = require('../middleware/authMiddleware');

router.use(authMiddleware);

router.post('/:inboundEmailId/analyze', replyController.analyzeReply);

module.exports = router;
