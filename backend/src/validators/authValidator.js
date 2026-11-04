const { z } = require('zod');

const registerSchema = z.object({
    name: z.string().min(1, 'Name is required'),
    email: z.string().email('Invalid email address').transform(e => e.toLowerCase()),
    password: z.string().min(8, 'Password must be at least 8 characters')
});

const loginSchema = z.object({
    email: z.string().email('Invalid email address').transform(e => e.toLowerCase()),
    password: z.string().min(1, 'Password is required')
});

const refreshSchema = z.object({
    refreshToken: z.string().min(1, 'Refresh token is required')
});

const logoutSchema = z.object({
    refreshToken: z.string().min(1, 'Refresh token is required')
});

const validate = (schema, source = 'body') => (req, res, next) => {
    try {
        req[source] = schema.parse(req[source]);
        next();
    } catch (error) {
        const validationError = new Error(error.errors[0].message);
        validationError.statusCode = 400;
        validationError.code = 'VALIDATION_ERROR';
        next(validationError);
    }
};

module.exports = {
    validate,
    registerSchema,
    loginSchema,
    refreshSchema,
    logoutSchema
};
