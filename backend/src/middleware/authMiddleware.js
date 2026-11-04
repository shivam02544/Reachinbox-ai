const { verifyAccessToken } = require('../utils/jwt');
const authRepository = require('../repositories/authRepository');

const authMiddleware = async (req, res, next) => {
    try {
        const authHeader = req.headers.authorization;
        if (!authHeader || !authHeader.startsWith('Bearer ')) {
            throwAuthError('No token provided');
        }

        const token = authHeader.split(' ')[1];
        const decoded = verifyAccessToken(token);

        if (!decoded) {
            throwAuthError('Invalid or expired token');
        }

        // Find user to ensure they still exist and are active
        const user = await authRepository.findUserById(decoded.sub);
        if (!user || user.status !== 'ACTIVE') {
            throwAuthError('User not found or account inactive');
        }

        req.user = user;
        next();
    } catch (error) {
        next(error);
    }
};

const throwAuthError = (message) => {
    const error = new Error(message);
    error.statusCode = 401;
    error.code = 'UNAUTHORIZED';
    throw error;
};

module.exports = authMiddleware;
