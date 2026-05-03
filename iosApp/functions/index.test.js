const test = require("node:test");
const assert = require("node:assert/strict");

const { _private } = require("./index.js");

test("buildDerivedNotificationPlan suppresses duplicate due-soon notifications and preserves read state", () => {
  const plan = _private.buildDerivedNotificationPlan({
    householdId: "house-1",
    todayDateKey: "2026-04-13",
    nowEpochMs: 5000,
    members: [
      { userId: "owner-1", displayName: "Erick", role: "Owner", active: true },
      { userId: "member-1", displayName: "Emma", role: "Member", active: true },
    ],
    chores: [
      {
        id: "chore-1",
        title: "Dog walk",
        assignee: "Emma",
        assigneeId: "member-1",
        isActive: true,
        pendingApproval: false,
        status: "DueSoon",
        dueLabel: "Due before dinner",
      },
    ],
    decisions: [],
    existingNotifications: [
      {
        id: "due-soon|house-1|chore-1|2026-04-13",
        householdId: "house-1",
        choreId: "chore-1",
        type: "DueSoon",
        title: "Dog walk is due soon",
        body: "Emma is up next. Due before dinner",
        createdAtEpochMs: 1234,
        recipientUserIds: ["member-1", "owner-1"],
        readReceipts: [{ userId: "member-1", readAtEpochMs: 2222 }],
        ref: { delete: async () => undefined },
      },
    ],
  });

  assert.equal(plan.upserts.length, 1);
  assert.equal(plan.deletes.length, 0);
  assert.equal(plan.upserts[0].id, "due-soon|house-1|chore-1|2026-04-13");
  assert.equal(plan.upserts[0].createdAtEpochMs, 1234);
  assert.deepEqual(plan.upserts[0].readReceipts, [{ userId: "member-1", readAtEpochMs: 2222 }]);
});

test("due alerts target the assignee and managers only", () => {
  const recipients = _private.dueAlertRecipientUserIds(
    {
      assignee: "Emma",
      assigneeId: "member-1",
    },
    [
      { userId: "owner-1", displayName: "Erick", role: "Owner", active: true },
      { userId: "admin-1", displayName: "Mila", role: "Admin", active: true },
      { userId: "member-1", displayName: "Emma", role: "Member", active: true },
      { userId: "member-2", displayName: "Ava", role: "Member", active: true },
    ],
  );

  assert.deepEqual(recipients, ["admin-1", "member-1", "owner-1"]);
});

test("assignment alerts target the original member, new member, and managers", () => {
  const plan = _private.buildDerivedNotificationPlan({
    householdId: "house-1",
    todayDateKey: "2026-04-13",
    nowEpochMs: 7000,
    members: [
      { userId: "owner-1", displayName: "Erick", role: "Owner", active: true },
      { userId: "member-1", displayName: "Emma", role: "Member", active: true },
      { userId: "member-2", displayName: "Mila", role: "Member", active: true },
      { userId: "member-3", displayName: "Ava", role: "Member", active: true },
    ],
    chores: [
      {
        id: "bathroom",
        title: "Bathroom reset",
        assignee: "Emma",
        assigneeId: "member-1",
        repeatsEnabled: true,
        repeatStartDate: "Today",
        repeatFrequency: "Weekly",
        selectedWeekdays: ["Tuesday"],
        isActive: true,
        pendingApproval: false,
        status: "Scheduled",
      },
    ],
    decisions: [
      {
        choreId: "bathroom",
        originalAssignee: "Emma",
        coverMember: "Mila",
        scopeStartDate: "2026-04-14",
        scopeEndDate: "2026-04-14",
        status: "Accepted",
      },
    ],
    existingNotifications: [],
  });

  assert.equal(plan.upserts.length, 1);
  assert.deepEqual(plan.upserts[0].recipientUserIds, ["member-1", "member-2", "owner-1"]);
});

test("stale derived notifications are deleted when the chore is no longer due soon", () => {
  const staleRef = { delete: async () => undefined };
  const plan = _private.buildDerivedNotificationPlan({
    householdId: "house-1",
    todayDateKey: "2026-04-13",
    nowEpochMs: 8000,
    members: [{ userId: "owner-1", displayName: "Erick", role: "Owner", active: true }],
    chores: [
      {
        id: "chore-1",
        title: "Dog walk",
        assignee: "Erick",
        assigneeId: "owner-1",
        isActive: true,
        pendingApproval: false,
        status: "Completed",
      },
    ],
    decisions: [],
    existingNotifications: [
      {
        id: "due-soon|house-1|chore-1|2026-04-13",
        householdId: "house-1",
        choreId: "chore-1",
        type: "DueSoon",
        ref: staleRef,
      },
    ],
  });

  assert.equal(plan.upserts.length, 0);
  assert.equal(plan.deletes.length, 1);
  assert.equal(plan.deletes[0].ref, staleRef);
});

test("push delivery skips read receipt updates but sends on recipient changes", () => {
  assert.equal(
    _private.shouldSendNotificationPush(
      {
        type: "DueSoon",
        title: "Dog walk is due soon",
        body: "Emma is up next. Due before dinner",
        recipientUserIds: ["member-1"],
        readReceipts: [],
      },
      {
        type: "DueSoon",
        title: "Dog walk is due soon",
        body: "Emma is up next. Due before dinner",
        recipientUserIds: ["member-1"],
        readReceipts: [{ userId: "member-1", readAtEpochMs: 1 }],
      },
    ),
    false,
  );

  assert.equal(
    _private.shouldSendNotificationPush(
      {
        type: "DueSoon",
        title: "Dog walk is due soon",
        body: "Emma is up next. Due before dinner",
        recipientUserIds: ["member-1"],
      },
      {
        type: "DueSoon",
        title: "Dog walk is due soon",
        body: "Emma is up next. Due before dinner",
        recipientUserIds: ["member-1", "owner-1"],
      },
    ),
    true,
  );

  assert.equal(
    _private.shouldSendNotificationPush(
      {
        type: "DueSoon",
        title: "Dog walk is due soon",
        body: "Emma is up next. Due before dinner",
        recipientUserIds: ["member-1"],
      },
      {
        type: "DueSoon",
        title: "Dog walk is overdue",
        body: "Emma still owns this one. Missed yesterday",
        recipientUserIds: ["member-1"],
      },
    ),
    true,
  );
});
