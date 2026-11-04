-- Add email threading fields used for reply matching and follow-ups.
ALTER TABLE "Email"
    ADD COLUMN "providerMessageId" TEXT,
    ADD COLUMN "parentEmailId" UUID,
    ADD COLUMN "followUpNumber" INTEGER;

CREATE UNIQUE INDEX "Email_providerMessageId_key" ON "Email"("providerMessageId");
CREATE INDEX "Email_parentEmailId_idx" ON "Email"("parentEmailId");

ALTER TABLE "Email"
    ADD CONSTRAINT "Email_parentEmailId_fkey"
    FOREIGN KEY ("parentEmailId") REFERENCES "Email"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- Add resume, job requirement, and match analysis storage.
CREATE TABLE "Resume" (
    "id" UUID NOT NULL,
    "userId" UUID NOT NULL,
    "fileName" TEXT NOT NULL,
    "mimeType" TEXT NOT NULL,
    "parsedText" TEXT,
    "structuredData" JSONB,
    "status" TEXT NOT NULL DEFAULT 'UPLOADED',
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,
    CONSTRAINT "Resume_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "JobRequirement" (
    "id" UUID NOT NULL,
    "userId" UUID NOT NULL,
    "title" TEXT,
    "company" TEXT,
    "description" TEXT NOT NULL,
    "location" TEXT,
    "employmentType" TEXT,
    "additionalNotes" TEXT,
    "structuredData" JSONB,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,
    CONSTRAINT "JobRequirement_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "MatchAnalysis" (
    "id" UUID NOT NULL,
    "userId" UUID NOT NULL,
    "resumeId" UUID NOT NULL,
    "jobRequirementId" UUID NOT NULL,
    "result" JSONB NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,
    CONSTRAINT "MatchAnalysis_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "Resume_userId_key" ON "Resume"("userId");
CREATE INDEX "Resume_userId_idx" ON "Resume"("userId");
CREATE INDEX "JobRequirement_userId_idx" ON "JobRequirement"("userId");
CREATE INDEX "MatchAnalysis_userId_idx" ON "MatchAnalysis"("userId");
CREATE INDEX "MatchAnalysis_jobRequirementId_idx" ON "MatchAnalysis"("jobRequirementId");
CREATE UNIQUE INDEX "MatchAnalysis_resumeId_jobRequirementId_key" ON "MatchAnalysis"("resumeId", "jobRequirementId");

ALTER TABLE "Resume"
    ADD CONSTRAINT "Resume_userId_fkey"
    FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "JobRequirement"
    ADD CONSTRAINT "JobRequirement_userId_fkey"
    FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "MatchAnalysis"
    ADD CONSTRAINT "MatchAnalysis_userId_fkey"
    FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "MatchAnalysis"
    ADD CONSTRAINT "MatchAnalysis_resumeId_fkey"
    FOREIGN KEY ("resumeId") REFERENCES "Resume"("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "MatchAnalysis"
    ADD CONSTRAINT "MatchAnalysis_jobRequirementId_fkey"
    FOREIGN KEY ("jobRequirementId") REFERENCES "JobRequirement"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- Add inbound message storage for reply intelligence.
CREATE TABLE "InboundEmail" (
    "id" UUID NOT NULL,
    "messageId" TEXT NOT NULL,
    "inReplyTo" TEXT,
    "senderAccountId" UUID NOT NULL,
    "fromEmail" TEXT NOT NULL,
    "toEmail" TEXT NOT NULL,
    "subject" TEXT,
    "textBody" TEXT,
    "htmlBody" TEXT,
    "matchedEmailId" UUID,
    "analysis" JSONB,
    "receivedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "InboundEmail_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "InboundEmail_messageId_key" ON "InboundEmail"("messageId");
CREATE INDEX "InboundEmail_senderAccountId_idx" ON "InboundEmail"("senderAccountId");
CREATE INDEX "InboundEmail_matchedEmailId_idx" ON "InboundEmail"("matchedEmailId");
CREATE INDEX "InboundEmail_messageId_idx" ON "InboundEmail"("messageId");

ALTER TABLE "InboundEmail"
    ADD CONSTRAINT "InboundEmail_senderAccountId_fkey"
    FOREIGN KEY ("senderAccountId") REFERENCES "SenderAccount"("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "InboundEmail"
    ADD CONSTRAINT "InboundEmail_matchedEmailId_fkey"
    FOREIGN KEY ("matchedEmailId") REFERENCES "Email"("id") ON DELETE SET NULL ON UPDATE CASCADE;
