-- CreateTable
CREATE TABLE "SenderCredential" (
    "id" UUID NOT NULL,
    "senderAccountId" UUID NOT NULL,
    "smtpHost" TEXT NOT NULL,
    "smtpPort" INTEGER NOT NULL,
    "smtpUsername" TEXT NOT NULL,
    "encryptedSecret" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "SenderCredential_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "SenderCredential_senderAccountId_key" ON "SenderCredential"("senderAccountId");

-- AddForeignKey
ALTER TABLE "SenderCredential" ADD CONSTRAINT "SenderCredential_senderAccountId_fkey" FOREIGN KEY ("senderAccountId") REFERENCES "SenderAccount"("id") ON DELETE CASCADE ON UPDATE CASCADE;
