const prisma = require('../utils/prisma');

class InboundMailService {
    /**
     * Process normalized inbound email payload
     */
    async processInboundMail(payload) {
        const { messageId, inReplyTo, toEmail, fromEmail, subject, textBody, htmlBody } = payload;

        if (!messageId || !toEmail || !fromEmail) {
            const error = new Error('Incomplete inbound email payload');
            error.statusCode = 400;
            throw error;
        }

        // 1. Idempotency Check: Don't process duplicate inbound messages
        const existingInbound = await prisma.inboundEmail.findUnique({
            where: { messageId }
        });

        if (existingInbound) {
            return existingInbound;
        }

        // 2. Identify Sender Account
        const senderAccount = await prisma.senderAccount.findFirst({
            where: { email: { equals: toEmail, mode: 'insensitive' } }
        });

        if (!senderAccount) {
            console.warn(`[InboundMail] No matching sender account found for recipient: ${toEmail}`);
            const error = new Error('No matching sender account found');
            error.statusCode = 404;
            throw error;
        }

        // 3. Deterministic Thread Matching
        let matchedEmail = null;

        // Primary Match: In-Reply-To header matching providerMessageId
        if (inReplyTo) {
            matchedEmail = await prisma.email.findFirst({
                where: {
                    userId: senderAccount.userId,
                    providerMessageId: inReplyTo
                }
            });
        }

        // Fallback Match: Match recipient email and sender account
        if (!matchedEmail) {
            matchedEmail = await prisma.email.findFirst({
                where: {
                    userId: senderAccount.userId,
                    senderAccountId: senderAccount.id,
                    recipient: { equals: fromEmail, mode: 'insensitive' },
                    status: 'SENT'
                },
                orderBy: { createdAt: 'desc' }
            });
        }

        // 4. Save InboundEmail
        const inboundEmail = await prisma.inboundEmail.create({
            data: {
                messageId,
                inReplyTo,
                senderAccountId: senderAccount.id,
                fromEmail,
                toEmail,
                subject,
                textBody,
                htmlBody,
                matchedEmailId: matchedEmail ? matchedEmail.id : null
            }
        });

        // 5. Phase 5 Integration: Reply-Aware Follow-Up Suppression
        if (matchedEmail) {
            await this._suppressPendingFollowUps(senderAccount.userId, matchedEmail);

            // Log activity
            await prisma.activity.create({
                data: {
                    userId: senderAccount.userId,
                    emailId: matchedEmail.id,
                    type: 'EMAIL_SENT', // Use existing enum ActivityType
                    message: `Reply received from ${fromEmail} for email "${matchedEmail.subject}"`
                }
            });
        }

        return inboundEmail;
    }

    /**
     * Suppresses/Cancels all pending scheduled follow-ups for a conversation when a reply is received
     */
    async _suppressPendingFollowUps(userId, matchedEmail) {
        // Parent thread root email ID
        const rootEmailId = matchedEmail.parentEmailId || matchedEmail.id;

        // Find all child follow-ups (or self if root) that are currently scheduled
        const pendingFollowUps = await prisma.email.findMany({
            where: {
                userId,
                OR: [
                    { id: rootEmailId },
                    { parentEmailId: rootEmailId }
                ],
                status: 'SCHEDULED'
            },
            include: {
                scheduledEmails: {
                    where: { status: 'SCHEDULED' }
                }
            }
        });

        for (const followUp of pendingFollowUps) {
            // Update email status to SKIPPED
            await prisma.email.update({
                where: { id: followUp.id },
                data: { status: 'CANCELLED' } // Use existing EmailStatus enum
            });

            // Update scheduled email records
            for (const sched of followUp.scheduledEmails) {
                await prisma.scheduledEmail.update({
                    where: { id: sched.id },
                    data: { status: 'CANCELLED' }
                });
            }

            // Log activity
            await prisma.activity.create({
                data: {
                    userId,
                    emailId: followUp.id,
                    type: 'EMAIL_CANCELLED',
                    message: `Scheduled follow-up cancelled because recipient ${matchedEmail.recipient} replied.`
                }
            });
        }
    }
}

module.exports = new InboundMailService();
