const { z } = require('zod');

const createTemplateSchema = z.object({
    name: z.string().min(1, 'Name is required').max(100, 'Name is too long'),
    subject: z.string().min(1, 'Subject is required').max(255, 'Subject is too long'),
    bodyText: z.string().min(1, 'Body text is required'),
    bodyHtml: z.string().optional()
});

const updateTemplateSchema = z.object({
    name: z.string().min(1).max(100).optional(),
    subject: z.string().min(1).max(255).optional(),
    bodyText: z.string().min(1).optional(),
    bodyHtml: z.string().optional()
});

const templateQuerySchema = z.object({
    page: z.string().optional().transform(val => val ? parseInt(val) : 1),
    limit: z.string().optional().transform(val => val ? parseInt(val) : 20),
    search: z.string().optional(),
    sortBy: z.enum(['name', 'subject', 'createdAt', 'updatedAt']).default('updatedAt'),
    sortOrder: z.enum(['asc', 'desc']).default('desc')
});

const templateIdParamSchema = z.object({
    id: z.string().uuid('Invalid template ID')
});

module.exports = {
    createTemplateSchema,
    updateTemplateSchema,
    templateQuerySchema,
    templateIdParamSchema
};
