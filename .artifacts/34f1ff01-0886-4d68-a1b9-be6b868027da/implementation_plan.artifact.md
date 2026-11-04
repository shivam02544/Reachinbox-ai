# ReachInbox - System Optimization & Reliability Plan

This plan outlines the systematic technical improvements for the ReachInbox ecosystem, focusing on backend database performance, Android lifecycle reliability, and overall system scalability.

## User Review Required

> [!IMPORTANT]
> All existing development data (`reachinbox_greenfield`) and Docker volumes will be strictly preserved. No destructive database resets will be performed.

## Proposed Changes

### [Backend - Performance & Scaling Layer]

#### [MODIFY] [campaignRepository.js](file:///C:/Users/antes/AndroidStudioProjects/ReachInbox/backend/src/repositories/campaignRepository.js)
- Refactor `findCampaignById` to remove the eager loading of all `recipients`.
- Implement `findRecipientsByCampaignId` with pagination support (`skip`, `take`).
- Create `getBatchCampaignStats` to fetch statistics for multiple campaign IDs in a single query using Prisma's `groupBy`.

#### [MODIFY] [campaignService.js](file:///C:/Users/antes/AndroidStudioProjects/ReachInbox/backend/src/services/campaignService.js)
- Update `getCampaigns` to use the new batch statistics method, eliminating the **N+1 query bottleneck**.
- Update `getCampaignDetails` to handle lightweight campaign metadata and separate recipient list fetching.
- Add `retryFailedRecipients` logic to safely move `FAILED` records back to `PENDING`.

---

### [Android - Performance & Lifecycle Layer]

#### [MODIFY] [CampaignDetailsFragment.java](file:///C:/Users/antes/AndroidStudioProjects/ReachInbox/app/src/main/java/com/reachinbox/app/ui/campaign/CampaignDetailsFragment.java)
- Optimize `pollHandler` logic: automatically stop polling once the campaign status is `COMPLETED`, `CANCELLED`, or `FAILED`.
- Integrate paginated recipient loading for the detail view.

#### [MODIFY] [CampaignAdapter.java](file:///C:/Users/antes/AndroidStudioProjects/ReachInbox/app/src/main/java/com/reachinbox/app/adapter/CampaignAdapter.java) & [EmailAdapter.java](file:///C:/Users/antes/AndroidStudioProjects/ReachInbox/app/src/main/java/com/reachinbox/app/adapter/EmailAdapter.java)
- Replace `notifyDataSetChanged()` with `DiffUtil` for efficient, stutter-free list updates.

## Verification Plan

### Automated Tests
- Execute full backend test suite to ensure zero regressions in campaign lifecycle logic:
  `npm test -- --runInBand`

### Manual Verification
- Verify that the Campaign List loads significantly faster by monitoring Docker backend logs for reduced query counts.
- Verify that Android polling stops precisely when a campaign reaches a terminal state.
