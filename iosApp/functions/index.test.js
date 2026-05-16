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

test("due alerts are scoped to chores scheduled for the current date", () => {
  const plan = _private.buildDerivedNotificationPlan({
    householdId: "house-1",
    todayDateKey: "2026-04-13",
    nowEpochMs: 9000,
    members: [{ userId: "owner-1", displayName: "Erick", role: "Owner", active: true }],
    chores: [
      {
        id: "weekly-tuesday",
        title: "Trash bins",
        assignee: "Erick",
        assigneeId: "owner-1",
        isActive: true,
        pendingApproval: false,
        repeatsEnabled: true,
        repeatStartDate: "Today",
        repeatFrequency: "Weekly",
        selectedWeekdays: ["Tuesday"],
        status: "DueSoon",
      },
    ],
    decisions: [],
    existingNotifications: [],
  });

  assert.equal(plan.upserts.length, 0);
});

test("stale completed status resets for the next date occurrence", () => {
  const plan = _private.buildDerivedNotificationPlan({
    householdId: "house-1",
    todayDateKey: "2026-04-13",
    nowEpochMs: 10000,
    members: [{ userId: "owner-1", displayName: "Erick", role: "Owner", active: true }],
    chores: [
      {
        id: "daily-reset",
        title: "Kitchen reset",
        assignee: "Erick",
        assigneeId: "owner-1",
        isActive: true,
        pendingApproval: false,
        repeatsEnabled: true,
        repeatStartDate: "Today",
        repeatFrequency: "Daily",
        dailyRepeatMode: "EveryDay",
        status: "Completed",
        dueLabel: "Completed",
        completedAtEpochMs: Date.parse("2026-04-12T16:00:00.000Z"),
      },
    ],
    decisions: [],
    existingNotifications: [],
  });

  assert.equal(plan.upserts.length, 1);
  assert.equal(plan.upserts[0].id, "due-soon|house-1|daily-reset|2026-04-13");
  assert.equal(plan.upserts[0].type, "DueSoon");
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

test("feedback pal triage classifies blocking bugs above ideas", () => {
  const bug = _private.classifyFeedbackPalSubmission({
    reportedIntent: "Bug",
    message: "The app crashed when I deleted a household and now chores are missing.",
    screenRoute: "Family",
  });
  const idea = _private.classifyFeedbackPalSubmission({
    reportedIntent: "FeatureIdea",
    message: "It would be nice to add a weekly summary export later.",
    screenRoute: "Reports",
  });

  assert.equal(bug.type, "Bug");
  assert.equal(bug.priority, "P0");
  assert.ok(bug.priorityScore > idea.priorityScore);
  assert.ok(bug.signals.includes("blocking failure"));
  assert.equal(idea.type, "Idea");
});

test("feedback pal triage lowers effort for small polish items", () => {
  const result = _private.classifyFeedbackPalSubmission({
    reportedIntent: "ConfusingFlow",
    message: "The label and icon padding looks off on the Home tab.",
    screenRoute: "Home",
  });

  assert.equal(result.type, "ConfusingFlow");
  assert.equal(result.effort, "Quick");
  assert.ok(result.signals.includes("likely quick polish"));
});

test("feedback pal Linear sync only runs once for new unsynced submissions", () => {
  const after = {
    exists: true,
    data: () => ({
      source: "FeedbackPal",
      message: "The home date carousel is confusing when I scroll.",
    }),
  };

  assert.equal(_private.shouldSyncFeedbackItemToLinear(null, after), true);
  assert.equal(_private.shouldSyncFeedbackItemToLinear({ exists: true }, after), false);
  assert.equal(
    _private.shouldSyncFeedbackItemToLinear(
      null,
      {
        exists: true,
        data: () => ({
          source: "FeedbackPal",
          message: "The home date carousel is confusing when I scroll.",
          linearIssueId: "lin-1",
        }),
      },
    ),
    false,
  );
});

test("feedback pal Linear target resolves the Symphony project slug and team", () => {
  const project = _private.resolveLinearProjectFromNodes(
    [
      {
        id: "project-other",
        name: "Other",
        url: "https://linear.app/sorto/project/other-project-aabbcc",
        teams: { nodes: [{ id: "team-1", key: "APP", name: "App" }] },
      },
      {
        id: "project-chorepilot",
        name: "ChorePilot KMM App",
        url: "https://linear.app/sorto/project/chorepilot-kmm-app-eb2390df7ac3",
        teams: { nodes: [{ id: "team-2", key: "CHO", name: "ChorePilot" }] },
      },
    ],
    "chorepilot-kmm-app-eb2390df7ac3",
  );
  const team = _private.resolveLinearTeamForProject(project);

  assert.equal(project.id, "project-chorepilot");
  assert.equal(team.id, "team-2");
});

test("feedback pal Linear issue payload maps priority and preserves context", () => {
  const payload = _private.buildLinearIssuePayload({
    feedback: {
      householdName: "Sorto House",
      userId: "user-1",
      userName: "Erick",
      palKind: "Penguin",
      reportedIntent: "Bug",
      screenRoute: "Home",
      message: "The app crashes after I complete all chores.",
      type: "Bug",
      priority: "P0",
      priorityScore: 92,
      effort: "Medium",
      signals: ["blocking failure", "core chore workflow"],
    },
    householdId: "house-1",
    feedbackId: "feedback-1",
    firestorePath: "households/house-1/feedbackItems/feedback-1",
    target: {
      teamId: "team-1",
      teamKey: "CHO",
      projectId: "project-1",
      projectName: "ChorePilot",
    },
  });

  assert.equal(payload.input.teamId, "team-1");
  assert.equal(payload.input.projectId, "project-1");
  assert.equal(payload.input.priority, 1);
  assert.match(payload.input.title, /^\[P0\] Bug: Investigate crash or blocked state on Home/);
  assert.match(payload.input.description, /Submitted from ChorePilot FeedbackPal/);
  assert.match(payload.input.description, /## Problem/);
  assert.match(payload.input.description, /## Next step/);
  assert.match(payload.input.description, /## Acceptance criteria/);
  assert.match(payload.input.description, /"firestorePath": "households\/house-1\/feedbackItems\/feedback-1"/);
});

test("feedback pal readable ticket turns raw reports into product-ticket details", () => {
  const ticket = _private.buildReadableFeedbackTicket({
    reportedIntent: "Bug",
    message: "I tried sending the bug but it says unauthenticated.",
    screenRoute: "FeedbackPal",
    userName: "Erick",
    householdName: "Sorto",
    triage: {
      type: "Bug",
      signals: ["blocking failure"],
    },
  });

  assert.equal(ticket.title, "Bug: Fix authentication or permission failure on FeedbackPal");
  assert.match(ticket.problem, /Erick from Sorto reported a bug on FeedbackPal/);
  assert.match(ticket.nextStep, /Firebase user/);
  assert.ok(ticket.acceptanceCriteria.some((item) => item.includes("root cause")));
});
