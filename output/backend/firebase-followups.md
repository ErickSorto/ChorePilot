# Firebase Follow-Ups

## Needed before production
- Add `google-services.json` to `composeApp/` so Android can initialize Firebase automatically.
- Add `GoogleService-Info.plist` to `iosApp/iosApp/` so iOS can initialize Firebase automatically.
- Lock down Firestore and Storage security rules around `households/{householdId}` membership.
- Add App Check once the basic flows are stable.

## Auth and household
- Add account creation UI for owners who want email/password instead of anonymous ownership.
- Add invite role selection so admins can generate member vs admin invite codes intentionally.
- Add invite expiration and revoke/regenerate controls in the family invite screen.
- Add anonymous-account upgrade flow for guests who later want a real login.

## Chore engine
- Persist real proof image uploads to Firebase Storage and store proof metadata under `households/{householdId}/proofs`.
- Replace the current save-time assignee resolution with a true recurrence engine that resolves weekly cycles over time.
- Add soft-delete/deactivate UI for chores so the new backend delete/deactivate logic is reachable from the app.
- Add server timestamps and audit metadata for chore creation, updates, approvals, and completions.

## Cost and scale
- Add pagination or day-scoped queries once household chore volume grows.
- Keep invite lookup as direct document reads by code to avoid collection scans.
- Consider denormalized household summary counters only after real usage proves they are needed.
