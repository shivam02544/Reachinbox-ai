const request = require('supertest');
const app = require('../app');
const prisma = require('../utils/prisma');

describe('Campaign API', () => {
    const testUser = {
        name: 'Campaign User',
        email: 'campaign_test@example.com',
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
                displayName: 'Campaign Sender',
                email: 'campaign_sender@example.com',
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

    let campaignId;

    test('POST /api/v1/campaigns - Should create a new DRAFT campaign', async () => {
        const res = await request(app)
            .post('/api/v1/campaigns')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                name: 'Test Campaign',
                senderAccountId: senderId,
                subject: 'Hello Campaign',
                bodyText: 'This is a test campaign'
            });

        expect(res.statusCode).toBe(201);
        expect(res.body.success).toBe(true);
        expect(res.body.data.status).toBe('DRAFT');
        campaignId = res.body.data.id;
    });

    test('POST /api/v1/campaigns/:id/recipients - Should add recipients to draft', async () => {
        const res = await request(app)
            .post(`/api/v1/campaigns/${campaignId}/recipients`)
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                recipients: ['rec1@example.com', 'rec2@example.com', 'rec1@example.com'] // 1 duplicate
            });

        expect(res.statusCode).toBe(200);
        expect(res.body.data.added).toBe(2);
    });

    test('GET campaign details filters recipient page without changing aggregate stats', async () => {
        const res = await request(app)
            .get(`/api/v1/campaigns/${campaignId}?recipientSearch=rec2%40example.com&recipientPage=1&recipientLimit=1`)
            .set('Authorization', `Bearer ${accessToken}`);
        expect(res.statusCode).toBe(200);
        expect(res.body.data.recipients).toHaveLength(1);
        expect(res.body.data.recipients[0].recipient).toBe('rec2@example.com');
        expect(res.body.data.stats.total).toBe(2);
    });

    test('GET /api/v1/campaigns/:id - Should return campaign details with stats', async () => {
        const res = await request(app)
            .get(`/api/v1/campaigns/${campaignId}`)
            .set('Authorization', `Bearer ${accessToken}`);

        expect(res.statusCode).toBe(200);
        expect(res.body.data.stats.total).toBe(2);
    });

    test('GET campaign details filters recipients by email and honors paging', async () => {
        const res = await request(app)
            .get(`/api/v1/campaigns/${campaignId}?recipientSearch=rec2%40example.com&recipientPage=1&recipientLimit=1`)
            .set('Authorization', `Bearer ${accessToken}`);
        expect(res.statusCode).toBe(200);
        expect(res.body.data.recipients).toHaveLength(1);
        expect(res.body.data.recipients[0].recipient).toBe('rec2@example.com');
        expect(res.body.data.stats.total).toBe(2);
    });

    test('POST /api/v1/campaigns/:id/ready - Should transition to READY', async () => {
        const res = await request(app)
            .post(`/api/v1/campaigns/${campaignId}/ready`)
            .set('Authorization', `Bearer ${accessToken}`);

        expect(res.statusCode).toBe(200);
        expect(res.body.data.status).toBe('READY');
    });

    test('PATCH /api/v1/campaigns/:id - Should reject update for non-DRAFT', async () => {
        const res = await request(app)
            .patch(`/api/v1/campaigns/${campaignId}`)
            .set('Authorization', `Bearer ${accessToken}`)
            .send({ name: 'New Name' });

        expect(res.statusCode).toBe(409);
    });

    test('POST /api/v1/campaigns/:id/start - Should transition to RUNNING', async () => {
        const res = await request(app)
            .post(`/api/v1/campaigns/${campaignId}/start`)
            .set('Authorization', `Bearer ${accessToken}`);

        expect(res.statusCode).toBe(200);
        expect(res.body.data.status).toBe('RUNNING');
    });

    test('POST /api/v1/campaigns/:id/pause - Should transition to PAUSED', async () => {
        const res = await request(app)
            .post(`/api/v1/campaigns/${campaignId}/pause`)
            .set('Authorization', `Bearer ${accessToken}`);

        expect(res.statusCode).toBe(200);
        expect(res.body.data.status).toBe('PAUSED');
    });

    test('POST /api/v1/campaigns/:id/cancel - Should transition to CANCELLED', async () => {
        const res = await request(app)
            .post(`/api/v1/campaigns/${campaignId}/cancel`)
            .set('Authorization', `Bearer ${accessToken}`);

        expect(res.statusCode).toBe(200);
        expect(res.body.data.status).toBe('CANCELLED');

        // Check recipients
        const campaign = await prisma.campaign.findUnique({
            where: { id: campaignId },
            include: { recipients: true }
        });
        expect(campaign.recipients.every(r => r.status === 'CANCELLED')).toBe(true);
    });

    test('User Isolation - Should not see others campaigns', async () => {
        const userB = { name: 'User B', email: 'userb_camp@example.com', password: 'Password123!' };
        await prisma.user.deleteMany({ where: { email: userB.email } });
        const regB = await request(app).post('/api/v1/auth/register').send(userB);
        const tokenB = regB.body.data.accessToken;

        const res = await request(app)
            .get('/api/v1/campaigns')
            .set('Authorization', `Bearer ${tokenB}`);

        expect(res.body.data.campaigns.length).toBe(0);
        await prisma.user.deleteMany({ where: { email: userB.email } });
    });
});
