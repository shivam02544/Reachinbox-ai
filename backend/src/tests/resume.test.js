const request = require('supertest');
const app = require('../app');
const { generateAccessToken } = require('../utils/jwt');
const geminiClient = require('../ai/geminiClient');
const prisma = require('../utils/prisma');
const authRepository = require('../repositories/authRepository');

// Mock dependencies
jest.mock('../ai/geminiClient', () => {
    return {
        generateContent: jest.fn(),
        init: jest.fn(),
        getModel: jest.fn()
    };
});

jest.mock('../repositories/authRepository', () => {
    return {
        findUserById: jest.fn()
    };
});

// Mock Prisma
jest.mock('../utils/prisma', () => {
    return {
        resume: {
            findUnique: jest.fn(),
            update: jest.fn(),
            create: jest.fn(),
            delete: jest.fn(),
            deleteMany: jest.fn(),
        },
        user: {
            findUnique: jest.fn(),
            create: jest.fn(),
            delete: jest.fn(),
            deleteMany: jest.fn(),
        }
    };
});

jest.mock('pdf2json', () => {
    return class MockPDFParser {
        constructor() {
            this.listeners = {};
        }

        on(event, listener) {
            this.listeners[event] = listener;
        }

        parseBuffer(buffer) {
            const content = buffer.toString();
            if (content.includes('CORRUPT')) {
                this.listeners.pdfParser_dataError({ parserError: new Error('Invalid PDF') });
            } else {
                this.rawText = content.includes('EMPTY') || content.includes('SHORT')
                    ? '  \n  '
                    : 'Prabhat Kumar\nSoftware Engineer\nSkills: Java, Node.js\nExperience: 5 years';
                this.listeners.pdfParser_dataReady({});
            }
        }

        getRawTextContent() {
            return this.rawText;
        }
    };
});

describe('Resume API Endpoints', () => {
    let authToken;
    const mockUserId = '11111111-1111-1111-1111-111111111111';

    beforeAll(() => {
        authToken = generateAccessToken(mockUserId);

        authRepository.findUserById.mockResolvedValue({
            id: mockUserId,
            email: 'resume-test@example.com',
            status: 'ACTIVE'
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('POST /api/v1/ai/resume', () => {
        it('should return 401 if unauthenticated', async () => {
            const res = await request(app)
                .post('/api/v1/ai/resume')
                .attach('resume', Buffer.from('test'), 'test.pdf');

            expect(res.statusCode).toBe(401);
            expect(res.body.success).toBe(false);
        });

        it('should return 400 if no file is uploaded', async () => {
            const res = await request(app)
                .post('/api/v1/ai/resume')
                .set('Authorization', `Bearer ${authToken}`);

            expect(res.statusCode).toBe(400);
            expect(res.body.message).toContain('No file uploaded');
        });

        it('should return 500/400 if file is not PDF', async () => {
            const res = await request(app)
                .post('/api/v1/ai/resume')
                .set('Authorization', `Bearer ${authToken}`)
                .attach('resume', Buffer.from('test text'), { filename: 'test.txt', contentType: 'text/plain' });

            // Expected to be blocked by multer file filter
            expect(res.statusCode).toBe(500);
            expect(res.body.message).toContain('Only PDF is supported');
        });

        it('should return 400 if PDF contains insufficient text', async () => {
            const res = await request(app)
                .post('/api/v1/ai/resume')
                .set('Authorization', `Bearer ${authToken}`)
                .attach('resume', Buffer.from('SHORT'), { filename: 'empty.pdf', contentType: 'application/pdf' });

            expect(res.statusCode).toBe(400);
        });

        it('should upload, parse, and analyze valid PDF', async () => {
            const mockStructuredData = {
                name: 'Prabhat Kumar',
                skills: ['Java', 'Node.js'],
                experience: ['Software Engineer - 5 years']
            };
            geminiClient.generateContent.mockResolvedValueOnce(JSON.stringify(mockStructuredData));

            prisma.resume.findUnique.mockResolvedValueOnce(null); // No existing resume
            prisma.resume.create.mockResolvedValueOnce({
                id: 'resume-id-1',
                userId: mockUserId,
                fileName: 'resume.pdf',
                structuredData: mockStructuredData
            });

            const res = await request(app)
                .post('/api/v1/ai/resume')
                .set('Authorization', `Bearer ${authToken}`)
                .attach('resume', Buffer.from('VALID CONTENT'), { filename: 'resume.pdf', contentType: 'application/pdf' });

            expect(res.statusCode).toBe(200);
            expect(res.body.success).toBe(true);
            expect(res.body.data.fileName).toBe('resume.pdf');
            expect(res.body.data.structuredData).toEqual(mockStructuredData);
            expect(res.body.data.parsedText).toBeUndefined(); // Should be omitted

            expect(prisma.resume.create).toHaveBeenCalled();
        });
    });

    describe('GET /api/v1/ai/resume', () => {
        it('should retrieve the user resume', async () => {
            prisma.resume.findUnique.mockResolvedValueOnce({
                fileName: 'resume.pdf',
                structuredData: {}
            });

            const res = await request(app)
                .get('/api/v1/ai/resume')
                .set('Authorization', `Bearer ${authToken}`);

            expect(res.statusCode).toBe(200);
            expect(res.body.success).toBe(true);
            expect(res.body.data.fileName).toBe('resume.pdf');
        });
    });

    describe('DELETE /api/v1/ai/resume', () => {
        it('should delete the user resume', async () => {
            prisma.resume.delete.mockResolvedValueOnce(true);

            const res = await request(app)
                .delete('/api/v1/ai/resume')
                .set('Authorization', `Bearer ${authToken}`);

            expect(res.statusCode).toBe(200);
            expect(res.body.success).toBe(true);
        });
    });
});
