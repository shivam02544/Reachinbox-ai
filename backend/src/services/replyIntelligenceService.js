const prisma = require('../utils/prisma');
const geminiClient = require('../ai/geminiClient');

class ReplyIntelligenceService {
    /**
     * Classifies inbound email reply and generates a suggested response
     */
    async analyzeReply(userId, inboundEmailId) {
        // 1. Fetch Inbound Email
        const inboundEmail = await prisma.inboundEmail.findUnique({
            where: { id: inboundEmailId },
            include: {
                senderAccount: true,
                matchedEmail: true
            }
        });

        if (!inboundEmail || inboundEmail.senderAccount.userId !== userId) {
            const error = new Error('Inbound email not found or access denied.');
            error.statusCode = 404;
            throw error;
        }

        // Return cached analysis if already present
        if (inboundEmail.analysis) {
            return inboundEmail.analysis;
        }

        // 2. Gather Context
        let originalEmailContext = 'No original email matched.';
        if (inboundEmail.matchedEmail) {
            originalEmailContext = `
Original Subject: ${inboundEmail.matchedEmail.subject}
Original Body: ${inboundEmail.matchedEmail.textBody || inboundEmail.matchedEmail.htmlBody || ''}
`;
        }

        const prompt = `
You are an AI assistant analyzing an inbound email reply to an outreach message.

Follow these rules STRICTLY:
1. Classify the reply into ONE of: "INTERESTED", "NOT_INTERESTED", "REQUEST_FOR_INFORMATION", "NEEDS_FOLLOW_UP", "INTERVIEW_RELATED", "OUT_OF_OFFICE", "UNCLEAR".
2. Provide a 1-2 sentence summary of the reply.
3. Suggest a concise, professional response.
4. Do NOT invent promises or claim facts not supported by the email context.

${originalEmailContext}

Inbound Email Reply from ${inboundEmail.fromEmail}:
Subject: ${inboundEmail.subject || ''}
Body:
${inboundEmail.textBody || inboundEmail.htmlBody || ''}

Output ONLY valid JSON matching this exact structure:
{
  "classification": "INTERESTED",
  "summary": "Brief 1-2 sentence summary",
  "suggestedAction": "Recommended next step",
  "confidence": "HIGH",
  "suggestedResponse": {
    "subject": "Re: Subject",
    "bodyText": "Suggested response text"
  }
}
`;

        // 3. Call Gemini
        const rawResponse = await geminiClient.generateContent(prompt);
        let jsonStr = rawResponse.replace(/```json/g, '').replace(/```/g, '').trim();

        let analysisResult = null;
        try {
            analysisResult = JSON.parse(jsonStr);
        } catch (err) {
            console.error('Failed to parse Gemini Reply Intelligence JSON:', jsonStr);
            const error = new Error('AI returned an invalid structured reply analysis.');
            error.statusCode = 500;
            throw error;
        }

        // 4. Save analysis to DB
        await prisma.inboundEmail.update({
            where: { id: inboundEmailId },
            data: { analysis: analysisResult }
        });

        return analysisResult;
    }
}

module.exports = new ReplyIntelligenceService();
