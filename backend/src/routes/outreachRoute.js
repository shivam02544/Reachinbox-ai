const express = require('express');
const router = express.Router();
const outreachController = require('../controllers/outreachController');
const authMiddleware = require('../middleware/authMiddleware');
const { generateOutreachSchema } = require('../validators/outreachValidator');
const { validate } = require('../validators/aiValidator'); // get validate from common validator

router.use(authMiddleware);

router.post('/generate', validate(generateOutreachSchema), outreachController.generateOutreach);

module.exports = router;
