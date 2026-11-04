const express = require('express');
const router = express.Router();
const multer = require('multer');
const resumeController = require('../controllers/resumeController');

// Configure multer for memory storage
const upload = multer({
    storage: multer.memoryStorage(),
    limits: {
        fileSize: 5 * 1024 * 1024 // 5MB limit
    },
    fileFilter: (req, file, cb) => {
        if (file.mimetype === 'application/pdf') {
            cb(null, true);
        } else {
            cb(new Error('Invalid file format. Only PDF is supported.'));
        }
    }
});

/**
 * @route   POST /api/v1/ai/resume
 * @desc    Upload and analyze resume
 * @access  Private
 */
router.post('/', upload.single('resume'), resumeController.uploadResume);

/**
 * @route   GET /api/v1/ai/resume
 * @desc    Get current user's resume
 * @access  Private
 */
router.get('/', resumeController.getResume);

/**
 * @route   DELETE /api/v1/ai/resume
 * @desc    Delete current user's resume
 * @access  Private
 */
router.delete('/', resumeController.deleteResume);

module.exports = router;
