const nodemailer = require('nodemailer');
const senderRepository = require('../repositories/senderRepository');
const prisma = require('../utils/prisma');
const { encrypt, decrypt } = require('../utils/encryption');

const addSender = async (userId, senderData) => {
    const { smtpPassword, ...data } = senderData;

    const encryptedSecret = encrypt(smtpPassword);

    const credentialData = {
        smtpHost: senderData.smtpHost,
        smtpPort: parseInt(senderData.smtpPort),
        smtpUsername: senderData.smtpUsername,
        encryptedSecret
    };

    const accountData = {
        displayName: data.displayName,
        email: data.email,
        provider: data.provider || 'SMTP',
        status: 'DISCONNECTED'
    };

    const sender = await senderRepository.createSender(userId, accountData, credentialData);

    // Log activity
    await prisma.activity.create({
        data: {
            userId,
            type: 'SENDER_CONNECTED',
            message: `New sender added: ${sender.email}`
        }
    });

    return sanitizeSender(sender);
};

const getSenders = async (userId) => {
    const senders = await senderRepository.findSendersByUserId(userId);
    return senders.map(sanitizeSender);
};

const getSender = async (id, userId) => {
    const sender = await senderRepository.findSenderById(id, userId);
    if (!sender) {
        const error = new Error('Sender not found');
        error.statusCode = 404;
        error.code = 'NOT_FOUND';
        throw error;
    }
    return sanitizeSender(sender);
};

const verifySender = async (id, userId) => {
    const sender = await senderRepository.findSenderById(id, userId);
    if (!sender || !sender.credential) {
        const error = new Error('Sender not found or missing credentials');
        error.statusCode = 404;
        error.code = 'NOT_FOUND';
        throw error;
    }

    const password = decrypt(sender.credential.encryptedSecret);

    const transporter = nodemailer.createTransport({
        host: sender.credential.smtpHost,
        port: sender.credential.smtpPort,
        secure: sender.credential.smtpPort === 465,
        auth: {
            user: sender.credential.smtpUsername,
            pass: password
        }
    });

    try {
        await transporter.verify();
        await senderRepository.updateSenderStatus(id, 'CONNECTED');
        return { status: 'CONNECTED' };
    } catch (error) {
        console.error('SMTP Verification failed:', error.message);
        await senderRepository.updateSenderStatus(id, 'ERROR');
        return { status: 'ERROR', message: 'Unable to connect to this sender. Check SMTP settings.' };
    }
};

const deleteSender = async (id, userId) => {
    const sender = await senderRepository.findSenderById(id, userId);
    if (!sender) {
        const error = new Error('Sender not found');
        error.statusCode = 404;
        error.code = 'NOT_FOUND';
        throw error;
    }

    await senderRepository.deleteSender(id, userId);

    // Log activity
    await prisma.activity.create({
        data: {
            userId,
            type: 'SENDER_DISCONNECTED',
            message: `Sender removed: ${sender.email}`
        }
    });
};

const sanitizeSender = (sender) => {
    const { credential, ...safeSender } = sender;
    return safeSender;
};

module.exports = {
    addSender,
    getSenders,
    getSender,
    verifySender,
    deleteSender
};
