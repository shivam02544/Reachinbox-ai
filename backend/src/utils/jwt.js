const jwt = require('jsonwebtoken');

const generateAccessToken = (user) => {
    const payload = {
        sub: user.id,
        email: user.email
    };

    const secret = process.env.JWT_SECRET;
    const expiresIn = process.env.JWT_ACCESS_EXPIRES_IN || '15m';

    return jwt.sign(payload, secret, { expiresIn });
};

const verifyAccessToken = (token) => {
    try {
        return jwt.verify(token, process.env.JWT_SECRET);
    } catch (error) {
        return null;
    }
};

module.exports = {
    generateAccessToken,
    verifyAccessToken
};
