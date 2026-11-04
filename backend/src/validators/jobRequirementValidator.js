const { z } = require('zod');

const jobRequirementSchema = z.object({
    body: z.object({
        title: z.string().max(200, "Title is too long").optional(),
        company: z.string().max(200, "Company name is too long").optional(),
        description: z.string({
            required_error: "Job description is required"
        }).min(20, "Job description is too short").max(15000, "Job description exceeds maximum length"),
        location: z.string().max(200).optional(),
        employmentType: z.string().max(100).optional(),
        additionalNotes: z.string().max(1000).optional()
    })
});

const matchSchema = z.object({
    body: z.object({
        jobRequirementId: z.string({
            required_error: "Job Requirement ID is required"
        }).uuid("Invalid Job Requirement ID")
    })
});

const jobFromUrlSchema = z.object({
    body: z.object({
        url: z.string({
            required_error: "Job URL is required"
        }).url("Invalid job URL").max(2048),
        recipient: z.object({
            name: z.string().max(100).optional(),
            email: z.string().email("Invalid recipient email"),
            company: z.string().max(100).optional(),
            role: z.string().max(100).optional()
        }).optional(),
        instructions: z.string().max(500).optional(),
        tone: z.enum(['professional', 'concise', 'friendly_professional', 'formal']).optional()
    })
});

module.exports = {
    jobRequirementSchema,
    matchSchema,
    jobFromUrlSchema
};
