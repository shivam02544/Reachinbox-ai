const { GoogleGenerativeAI } = require('@google/generative-ai');

/**
 * Singleton Gemini client wrapper
 */
class GeminiClient {
    constructor() {
        this.genAI = null;
        this.modelName = process.env.GEMINI_MODEL || 'gemini-1.5-pro';
        this.isInitialized = false;
    }

    /**
     * Initializes the GoogleGenerativeAI client.
     */
    init() {
        const apiKey = process.env.GEMINI_API_KEY;
        if (!apiKey) {
            console.warn('GEMINI_API_KEY is not defined. AI features will fail.');
            return false;
        }

        try {
            this.genAI = new GoogleGenerativeAI(apiKey);
            this.isInitialized = true;
            return true;
        } catch (error) {
            console.error('Failed to initialize Gemini Client:', error);
            return false;
        }
    }

    /**
     * Gets an instance of the configured generative model.
     * @returns {import('@google/generative-ai').GenerativeModel}
     */
    getModel() {
        if (!this.isInitialized || !this.genAI) {
            if (!this.init()) {
                throw new Error('Gemini API is not configured or failed to initialize.');
            }
        }
        return this.genAI.getGenerativeModel({ model: this.modelName });
    }

    /**
     * Helper to call generateContent with common error handling.
     * @param {string} prompt
     * @returns {Promise<string>}
     */
    async generateContent(prompt) {
        try {
            const model = this.getModel();

            // Generate content
            const result = await model.generateContent(prompt);
            const response = await result.response;
            const text = response.text();

            return text;
        } catch (error) {
            this._handleError(error);
        }
    }

    /**
     * Centralized error handler to normalize Gemini errors
     */
    _handleError(error) {
        console.error('Gemini API Error:', error.message || error);

        // Timeout, Service Unavailable (503), or Network issues
        if (error.status === 503 || (error.message && (error.message.includes('fetch') || error.message.includes('timeout') || error.message.includes('503')))) {
            const normalizedError = new Error('AI service is currently experiencing high demand and is temporarily unavailable. Please try again in a few moments.');
            normalizedError.statusCode = 503;
            normalizedError.code = 'GEMINI_TIMEOUT';
            throw normalizedError;
        }

        // Rate Limits
        if (error.status === 429 || (error.message && error.message.includes('429'))) {
            const normalizedError = new Error('AI service rate limit exceeded. Please try again later.');
            normalizedError.statusCode = 429;
            normalizedError.code = 'GEMINI_RATE_LIMIT';
            throw normalizedError;
        }

        // Invalid API Key / Auth
        if (error.status === 401 || error.status === 403 || (error.message && (error.message.includes('401') || error.message.includes('403')))) {
            const normalizedError = new Error('AI service configuration error.');
            normalizedError.statusCode = 500;
            normalizedError.code = 'GEMINI_AUTH_ERROR';
            throw normalizedError;
        }

        // Default error
        const normalizedError = new Error('AI service encountered an unexpected error.');
        normalizedError.statusCode = 500;
        normalizedError.code = 'GEMINI_ERROR';
        throw normalizedError;
    }
}

// Export a singleton instance
module.exports = new GeminiClient();
