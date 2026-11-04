# 🚀 ReachInbox AI (Mobile App Development Project)

[![Android](https://img.shields.io/badge/Android-Java-green.svg)]()
[![Node.js](https://img.shields.io/badge/Node.js-Express-blue.svg)]()
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Prisma-blue.svg)]()
[![Redis](https://img.shields.io/badge/Redis-BullMQ-red.svg)]()
[![Gemini AI](https://img.shields.io/badge/AI-Google_Gemini-orange.svg)]()

**ReachInbox AI** is a comprehensive, production-ready AI-powered cold email outreach, candidate/job matching, and automated follow-up platform. It bridges a native Android client application with a highly scalable Node.js backend, orchestrating complex recruitment and sales outreach workflows using Google's Gemini AI.

---

## 👥 Team Members
Developed as a **Mobile App Development Project** by:
* **Prabhat Kumar**
* **Isha Kumari**
* **Abhishek Sharma**

---

## 📑 Table of Contents
1. [Core Features & Walkthrough](#-core-features--walkthrough)
2. [System Architecture](#-system-architecture)
3. [Technology Stack](#-technology-stack)
4. [Getting Started (Local Development)](#-getting-started-local-development)
5. [Inbound Webhook Configuration](#-inbound-webhook-configuration)
6. [Security & Isolation](#-security--isolation)

---

## ✨ Core Features & Walkthrough

### 1. Dashboard & Campaign Activity
Monitor your total sent, scheduled, and failed emails at a glance. Access detailed historical activity logs of all AI generations and email dispatches.

<img src="reachinbox-screenshot/dashboard.jpeg" width="300" alt="Dashboard Overview"/>
<br><em>> The main dashboard displaying high-level metrics and recent user activity.</em>

### 2. Resume Intelligence (PDF Parsing)
Upload standard or complex PDF resumes directly from your Android device. The backend securely extracts the text using `pdf2json` and utilizes **Gemini AI** to structure the candidate's profile (Skills, Education, Experience, Projects, Achievements).

<img src="reachinbox-screenshot/resume.jpeg" width="300" alt="Resume Upload and Parse"/>
<br><em>> Instant PDF parsing and AI structuring of candidate profiles.</em>

### 3. Job Requirement & AI Matching
Input a job description (or import via URL). ReachInbox AI performs a deterministic **Match Analysis** against the uploaded resume, identifying exact skill matches, missing requirements, potential gaps, and highly targeted personalization points to use in outreach.

<img src="reachinbox-screenshot/match-analysis.jpeg" width="300" alt="Job Matching Analysis"/>
<br><em>> Detailed gap analysis between the candidate's resume and the target job description.</em>

### 4. AI Personalized Outreach Generation
Generate perfectly grounded, professional cold emails. The AI synthesizes the resume data and job gaps to write an email that specifically highlights why the candidate is a perfect fit. Supports multiple tones (Professional, Concise, Friendly, Formal).

<img src="reachinbox-screenshot/outreach.jpeg" width="300" alt="Outreach Generation"/>
<br><em>> Generating a personalized outreach draft using AI.</em>

### 5. Seamless Email Compose, Send & Schedule
Edit AI-generated drafts in a fully-featured composer. Send immediately via Nodemailer (SMTP), or schedule the email for a future date. Background dispatching is handled reliably by **BullMQ** and **Redis**.

<img src="reachinbox-screenshot/compose-schedule.jpeg" width="300" alt="Compose and Schedule"/>
<br><em>> Reviewing the draft and setting a scheduled delivery time.</em>

### 6. Automated Follow-Ups & Threading
Schedule sequenced follow-up emails (e.g., "Polite Bump", "Final Wrap-up"). Follow-ups are automatically chained to the original parent email using `In-Reply-To` headers, preserving the conversation thread in the recipient's inbox.

<img src="reachinbox-screenshot/followup.jpeg" width="300" alt="Follow-Up Generation"/>
<br><em>> Generating a contextual follow-up sequence linked to the parent email.</em>

### 7. Inbound Reply Detection & Follow-up Suppression
ReachInbox listens for inbound replies via a provider-agnostic webhook. When a recipient replies, the system matches the thread and **automatically cancels any pending scheduled follow-ups**, ensuring you never accidentally bump a lead who has already responded.

### 8. AI Reply Intelligence
When a reply is received, the AI analyzes the incoming message to classify the intent (`INTERESTED`, `NOT_INTERESTED`, `INTERVIEW_RELATED`, etc.), summarizes the response, and generates an editable suggested reply draft for the user.

<img src="reachinbox-screenshot/reply-analysis.jpeg" width="300" alt="Reply Intelligence"/>
<br><em>> Viewing an inbound reply and utilizing AI to draft a suggested response.</em>

---

## 🏗 System Architecture

```text
                                ANDROID CLIENT
                               (Java / Views / Material3)
                                          │
                                     Retrofit / OkHttp
                                          │
                                          ▼
                                NODE.JS / EXPRESS API
                                          │
       ┌───────────────────────────┬──────┴────────────────────┬───────────────────────────┐
       │                           │                           │                           │
       ▼                           ▼                           ▼                           ▼
PostgreSQL / Prisma          Redis / BullMQ             Google Gemini AI           Inbound Webhook
(Users, Emails, Resumes,    (Email Scheduling &        (Resume Parsing, Job        (Inbound Email Parse &
 Jobs, Matches, Activities)   Worker Queues)            Matching, Outreach,         Reply Suppression)
       │                           │                    Follow-ups, Replies)
       │                           ▼
       └──────────────────► Nodemailer / SMTP
                                   │
                                   ▼
                            Recipient Inbox
```

---

## 💻 Technology Stack

**Mobile Client (Android)**
* Java, Android SDK (Min API 24, Target API 36)
* XML Layouts & Material Design 3
* Navigation Component & ViewBinding
* Retrofit2 & OkHttp3 (Networking)
* EncryptedSharedPreferences (Security)

**Backend API (Node.js)**
* Node.js & Express.js
* Prisma ORM
* Zod (Schema Validation)
* JSON Web Tokens (JWT) & bcrypt

**Infrastructure & Background Workers**
* PostgreSQL 16 (Relational Database)
* Redis 7 (In-memory Datastore)
* BullMQ (Robust Background Jobs / Scheduling)
* Nodemailer (SMTP Dispatch)
* Docker & Docker Compose

**Artificial Intelligence**
* Google Generative AI (`@google/generative-ai`)
* Gemini 1.5 Pro / Gemini 3.5 Flash Models

---

## 🚀 Getting Started (Local Development)

### 1. Start Infrastructure (PostgreSQL & Redis)
Ensure Docker Desktop is running, then spin up the database and Redis cache:
```bash
docker compose -f infrastructure/docker-compose.yml up -d
```

### 2. Backend Setup & Configuration
Navigate to the backend directory and copy the environment template:
```bash
cd backend
cp .env.example .env
```
Ensure your `.env` contains valid keys for your local setup:
```ini
PORT=5000
NODE_ENV=development

# Database
DATABASE_URL="postgresql://reachinbox_user:reachinbox_pass@localhost:5433/reachinbox_greenfield"

# Authentication & Security
JWT_SECRET="your_jwt_secret_here"
ENCRYPTION_KEY="your_32_character_secret_key_here" # Must be exactly 32 chars for AES-256

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Google Gemini AI
GEMINI_API_KEY="your_actual_gemini_api_key"
GEMINI_MODEL="gemini-1.5-pro"
```

Install dependencies, run migrations, and start the API:
```bash
npm install
npx prisma generate
npx prisma migrate dev
npm start
```

### 3. Start the Background Worker
In a new terminal window, start the BullMQ worker to process scheduled emails:
```bash
cd backend
npm run worker
```

### 4. Build the Android Application
Open the project in Android Studio, or build via Gradle:
```bash
./gradlew clean app:assembleDebug
```
*Note: Make sure `BASE_URL` in `app/build.gradle.kts` matches your backend's local network IP if debugging on a physical device over Wi-Fi (e.g., `http://192.168.x.x:5000/api/v1/`).*

---

## 🪝 Inbound Webhook Configuration

To enable **Reply Intelligence** and **Follow-Up Suppression**, configure your external mail provider (SendGrid Inbound Parse, Postmark, AWS SES, or custom IMAP poller) to `POST` to the following endpoint:

`POST http://<your-server-domain>:5000/api/v1/inbound/webhook`

**Required JSON Payload Schema:**
```json
{
  "messageId": "<unique-provider-message-id>",
  "inReplyTo": "<original-outbound-message-id>",
  "toEmail": "sender@yourdomain.com",
  "fromEmail": "recruiter@company.com",
  "subject": "Re: Application",
  "textBody": "Thanks for reaching out! Let's talk next Tuesday.",
  "htmlBody": "<p>Thanks for reaching out! Let's talk next Tuesday.</p>"
}
```

---

## 🔒 Security & Isolation

- **Tenant Isolation**: Every database query explicitly checks `userId` or `senderAccount.userId`.
- **Secret Management**: Gemini API keys, JWT secrets, and Database credentials exist strictly on the backend.
- **Data Encryption**: SMTP credentials are encrypted at rest using AES-256-GCM. Android tokens are encrypted using `EncryptedSharedPreferences`.
- **Prompt Injection Defense**: User instructions and raw job descriptions are securely bounded in prompt delimiters and treated strictly as untrusted data. AI output is parsed and strictly validated against Zod schemas.
- **Mandatory User Approval**: AI-generated outreach or replies are NEVER dispatched autonomously. Explicit user review, editing, and confirmation are strictly enforced.

---
*Developed for ReachInbox AI.*
