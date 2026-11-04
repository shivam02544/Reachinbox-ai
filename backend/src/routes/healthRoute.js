const express = require('express');
const router = express.Router();
const prisma = require('../utils/prisma');
const { redisConnection } = require('../config/redis');

/**
 * @route   GET /api/v1/health
 * @desc    Health check endpoint (API + Database + Redis)
 * @access  Public
 */
router.get('/', async (req, res) => {
    let dbStatus = 'unhealthy';
    let redisStatus = 'unhealthy';
    let statusCode = 200;

    try {
        // Simple query to check DB connectivity
        await prisma.$queryRaw`SELECT 1`;
        dbStatus = 'healthy';
    } catch (error) {
        console.error('Database health check failed:', error.message);
        dbStatus = 'unreachable';
        statusCode = 503;
    }

    try {
        const pong = await redisConnection.ping();
        if (pong === 'PONG') redisStatus = 'healthy';
    } catch (error) {
        console.error('Redis health check failed:', error.message);
        redisStatus = 'unreachable';
        statusCode = 503;
    }

    res.status(statusCode).json({
        success: dbStatus === 'healthy' && redisStatus === 'healthy',
        message: (dbStatus === 'healthy' && redisStatus === 'healthy')
            ? 'ReachInbox API is fully operational'
            : 'ReachInbox API is partially degraded',
        timestamp: new Date().toISOString(),
        env: process.env.NODE_ENV || 'development',
        services: {
            api: 'healthy',
            database: dbStatus,
            redis: redisStatus
        }
    });
});

module.exports = router;
