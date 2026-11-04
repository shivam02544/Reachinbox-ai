const request = require('supertest');
const app = require('../app');
const { generateAccessToken } = require('../utils/jwt');
const geminiClient = require('../ai/geminiClient');
const prisma = require('../utils/prisma');
const authRepository = require('../repositories/authRepository');

jest.mock('../ai/geminiClient', () => ({
    generateContent: jest.fn(),
    init: jest.fn(),
    getModel: jest.fn()
}));

jest.mock('../repositories/authRepository', () => ({
    findUserById: jest.fn()
}));

jest.mock('../utils/prisma', () => ({
    resume: {
        findUnique: jest.fn()
    },
    jobRequirement: {
        findFirst: jest.fn()
    },
    matchAnalysis: {
        findFirst: jest.fn(),
        create: jest.fn(),
        update: jest.fn()
    }
}));

describe('Match API Endpoints', () => {
    let authToken;
    const mockUserId = '11111111-1111-1111-1111-111111111111';

    beforeAll(() => {
        authToken = generateAccessToken(mockUserId);
        authRepository.findUserById.mockResolvedValue({
            id: mockUserId,
            email: 'match-test@example.com',
            status: 'ACTIVE'
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('POST /api/v1/ai/match', () => {
        it('should return 400 if jobRequirementId is missing', async () => {
            const res = await request(app)
                .post('/api/v1/ai/match')
                .set('Authorization', `Bearer ${authToken}`)
                .send({});

            expect(res.statusCode).toBe(400);
            expect(res.body.success).toBe(false);
        });

        it('should return 404 if resume is not found', async () => {
            prisma.resume.findUnique.mockResolvedValueOnce(null);

            const res = await request(app)
                .post('/api/v1/ai/match')
                .set('Authorization', `Bearer ${authToken}`)
                .send({ jobRequirementId: '12345678-1234-1234-1234-123456789012' });

            expect(res.statusCode).toBe(404);
            expect(res.body.message).toContain('upload a resume');
        });

        it('should return 404 if job requirement is not found', async () => {
            prisma.resume.findUnique.mockResolvedValueOnce({ id: 'res-1', structuredData: {} });
            prisma.jobRequirement.findFirst.mockResolvedValueOnce(null);

            const res = await request(app)
                .post('/api/v1/ai/match')
                .set('Authorization', `Bearer ${authToken}`)
                .send({ jobRequirementId: '12345678-1234-1234-1234-123456789012' });

            expect(res.statusCode).toBe(404);
            expect(res.body.message).toContain('Job requirement not found');
        });

        it('should generate match and save successfully', async () => {
            const mockMatchResult = {
                matchingSkills: ['Java'],
                missingSkills: ['AWS']
            };

            prisma.resume.findUnique.mockResolvedValueOnce({ id: 'res-1', structuredData: {} });
            prisma.jobRequirement.findFirst.mockResolvedValueOnce({ id: 'job-1', structuredData: {} });
            geminiClient.generateContent.mockResolvedValueOnce(JSON.stringify(mockMatchResult));

            prisma.matchAnalysis.findFirst.mockResolvedValueOnce(null);
            prisma.matchAnalysis.create.mockResolvedValueOnce({
                id: 'match-1',
                result: mockMatchResult
            });

            const res = await request(app)
                .post('/api/v1/ai/match')
                .set('Authorization', `Bearer ${authToken}`)
                .send({ jobRequirementId: '12345678-1234-1234-1234-123456789012' });

            expect(res.statusCode).toBe(200);
            expect(res.body.success).toBe(true);
            expect(res.body.data.result).toEqual(mockMatchResult);
            expect(prisma.matchAnalysis.create).toHaveBeenCalled();
        });
    });
});
