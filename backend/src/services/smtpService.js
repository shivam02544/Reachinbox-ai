const nodemailer = require('nodemailer');
const { decrypt } = require('../utils/encryption');

/**
 * Service to handle actual email transport via SMTP
 */
const sendEmail = async (senderAccount, emailData) => {
    if (!senderAccount.credential) {
        throw new Error('Sender account is missing credentials');
    }

    const password = decrypt(senderAccount.credential.encryptedSecret);

    const transporter = nodemailer.createTransport({
        host: senderAccount.credential.smtpHost,
        port: senderAccount.credential.smtpPort,
        secure: senderAccount.credential.smtpPort === 465,
        auth: {
            user: senderAccount.credential.smtpUsername,
            pass: password
        }
    });

    const mailOptions = {
        from: `"${senderAccount.displayName}" <${senderAccount.email}>`,
        to: emailData.recipient,
        subject: emailData.subject,
        text: emailData.textBody,
        html: emailData.htmlBody
    };

    try {
        const info = await transporter.sendMail(mailOptions);
        console.log('Email sent: %s', info.messageId);
        return { success: true, messageId: info.messageId };
    } catch (error) {
        console.error('SMTP Error:', error.message);
        throw new Error('Failed to send email via SMTP');
    }
};

module.exports = {
    sendEmail
};
