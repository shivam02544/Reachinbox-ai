const emailRepository = require('../repositories/emailRepository');
const senderRepository = require('../repositories/senderRepository');
const smtpService = require('./smtpService');
const queueService = require('./queueService');
const prisma = require('../utils/prisma');

const sendEmailImmediate = async (userId, emailData) => {
    const { senderAccountId, recipient, subject, htmlBody, textBody, idempotencyKey, parentEmailId, followUpNumber } = emailData;

    // 1. Check Idempotency
    if (idempotencyKey) {
        const existingEmail = await emailRepository.findEmailByIdempotencyKey(userId, idempotencyKey);
        if (existingEmail) {
            return existingEmail;
        }
    }

    // 2. Verify Sender Ownership and Status
    const sender = await senderRepository.findSenderById(senderAccountId, userId);
    if (!sender) {
        const error = new Error('Sender account not found or access denied');
        error.statusCode = 404;
        error.code = 'SENDER_NOT_FOUND';
        throw error;
    }

    if (sender.status !== 'CONNECTED') {
        const error = new Error('Sender account is not connected. Please verify it first.');
        error.statusCode = 400;
        error.code = 'SENDER_NOT_CONNECTED';
        throw error;
    }

    // 3. Create Email Record (with Unique Constraint Protection)
    let email;
    try {
        email = await emailRepository.createEmail({
            userId,
            senderAccountId,
            recipient,
            subject,
            htmlBody,
            textBody,
            status: 'PROCESSING',
            idempotencyKey,
            parentEmailId,
            followUpNumber
        });
    } catch (error) {
        // Handle unique constraint violation (P2002) for idempotencyKey
        if (error.code === 'P2002') {
            return await emailRepository.findEmailByIdempotencyKey(userId, idempotencyKey);
        }
        throw error;
    }

    try {
        // 4. Attempt SMTP send
        const sendResult = await smtpService.sendEmail(sender, { recipient, subject, htmlBody, textBody });

        // 5. Update status to SENT & store providerMessageId
        const updatedEmail = await prisma.email.update({
            where: { id: email.id },
            data: {
                status: 'SENT',
                providerMessageId: sendResult.messageId
            }
        });

        // 6. Log activity
        await prisma.activity.create({
            data: {
                userId,
                emailId: email.id,
                type: 'EMAIL_SENT',
                message: `Email sent to ${recipient}`
            }
        });

        return updatedEmail;
    } catch (error) {
        // Update status to FAILED
        await emailRepository.updateEmailStatus(email.id, 'FAILED');

        // Log activity
        await prisma.activity.create({
            data: {
                userId,
                emailId: email.id,
                type: 'EMAIL_FAILED',
                message: `Failed to send email to ${recipient}: ${error.message}`
            }
        });

        throw error;
    }
};

const saveDraft = async (userId, draftData) => {
    return await emailRepository.createEmail({
        ...draftData,
        userId,
        status: 'DRAFT'
    });
};

const getDrafts = async (userId, page = 1, limit = 20) => {
    const finalLimit = Math.min(limit, 100);
    const skip = (page - 1) * finalLimit;
    const [drafts, total] = await Promise.all([
        emailRepository.findDraftsByUserId(userId, skip, finalLimit),
        emailRepository.countDraftsByUserId(userId)
    ]);

    return {
        drafts,
        pagination: {
            total,
            page,
            limit: finalLimit,
            totalPages: Math.ceil(total / finalLimit)
        }
    };
};

const updateDraft = async (id, userId, data) => {
    return await emailRepository.updateDraft(id, userId, data);
};

const deleteDraft = async (id, userId) => {
    return await emailRepository.deleteDraft(id, userId);
};

