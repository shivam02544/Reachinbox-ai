const express = require('express');
const router = express.Router();
const jobRequirementController = require('../controllers/jobRequirementController');
const authMiddleware = require('../middleware/authMiddleware');
const { jobRequirementSchema, jobFromUrlSchema } = require('../validators/jobRequirementValidator');
const { validate } = require('../validators/aiValidator'); // get validate from common validator

router.use(authMiddleware);

router.post('/', validate(jobRequirementSchema), jobRequirementController.createJobRequirement);
router.post('/from-url', validate(jobFromUrlSchema), jobRequirementController.createFromUrl);
router.get('/', jobRequirementController.getAllJobRequirements);
router.get('/:id', jobRequirementController.getJobRequirement);
router.delete('/:id', jobRequirementController.deleteJobRequirement);

module.exports = router;
