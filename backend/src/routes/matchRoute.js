const express = require('express');
const router = express.Router();
const matchController = require('../controllers/matchController');
const authMiddleware = require('../middleware/authMiddleware');
const { matchSchema } = require('../validators/jobRequirementValidator');
const { validate } = require('../validators/aiValidator'); // get validate from common validator

router.use(authMiddleware);

router.post('/', validate(matchSchema), matchController.generateMatch);
router.get('/:jobRequirementId', matchController.getMatch);

module.exports = router;
