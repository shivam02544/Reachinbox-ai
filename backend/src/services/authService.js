const authRepository = require('../repositories/authRepository');
const { hashPassword, comparePassword } = require('../utils/password');
const { generateAccessToken } = require('../utils/jwt');
const { generateRawToken, hashToken } = require('../utils/refreshToken');

const register = async (name, email, password) => {
    const existingUser = await authRepository.findUserByEmail(email);
    if (existingUser) {
        const error = new Error('Email already in use');
        error.statusCode = 400;
        error.code = 'DUPLICATE_EMAIL';
        throw error;
    }

    const passwordHash = await hashPassword(password);
    const user = await authRepository.createUser({
        name,
        email,
        passwordHash
    });

    const tokens = await issueTokens(user);

    return {
        user: sanitizeUser(user),
        ...tokens
    };
};

const login = async (email, password) => {
    const user = await authRepository.findUserByEmail(email);
    if (!user || user.status === 'DELETED') {
        throwInvalidCredentials();
    }

    if (user.status === 'SUSPENDED') {
        const error = new Error('Account is suspended');
        error.statusCode = 403;
        error.code = 'ACCOUNT_SUSPENDED';
        throw error;
    }

    const isPasswordValid = await comparePassword(password, user.passwordHash);
    if (!isPasswordValid) {
        throwInvalidCredentials();
    }

    const tokens = await issueTokens(user);

    return {
        user: sanitizeUser(user),
        ...tokens
    };
};

const refresh = async (rawRefreshToken) => {
    const tokenHash = hashToken(rawRefreshToken);
    const session = await authRepository.findRefreshTokenByHash(tokenHash);

    if (!session) {
        const error = new Error('Invalid refresh token');
        error.statusCode = 401;
        error.code = 'INVALID_REFRESH_TOKEN';
        throw error;
    }

    // Reuse detection
    if (session.revokedAt) {
        // Potential theft: Revoke all tokens for this user
        await authRepository.revokeRefreshTokensForUser(session.userId);
        const error = new Error('Refresh token has been revoked. All sessions invalidated.');
        error.statusCode = 401;
        error.code = 'REVOKED_REFRESH_TOKEN';
        throw error;
    }

    if (session.expiresAt < new Date()) {
        const error = new Error('Refresh token expired');
        error.statusCode = 401;
        error.code = 'EXPIRED_REFRESH_TOKEN';
        throw error;
    }

    // Rotate token
    await authRepository.updateRefreshToken(session.id, { revokedAt: new Date() });
    const newTokens = await issueTokens(session.user);

    return {
        ...newTokens,
        expiresIn: 900 // 15 mins in seconds for the access token
    };
};

const logout = async (rawRefreshToken) => {
    const tokenHash = hashToken(rawRefreshToken);
    const session = await authRepository.findRefreshTokenByHash(tokenHash);

    if (session && !session.revokedAt) {
        await authRepository.updateRefreshToken(session.id, { revokedAt: new Date() });
    }
};

const issueTokens = async (user) => {
    const accessToken = generateAccessToken(user);
    const rawRefreshToken = generateRawToken();
    const tokenHash = hashToken(rawRefreshToken);

    const days = parseInt(process.env.REFRESH_TOKEN_EXPIRES_DAYS) || 7;
    const expiresAt = new Date();
    expiresAt.setDate(expiresAt.getDate() + days);

    await authRepository.createRefreshToken({
        tokenHash,
        userId: user.id,
        expiresAt
    });

    return {
        accessToken,
        refreshToken: rawRefreshToken
    };
};

const sanitizeUser = (user) => {
    const { passwordHash, ...safeUser } = user;
    return safeUser;
};

const throwInvalidCredentials = () => {
    const error = new Error('Invalid email or password');
    error.statusCode = 401;
    error.code = 'INVALID_CREDENTIALS';
    throw error;
};

module.exports = {
    register,
    login,
    refresh,
    logout,
    sanitizeUser
};
