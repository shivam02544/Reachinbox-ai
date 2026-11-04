const { z } = require('zod');

const generateFollowUpSchema = z.object({
    body: z.object({
        parentEmailId: z.string({
            required_error: "Parent Email ID is required"
        }).uuid("Invalid Parent Email ID"),
        instructions: z.string().max(500).optional(),
        followUpNumber: z.number().int().min(1).max(2).default(1)
    })
});

module.exports = {
    generateFollowUpSchema
};
