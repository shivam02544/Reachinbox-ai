const request = require('supertest');
const app = require('../app');
const prisma = require('../utils/prisma');

describe('Authentication API', () => {
    const testUser = {
        name: 'Test User',
        email: 'test@example.com',
        password: 'Password123!'
    };

    beforeAll(async () => {
        // Cleanup test user if exists
        await prisma.user.deleteMany({
            where: { email: testUser.email }
        });
    });

    afterAll(async () => {
        await prisma.user.deleteMany({
            where: { email: testUser.email }
        });
        await prisma.$disconnect();
    });

    let accessToken;
    let refreshToken;

    test('POST /api/v1/auth/register - Should register a new user', async () => {
        const res = await request(app)
            .post('/api/v1/auth/register')
            .send(testUser);

        expect(res.statusCode).toBe(201);
        expect(res.body.success).toBe(true);
        expect(res.body.data.user.email).toBe(testUser.email);
        expect(res.body.data.accessToken).toBeDefined();
        expect(res.body.data.refreshToken).toBeDefined();
    });

    test('POST /api/v1/auth/login - Should login the user', async () => {
        const res = await request(app)
            .post('/api/v1/auth/login')
            .send({
                email: testUser.email,
                password: testUser.password
            });

        expect(res.statusCode).toBe(200);
        expect(res.body.success).toBe(true);
        accessToken = res.body.data.accessToken;
        refreshToken = res.body.data.refreshToken;
    });

    test('GET /api/v1/auth/session - Should return user session', async () => {
        const res = await request(app)
            .get('/api/v1/auth/session')
            .set('Authorization', `Bearer ${accessToken}`);

        expect(res.statusCode).toBe(200);
        expect(res.body.success).toBe(true);
        expect(res.body.data.user.email).toBe(testUser.email);
    });

    test('POST /api/v1/auth/refresh - Should rotate tokens', async () => {
        const res = await request(app)
            .post('/api/v1/auth/refresh')
            .send({ refreshToken });

        expect(res.statusCode).toBe(200);
        expect(res.body.success).toBe(true);
        expect(res.body.data.accessToken).toBeDefined();
        expect(res.body.data.refreshToken).toBeDefined();

        // Update tokens for next tests
        accessToken = res.body.data.accessToken;
        const oldRefreshToken = refreshToken;
        refreshToken = res.body.data.refreshToken;

        // Verify old refresh token is rejected (Reuse detection)
        const reuseRes = await request(app)
            .post('/api/v1/auth/refresh')
            .send({ refreshToken: oldRefreshToken });

        expect(reuseRes.statusCode).toBe(401);
    });

    test('POST /api/v1/auth/logout - Should logout user', async () => {
        const res = await request(app)
            .post('/api/v1/auth/logout')
            .send({ refreshToken });

        expect(res.statusCode).toBe(200);
        expect(res.body.success).toBe(true);
    });
});
