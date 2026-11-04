afterAll(async () => {
    const queueServicePath = require.resolve('../services/queueService');
    const redisPath = require.resolve('../config/redis');

    if (require.cache[queueServicePath]) {
        await require(queueServicePath).closeQueues();
    }

    if (require.cache[redisPath]) {
        await require(redisPath).redisConnection.quit();
    }
});