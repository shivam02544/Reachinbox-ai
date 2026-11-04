const IORedis = require('ioredis');

const redisConfig = {
    host: process.env.REDIS_HOST || 'localhost',
    port: process.env.REDIS_PORT || 6379,
    password: process.env.REDIS_PASSWORD || undefined,
    maxRetriesPerRequest: null, // Required by BullMQ
    enableOfflineQueue: false,
    retryStrategy: (times) => Math.min(times * 200, 5000),
};

const redisConnection = new IORedis(redisConfig);

redisConnection.on('error', (err) => {
    console.error('Redis connection error:', err.message);
});

module.exports = {
    redisConfig,
    redisConnection
};
