const request = require('supertest');
const app = require('../app');
const prisma = require('../utils/prisma');

describe('Sender API', () => {
    const testUser = {
        name: 'Sender User',
        email: 'sender_test@example.com',
        password: 'Password123!'
    };

    let accessToken;
    let userId;

    beforeAll(async () => {
        await prisma.user.deleteMany({ where: { email: testUser.email } });
        const regRes = await request(app).post('/api/v1/auth/register').send(testUser);
        accessToken = regRes.body.data.accessToken;
        userId = regRes.body.data.user.id;
    });

    afterAll(async () => {
        await prisma.user.deleteMany({ where: { email: testUser.email } });
        await prisma.$disconnect();
    });

    test('POST /api/v1/senders - Should add a new sender', async () => {
        const senderData = {
            displayName: 'My SMTP',
            email: 'sender@example.com',
            smtpHost: 'smtp.example.com',
            smtpPort: 587,
            smtpUsername: 'sender@example.com',
            smtpPassword: 'password123'
        };

        const res = await request(app)
            .post('/api/v1/senders')
            .set('Authorization', `Bearer ${accessToken}`)
            .send(senderData);

        expect(res.statusCode).toBe(201);
        expect(res.body.success).toBe(true);
        expect(res.body.data.email).toBe(senderData.email);
        expect(res.body.data.smtpPassword).toBeUndefined(); // Security check
    });

    test('GET /api/v1/senders - Should list senders', async () => {
        const res = await request(app)
            .get('/api/v1/senders')
            .set('Authorization', `Bearer ${accessToken}`);

        expect(res.statusCode).toBe(200);
        expect(res.body.data.length).toBeGreaterThan(0);
    });

    test('User Isolation - Should not see others senders', async () => {
        const userB = { name: 'User B', email: 'userb_sender@example.com', password: 'Password123!' };
        await prisma.user.deleteMany({ where: { email: userB.email } });
        const regB = await request(app).post('/api/v1/auth/register').send(userB);

        const res = await request(app)
            .get('/api/v1/senders')
            .set('Authorization', `Bearer ${regB.body.data.accessToken}`);

        expect(res.body.data.length).toBe(0);
        await prisma.user.deleteMany({ where: { email: userB.email } });
    });
});
