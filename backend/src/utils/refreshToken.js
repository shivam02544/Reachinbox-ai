const crypto = require('crypto');

/**
 * Generates a cryptographically secure random token (32 bytes)
 * Returns the raw token as a base64url string for the client.
 */
const generateRawToken = () => {
    return crypto.randomBytes(32).toString('base64url');
};

/**
 * Hashes a raw refresh token using SHA-256 for database storage.
 */
const hashToken = (rawToken) => {
    return crypto.createHash('sha256').update(rawToken).digest('hex');
};

module.exports = {
    generateRawToken,
    hashToken
};
