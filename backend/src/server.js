require('dotenv').config();
const app = require('./app');
const prisma = require('./utils/prisma');

const PORT = process.env.PORT || 3000;

const server = app.listen(PORT, '0.0.0.0', () => {
    console.log(`ReachInbox API is running on port ${PORT} in ${process.env.NODE_ENV || 'development'} mode`);
    console.log(`Server is accessible at http://0.0.0.0:${PORT}`);
});

// Graceful shutdown
const shutdown = async () => {
    console.log('Shutting down server...');
    server.close(async () => {
        console.log('HTTP server closed.');
        await prisma.$disconnect();
        console.log('Database connection closed.');
        process.exit(0);
    });
};

process.on('SIGTERM', shutdown);
process.on('SIGINT', shutdown);

// Handle unhandled rejections
process.on('unhandledRejection', (err) => {
    console.error('Unhandled Rejection:', err);
    // Graceful shutdown on unhandled rejection
    // shutdown();
});

// Handle uncaught exceptions
process.on('uncaughtException', (err) => {
    console.error('Uncaught Exception:', err);
    process.exit(1);
});
