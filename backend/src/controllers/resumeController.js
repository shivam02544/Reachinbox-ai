const resumeService = require('../services/resumeService');

exports.uploadResume = async (req, res, next) => {
    try {
        if (!req.file) {
            const error = new Error('No file uploaded');
            error.statusCode = 400;
            throw error;
        }

        const resume = await resumeService.processResume(req.user.id, req.file);

        // Omit the large parsedText from the response to save bandwidth
        const { parsedText, ...responseData } = resume;

        res.status(200).json({
            success: true,
            data: responseData
        });
    } catch (error) {
        next(error);
    }
};

exports.getResume = async (req, res, next) => {
    try {
        const resume = await resumeService.getResume(req.user.id);

        if (!resume) {
            return res.status(200).json({
                success: true,
                data: null
            });
        }

        // Omit parsedText from response
        const { parsedText, ...responseData } = resume;

        res.status(200).json({
            success: true,
            data: responseData
        });
    } catch (error) {
        next(error);
    }
};

exports.deleteResume = async (req, res, next) => {
    try {
        await resumeService.deleteResume(req.user.id);

        res.status(200).json({
            success: true,
            message: 'Resume deleted successfully'
        });
    } catch (error) {
        next(error);
    }
};
