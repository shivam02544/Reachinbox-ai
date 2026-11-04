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
    inboundEmail: {
        findUnique: jest.fn(),
        update: jest.fn()
    }
}));

describe('Reply Intelligence API', () => {
    let authToken;
    const mockUserId = '11111111-1111-1111-1111-111111111111';

    beforeAll(() => {
        authToken = generateAccessToken(mockUserId);
        authRepository.findUserById.mockResolvedValue({
            id: mockUserId,
            email: 'reply-test@example.com',
            status: 'ACTIVE'
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('POST /api/v1/ai/reply/:inboundEmailId/analyze', () => {
        const mockInboundId = '22222222-2222-2222-2222-222222222222';

        it('should return 404 if inbound email is missing or unauthorized', async () => {
            prisma.inboundEmail.findUnique.mockResolvedValueOnce(null);

            const res = await request(app)
                .post(`/api/v1/ai/reply/${mockInboundId}/analyze`)
                .set('Authorization', `Bearer ${authToken}`);

            expect(res.statusCode).toBe(404);
            expect(res.body.message).toContain('access denied');
        });

        it('should analyze reply and return structured classification & suggestion', async () => {
            const mockInboundEmail = {
                id: mockInboundId,
                fromEmail: 'client@enterprise.com',
                textBody: 'I would love to schedule an interview next Tuesday.',
                senderAccount: { userId: mockUserId },
                matchedEmail: { subject: 'Application for Engineer' },
                analysis: null
            };

            const mockAIAnalysis = {
                classification: 'INTERVIEW_RELATED',
                summary: 'The client wants to schedule an interview next Tuesday.',
                suggestedAction: 'Propose available times on Tuesday.',
                confidence: 'HIGH',
                suggestedResponse: {
                    subject: 'Re: Application for Engineer',
                    bodyText: 'Thanks for reaching out! Tuesday works great for me.'
                }
            };

            prisma.inboundEmail.findUnique.mockResolvedValueOnce(mockInboundEmail);
            geminiClient.generateContent.mockResolvedValueOnce(JSON.stringify(mockAIAnalysis));
            prisma.inboundEmail.update.mockResolvedValueOnce({ ...mockInboundEmail, analysis: mockAIAnalysis });

            const res = await request(app)
                .post(`/api/v1/ai/reply/${mockInboundId}/analyze`)
                .set('Authorization', `Bearer ${authToken}`);

            expect(res.statusCode).toBe(200);
            expect(res.body.success).toBe(true);
            expect(res.body.data.classification).toBe('INTERVIEW_RELATED');
            expect(res.body.data.suggestedResponse.bodyText).toContain('Tuesday works great');
        });
    });
});
