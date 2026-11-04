const { z } = require('zod');

const generateOutreachSchema = z.object({
    body: z.object({
        jobRequirementId: z.string({
            required_error: "Job Requirement ID is required"
        }).uuid("Invalid Job Requirement ID"),
        recipient: z.object({
            name: z.string().max(100).optional(),
            email: z.string().email("Invalid recipient email"),
            company: z.string().max(100).optional(),
            role: z.string().max(100).optional()
        }),
        instructions: z.string().max(500).optional(),
        tone: z.enum(['professional', 'concise', 'friendly_professional', 'formal']).default('professional')
    })
});

module.exports = {
    generateOutreachSchema
};
