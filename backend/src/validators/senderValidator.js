const { z } = require('zod');

const createSenderSchema = z.object({
    displayName: z.string().min(1, 'Display name is required'),
    email: z.string().email('Invalid email address'),
    provider: z.enum(['SMTP']).default('SMTP'),
    smtpHost: z.string().min(1, 'SMTP host is required'),
    smtpPort: z.number().int().positive(),
    smtpUsername: z.string().min(1, 'SMTP username is required'),
    smtpPassword: z.string().min(1, 'SMTP password is required')
});

const updateSenderSchema = z.object({
    displayName: z.string().min(1).optional()
});

const senderIdParamSchema = z.object({
    id: z.string().uuid('Invalid sender ID')
});

module.exports = {
    createSenderSchema,
    updateSenderSchema,
    senderIdParamSchema
};
