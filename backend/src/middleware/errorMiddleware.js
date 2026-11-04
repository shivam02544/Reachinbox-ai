/**
 * Centralized error handler
 */
const errorHandler = (err, req, res, next) => {
    console.error(err.stack);

    const statusCode = err.statusCode || 500;
    const message = err.message || 'Internal Server Error';
    const errorCode = err.code || 'INTERNAL_ERROR';

    const response = {
        success: false,
        message: statusCode === 500 && process.env.NODE_ENV === 'production'
            ? 'Something went wrong on our end'
            : message,
        error: {
            code: errorCode
        }
    };

    // Include stack trace only in development
    if (process.env.NODE_ENV === 'development') {
        response.error.stack = err.stack;
    }

    res.status(statusCode).json(response);
};

/**
 * 404 Not Found handler
 */
const notFoundHandler = (req, res, next) => {
    const err = new Error(`Route not found - ${req.originalUrl}`);
    err.statusCode = 404;
    err.code = 'NOT_FOUND';
    next(err);
};

module.exports = {
    errorHandler,
    notFoundHandler
};
