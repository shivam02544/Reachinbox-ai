const aiService = require('../services/aiService');

/**
 * Controller for AI endpoints.
 * Phase 1 includes basic health and test generation logic.
 */

exports.getHealth = async (req, res, next) => {
    try {
        // Return a quick response indicating the AI router is mounted.
        // We do a full connection check optionally, but for a basic health check,
        // we can just verify if the service is loaded and GEMINI_API_KEY is present.
        const apiKeyPresent = !!process.env.GEMINI_API_KEY;

        res.status(200).json({
            success: true,
            data: {
                status: apiKeyPresent ? 'configured' : 'unconfigured',
                model: process.env.GEMINI_MODEL || 'gemini-1.5-pro'
            }
        });
    } catch (error) {
        next(error);
    }
};

exports.testGenerate = async (req, res, next) => {
    try {
        const { prompt } = req.body;

        const result = await aiService.testGenerate(prompt);

        res.status(200).json({
            success: true,
            data: {
                result
            }
        });
    } catch (error) {
        next(error);
    }
};

exports.improveEmail = async (req, res, next) => {
    try {
        const suggestion = await aiService.improveEmail(req.body);
        res.status(200).json({ success: true, data: suggestion });
    } catch (error) {
        next(error);
    }
};
