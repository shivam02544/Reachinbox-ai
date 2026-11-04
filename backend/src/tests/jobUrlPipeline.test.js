const request = require('supertest');
const app = require('../app');
const { generateAccessToken } = require('../utils/jwt');
const geminiClient = require('../ai/geminiClient');
const prisma = require('../utils/prisma');
const authRepository = require('../repositories/authRepository');
const jobUrlFetcher = require('../services/jobUrlFetcher');

jest.mock('../ai/geminiClient', () => ({
    generateContent: jest.fn(),
    init: jest.fn(),
    getModel: jest.fn()
}));

jest.mock('../repositories/authRepository', () => ({
    findUserById: jest.fn()
}));

jest.mock('../services/jobUrlFetcher', () => ({
    fetchJobPageText: jest.fn()
}));

jest.mock('../utils/prisma', () => ({
    jobRequirement: {
        create: jest.fn(),
        findFirst: jest.fn()
    },
    resume: {
        findUnique: jest.fn()
    },
    matchAnalysis: {
        findFirst: jest.fn(),
        create: jest.fn(),
        update: jest.fn()
    }
}));

describe('POST /api/v1/ai/job/from-url', () => {
    let authToken;
    const mockUserId = '11111111-1111-1111-1111-111111111111';

    beforeAll(() => {
        authToken = generateAccessToken(mockUserId);
        authRepository.findUserById.mockResolvedValue({
            id: mockUserId,
            email: 'joburl-test@example.com',
            status: 'ACTIVE'
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should return 400 for an invalid URL', async () => {
        const res = await request(app)
            .post('/api/v1/ai/job/from-url')
            .set('Authorization', `Bearer ${authToken}`)
            .send({ url: 'not-a-url' });

        expect(res.statusCode).toBe(400);
        expect(res.body.success).toBe(false);
    });

    it('should fetch URL, create job, match resume, and return suggested send time', async () => {
        jobUrlFetcher.fetchJobPageText.mockResolvedValueOnce(
            'Senior Backend Engineer at Acme. Required: Node.js, PostgreSQL. 5+ years experience building APIs.'
        );

        const mockJobAnalysis = {
            jobTitle: 'Senior Backend Engineer',
            company: 'Acme',
            requiredSkills: ['Node.js', 'PostgreSQL']
        };
        const mockMatch = {
            matchingSkills: ['Node.js'],
            missingSkills: ['PostgreSQL'],
            relevantExperience: ['Built APIs'],
            relevantProjects: [],
            matchingQualifications: [],
            potentialGaps: [],
            personalizationPoints: ['Node.js API experience']
        };

        geminiClient.generateContent
            .mockResolvedValueOnce(JSON.stringify(mockJobAnalysis))
            .mockResolvedValueOnce(JSON.stringify(mockMatch));

        const createdJobId = '12345678-1234-1234-1234-123456789012';

        prisma.jobRequirement.create.mockResolvedValueOnce({
            id: createdJobId,
            userId: mockUserId,
            title: 'Senior Backend Engineer',
            company: 'Acme',
            description: 'Senior Backend Engineer at Acme. Required: Node.js, PostgreSQL. 5+ years experience building APIs.',
            structuredData: mockJobAnalysis
        });

        prisma.resume.findUnique.mockResolvedValueOnce({
            id: 'resume-1',
            structuredData: { name: 'Test', skills: ['Node.js'] }
        });
        prisma.jobRequirement.findFirst.mockResolvedValueOnce({
            id: createdJobId,
            structuredData: mockJobAnalysis
        });
        prisma.matchAnalysis.findFirst.mockResolvedValueOnce(null);
        prisma.matchAnalysis.create.mockResolvedValueOnce({
            id: 'match-1',
            userId: mockUserId,
            jobRequirementId: createdJobId,
            result: mockMatch
        });

        const res = await request(app)
            .post('/api/v1/ai/job/from-url')
            .set('Authorization', `Bearer ${authToken}`)
            .send({ url: 'https://jobs.example.com/senior-backend' });

        expect(res.statusCode).toBe(201);
        expect(res.body.success).toBe(true);
        expect(res.body.data.jobRequirement.id).toBe(createdJobId);
        expect(res.body.data.match.result.matchingSkills).toEqual(['Node.js']);
        expect(res.body.data.sourceUrl).toBe('https://jobs.example.com/senior-backend');
        expect(res.body.data.suggestedSendAt).toMatch(/^\d{4}-\d{2}-\d{2}T/);
        expect(jobUrlFetcher.fetchJobPageText).toHaveBeenCalledWith('https://jobs.example.com/senior-backend');
    });

    it('should generate outreach when recipient is provided', async () => {
        jobUrlFetcher.fetchJobPageText.mockResolvedValueOnce(
            'Frontend Engineer at Beta Corp. React and TypeScript required for product work.'
        );

        geminiClient.generateContent
            .mockResolvedValueOnce(JSON.stringify({ jobTitle: 'Frontend Engineer', company: 'Beta Corp', requiredSkills: ['React'] }))
            .mockResolvedValueOnce(JSON.stringify({
                matchingSkills: ['React'],
                missingSkills: [],
                relevantExperience: [],
                relevantProjects: [],
                matchingQualifications: [],
                potentialGaps: [],
                personalizationPoints: ['React experience']
            }))
            .mockResolvedValueOnce(JSON.stringify({
                subject: 'Frontend role at Beta',
                bodyText: 'Hello',
                bodyHtml: '<p>Hello</p>',
                personalizationPoints: ['React experience'],
                callToAction: 'Open to a chat?'
            }));

        const jobId = '12345678-1234-1234-1234-123456789099';
        prisma.jobRequirement.create.mockResolvedValueOnce({
            id: jobId,
            userId: mockUserId,
            title: 'Frontend Engineer',
            company: 'Beta Corp',
            description: 'Frontend Engineer at Beta Corp. React and TypeScript required for product work.',
            structuredData: { jobTitle: 'Frontend Engineer', company: 'Beta Corp', requiredSkills: ['React'] }
        });
        prisma.resume.findUnique
            .mockResolvedValueOnce({ id: 'resume-1', structuredData: { name: 'Test', skills: ['React'] } })
            .mockResolvedValueOnce({ id: 'resume-1', structuredData: { name: 'Test', skills: ['React'] } });
        prisma.jobRequirement.findFirst
            .mockResolvedValueOnce({
                id: jobId,
                structuredData: { jobTitle: 'Frontend Engineer', company: 'Beta Corp', requiredSkills: ['React'] }
            })
            .mockResolvedValueOnce({
                id: jobId,
                company: 'Beta Corp',
                structuredData: { jobTitle: 'Frontend Engineer', company: 'Beta Corp', requiredSkills: ['React'] }
            });
        prisma.matchAnalysis.findFirst
            .mockResolvedValueOnce(null)
            .mockResolvedValueOnce({
                id: 'match-2',
                result: { matchingSkills: ['React'], personalizationPoints: ['React experience'] }
            });
        prisma.matchAnalysis.create.mockResolvedValueOnce({
            id: 'match-2',
            userId: mockUserId,
            jobRequirementId: jobId,
            result: {
                matchingSkills: ['React'],
                missingSkills: [],
                relevantExperience: [],
                relevantProjects: [],
                matchingQualifications: [],
                potentialGaps: [],
                personalizationPoints: ['React experience']
            }
        });

        const res = await request(app)
            .post('/api/v1/ai/job/from-url')
            .set('Authorization', `Bearer ${authToken}`)
            .send({
                url: 'https://jobs.example.com/frontend',
                recipient: { email: 'hr@beta.com', name: 'Hiring Manager', company: 'Beta Corp' },
                tone: 'concise'
            });

        expect(res.statusCode).toBe(201);
        expect(res.body.success).toBe(true);
        expect(res.body.data.outreach.subject).toBe('Frontend role at Beta');
        expect(res.body.data.outreach.bodyText).toBe('Hello');
    });
});