const sendDraft = async (id, userId, idempotencyKey) => {
    const draft = await emailRepository.findEmailById(id, userId);
    if (!draft || draft.status !== 'DRAFT') {
        const error = new Error('Draft not found or invalid status');
        error.statusCode = 404;
        error.code = 'DRAFT_NOT_FOUND';
        throw error;
    }

    // Check if draft has required fields
    if (!draft.recipient || !draft.subject || (!draft.htmlBody && !draft.textBody)) {
        const error = new Error('Recipient, subject, and body are required to send');
        error.statusCode = 400;
        error.code = 'INCOMPLETE_DRAFT';
        throw error;
    }

    // Convert draft to send immediate request
    return await sendEmailImmediate(userId, {
        senderAccountId: draft.senderAccountId,
        recipient: draft.recipient,
        subject: draft.subject,
        htmlBody: draft.htmlBody,
        textBody: draft.textBody,
        idempotencyKey,
        parentEmailId: draft.parentEmailId,
        followUpNumber: draft.followUpNumber
    });
};

// Re-using existing scheduleEmail but modified to handle draft transition if needed
// Actually, it's better to have a dedicated scheduleDraft or just update the draft and then schedule.
const scheduleDraft = async (id, userId, scheduledAt) => {
    const draft = await emailRepository.findEmailById(id, userId);
    if (!draft || draft.status !== 'DRAFT') {
        const error = new Error('Draft not found or invalid status');
        error.statusCode = 404;
        error.code = 'DRAFT_NOT_FOUND';
        throw error;
    }

    // Update draft data then schedule
    return await scheduleEmail(userId, {
        senderAccountId: draft.senderAccountId,
        recipient: draft.recipient,
        subject: draft.subject,
        htmlBody: draft.htmlBody,
        textBody: draft.textBody,
        scheduledAt,
        parentEmailId: draft.parentEmailId,
        followUpNumber: draft.followUpNumber
    });
};

const scheduleEmail = async (userId, emailData) => {
    const { senderAccountId, recipient, subject, htmlBody, textBody, scheduledAt, parentEmailId, followUpNumber } = emailData;
    const scheduledDate = new Date(scheduledAt);

    if (scheduledDate <= new Date()) {
        const error = new Error('Scheduled time must be in the future');
        error.statusCode = 400;
        error.code = 'INVALID_SCHEDULED_TIME';
        throw error;
    }

    // 1. Verify Sender
    const sender = await senderRepository.findSenderById(senderAccountId, userId);
    if (!sender || sender.status !== 'CONNECTED') {
        const error = new Error('Valid connected sender account required');
        error.statusCode = 400;
        error.code = 'INVALID_SENDER';
        throw error;
    }

    // 2. Database transaction to create records
    return await prisma.$transaction(async (tx) => {
        const email = await tx.email.create({
            data: {
                userId,
                senderAccountId,
                recipient,
                subject,
                htmlBody,
                textBody,
                status: 'SCHEDULED',
                parentEmailId,
                followUpNumber
            }
        });

        const scheduledEmail = await tx.scheduledEmail.create({
            data: {
                emailId: email.id,
                scheduledAt: scheduledDate,
                status: 'SCHEDULED'
            }
        });

        await tx.activity.create({
            data: {
                userId,
                emailId: email.id,
                type: 'EMAIL_SCHEDULED',
                message: `Email scheduled for ${recipient} at ${scheduledAt}`
            }
        });

        // 3. Add to BullMQ
        try {
            await queueService.scheduleEmailJob(scheduledEmail.id, scheduledDate);
        } catch (queueError) {
            console.error('Failed to queue email job:', queueError);
            throw new Error('Failed to schedule email in queue');
        }

        return scheduledEmail;
    });
};

