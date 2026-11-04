const express = require('express');
const dashboardController = require('../controllers/dashboardController');
const authMiddleware = require('../middleware/authMiddleware');

const router = express.Router();

/**
 * @route   GET /api/v1/dashboard
 * @desc    Get dashboard statistics and recent activity
 * @access  Private
 */
router.get('/', authMiddleware, dashboardController.getDashboard);

module.exports = router;
