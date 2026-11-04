const { z } = require('zod');

const scheduleEmailSchema = z.object({
    senderAccountId: z.string().uuid('Invalid sender ID'),
    recipient: z.string().email('Invalid recipient email address'),
    subject: z.string().min(1, 'Subject is required'),
    htmlBody: z.string().optional(),
    textBody: z.string().optional(),
    scheduledAt: z.string().datetime()
}).refine(data => data.htmlBody || data.textBody, {
    message: "Either HTML or text body must be provided",
    path: ["textBody"]
});

const scheduledEmailIdParamSchema = z.object({
    id: z.string().uuid('Invalid scheduled email ID')
});

module.exports = {
    scheduleEmailSchema,
    scheduledEmailIdParamSchema
};