const cancelScheduledEmail = async (userId, scheduledEmailId) => {
    const scheduledEmail = await emailRepository.findScheduledEmailById(scheduledEmailId, userId);

    if (!scheduledEmail) {
        const error = new Error('Scheduled email not found');
        error.statusCode = 404;
        error.code = 'NOT_FOUND';
        throw error;
    }

    if (scheduledEmail.status !== 'SCHEDULED') {
        const error = new Error(`Cannot cancel email with status: ${scheduledEmail.status}`);
        error.statusCode = 409;
        error.code = 'INVALID_STATUS';
        throw error;
    }

    return await prisma.$transaction(async (tx) => {
        const updated = await tx.scheduledEmail.update({
            where: { id: scheduledEmailId },
            data: { status: 'CANCELLED' }
        });

        await tx.email.update({
            where: { id: scheduledEmail.emailId },
            data: { status: 'CANCELLED' }
        });

        await tx.activity.create({
            data: {
                userId,
                emailId: scheduledEmail.emailId,
                type: 'EMAIL_CANCELLED',
                message: `Scheduled email to ${scheduledEmail.email.recipient} cancelled`
            }
        });

        try {
            await queueService.cancelEmailJob(scheduledEmailId);
        } catch (e) {
            console.warn('Could not remove job from queue, but DB is updated:', e.message);
        }

        return updated;
    });
};

const getScheduledEmails = async (userId, page = 1, limit = 20) => {
    const finalLimit = Math.min(limit, 100);
    const skip = (page - 1) * finalLimit;
    const [scheduledEmails, total] = await Promise.all([
        emailRepository.findScheduledEmailsByUserId(userId, skip, finalLimit),
        emailRepository.countScheduledEmailsByUserId(userId)
    ]);

    return {
        scheduledEmails,
        pagination: {
            total,
            page,
            limit: finalLimit,
            totalPages: Math.ceil(total / finalLimit)
        }
    };
};

const getEmails = async (userId, query = {}) => {
    const page = parseInt(query.page) || 1;
    const limit = Math.min(parseInt(query.limit) || 20, 100);
    const sortBy = query.sortBy || 'createdAt';
    const sortOrder = query.sortOrder || 'desc';
    const filters = { ...query };
    delete filters.page;
    delete filters.limit;
    delete filters.sortBy;
    delete filters.sortOrder;

    const skip = (page - 1) * limit;
    const sort = { [sortBy]: sortOrder };

    const [emails, total] = await Promise.all([
        emailRepository.findEmailsByUserId(userId, filters, skip, limit, sort),
        emailRepository.countEmailsByUserId(userId, filters)
    ]);

    const totalPages = Math.ceil(total / limit);

    return {
        emails,
        pagination: {
            total,
            page,
            limit,
            totalPages,
            hasNextPage: page < totalPages,
            hasPreviousPage: page > 1
        }
    };
};

const getEmailDetails = async (id, userId) => {
    const email = await emailRepository.findEmailById(id, userId);
    if (!email) {
        const error = new Error('Email not found');
        error.statusCode = 404;
        error.code = 'NOT_FOUND';
        throw error;
    }
    return email;
};

const getEmailThread = async (id, userId) => {
    // 1. Get initial email
    const email = await emailRepository.findEmailById(id, userId);
    if (!email) {
        const error = new Error('Email not found');
        error.statusCode = 404;
        throw error;
    }

    const rootEmailId = email.parentEmailId || email.id;

    // 2. Fetch root email and all child follow-ups
    const outboundEmails = await prisma.email.findMany({
        where: {
            userId,
            OR: [
                { id: rootEmailId },
                { parentEmailId: rootEmailId }
            ]
        },
        orderBy: { createdAt: 'asc' },
        include: {
            senderAccount: {
                select: { id: true, displayName: true, email: true }
            },
            inboundEmails: true
        }
    });

    // 3. Fetch any inbound emails linked to any of these outbound emails
    const outboundEmailIds = outboundEmails.map(e => e.id);
    const inboundEmails = await prisma.inboundEmail.findMany({
        where: {
            matchedEmailId: { in: outboundEmailIds }
        },
        orderBy: { createdAt: 'asc' }
    });

    return {
        rootEmailId,
        outboundEmails,
        inboundEmails
    };
};

module.exports = {
    sendEmailImmediate,
    getEmails,
    getEmailDetails,
    getEmailThread,
    scheduleEmail,
    cancelScheduledEmail,
    getScheduledEmails,
    saveDraft,
    getDrafts,
    updateDraft,
    deleteDraft,
    sendDraft,
    scheduleDraft
};
