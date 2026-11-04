const prisma = require('../utils/prisma');
const geminiClient = require('../ai/geminiClient');
const PDFParser = require('pdf2json');

class ResumeService {
    /**
     * Uploads and analyzes a resume
     */
    async processResume(userId, file) {
        if (!file || file.mimetype !== 'application/pdf') {
            const error = new Error('Invalid file format. Only PDF is supported.');
            error.statusCode = 400;
            throw error;
        }

        if (file.size > 5 * 1024 * 1024) { // 5MB limit
            const error = new Error('File is too large. Maximum size is 5MB.');
            error.statusCode = 400;
            throw error;
        }

        // 1. Extract text from PDF using pdf2json
        let parsedText = await this._extractTextFromPDF(file.buffer);

        // Normalize text
        parsedText = (parsedText || '').replace(/\s+/g, ' ').trim();

        if (parsedText.length < 50) {
            const error = new Error('RESUME_TEXT_NOT_EXTRACTABLE: Insufficient text found in PDF (e.g., scanned image).');
            error.statusCode = 400;
            throw error;
        }

        // 2. Analyze with Gemini
        let structuredData = null;
        try {
            structuredData = await this._analyzeWithGemini(parsedText);
        } catch (err) {
            throw err;
        }

        // 3. Save to database
        const resumeData = {
            userId,
            fileName: file.originalname,
            mimeType: file.mimetype,
            parsedText,
            structuredData,
            status: 'READY'
        };

        const existingResume = await prisma.resume.findUnique({
            where: { userId }
        });

        if (existingResume) {
            return await prisma.resume.update({
                where: { id: existingResume.id },
                data: resumeData
            });
        } else {
            return await prisma.resume.create({
                data: resumeData
            });
        }
    }

    /**
     * Helper to extract text from PDF buffer using pdf2json
     */
    _extractTextFromPDF(buffer) {
        return new Promise((resolve, reject) => {
            const pdfParser = new PDFParser(this, 1); // 1 = returns raw text

            pdfParser.on("pdfParser_dataError", errData => {
                const errorMsg = errData.parserError ? errData.parserError.toString() : 'Unknown error';
                console.error('Failed to parse PDF:', errorMsg);

                const error = new Error('Could not read PDF file. It might be corrupted or password protected.');
                error.statusCode = 400;
                reject(error);
            });

            pdfParser.on("pdfParser_dataReady", pdfData => {
                resolve(pdfParser.getRawTextContent());
            });

            try {
                pdfParser.parseBuffer(buffer);
            } catch (err) {
                const error = new Error('Failed to parse PDF buffer.');
                error.statusCode = 400;
                reject(error);
            }
        });
    }

    /**
     * Helper to call Gemini and enforce JSON structure
     */
    async _analyzeWithGemini(text) {
        const prompt = `
You are an expert recruiter and resume parser.
Extract the following information from the provided resume text.
Output ONLY valid JSON matching this exact structure:
{
  "name": "Full Name",
  "email": "Email Address",
  "phone": "Phone Number",
  "summary": "Brief professional summary",
  "skills": ["skill1", "skill2"],
  "education": ["Degree/Institution"],
  "experience": ["Job Title at Company"],
  "projects": ["Project Name"],
  "certifications": ["Certification"],
  "achievements": ["Achievement"],
  "links": ["URL"]
}

If any information is not present, use null or an empty array [] as appropriate.
Do not invent or hallucinate information.

Resume Text:
---
${text.substring(0, 15000)}
---
`;
        const rawResponse = await geminiClient.generateContent(prompt);

        // Clean the response (sometimes Gemini wraps JSON in markdown blocks)
        let jsonStr = rawResponse.replace(/```json/g, '').replace(/```/g, '').trim();

        try {
            const parsed = JSON.parse(jsonStr);
            return parsed;
        } catch (err) {
            console.error('Failed to parse Gemini JSON output:', jsonStr);
            const error = new Error('AI returned an invalid structured response.');
            error.statusCode = 500;
            throw error;
        }
    }

    /**
     * Gets the user's resume
     */
    async getResume(userId) {
        return await prisma.resume.findUnique({
            where: { userId }
        });
    }

    /**
     * Deletes the user's resume
     */
    async deleteResume(userId) {
        try {
            await prisma.resume.delete({
                where: { userId }
            });
            return true;
        } catch (error) {
            // Prisma throws if not found
            if (error.code === 'P2025') {
                return false; // Not found, practically deleted
            }
            throw error;
        }
    }
}

module.exports = new ResumeService();
