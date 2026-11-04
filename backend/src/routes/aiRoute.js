const express = require('express');
const router = express.Router();
const aiController = require('../controllers/aiController');
const resumeRoutes = require('./resumeRoute');
const jobRoutes = require('./jobRoute');
const matchRoutes = require('./matchRoute');
const outreachRoutes = require('./outreachRoute');
const followUpRoutes = require('./followUpRoute');
const replyRoutes = require('./replyRoute');
const authMiddleware = require('../middleware/authMiddleware');
const { validate, testGenerateSchema, improveEmailSchema } = require('../validators/aiValidator');

// Apply auth middleware to all AI routes
router.use(authMiddleware);

// Mount Sub-Routes
router.use('/resume', resumeRoutes);
router.use('/job', jobRoutes);
router.use('/match', matchRoutes);
router.use('/outreach', outreachRoutes);
router.use('/followup', followUpRoutes);
router.use('/reply', replyRoutes);

/**
 * @route   GET /api/v1/ai/health
 * @desc    Check AI service status/configuration
 * @access  Private
 */
router.get('/health', aiController.getHealth);

/**
 * @route   POST /api/v1/ai/test
 * @desc    Test AI generation
 * @access  Private
 */
router.post('/test', validate(testGenerateSchema), aiController.testGenerate);

router.post('/compose/improve', validate(improveEmailSchema), aiController.improveEmail);

module.exports = router;
