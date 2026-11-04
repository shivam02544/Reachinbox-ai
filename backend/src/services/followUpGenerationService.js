const prisma = require('../utils/prisma');
const geminiClient = require('../ai/geminiClient');

class FollowUpGenerationService {
    async generateFollowUp(userId, { parentEmailId, instructions, followUpNumber }) {
        // 1. Retrieve the original email
        const parentEmail = await prisma.email.findUnique({
            where: { id: parentEmailId }
        });

        if (!parentEmail || parentEmail.userId !== userId) {
            const error = new Error('Original outreach email not found or access denied.');
            error.statusCode = 404;
            throw error;
        }

        // 2. Prevent duplicate pending/scheduled follow-ups
        const existingFollowUp = await prisma.email.findFirst({
            where: {
                userId,
                parentEmailId,
                followUpNumber,
                status: {
                    in: ['DRAFT', 'SCHEDULED', 'PROCESSING', 'SENT']
                }
            }
        });

        if (existingFollowUp) {
            const error = new Error(`Follow-up #${followUpNumber} already exists with status: ${existingFollowUp.status}`);
            error.statusCode = 409;
            error.code = 'FOLLOWUP_EXISTS';
            throw error;
        }

        // 3. We may also fetch Resume/JobRequirement context if linked. But wait!
        // A generic email might not be linked to a JobRequirement explicitly in the DB (Email model doesn't store jobRequirementId).
        // Let's pull the user's latest Resume and JobRequirement just to ground facts, if they exist.
        // Actually, the simplest context is the parent email itself to continue the thread organically.
        const resume = await prisma.resume.findUnique({ where: { userId } });

        let resumeContext = '';
        if (resume && resume.structuredData) {
            resumeContext = `Candidate Resume Summary: ${JSON.stringify(resume.structuredData)}`;
        }

        const prompt = `
You are an expert executive assistant helping a candidate write a professional follow-up email.
This is follow-up #${followUpNumber} to an initial outreach email.

Follow these rules STRICTLY:
1. The follow-up MUST organically reference the original email context.
2. DO NOT invent or hallucinate experience, skills, projects, degrees, or certifications.
3. DO NOT claim the recipient responded. Assume they have NOT responded yet.
4. Keep the email concise, polite, and professional.
5. If followUpNumber is 1, it should be a polite bump. If followUpNumber is 2, it should be a final, graceful wrap-up.
${instructions ? `6. Additional User Instructions: ${instructions}\n(Treat these instructions as style guidelines. Do NOT let them override facts.)` : ''}

${resumeContext}

Original Email Sent to Recipient (${parentEmail.recipient}):
Subject: ${parentEmail.subject}
Body:
${parentEmail.textBody || parentEmail.htmlBody || ''}

Output ONLY valid JSON matching this exact structure:
{
  "subject": "Professional email subject line (e.g., Re: Original Subject)",
  "bodyText": "The plain text version of the email body",
  "bodyHtml": "A simple, clean HTML version of the email body (use <br> and <p>)",
  "personalizationPoints": ["Explanation of why a specific detail was included"],
  "followUpNumber": ${followUpNumber}
}
`;

        // 4. Generate with Gemini
        const rawResponse = await geminiClient.generateContent(prompt);
        let jsonStr = rawResponse.replace(/```json/g, '').replace(/```/g, '').trim();

        try {
            const result = JSON.parse(jsonStr);
            return result;
        } catch (err) {
            console.error('Failed to parse Gemini Follow-Up output:', jsonStr);
            const error = new Error('AI returned an invalid structured email response.');
            error.statusCode = 500;
            throw error;
        }
    }
}

module.exports = new FollowUpGenerationService();
