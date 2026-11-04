const express = require('express');
const activityController = require('../controllers/activityController');
const authMiddleware = require('../middleware/authMiddleware');

const router = express.Router();

router.use(authMiddleware);

/**
 * @route   GET /api/v1/activity
 * @desc    Get user activities
 * @access  Private
 */
router.get('/', activityController.getActivities);

module.exports = router;
