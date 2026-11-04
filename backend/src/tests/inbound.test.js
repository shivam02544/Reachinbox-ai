const request = require('supertest');
const app = require('../app');
const prisma = require('../utils/prisma');

jest.mock('../utils/prisma', () => ({
    inboundEmail: {
        findUnique: jest.fn(),
        create: jest.fn()
    },
    senderAccount: {
        findFirst: jest.fn()
    },
    email: {
        findFirst: jest.fn(),
        findMany: jest.fn(),
        update: jest.fn()
    },
    scheduledEmail: {
        update: jest.fn()
    },
    activity: {
        create: jest.fn()
    }
}));

describe('Inbound Mail Webhook & Reply Detection', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('POST /api/v1/inbound/webhook', () => {
        const validPayload = {
            messageId: '<msg-111@mail.com>',
            inReplyTo: '<outbound-111@reachinbox.ai>',
            toEmail: 'sender@reachinbox.ai',
            fromEmail: 'client@enterprise.com',
            subject: 'Re: Interested in software',
            textBody: 'Yes, I am interested!'
        };

        it('should return 400 for missing mandatory payload fields', async () => {
            const res = await request(app)
                .post('/api/v1/inbound/webhook')
                .send({ messageId: '<msg-111@mail.com>' });

            expect(res.statusCode).toBe(400);
            expect(res.body.success).toBe(false);
        });

        it('should return 404 if toEmail does not match any registered sender account', async () => {
            prisma.inboundEmail.findUnique.mockResolvedValueOnce(null); // Not duplicate
            prisma.senderAccount.findFirst.mockResolvedValueOnce(null); // No sender

            const res = await request(app)
                .post('/api/v1/inbound/webhook')
                .send(validPayload);

            expect(res.statusCode).toBe(404);
            expect(res.body.message).toContain('sender account');
        });

        it('should process inbound email, match thread, and suppress pending follow-ups', async () => {
            const mockSender = { id: 'sender-1', userId: 'user-1', email: 'sender@reachinbox.ai' };
            const mockMatchedEmail = { id: 'outbound-email-1', userId: 'user-1', recipient: 'client@enterprise.com', subject: 'Interested in software' };
            const mockInbound = { id: 'inbound-1', messageId: validPayload.messageId };

            prisma.inboundEmail.findUnique.mockResolvedValueOnce(null);
            prisma.senderAccount.findFirst.mockResolvedValueOnce(mockSender);
            prisma.email.findFirst.mockResolvedValueOnce(mockMatchedEmail); // Primary match
            prisma.inboundEmail.create.mockResolvedValueOnce(mockInbound);

            // Mock pending follow-ups check
            prisma.email.findMany.mockResolvedValueOnce([
                {
                    id: 'followup-email-1',
                    status: 'SCHEDULED',
                    scheduledEmails: [{ id: 'sched-1' }]
                }
            ]);

            const res = await request(app)
                .post('/api/v1/inbound/webhook')
                .send(validPayload);

            expect(res.statusCode).toBe(200);
            expect(res.body.success).toBe(true);
            expect(res.body.data.id).toBe('inbound-1');

            // Verify follow-up suppression was executed
            expect(prisma.email.update).toHaveBeenCalledWith({
                where: { id: 'followup-email-1' },
                data: { status: 'CANCELLED' }
            });
            expect(prisma.scheduledEmail.update).toHaveBeenCalledWith({
                where: { id: 'sched-1' },
                data: { status: 'CANCELLED' }
            });
        });
    });
});
