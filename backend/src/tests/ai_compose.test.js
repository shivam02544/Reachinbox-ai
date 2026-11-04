const request = require('supertest');
const app = require('../app');
const { generateAccessToken } = require('../utils/jwt');
const geminiClient = require('../ai/geminiClient');
const authRepository = require('../repositories/authRepository');

jest.mock('../ai/geminiClient', () => ({ generateContent: jest.fn() }));
jest.mock('../repositories/authRepository', () => ({ findUserById: jest.fn() }));

describe('POST /api/v1/ai/compose/improve', () => {
    const userId = '12345678-1234-1234-1234-123456789012';
    const token = generateAccessToken(userId);

    beforeEach(() => {
        authRepository.findUserById.mockResolvedValue({ id: userId, status: 'ACTIVE' });
        geminiClient.generateContent.mockReset();
    });

    it('requires authentication and email body', async () => {
        const unauthorized = await request(app).post('/api/v1/ai/compose/improve').send({ body: 'Hello' });
        expect(unauthorized.statusCode).toBe(401);

        const invalid = await request(app).post('/api/v1/ai/compose/improve')
            .set('Authorization', `Bearer ${token}`).send({ body: '' });
        expect(invalid.statusCode).toBe(400);
    });

    it('returns validated subject and body suggestions', async () => {
        geminiClient.generateContent.mockResolvedValue(JSON.stringify({ subject: 'Following up', body: 'Hello, I wanted to follow up.' }));
        const response = await request(app).post('/api/v1/ai/compose/improve')
            .set('Authorization', `Bearer ${token}`)
            .send({ subject: '', body: 'Hey, checking in', tone: 'professional' });
        expect(response.statusCode).toBe(200);
        expect(response.body.data).toEqual({ subject: 'Following up', body: 'Hello, I wanted to follow up.' });
    });
});
