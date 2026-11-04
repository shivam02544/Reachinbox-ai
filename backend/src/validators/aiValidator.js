const { z } = require('zod');

const testGenerateSchema = z.object({
    body: z.object({
        prompt: z.string({
            required_error: "Prompt is required",
            invalid_type_error: "Prompt must be a string",
        }).min(1, "Prompt cannot be empty").max(1000, "Prompt exceeds 1000 characters maximum")
    })
});

const improveEmailSchema = z.object({
    body: z.object({
        subject: z.string().max(200).optional().default(''),
        body: z.string().min(1, 'Email body is required').max(12000),
        tone: z.enum(['professional', 'friendly', 'concise', 'formal']).optional().default('professional')
    })
});

/**
 * Validates request payload against the provided Zod schema
 */
const validate = (schema) => (req, res, next) => {
    try {
        schema.parse({
            body: req.body,
            query: req.query,
            params: req.params,
        });
        next();
    } catch (error) {
        if (error instanceof z.ZodError) {
            return res.status(400).json({
                success: false,
                message: "Validation failed",
                errors: error.errors.map(err => ({
                    field: err.path.join('.'),
                    message: err.message
                }))
            });
        }
        next(error);
    }
};

module.exports = {
    testGenerateSchema,
    improveEmailSchema,
    validate
};
