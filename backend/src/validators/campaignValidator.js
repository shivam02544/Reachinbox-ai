const { z } = require('zod');

const createCampaignSchema = z.object({
    name: z.string().min(1, 'Campaign name is required').max(100),
    senderAccountId: z.string().uuid('Invalid sender account ID'),
    subject: z.string().min(1, 'Subject is required').max(255),
    bodyHtml: z.string().optional(),
    bodyText: z.string().optional()
}).refine(data => data.bodyHtml || data.bodyText, {
    message: "Either HTML or text body must be provided",
    path: ["bodyText"]
});

const updateCampaignSchema = z.object({
    name: z.string().min(1).max(100).optional(),
    senderAccountId: z.string().uuid().optional(),
    subject: z.string().min(1).max(255).optional(),
    bodyHtml: z.string().optional(),
    bodyText: z.string().optional()
});

const addRecipientsSchema = z.object({
    recipients: z.array(z.string().email('Invalid email address')).min(1, 'At least one recipient is required')
});

const campaignIdParamSchema = z.object({
    id: z.string().uuid('Invalid campaign ID')
});

const campaignRecipientParamsSchema = z.object({
    campaignId: z.string().uuid('Invalid campaign ID'),
    recipientId: z.string().uuid('Invalid recipient ID')
});

module.exports = {
    createCampaignSchema,
    updateCampaignSchema,
    addRecipientsSchema,
    campaignIdParamSchema,
    campaignRecipientParamsSchema
};
