# ReachInbox - System Optimization Pass 2 Plan

This second pass focuses on deepening system security, reliability (worker idempotency), and polishing the Android UI performance across all remaining modules.

## Proposed Changes

### [Backend - Security & Robustness Layer]

#### [NEW] [uuidValidator.js](file:///C:/Users/antes/AndroidStudioProjects/ReachInbox/backend/src/middleware/uuidValidator.js)
- Implement a global UUID validation middleware to catch malformed IDs in route parameters before they reach Prisma, ensuring controlled 400 responses instead of 500 errors.

#### [MODIFY] [authRoute.js](file:///C:/Users/antes/AndroidStudioProjects/ReachInbox/backend/src/routes/authRoute.js) & Others
- Integrate UUID validation into all user-owned resource routes.

---

### [Backend - Reliability & Scaling Layer]

#### [MODIFY] [campaignWorker.js](file:///C:/Users/antes/AndroidStudioProjects/ReachInbox/backend/src/workers/campaignWorker.js)
- **Enhanced Idempotency:** Refine `handleDelivery` to strictly skip SMTP sending if a terminal `Email` status (`SENT`, `FAILED`) is detected for the unique `idempotencyKey`.

#### [MODIFY] [Global Middleware]
- Implement a global maximum page `limit` (e.g., 100) across all paginated endpoints to prevent memory-exhaustion attacks.

---

### [Backend - New Features]

#### [MODIFY] [campaignService.js](file:///C:/Users/antes/AndroidStudioProjects/ReachInbox/backend/src/services/campaignService.js)
- **Retry Failed Recipients:** Add an endpoint and service method to move recipients from `FAILED` back to `PENDING` status for campaigns in the `READY` or `PAUSED` state.
- **Duplicate Detection:** Update `addRecipients` to return detailed counts of detected duplicates during import.

---

### [Android - Performance & UI Polish]

#### [MODIFY] [RecipientAdapter.java](file:///C:/Users/antes/AndroidStudioProjects/ReachInbox/app/src/main/java/com/reachinbox/app/adapter/RecipientAdapter.java) & Others
- Implement `DiffUtil` for high-performance list rendering.
- Externalize all hardcoded strings (e.g., "Error: ", "Sent at: ") to `strings.xml`.

#### [MODIFY] [TemplatesFragment.java](file:///C:/Users/antes/AndroidStudioProjects/ReachInbox/app/src/main/java/com/reachinbox/app/ui/settings/TemplatesFragment.java)
- Ensure `searchHandler` callbacks are safely removed in `onDestroyView`.

## Verification Plan

### Automated Verification
- Run the full test suite (43 baseline tests).
- Add new integration tests for malformed UUID handling and worker idempotency crash scenarios.

### Manual Verification
- Test IDOR scenarios: Attempt to access User B's resource with User A's token.
- Verify that malformed UUIDs in URLs return a clear 400 Bad Request.
- Verify that "Retry Failed" updates the Campaign Details UI correctly on the physical device.
