const jobUrlFetcher = require('./jobUrlFetcher');
const jobRequirementService = require('./jobRequirementService');
const matchService = require('./matchService');
const outreachGenerationService = require('./outreachGenerationService');

const nextWeekdayAtUtcHour = (hour = 14) => {
    const date = new Date();
    date.setUTCDate(date.getUTCDate() + 1);
    date.setUTCHours(hour, 0, 0, 0);

    while (date.getUTCDay() === 0 || date.getUTCDay() === 6) {
        date.setUTCDate(date.getUTCDate() + 1);
    }

    return date.toISOString();
};

class JobUrlPipelineService {
    async run(userId, { url, recipient, instructions, tone }) {
        const pageText = await jobUrlFetcher.fetchJobPageText(url);

        const jobRequirement = await jobRequirementService.createJobRequirement(userId, {
            description: pageText,
            additionalNotes: `Imported from ${url}`
        });

        const match = await matchService.generateMatch(userId, jobRequirement.id);

        let outreach = null;
        if (recipient && recipient.email) {
            outreach = await outreachGenerationService.generateOutreach(userId, {
                jobRequirementId: jobRequirement.id,
                recipient,
                instructions,
                tone: tone || 'professional'
            });
        }

        return {
            sourceUrl: url,
            jobRequirement,
            match,
            outreach,
            suggestedSendAt: nextWeekdayAtUtcHour(14)
        };
    }
}

module.exports = new JobUrlPipelineService();
