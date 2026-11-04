const request = require('supertest');
const app = require('../app');
const prisma = require('../utils/prisma');

describe('Dashboard API', () => {
    const testUser = {
        name: 'Dashboard User',
        email: 'dashboard@example.com',
        password: 'Password123!'
    };

    let accessToken;

    beforeAll(async () => {
        // Cleanup and create test user
        await prisma.user.deleteMany({ where: { email: testUser.email } });
        const regRes = await request(app)
            .post('/api/v1/auth/register')
            .send(testUser);
        accessToken = regRes.body.data.accessToken;
    });

    afterAll(async () => {
        await prisma.user.deleteMany({ where: { email: testUser.email } });
        await prisma.$disconnect();
    });

    test('GET /api/v1/dashboard - Should return dashboard data for authenticated user', async () => {
        const res = await request(app)
            .get('/api/v1/dashboard')
            .set('Authorization', `Bearer ${accessToken}`);

        expect(res.statusCode).toBe(200);
        expect(res.body.success).toBe(true);
        expect(res.body.data.stats).toBeDefined();
        expect(res.body.data.stats.totalEmails).toBe(0);
        expect(res.body.data.recentActivity).toBeInstanceOf(Array);
    });

    test('GET /api/v1/dashboard - Should return 401 for unauthenticated request', async () => {
        const res = await request(app).get('/api/v1/dashboard');
        expect(res.statusCode).toBe(401);
    });

    test('User Isolation - User B should not see User A data', async () => {
        // Create User B
        const userB = {
            name: 'User B',
            email: 'userb@example.com',
            password: 'Password123!'
        };
        await prisma.user.deleteMany({ where: { email: userB.email } });
        const regB = await request(app).post('/api/v1/auth/register').send(userB);
        const tokenB = regB.body.data.accessToken;
        const userIdB = regB.body.data.user.id;

        // Add some data for User A (testUser)
        const userA = await prisma.user.findUnique({ where: { email: testUser.email } });
        await prisma.senderAccount.create({
            data: {
                userId: userA.id,
                email: 'sender@userA.com',
                displayName: 'Sender A'
            }
        });

        // Get Dashboard for User B
        const resB = await request(app)
            .get('/api/v1/dashboard')
            .set('Authorization', `Bearer ${tokenB}`);

        expect(resB.statusCode).toBe(200);
        expect(resB.body.data.stats.totalSenders).toBe(0); // Should be 0 for User B

        // Cleanup User B
        await prisma.user.deleteMany({ where: { email: userB.email } });
    });
});
