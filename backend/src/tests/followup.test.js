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
    email: {
        findUnique: jest.fn(),
        findFirst: jest.fn()
    },
    resume: {
        findUnique: jest.fn()
    }
}));

describe('Follow-Up API Endpoints', () => {
    let authToken;
    const mockUserId = '11111111-1111-1111-1111-111111111111';

    beforeAll(() => {
        authToken = generateAccessToken(mockUserId);
        authRepository.findUserById.mockResolvedValue({
            id: mockUserId,
            email: 'followup-test@example.com',
            status: 'ACTIVE'
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('POST /api/v1/ai/followup/generate', () => {
        const validPayload = {
            parentEmailId: '22222222-2222-2222-2222-222222222222',
            followUpNumber: 1
        };

        it('should return 400 if parentEmailId is missing', async () => {
            const res = await request(app)
                .post('/api/v1/ai/followup/generate')
                .set('Authorization', `Bearer ${authToken}`)
                .send({ followUpNumber: 1 });

            expect(res.statusCode).toBe(400);
            expect(res.body.success).toBe(false);
        });

        it('should return 404 if parent email does not exist or user unowned', async () => {
            prisma.email.findUnique.mockResolvedValueOnce(null);

            const res = await request(app)
                .post('/api/v1/ai/followup/generate')
                .set('Authorization', `Bearer ${authToken}`)
                .send(validPayload);

            expect(res.statusCode).toBe(404);
            expect(res.body.message).toContain('access denied');
        });

        it('should return 409 if duplicate follow-up already exists', async () => {
            prisma.email.findUnique.mockResolvedValueOnce({
                id: validPayload.parentEmailId,
                userId: mockUserId
            });
            prisma.email.findFirst.mockResolvedValueOnce({
                id: 'dup-1',
                status: 'SCHEDULED'
            });

            const res = await request(app)
                .post('/api/v1/ai/followup/generate')
                .set('Authorization', `Bearer ${authToken}`)
                .send(validPayload);

            expect(res.statusCode).toBe(409);
            expect(res.body.message).toContain('already exists');
        });

        it('should generate valid follow-up successfully', async () => {
            prisma.email.findUnique.mockResolvedValueOnce({
                id: validPayload.parentEmailId,
                userId: mockUserId,
                recipient: 'hr@example.com',
                subject: 'Hello',
                textBody: 'Original Body'
            });
            prisma.email.findFirst.mockResolvedValueOnce(null); // No duplicate
            prisma.resume.findUnique.mockResolvedValueOnce(null); // Optional resume

            const mockGeminiResponse = {
                subject: 'Re: Hello',
                bodyText: 'Follow-up text',
                bodyHtml: '<p>Follow-up text</p>',
                personalizationPoints: ['Polite bump'],
                followUpNumber: 1
            };
            geminiClient.generateContent.mockResolvedValueOnce(JSON.stringify(mockGeminiResponse));

            const res = await request(app)
                .post('/api/v1/ai/followup/generate')
                .set('Authorization', `Bearer ${authToken}`)
                .send(validPayload);

            expect(res.statusCode).toBe(200);
            expect(res.body.success).toBe(true);
            expect(res.body.data).toEqual(mockGeminiResponse);
            expect(geminiClient.generateContent).toHaveBeenCalled();
        });
    });
});
