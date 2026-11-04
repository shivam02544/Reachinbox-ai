const request = require('supertest');
const app = require('../app');
const prisma = require('../utils/prisma');

describe('Email API', () => {
    const testUser = {
        name: 'Email User',
        email: 'email_test@example.com',
        password: 'Password123!'
    };

    let accessToken;
    let senderId;

    beforeAll(async () => {
        await prisma.user.deleteMany({ where: { email: testUser.email } });
        const regRes = await request(app).post('/api/v1/auth/register').send(testUser);
        accessToken = regRes.body.data.accessToken;

        // Create a connected sender
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

        // Mark it as connected manually for testing if needed,
        // though /senders endpoint should have set it.
        // Actually Service sets it to DISCONNECTED initially.
        await prisma.senderAccount.update({
            where: { id: senderId },
            data: { status: 'CONNECTED' }
        });
    });

    afterAll(async () => {
        await prisma.user.deleteMany({ where: { email: testUser.email } });
        await prisma.$disconnect();
    });

    test('POST /api/v1/emails - Should fail for non-owned sender', async () => {
        // Create another user
        const otherUser = { name: 'Other', email: 'other@example.com', password: 'Password123!' };
        await prisma.user.deleteMany({ where: { email: otherUser.email } });
        const regOther = await request(app).post('/api/v1/auth/register').send(otherUser);
        const otherToken = regOther.body.data.accessToken;

        const res = await request(app)
            .post('/api/v1/emails')
            .set('Authorization', `Bearer ${otherToken}`)
            .send({
                senderAccountId: senderId,
                recipient: 'rcp@example.com',
                subject: 'Fail',
                textBody: 'Fail',
                idempotencyKey: '550e8400-e29b-41d4-a716-446655440000'
            });

        expect(res.statusCode).toBe(404);
        await prisma.user.deleteMany({ where: { email: otherUser.email } });
    });

    // We can't really test actual SMTP send in CI without a mock or local SMTP
    // But we can test the logic up to the send attempt if we mock the smtpService.

    describe('Email Management (Search, Filter, Paginate)', () => {
        beforeAll(async () => {
            // Create some test emails
            const user = await prisma.user.findUnique({ where: { email: testUser.email } });
            await prisma.email.createMany({
                data: [
                    { userId: user.id, senderAccountId: senderId, recipient: 'search@example.com', subject: 'Find Me', status: 'SENT' },
                    { userId: user.id, senderAccountId: senderId, recipient: 'other@example.com', subject: 'Ignore Me', status: 'SENT' },
                    { userId: user.id, senderAccountId: senderId, recipient: 'draft@example.com', subject: 'My Draft', status: 'DRAFT' },
                    { userId: user.id, senderAccountId: senderId, recipient: 'failed@example.com', subject: 'Bad luck', status: 'FAILED' }
                ]
            });
        });

        test('GET /api/v1/emails - Should paginate results', async () => {
            const res = await request(app)
                .get('/api/v1/emails?page=1&limit=2')
                .set('Authorization', `Bearer ${accessToken}`);

            expect(res.statusCode).toBe(200);
            expect(res.body.data.emails.length).toBe(2);
            expect(res.body.data.pagination.total).toBeGreaterThanOrEqual(4);
            expect(res.body.data.pagination.totalPages).toBeDefined();
        });

        test('GET /api/v1/emails - Should filter by status', async () => {
            const res = await request(app)
                .get('/api/v1/emails?status=DRAFT')
                .set('Authorization', `Bearer ${accessToken}`);

            expect(res.statusCode).toBe(200);
            expect(res.body.data.emails.every(e => e.status === 'DRAFT')).toBe(true);
            expect(res.body.data.emails.length).toBe(1);
        });

        test('GET /api/v1/emails - Should search by subject', async () => {
            const res = await request(app)
                .get('/api/v1/emails?search=Find')
                .set('Authorization', `Bearer ${accessToken}`);

            expect(res.statusCode).toBe(200);
            expect(res.body.data.emails.some(e => e.subject.includes('Find'))).toBe(true);
            expect(res.body.data.emails.length).toBe(1);
        });

        test('GET /api/v1/emails - User Isolation should still hold', async () => {
            const userB = { name: 'User B', email: 'userb_mgmt@example.com', password: 'Password123!' };
            await prisma.user.deleteMany({ where: { email: userB.email } });
            const regB = await request(app).post('/api/v1/auth/register').send(userB);

            const res = await request(app)
                .get('/api/v1/emails')
                .set('Authorization', `Bearer ${regB.body.data.accessToken}`);

            expect(res.body.data.emails.length).toBe(0);
            await prisma.user.deleteMany({ where: { email: userB.email } });
        });
    });
});
