const request = require('supertest');
const app = require('../app');
const { generateAccessToken } = require('../utils/jwt');
const geminiClient = require('../ai/geminiClient');
const authRepository = require('../repositories/authRepository');

// Mock the geminiClient methods
jest.mock('../ai/geminiClient', () => {
    return {
        generateContent: jest.fn(),
        init: jest.fn(),
        getModel: jest.fn()
    };
});

// Mock the authRepository so we don't need the database
jest.mock('../repositories/authRepository', () => {
    return {
        findUserById: jest.fn()
    };
});

describe('AI Endpoints', () => {
    let authToken;
    const mockUserId = '12345678-1234-1234-1234-123456789012';

    beforeAll(() => {
        // Generate token for authentication
        authToken = generateAccessToken(mockUserId);

        // Mock finding the user as ACTIVE
        authRepository.findUserById.mockResolvedValue({
            id: mockUserId,
            email: 'ai-test@example.com',
            status: 'ACTIVE'
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
        authRepository.findUserById.mockResolvedValue({
            id: mockUserId,
            email: 'ai-test@example.com',
            status: 'ACTIVE'
        });
    });

    describe('GET /api/v1/ai/health', () => {
        it('should return 401 if unauthenticated', async () => {
            const res = await request(app).get('/api/v1/ai/health');
            expect(res.statusCode).toBe(401);
            expect(res.body.success).toBe(false);
        });

        it('should return AI health status if authenticated', async () => {
            const res = await request(app)
                .get('/api/v1/ai/health')
                .set('Authorization', `Bearer ${authToken}`);

            expect(res.statusCode).toBe(200);
            expect(res.body.success).toBe(true);
            expect(res.body.data).toHaveProperty('status');
        });
    });

    describe('POST /api/v1/ai/test', () => {
        it('should return 401 if unauthenticated', async () => {
            const res = await request(app)
                .post('/api/v1/ai/test')
                .send({ prompt: 'Hello' });
            expect(res.statusCode).toBe(401);
            expect(res.body.success).toBe(false);
        });

        it('should return 400 for empty prompt', async () => {
            const res = await request(app)
                .post('/api/v1/ai/test')
                .set('Authorization', `Bearer ${authToken}`)
                .send({ prompt: '' });

            expect(res.statusCode).toBe(400);
            expect(res.body.success).toBe(false);
        });

        it('should return 400 for excessively long prompt', async () => {
            const longPrompt = 'a'.repeat(1001);
            const res = await request(app)
                .post('/api/v1/ai/test')
                .set('Authorization', `Bearer ${authToken}`)
                .send({ prompt: longPrompt });

            expect(res.statusCode).toBe(400);
            expect(res.body.success).toBe(false);
        });

        it('should return generated content for valid prompt', async () => {
            // Mock successful AI response
            geminiClient.generateContent.mockResolvedValueOnce('Mocked AI response');

            const res = await request(app)
                .post('/api/v1/ai/test')
                .set('Authorization', `Bearer ${authToken}`)
                .send({ prompt: 'Hello AI' });

            expect(res.statusCode).toBe(200);
            expect(res.body.success).toBe(true);
            expect(res.body.data.result).toBe('Mocked AI response');
            expect(geminiClient.generateContent).toHaveBeenCalledWith('Hello AI');
        });

        it('should handle AI service timeout/errors correctly', async () => {
            // Mock a timeout error from the AI client
            const timeoutError = new Error('AI service is temporarily unavailable or timed out.');
            timeoutError.statusCode = 503;
            geminiClient.generateContent.mockRejectedValueOnce(timeoutError);

            const res = await request(app)
                .post('/api/v1/ai/test')
                .set('Authorization', `Bearer ${authToken}`)
                .send({ prompt: 'Hello AI' });

            expect(res.statusCode).toBe(503);
            expect(res.body.success).toBe(false);
            expect(res.body.message).toContain('unavailable or timed out');
        });
    });
});
