const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const healthRoutes = require('./routes/healthRoute');
const authRoutes = require('./routes/authRoute');
const dashboardRoutes = require('./routes/dashboardRoute');
const senderRoutes = require('./routes/senderRoute');
const emailRoutes = require('./routes/emailRoute');
const scheduledEmailRoutes = require('./routes/scheduledEmailRoute');
const activityRoutes = require('./routes/activityRoute');
const campaignRoutes = require('./routes/campaignRoute');
const templateRoutes = require('./routes/templateRoute');
const aiRoutes = require('./routes/aiRoute');
const inboundRoutes = require('./routes/inboundRoute');
const { errorHandler, notFoundHandler } = require('./middleware/errorMiddleware');

const app = express();

// Security middleware
app.use(helmet());

// CORS configuration
const allowedOrigins = process.env.ALLOWED_ORIGINS ? process.env.ALLOWED_ORIGINS.split(',') : [];
app.use(cors({
    origin: (origin, callback) => {
        // Allow requests with no origin (like mobile apps or curl requests)
        if (!origin) return callback(null, true);
        if (process.env.NODE_ENV === 'development' || allowedOrigins.indexOf(origin) !== -1) {
            callback(null, true);
        } else {
            callback(new Error('Not allowed by CORS'));
        }
    },
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'],
    credentials: true
}));

// Request logging
if (process.env.NODE_ENV !== 'test') {
    app.use(morgan('dev'));
}

// JSON body parsing
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// API Routes
app.use('/api/v1/health', healthRoutes);
app.use('/api/v1/auth', authRoutes);
app.use('/api/v1/dashboard', dashboardRoutes);
app.use('/api/v1/senders', senderRoutes);
app.use('/api/v1/emails', emailRoutes);
app.use('/api/v1/scheduled-emails', scheduledEmailRoutes);
app.use('/api/v1/activity', activityRoutes);
app.use('/api/v1/campaigns', campaignRoutes);
app.use('/api/v1/templates', templateRoutes);
app.use('/api/v1/ai', aiRoutes);
app.use('/api/v1/inbound', inboundRoutes);

// 404 handler
app.use(notFoundHandler);

// Centralized error handling
app.use(errorHandler);

module.exports = app;
