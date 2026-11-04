const jobRequirementService = require('../services/jobRequirementService');
const jobUrlPipelineService = require('../services/jobUrlPipelineService');

exports.createJobRequirement = async (req, res, next) => {
    try {
        const job = await jobRequirementService.createJobRequirement(req.user.id, req.body);

        res.status(201).json({
            success: true,
            data: job
        });
    } catch (error) {
        next(error);
    }
};

exports.getAllJobRequirements = async (req, res, next) => {
    try {
        const jobs = await jobRequirementService.getAllJobRequirements(req.user.id);

        res.status(200).json({
            success: true,
            data: jobs
        });
    } catch (error) {
        next(error);
    }
};

exports.getJobRequirement = async (req, res, next) => {
    try {
        const job = await jobRequirementService.getJobRequirement(req.user.id, req.params.id);

        if (!job) {
            return res.status(404).json({
                success: false,
                message: 'Job requirement not found'
            });
        }

        res.status(200).json({
            success: true,
            data: job
        });
    } catch (error) {
        next(error);
    }
};

exports.deleteJobRequirement = async (req, res, next) => {
    try {
        await jobRequirementService.deleteJobRequirement(req.user.id, req.params.id);

        res.status(200).json({
            success: true,
            message: 'Job requirement deleted successfully'
        });
    } catch (error) {
        next(error);
    }
};

exports.createFromUrl = async (req, res, next) => {
    try {
        const result = await jobUrlPipelineService.run(req.user.id, req.body);

        res.status(201).json({
            success: true,
            data: result
        });
    } catch (error) {
        next(error);
    }
};
