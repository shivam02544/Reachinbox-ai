const crypto = require('crypto');

const ALGORITHM = 'aes-256-gcm';
const IV_LENGTH = 16;
const SALT_LENGTH = 64;
const TAG_LENGTH = 16;

/**
 * Encrypts a string using AES-256-GCM
 * @param {string} text Plaintext to encrypt
 * @returns {string} Encrypted string in format: salt:iv:encryptedText:authTag
 */
const encrypt = (text) => {
    const secretKey = process.env.ENCRYPTION_KEY;
    if (!secretKey || secretKey.length < 32) {
        throw new Error('ENCRYPTION_KEY must be at least 32 characters');
    }

    const iv = crypto.randomBytes(IV_LENGTH);
    const salt = crypto.randomBytes(SALT_LENGTH);

    // Derive key using PBKDF2
    const key = crypto.pbkdf2Sync(secretKey, salt, 100000, 32, 'sha256');

    const cipher = crypto.createCipheriv(ALGORITHM, key, iv);

    let encrypted = cipher.update(text, 'utf8', 'hex');
    encrypted += cipher.final('hex');

    const authTag = cipher.getAuthTag().toString('hex');

    return `${salt.toString('hex')}:${iv.toString('hex')}:${encrypted}:${authTag}`;
};

/**
 * Decrypts a string using AES-256-GCM
 * @param {string} encryptedData Encrypted string in format: salt:iv:encryptedText:authTag
 * @returns {string} Decrypted plaintext
 */
const decrypt = (encryptedData) => {
    const secretKey = process.env.ENCRYPTION_KEY;
    if (!secretKey || secretKey.length < 32) {
        throw new Error('ENCRYPTION_KEY must be at least 32 characters');
    }
    const parts = encryptedData.split(':');

    if (parts.length !== 4) {
        throw new Error('Invalid encrypted data format');
    }

    const salt = Buffer.from(parts[0], 'hex');
    const iv = Buffer.from(parts[1], 'hex');
    const encryptedText = parts[2];
    const authTag = Buffer.from(parts[3], 'hex');

    const key = crypto.pbkdf2Sync(secretKey, salt, 100000, 32, 'sha256');

    const decipher = crypto.createDecipheriv(ALGORITHM, key, iv);
    decipher.setAuthTag(authTag);

    let decrypted = decipher.update(encryptedText, 'hex', 'utf8');
    decrypted += decipher.final('utf8');

    return decrypted;
};

module.exports = {
    encrypt,
    decrypt
};
