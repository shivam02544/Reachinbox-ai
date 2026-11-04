const request = require('supertest');
const app = require('../app');
const prisma = require('../utils/prisma');

describe('Activity API', () => {
    const testUser = {
        name: 'Activity User',
        email: 'activity_test@example.com',
        password: 'Password123!'
    };

    let accessToken;

    beforeAll(async () => {
        await prisma.user.deleteMany({ where: { email: testUser.email } });
        const regRes = await request(app).post('/api/v1/auth/register').send(testUser);
        accessToken = regRes.body.data.accessToken;

        // Create some activities
        const user = await prisma.user.findUnique({ where: { email: testUser.email } });
        await prisma.activity.createMany({
            data: [
                { userId: user.id, type: 'SENDER_CONNECTED', message: 'Test activity 1' },
                { userId: user.id, type: 'EMAIL_SENT', message: 'Test activity 2' }
            ]
        });
    });

    afterAll(async () => {
        await prisma.user.deleteMany({ where: { email: testUser.email } });
        await prisma.$disconnect();
    });

    test('GET /api/v1/activity - Should return user activities', async () => {
        const res = await request(app)
            .get('/api/v1/activity')
            .set('Authorization', `Bearer ${accessToken}`);

        expect(res.statusCode).toBe(200);
        expect(res.body.success).toBe(true);
        expect(res.body.data.activities.length).toBe(2);
    });

    test('User Isolation - User should not see other users activities', async () => {
        const otherUser = { name: 'Other', email: 'other_activity@example.com', password: 'Password123!' };
        await prisma.user.deleteMany({ where: { email: otherUser.email } });
        const regOther = await request(app).post('/api/v1/auth/register').send(otherUser);
        const otherToken = regOther.body.data.accessToken;
        const otherUserId = regOther.body.data.user.id;

        // Create an activity for otherUser
        await prisma.activity.create({
            data: {
                userId: otherUserId,
                type: 'SENDER_CONNECTED',
                message: 'Other user activity'
            }
        });

        const res = await request(app)
            .get('/api/v1/activity')
            .set('Authorization', `Bearer ${otherToken}`);

        expect(res.body.data.activities.length).toBe(1); // Only its own activity
        await prisma.user.deleteMany({ where: { email: otherUser.email } });
    });
});
