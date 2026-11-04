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
        findFirst: jest.fn()
    }
}));

describe('Outreach API Endpoints', () => {
    let authToken;
    const mockUserId = '11111111-1111-1111-1111-111111111111';

    beforeAll(() => {
        authToken = generateAccessToken(mockUserId);
        authRepository.findUserById.mockResolvedValue({
            id: mockUserId,
            email: 'outreach-test@example.com',
            status: 'ACTIVE'
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('POST /api/v1/ai/outreach/generate', () => {
        const validPayload = {
            jobRequirementId: '12345678-1234-1234-1234-123456789012',
            recipient: {
                email: 'hr@techcorp.com',
                name: 'Jane Doe',
                company: 'TechCorp'
            }
        };

        it('should return 400 if validation fails (e.g. invalid email)', async () => {
            const res = await request(app)
                .post('/api/v1/ai/outreach/generate')
                .set('Authorization', `Bearer ${authToken}`)
                .send({
                    jobRequirementId: '12345678-1234-1234-1234-123456789012',
                    recipient: { email: 'not-an-email' }
                });

            expect(res.statusCode).toBe(400);
            expect(res.body.success).toBe(false);
        });

        it('should return 404 if resume missing', async () => {
            prisma.resume.findUnique.mockResolvedValueOnce(null);

            const res = await request(app)
                .post('/api/v1/ai/outreach/generate')
                .set('Authorization', `Bearer ${authToken}`)
                .send(validPayload);

            expect(res.statusCode).toBe(404);
            expect(res.body.message).toContain('resume');
        });

        it('should return 404 if job missing', async () => {
            prisma.resume.findUnique.mockResolvedValueOnce({ id: 'r1', structuredData: {} });
            prisma.jobRequirement.findFirst.mockResolvedValueOnce(null);

            const res = await request(app)
                .post('/api/v1/ai/outreach/generate')
                .set('Authorization', `Bearer ${authToken}`)
                .send(validPayload);

            expect(res.statusCode).toBe(404);
            expect(res.body.message).toContain('Job');
        });

        it('should return 404 if match analysis missing', async () => {
            prisma.resume.findUnique.mockResolvedValueOnce({ id: 'r1', structuredData: {} });
            prisma.jobRequirement.findFirst.mockResolvedValueOnce({ id: 'j1', structuredData: {} });
            prisma.matchAnalysis.findFirst.mockResolvedValueOnce(null);

            const res = await request(app)
                .post('/api/v1/ai/outreach/generate')
                .set('Authorization', `Bearer ${authToken}`)
                .send(validPayload);

            expect(res.statusCode).toBe(404);
            expect(res.body.message).toContain('Match analysis not found');
        });

        it('should generate valid outreach successfully', async () => {
            prisma.resume.findUnique.mockResolvedValueOnce({ id: 'r1', structuredData: { name: 'Test' } });
            prisma.jobRequirement.findFirst.mockResolvedValueOnce({ id: 'j1', structuredData: { company: 'C' } });
            prisma.matchAnalysis.findFirst.mockResolvedValueOnce({ id: 'm1', result: { matchingSkills: [] } });

            const mockGeminiResponse = {
                subject: 'Application',
                bodyText: 'Hello',
                bodyHtml: '<p>Hello</p>',
                personalizationPoints: ['Point1']
            };
            geminiClient.generateContent.mockResolvedValueOnce(JSON.stringify(mockGeminiResponse));

            const res = await request(app)
                .post('/api/v1/ai/outreach/generate')
                .set('Authorization', `Bearer ${authToken}`)
                .send(validPayload);

            expect(res.statusCode).toBe(200);
            expect(res.body.success).toBe(true);
            expect(res.body.data).toEqual(mockGeminiResponse);
            expect(geminiClient.generateContent).toHaveBeenCalled();
        });
    });
});
