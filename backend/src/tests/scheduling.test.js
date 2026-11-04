const request = require('supertest');
const app = require('../app');
const prisma = require('../utils/prisma');

describe('Scheduling API', () => {
    const testUser = {
        name: 'Scheduler User',
        email: 'scheduler@example.com',
        password: 'Password123!'
    };

    let accessToken;
    let senderId;

    beforeAll(async () => {
        await prisma.user.deleteMany({ where: { email: testUser.email } });
        const regRes = await request(app).post('/api/v1/auth/register').send(testUser);
        accessToken = regRes.body.data.accessToken;

        const senderRes = await request(app)
            .post('/api/v1/senders')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                displayName: 'Test Sender',
                email: 'test@example.com',
                smtpHost: 'localhost',
                smtpPort: 1025,
                smtpUsername: 'test',
                smtpPassword: 'password'
            });
        senderId = senderRes.body.data.id;
        await prisma.senderAccount.update({
            where: { id: senderId },
            data: { status: 'CONNECTED' }
        });
    });

    afterAll(async () => {
        await prisma.user.deleteMany({ where: { email: testUser.email } });
        await prisma.$disconnect();
    });

    test('POST /api/v1/scheduled-emails - Should schedule a future email', async () => {
        const futureDate = new Date();
        futureDate.setHours(futureDate.getHours() + 1);

        const res = await request(app)
            .post('/api/v1/scheduled-emails')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                senderAccountId: senderId,
                recipient: 'rcp@example.com',
                subject: 'Future Email',
                textBody: 'Hello world',
                scheduledAt: futureDate.toISOString()
            });

        expect(res.statusCode).toBe(201);
        expect(res.body.success).toBe(true);
        expect(res.body.data.status).toBe('SCHEDULED');
    });

    test('POST /api/v1/scheduled-emails - Should reject past date', async () => {
        const pastDate = new Date();
        pastDate.setHours(pastDate.getHours() - 1);

        const res = await request(app)
            .post('/api/v1/scheduled-emails')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                senderAccountId: senderId,
                recipient: 'rcp@example.com',
                subject: 'Past Email',
                textBody: 'Hello world',
                scheduledAt: pastDate.toISOString()
            });

        expect(res.statusCode).toBe(400);
    });

    test('GET /api/v1/scheduled-emails - Should list scheduled emails', async () => {
        const res = await request(app)
            .get('/api/v1/scheduled-emails')
            .set('Authorization', `Bearer ${accessToken}`);

        expect(res.statusCode).toBe(200);
        expect(res.body.data.scheduledEmails.length).toBeGreaterThan(0);
    });
});
