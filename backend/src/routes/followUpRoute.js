const express = require('express');
const router = express.Router();
const followUpController = require('../controllers/followUpController');
const authMiddleware = require('../middleware/authMiddleware');
const { generateFollowUpSchema } = require('../validators/followUpValidator');
const { validate } = require('../validators/aiValidator'); // Common validator wrapper

router.use(authMiddleware);

router.post('/generate', validate(generateFollowUpSchema), followUpController.generateFollowUp);

module.exports = router;
