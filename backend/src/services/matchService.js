const prisma = require('../utils/prisma');
const geminiClient = require('../ai/geminiClient');

class MatchService {
    /**
     * Generates a match analysis between a user's resume and a job requirement
     */
    async generateMatch(userId, jobRequirementId) {
        // 1. Fetch Resume
        const resume = await prisma.resume.findUnique({
            where: { userId }
        });

        if (!resume || !resume.structuredData) {
            const error = new Error('No analyzed resume found. Please upload a resume first.');
            error.statusCode = 404;
            throw error;
        }

        // 2. Fetch Job Requirement
        const jobRequirement = await prisma.jobRequirement.findFirst({
            where: { id: jobRequirementId, userId }
        });

        if (!jobRequirement || !jobRequirement.structuredData) {
            const error = new Error('Job requirement not found or not analyzed.');
            error.statusCode = 404;
            throw error;
        }

        // 3. Analyze Match with Gemini
        let matchResult = null;
        try {
            matchResult = await this._analyzeMatch(resume.structuredData, jobRequirement.structuredData);
        } catch (err) {
            throw err;
        }

        // 4. Save MatchAnalysis
        const existingMatch = await prisma.matchAnalysis.findFirst({
            where: {
                userId,
                resumeId: resume.id,
                jobRequirementId: jobRequirement.id
            }
        });

        if (existingMatch) {
            return await prisma.matchAnalysis.update({
                where: { id: existingMatch.id },
                data: { result: matchResult }
            });
        } else {
            return await prisma.matchAnalysis.create({
                data: {
                    userId,
                    resumeId: resume.id,
                    jobRequirementId: jobRequirement.id,
                    result: matchResult
                }
            });
        }
    }

    /**
     * Retrieves an existing match
     */
    async getMatch(userId, jobRequirementId) {
        return await prisma.matchAnalysis.findFirst({
            where: { userId, jobRequirementId },
            include: {
                jobRequirement: true // Optionally include job details
            }
        });
    }

    /**
     * Calls Gemini to perform the match analysis
     */
    async _analyzeMatch(resumeData, jobData) {
        const prompt = `
You are an expert recruiter matching a candidate's resume to a job requirement.
Compare the Candidate Resume with the Job Requirement.

Provide an objective analysis. Do NOT make hiring decisions.
Answer: "How can this user's existing experience be connected truthfully to this job requirement?"

Output ONLY valid JSON matching this exact structure:
{
  "matchingSkills": ["skills present in both"],
  "missingSkills": ["required skills NOT found in resume"],
  "relevantExperience": ["resume experience items relevant to the job"],
  "relevantProjects": ["resume projects relevant to the job"],
  "matchingQualifications": ["qualifications met"],
  "potentialGaps": ["requirements that cannot be verified from the resume"],
  "personalizationPoints": ["Specific truthful points connecting the candidate to the role for an outreach email"]
}

If any category has no items, return an empty array [].
Do NOT hallucinate or invent experience, skills, or projects.

Candidate Resume Data:
${JSON.stringify(resumeData)}

Job Requirement Data:
${JSON.stringify(jobData)}
`;
        const rawResponse = await geminiClient.generateContent(prompt);
        let jsonStr = rawResponse.replace(/```json/g, '').replace(/```/g, '').trim();

        try {
            return JSON.parse(jsonStr);
        } catch (err) {
            console.error('Failed to parse Gemini Match output:', jsonStr);
            const error = new Error('AI returned an invalid structured match response.');
            error.statusCode = 500;
            throw error;
        }
    }
}

module.exports = new MatchService();
