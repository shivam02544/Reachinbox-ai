const geminiClient = require('../ai/geminiClient');
const { z } = require('zod');

class AIService {
    /**
     * Checks if the Gemini AI service is properly configured and reachable.
     * @returns {Promise<boolean>}
     */
    async checkHealth() {
        try {
            // A minimal, fast prompt to verify the connection
            const result = await geminiClient.generateContent('Return the exact text "OK" if you are receiving this message.');
            return result && result.includes('OK');
        } catch (error) {
            console.error('AI Health Check failed:', error);
            return false;
        }
    }

    /**
     * Test generation method for Phase 1.
     * @param {string} prompt
     * @returns {Promise<string>}
     */
    async testGenerate(prompt) {
        if (!prompt || prompt.trim() === '') {
            const error = new Error('Prompt cannot be empty');
            error.statusCode = 400;
            throw error;
        }

        // Limit the prompt size for this test endpoint
        if (prompt.length > 1000) {
            const error = new Error('Prompt exceeds maximum allowed length');
            error.statusCode = 400;
            throw error;
        }

        console.log(`[AI Service] Starting generation for prompt (length: ${prompt.length})`);
        const startTime = Date.now();

        try {
            const result = await geminiClient.generateContent(prompt);

            const duration = Date.now() - startTime;
            console.log(`[AI Service] Generation completed successfully in ${duration}ms`);

            return result;
        } catch (error) {
            const duration = Date.now() - startTime;
            console.error(`[AI Service] Generation failed after ${duration}ms:`, error.message);
            throw error;
        }
    }

    async improveEmail({ subject, body, tone }) {
        const toneDescriptions = {
            professional: 'polished and professional',
            friendly: 'warm and friendly while remaining professional',
            concise: 'brief and direct',
            formal: 'formal and respectful'
        };
        const prompt = `Rewrite the email below to be clear, natural, and ${toneDescriptions[tone] || toneDescriptions.professional}. Preserve its intent and all factual details. Do not add claims, promises, or facts. Return only valid JSON with string fields "subject" and "body". If the subject is blank, create a concise appropriate subject.\n\nSubject: ${subject || '(none)'}\n\nEmail body:\n${body}`;
        const response = await geminiClient.generateContent(prompt);
        const cleaned = response.replace(/^```(?:json)?\s*/i, '').replace(/\s*```$/, '').trim();
        let parsed;
        try {
            parsed = JSON.parse(cleaned);
        } catch {
            const error = new Error('AI returned an invalid email suggestion. Please try again.');
            error.statusCode = 502;
            throw error;
        }
        const result = z.object({ subject: z.string().min(1).max(200), body: z.string().min(1).max(12000) }).safeParse(parsed);
        if (!result.success) {
            const error = new Error('AI returned an incomplete email suggestion. Please try again.');
            error.statusCode = 502;
            throw error;
        }
        return result.data;
    }
}

module.exports = new AIService();
