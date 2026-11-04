const request = require('supertest');
const app = require('../app');
const prisma = require('../utils/prisma');

describe('Campaign Completion Logic', () => {
    const testUser = {
        name: 'Completion User',
        email: 'completion_test@example.com',
        password: 'Password123!'
    };

    let accessToken;
    let senderId;
    let userId;

    beforeAll(async () => {
        await prisma.user.deleteMany({ where: { email: testUser.email } });
        const regRes = await request(app).post('/api/v1/auth/register').send(testUser);
        accessToken = regRes.body.data.accessToken;
        userId = regRes.body.data.user.id;

        const senderRes = await request(app)
            .post('/api/v1/senders')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                displayName: 'Completion Sender',
                email: 'completion_sender@example.com',
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
        await prisma.user.deleteMany({ where: { id: userId } });
        await prisma.$disconnect();
    });

    test('Campaign should transition to COMPLETED when all recipients are terminal', async () => {
        const campaign = await prisma.campaign.create({
            data: {
                userId,
                senderAccountId: senderId,
                name: 'Test Completion 1',
                subject: 'S',
                bodyText: 'B',
                status: 'RUNNING'
            }
        });

        await prisma.campaignRecipient.createMany({
            data: [
                { campaignId: campaign.id, recipient: 'rec1@example.com', status: 'SENT' },
                { campaignId: campaign.id, recipient: 'rec2@example.com', status: 'FAILED' },
                { campaignId: campaign.id, recipient: 'rec3@example.com', status: 'CANCELLED' }
            ]
        });

        const campaignService = require('../services/campaignService');
        await campaignService.checkCompletion(campaign.id, userId);

        const updated = await prisma.campaign.findUnique({ where: { id: campaign.id } });
        expect(updated.status).toBe('COMPLETED');
    });

    test('Campaign should remain RUNNING if at least one recipient is not terminal', async () => {
        const statuses = ['PENDING', 'QUEUED', 'PROCESSING'];

        for (const activeStatus of statuses) {
            const campaign = await prisma.campaign.create({
                data: {
                    userId,
                    senderAccountId: senderId,
                    name: `Test Remain Running ${activeStatus}`,
                    subject: 'S',
                    bodyText: 'B',
                    status: 'RUNNING'
                }
            });

            await prisma.campaignRecipient.createMany({
                data: [
                    { campaignId: campaign.id, recipient: 'rec1@example.com', status: 'SENT' },
                    { campaignId: campaign.id, recipient: 'rec2@example.com', status: activeStatus }
                ]
            });

            const campaignService = require('../services/campaignService');
            await campaignService.checkCompletion(campaign.id, userId);

            const updated = await prisma.campaign.findUnique({ where: { id: campaign.id } });
            expect(updated.status).toBe('RUNNING');
        }
    });

    test('User isolation - checkCompletion should not affect campaigns not belonging to user', async () => {
        const userB = await prisma.user.create({
            data: { email: 'userb@example.com', passwordHash: 'h', name: 'B' }
        });

        const campaign = await prisma.campaign.create({
            data: {
                userId: userB.id,
                senderAccountId: senderId,
                name: 'User B Campaign',
                subject: 'S',
                bodyText: 'B',
                status: 'RUNNING'
            }
        });

        await prisma.campaignRecipient.create({
            data: { campaignId: campaign.id, recipient: 'b@ex.com', status: 'SENT' }
        });

        const campaignService = require('../services/campaignService');
        // Calling with userA's ID for userB's campaign
        await campaignService.checkCompletion(campaign.id, userId);

        const updated = await prisma.campaign.findUnique({ where: { id: campaign.id } });
        expect(updated.status).toBe('RUNNING'); // Should not have completed because userId mismatch

        // Cleanup userB
        await prisma.user.deleteMany({ where: { id: userB.id } });
    });
});
