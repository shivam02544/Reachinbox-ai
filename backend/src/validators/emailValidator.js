const { z } = require('zod');

const sendEmailSchema = z.object({
    senderAccountId: z.string().uuid('Invalid sender ID'),
    recipient: z.string().email('Invalid recipient email address'),
    subject: z.string().min(1, 'Subject is required'),
    htmlBody: z.string().optional(),
    textBody: z.string().optional(),
    idempotencyKey: z.string().uuid('Invalid idempotency key'),
    parentEmailId: z.string().uuid().optional(),
    followUpNumber: z.number().int().min(1).optional()
}).refine(data => data.htmlBody || data.textBody, {
    message: "Either HTML or text body must be provided",
    path: ["textBody"]
});

const draftSchema = z.object({
    senderAccountId: z.string().uuid('Invalid sender ID'),
    recipient: z.string().email('Invalid recipient email address').optional().or(z.literal('')),
    subject: z.string().optional(),
    htmlBody: z.string().optional(),
    textBody: z.string().optional(),
    parentEmailId: z.string().uuid().optional(),
    followUpNumber: z.number().int().min(1).optional()
});

const updateDraftSchema = z.object({
    senderAccountId: z.string().uuid('Invalid sender ID').optional(),
    recipient: z.string().email('Invalid recipient email address').optional().or(z.literal('')),
    subject: z.string().optional(),
    htmlBody: z.string().optional(),
    textBody: z.string().optional(),
    parentEmailId: z.string().uuid().optional(),
    followUpNumber: z.number().int().min(1).optional()
});

const getEmailsQuerySchema = z.object({
    page: z.string().optional().transform(v => v ? Math.max(1, parseInt(v)) : 1),
    limit: z.string().optional().transform(v => v ? Math.min(100, Math.max(1, parseInt(v))) : 20),
    search: z.string().optional(),
    status: z.enum(['DRAFT', 'PROCESSING', 'SCHEDULED', 'SENT', 'FAILED', 'CANCELLED']).optional(),
    senderAccountId: z.string().uuid().optional(),
    from: z.string().datetime().optional(),
    to: z.string().datetime().optional(),
    sortBy: z.enum(['createdAt', 'updatedAt', 'subject', 'recipient']).default('createdAt'),
    sortOrder: z.enum(['asc', 'desc']).default('desc')
});

const emailIdParamSchema = z.object({
    id: z.string().uuid('Invalid email ID')
});

module.exports = {
    sendEmailSchema,
    draftSchema,
    updateDraftSchema,
    getEmailsQuerySchema,
    emailIdParamSchema
};
