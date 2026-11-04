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
    jobRequirement: {
        create: jest.fn(),
        findFirst: jest.fn(),
        findMany: jest.fn(),
        delete: jest.fn()
    }
}));

describe('Job Requirement API Endpoints', () => {
    let authToken;
    const mockUserId = '11111111-1111-1111-1111-111111111111';

    beforeAll(() => {
        authToken = generateAccessToken(mockUserId);
        authRepository.findUserById.mockResolvedValue({
            id: mockUserId,
            email: 'job-test@example.com',
            status: 'ACTIVE'
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('POST /api/v1/ai/job', () => {
        it('should return 400 if description is missing', async () => {
            const res = await request(app)
                .post('/api/v1/ai/job')
                .set('Authorization', `Bearer ${authToken}`)
                .send({ title: 'Dev' });

            expect(res.statusCode).toBe(400);
            expect(res.body.success).toBe(false);
        });

        it('should create and analyze job requirement successfully', async () => {
            const mockJobAnalysis = {
                jobTitle: 'Backend Dev',
                company: 'Tech Corp',
                requiredSkills: ['Node.js']
            };
            geminiClient.generateContent.mockResolvedValueOnce(JSON.stringify(mockJobAnalysis));

            prisma.jobRequirement.create.mockResolvedValueOnce({
                id: 'job-123',
                userId: mockUserId,
                description: 'We need a Node.js developer...',
                structuredData: mockJobAnalysis
            });

            const res = await request(app)
                .post('/api/v1/ai/job')
                .set('Authorization', `Bearer ${authToken}`)
                .send({ description: 'We need a Node.js developer with 5 years of experience.' });

            expect(res.statusCode).toBe(201);
            expect(res.body.success).toBe(true);
            expect(res.body.data.structuredData).toEqual(mockJobAnalysis);
            expect(prisma.jobRequirement.create).toHaveBeenCalled();
        });
    });

    describe('GET /api/v1/ai/job/:id', () => {
        it('should return 404 if job not found', async () => {
            prisma.jobRequirement.findFirst.mockResolvedValueOnce(null);

            const res = await request(app)
                .get('/api/v1/ai/job/job-123')
                .set('Authorization', `Bearer ${authToken}`);

            expect(res.statusCode).toBe(404);
            expect(res.body.success).toBe(false);
        });

        it('should return job if found', async () => {
            prisma.jobRequirement.findFirst.mockResolvedValueOnce({
                id: 'job-123',
                userId: mockUserId
            });

            const res = await request(app)
                .get('/api/v1/ai/job/job-123')
                .set('Authorization', `Bearer ${authToken}`);

            expect(res.statusCode).toBe(200);
            expect(res.body.success).toBe(true);
            expect(res.body.data.id).toBe('job-123');
        });
    });
});
