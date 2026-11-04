const express = require('express');
const authController = require('../controllers/authController');
const authMiddleware = require('../middleware/authMiddleware');
const {
    validate,
    registerSchema,
    loginSchema,
    refreshSchema,
    logoutSchema
} = require('../validators/authValidator');
const rateLimit = require('express-rate-limit');

const router = express.Router();

// Rate limiting for auth endpoints
const authLimiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 20, // Limit each IP to 20 requests per window
    message: {
        success: false,
        message: 'Too many requests from this IP, please try again after 15 minutes',
        error: { code: 'TOO_MANY_REQUESTS' }
    },
    standardHeaders: true,
    legacyHeaders: false,
});

/**
 * @route   POST /api/v1/auth/register
 */
router.post('/register', authLimiter, validate(registerSchema), authController.register);

/**
 * @route   POST /api/v1/auth/login
 */
router.post('/login', authLimiter, validate(loginSchema), authController.login);

/**
 * @route   POST /api/v1/auth/refresh
 */
router.post('/refresh', authLimiter, validate(refreshSchema), authController.refresh);

/**
 * @route   POST /api/v1/auth/logout
 */
router.post('/logout', validate(logoutSchema), authController.logout);

/**
 * @route   GET /api/v1/auth/session
 */
router.get('/session', authMiddleware, authController.session);

module.exports = router;
