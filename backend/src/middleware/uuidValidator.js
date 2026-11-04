const { z } = require('zod');

const uuidSchema = z.string().uuid('Invalid ID format');

/**
 * Middleware to validate UUID parameters in the URL
 * @param {string[]} params Array of parameter names to validate
 */
const validateUuids = (params = ['id']) => (req, res, next) => {
    try {
        for (const param of params) {
            if (req.params[param]) {
                uuidSchema.parse(req.params[param]);
            }
        }
        next();
    } catch (error) {
        const validationError = new Error(error.errors[0].message);
        validationError.statusCode = 400;
        validationError.code = 'INVALID_ID';
        next(validationError);
    }
};

module.exports = validateUuids;
