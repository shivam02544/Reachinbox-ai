const request = require('supertest');
const app = require('../app');
const prisma = require('../utils/prisma');
const { generateAccessToken } = require('../utils/jwt');

describe('Template API', () => {
    let user1, user2;
    let token1, token2;

    beforeAll(async () => {
        // Create test users
        user1 = await prisma.user.create({
            data: {
                email: 'template1@example.com',
                passwordHash: 'hash',
                name: 'User 1'
            }
        });
        token1 = generateAccessToken(user1);

        user2 = await prisma.user.create({
            data: {
                email: 'template2@example.com',
                passwordHash: 'hash',
                name: 'User 2'
            }
        });
        token2 = generateAccessToken(user2);
    });

    afterAll(async () => {
        if (user1) {
            await prisma.template.deleteMany({ where: { userId: user1.id } });
            await prisma.user.deleteMany({ where: { id: user1.id } });
        }
        if (user2) {
            await prisma.template.deleteMany({ where: { userId: user2.id } });
            await prisma.user.deleteMany({ where: { id: user2.id } });
        }
        await prisma.$disconnect();
    });

    describe('POST /api/v1/templates', () => {
        test('Should create a new template', async () => {
            const res = await request(app)
                .post('/api/v1/templates')
                .set('Authorization', `Bearer ${token1}`)
                .send({
                    name: 'Test Template',
                    subject: 'Hello {{name}}',
                    bodyText: 'This is a test body'
                });

            expect(res.statusCode).toBe(201);
            expect(res.body.success).toBe(true);
            expect(res.body.data.name).toBe('Test Template');
            expect(res.body.data.userId).toBe(user1.id);
        });

        test('Should fail without required fields', async () => {
            const res = await request(app)
                .post('/api/v1/templates')
                .set('Authorization', `Bearer ${token1}`)
                .send({
                    name: 'Test Template'
                });

            expect(res.statusCode).toBe(400);
            expect(res.body.success).toBe(false);
        });
    });

    describe('GET /api/v1/templates', () => {
        beforeAll(async () => {
            await prisma.template.createMany({
                data: [
                    { userId: user1.id, name: 'Template A', subject: 'Sub A', bodyText: 'Body A' },
                    { userId: user1.id, name: 'Template B', subject: 'Sub B', bodyText: 'Body B' },
                    { userId: user2.id, name: 'Other User Template', subject: 'Sub C', bodyText: 'Body C' }
                ]
            });
        });

        test('Should list templates for user 1', async () => {
            const res = await request(app)
                .get('/api/v1/templates')
                .set('Authorization', `Bearer ${token1}`);

            expect(res.statusCode).toBe(200);
            expect(res.body.success).toBe(true);
            // 1 from previous test + 2 from beforeAll
            expect(res.body.data.templates.length).toBe(3);
            expect(res.body.data.templates.every(t => t.userId === user1.id)).toBe(true);
        });

        test('Should support search', async () => {
            const res = await request(app)
                .get('/api/v1/templates?search=Template A')
                .set('Authorization', `Bearer ${token1}`);

            expect(res.statusCode).toBe(200);
            expect(res.body.data.templates.length).toBe(1);
            expect(res.body.data.templates[0].name).toBe('Template A');
        });
    });

    describe('GET /api/v1/templates/:id', () => {
        test('Should get template details', async () => {
            const template = await prisma.template.findFirst({ where: { userId: user1.id } });

            const res = await request(app)
                .get(`/api/v1/templates/${template.id}`)
                .set('Authorization', `Bearer ${token1}`);

            expect(res.statusCode).toBe(200);
            expect(res.body.data.id).toBe(template.id);
        });

        test('Should not allow user 2 to get user 1 template', async () => {
            const template = await prisma.template.findFirst({ where: { userId: user1.id } });

            const res = await request(app)
                .get(`/api/v1/templates/${template.id}`)
                .set('Authorization', `Bearer ${token2}`);

            expect(res.statusCode).toBe(404);
        });
    });

    describe('PATCH /api/v1/templates/:id', () => {
        test('Should update template', async () => {
            const template = await prisma.template.findFirst({ where: { userId: user1.id } });

            const res = await request(app)
                .patch(`/api/v1/templates/${template.id}`)
                .set('Authorization', `Bearer ${token1}`)
                .send({
                    name: 'Updated Name'
                });

            expect(res.statusCode).toBe(200);
            expect(res.body.data.name).toBe('Updated Name');
        });

        test('Should not allow cross-user update', async () => {
            const template = await prisma.template.findFirst({ where: { userId: user1.id } });

            const res = await request(app)
                .patch(`/api/v1/templates/${template.id}`)
                .set('Authorization', `Bearer ${token2}`)
                .send({
                    name: 'Evil Hack'
                });

            expect(res.statusCode).toBe(404);
        });
    });

    describe('DELETE /api/v1/templates/:id', () => {
        test('Should delete template', async () => {
            const template = await prisma.template.create({
                data: { userId: user1.id, name: 'To Delete', subject: 'S', bodyText: 'B' }
            });

            const res = await request(app)
                .delete(`/api/v1/templates/${template.id}`)
                .set('Authorization', `Bearer ${token1}`);

            expect(res.statusCode).toBe(200);

            const deleted = await prisma.template.findUnique({ where: { id: template.id } });
            expect(deleted).toBeNull();
        });

        test('Should not allow cross-user delete', async () => {
            const template = await prisma.template.findFirst({ where: { userId: user1.id } });

            const res = await request(app)
                .delete(`/api/v1/templates/${template.id}`)
                .set('Authorization', `Bearer ${token2}`);

            expect(res.statusCode).toBe(404);
        });
    });
});
