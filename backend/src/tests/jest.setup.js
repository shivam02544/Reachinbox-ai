require('dotenv').config();

// Override DATABASE_URL with TEST_DATABASE_URL for tests
if (process.env.TEST_DATABASE_URL) {
    process.env.DATABASE_URL = process.env.TEST_DATABASE_URL;
} else {
    console.error('CRITICAL ERROR: TEST_DATABASE_URL is not defined in .env');
    process.exit(1);
}

// Hard safety guard
const databaseUrl = process.env.DATABASE_URL || '';
if (databaseUrl.includes('reachinbox_greenfield')) {
    console.error('CRITICAL SAFETY ERROR: REFUSING TO RUN TESTS AGAINST DEVELOPMENT DATABASE (reachinbox_greenfield)');
    process.exit(1);
}

if (!databaseUrl.includes('reachinbox_test')) {
    console.error('CRITICAL SAFETY ERROR: DATABASE_URL must point to reachinbox_test during tests. Current URL: ' + databaseUrl);
    process.exit(1);
}

console.log('Test environment initialized. Using database:', databaseUrl.split('@')[1]);
