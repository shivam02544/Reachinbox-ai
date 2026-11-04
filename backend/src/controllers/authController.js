const authService = require('../services/authService');

const register = async (req, res, next) => {
    try {
        const { name, email, password } = req.body;
        const result = await authService.register(name, email, password);

        res.status(201).json({
            success: true,
            data: result
        });
    } catch (error) {
        next(error);
    }
};

const login = async (req, res, next) => {
    try {
        const { email, password } = req.body;
        const result = await authService.login(email, password);

        res.status(200).json({
            success: true,
            data: result
        });
    } catch (error) {
        next(error);
    }
};

const refresh = async (req, res, next) => {
    try {
        const { refreshToken } = req.body;
        const result = await authService.refresh(refreshToken);

        res.status(200).json({
            success: true,
            data: result
        });
    } catch (error) {
        next(error);
    }
};

const logout = async (req, res, next) => {
    try {
        const { refreshToken } = req.body;
        await authService.logout(refreshToken);

        res.status(200).json({
            success: true,
            message: 'Logged out successfully'
        });
    } catch (error) {
        // Logout should be idempotent and not fail even if token is invalid
        res.status(200).json({
            success: true,
            message: 'Logged out successfully'
        });
    }
};

const session = async (req, res, next) => {
    try {
        // User is attached by authMiddleware
        res.status(200).json({
            success: true,
            data: {
                user: authService.sanitizeUser(req.user)
            }
        });
    } catch (error) {
        next(error);
    }
};

module.exports = {
    register,
    login,
    refresh,
    logout,
    session
};
