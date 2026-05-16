const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");

const OPENAI_ENDPOINT = "https://api.openai.com/v1/responses";
const OPENAI_MODEL = "gpt-4.1-mini";
const LINEAR_ENDPOINT = "https://api.linear.app/graphql";
const LINEAR_PROJECT_SLUG = "chorepilot-kmm-app-eb2390df7ac3";
const PUSH_NOTIFICATION_CHANNEL_ID = "proof_lifecycle_updates";
const ANDROID_NOTIFICATION_ICON = "ic_stat_chorepilot_notification";
const ANDROID_NOTIFICATION_COLOR = "#5554B1";

if (!admin.apps.length) {
  admin.initializeApp();
}

const firestore = admin.firestore();
const storage = admin.storage();
const messaging = admin.messaging();

exports.analyzeDiscoveryRoom = onCall(
  {
    cors: true,
    timeoutSeconds: 60,
    memory: "512MiB",
    secrets: ["OPENAI_API_KEY"],
  },
  async (request) => {
    if (!request.auth?.uid) {
      throw new HttpsError("unauthenticated", "Sign in before using discovery mode.");
    }
    const apiKey = process.env.OPENAI_API_KEY;
    if (!apiKey) {
      throw new HttpsError("failed-precondition", "OPENAI_API_KEY is not configured for Cloud Functions.");
    }

    const data = request.data || {};
    const { householdId, householdName, userId, userName, mimeType, imageBase64 } = data;
    if (!householdId || !userId || !imageBase64 || !mimeType) {
      throw new HttpsError("invalid-argument", "Discovery analysis needs household, user, and image data.");
    }
    if (request.auth.uid !== userId) {
      throw new HttpsError("permission-denied", "You can only analyze discovery photos for yourself.");
    }
    const today = new Date();
    const todayIso = today.toISOString().slice(0, 10);
    const currentWeekday = new Intl.DateTimeFormat("en-US", { weekday: "long" }).format(today);
    const allowedOneTimeDates = ["Today", "Tomorrow", "This Friday", "Next Monday"];

    const response = await fetch(OPENAI_ENDPOINT, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${apiKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model: OPENAI_MODEL,
        input: [
          {
            role: "system",
            content: [
              {
                type: "input_text",
                text: [
                  "You analyze household room photos and turn visible mess or maintenance needs into chore drafts.",
                  "Return only realistic chores that someone could act on in this home.",
                  "Prefer 2 to 6 concise chores ordered from highest impact to lowest impact.",
                  "Keep chore titles short and scannable, ideally 2 to 5 words.",
                  "Do not put long explanations, multiple steps, or room condition details in the title.",
                  "Put any useful extra context in the description field instead.",
                  "Use existing enum values exactly when you choose them.",
                  `Today is ${todayIso} (${currentWeekday}).`,
                  `For one-time chores, selectedOneTimeDate must be one of: ${allowedOneTimeDates.join(", ")}.`,
                  "Never return a past date, a specific calendar date, or a year.",
                  "If you are unsure about scheduling, prefer a one-time chore due Tomorrow.",
                ].join(" "),
              },
            ],
          },
          {
            role: "user",
            content: [
              {
                type: "input_text",
                text: [
                  `Household: ${householdName || "Household"}.`,
                  `User: ${userName || "Household admin"}.`,
                  `Current date: ${todayIso}.`,
                  "Analyze this room photo and draft chores with title, description, room, effort, proof suggestion, and optional schedule defaults.",
                ].join(" "),
              },
              {
                type: "input_image",
                image_url: `data:${mimeType};base64,${imageBase64}`,
              },
            ],
          },
        ],
        text: {
          format: {
            type: "json_schema",
            name: "discovery_drafts",
            strict: true,
            schema: {
              type: "object",
              additionalProperties: false,
              properties: {
                drafts: {
                  type: "array",
                  minItems: 0,
                  items: {
                    type: "object",
                    additionalProperties: false,
                    properties: {
                      id: { type: "string" },
                      title: { type: "string" },
                      description: { type: "string" },
                      roomName: { type: "string" },
                      effort: {
                        type: "string",
                        enum: ["Light", "Standard", "Heavy"],
                      },
                      proofNeededSuggested: { type: "boolean" },
                      repeatsEnabled: { type: "boolean" },
                      repeatFrequency: {
                        type: "string",
                        enum: ["Daily", "Weekly", "Monthly", "Custom"],
                      },
                      dailyMode: {
                        type: "string",
                        enum: ["EveryDay", "Weekdays", "SpecificDays"],
                      },
                      selectedWeekdays: {
                        type: "array",
                        items: {
                          type: "string",
                          enum: ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"],
                        },
                      },
                      selectedOneTimeDate: { type: "string" },
                    },
                    required: [
                      "id",
                      "title",
                      "description",
                      "roomName",
                      "effort",
                      "proofNeededSuggested",
                      "repeatsEnabled",
                      "repeatFrequency",
                      "dailyMode",
                      "selectedWeekdays",
                      "selectedOneTimeDate",
                    ],
                  },
                },
              },
              required: ["drafts"],
            },
          },
        },
      }),
    });

    if (!response.ok) {
      const errorBody = await response.text();
      throw new HttpsError("internal", `OpenAI room analysis failed: ${errorBody}`);
    }

    const result = await response.json();
    const outputText = result.output_text || extractOutputText(result.output);
    if (!outputText) {
      throw new HttpsError("internal", "OpenAI returned an empty discovery analysis response.");
    }

    try {
      return JSON.parse(outputText);
    } catch (error) {
      throw new HttpsError("internal", `Discovery analysis returned invalid JSON: ${error.message}`);
    }
  },
);

exports.deleteHouseholdCascade = onCall(
  {
    cors: true,
    timeoutSeconds: 120,
    memory: "1GiB",
  },
  async (request) => {
    if (!request.auth?.uid) {
      throw new HttpsError("unauthenticated", "Sign in before deleting a household.");
    }

    const householdId = typeof request.data?.householdId === "string"
      ? request.data.householdId.trim()
      : "";
    if (!householdId) {
      throw new HttpsError("invalid-argument", "A household ID is required.");
    }

    const householdRef = firestore.collection("households").doc(householdId);
    const householdSnapshot = await householdRef.get();
    if (!householdSnapshot.exists) {
      throw new HttpsError("not-found", "We couldn't find that household anymore.");
    }

    const household = householdSnapshot.data() || {};
    if (household.ownerUserId !== request.auth.uid) {
      throw new HttpsError("permission-denied", "Only the household owner can delete the household.");
    }

    const membersSnapshot = await householdRef.collection("members").get();
    const members = membersSnapshot.docs.map((doc) => doc.data() || {});
    const activeMembers = members.filter((member) => member.active !== false);
    if (activeMembers.length !== 1 || activeMembers[0]?.userId !== request.auth.uid) {
      throw new HttpsError(
        "failed-precondition",
        "Transfer ownership before leaving the household.",
      );
    }

    await Promise.all(members.map((member) => removeHouseholdFromUser(member.userId, householdId)));

    const activeInviteCode = typeof household.activeInviteCode === "string"
      ? household.activeInviteCode.trim().toUpperCase()
      : "";
    if (activeInviteCode) {
      await firestore.collection("householdInvites").doc(activeInviteCode).delete().catch(() => undefined);
    }

    await deleteFilesWithPrefix(`households/${householdId}/images/`);
    await firestore.recursiveDelete(householdRef);

    return { success: true };
  },
);

exports.submitFeedbackPalItem = onCall(
  {
    cors: true,
    invoker: "public",
    timeoutSeconds: 30,
    memory: "256MiB",
  },
  async (request) => {
    if (!request.auth?.uid) {
      throw new HttpsError("unauthenticated", "Sign in before sending feedback.");
    }

    const data = request.data || {};
    const householdId = cleanString(data.householdId, 120);
    const userId = cleanString(data.userId, 120);
    const message = cleanString(data.message, 4000);
    if (!householdId || !userId || message.length < 8) {
      throw new HttpsError("invalid-argument", "Feedback needs a household, user, and message.");
    }
    if (request.auth.uid !== userId) {
      throw new HttpsError("permission-denied", "You can only send feedback as yourself.");
    }

    const memberSnapshot = await firestore.collection("households")
      .doc(householdId)
      .collection("members")
      .doc(userId)
      .get();
    if (!memberSnapshot.exists || memberSnapshot.data()?.active === false) {
      throw new HttpsError("permission-denied", "Join this household before sending feedback.");
    }

    const nowEpochMs = Date.now();
    const triage = classifyFeedbackPalSubmission({
      reportedIntent: cleanString(data.reportedIntent, 40),
      message,
      screenRoute: cleanString(data.screenRoute, 160),
    });
    const readableTicket = buildReadableFeedbackTicket({
      reportedIntent: cleanString(data.reportedIntent, 40),
      message,
      screenRoute: cleanString(data.screenRoute, 160),
      userName: cleanString(data.userName, 160),
      householdName: cleanString(data.householdName, 160),
      triage,
    });
    const feedbackRef = firestore.collection("households")
      .doc(householdId)
      .collection("feedbackItems")
      .doc();
    const document = {
      id: feedbackRef.id,
      householdId,
      householdName: cleanString(data.householdName, 160),
      userId,
      userName: cleanString(data.userName, 160),
      palKind: cleanString(data.palKind, 40),
      reportedIntent: cleanString(data.reportedIntent, 40),
      screenRoute: cleanString(data.screenRoute, 160),
      message,
      source: "FeedbackPal",
      status: "New",
      createdAtEpochMs: nowEpochMs,
      updatedAtEpochMs: nowEpochMs,
      optimizedTitle: readableTicket.title,
      optimizedProblem: readableTicket.problem,
      suggestedNextStep: readableTicket.nextStep,
      acceptanceCriteria: readableTicket.acceptanceCriteria,
      ...triage,
    };

    await feedbackRef.set(document);

    return {
      id: document.id,
      type: document.type,
      priority: document.priority,
      priorityScore: document.priorityScore,
      effort: document.effort,
      summary: document.summary,
      signals: document.signals,
    };
  },
);

exports.syncFeedbackPalItemToLinear = onDocumentWritten(
  {
    document: "households/{householdId}/feedbackItems/{feedbackId}",
    timeoutSeconds: 60,
    memory: "256MiB",
    secrets: ["LINEAR_API_KEY"],
  },
  async (event) => {
    const before = event.data?.before;
    const after = event.data?.after;
    if (!shouldSyncFeedbackItemToLinear(before, after)) return;

    const feedback = after.data() || {};
    const feedbackRef = after.ref;
    const nowEpochMs = Date.now();

    try {
      const apiKey = process.env.LINEAR_API_KEY;
      if (!apiKey) {
        throw new Error("LINEAR_API_KEY is not configured for Cloud Functions.");
      }

      const target = await resolveLinearFeedbackTarget(apiKey, LINEAR_PROJECT_SLUG);
      const payload = buildLinearIssuePayload({
        feedback,
        householdId: event.params?.householdId || feedback.householdId || "",
        feedbackId: event.params?.feedbackId || feedback.id || after.id,
        firestorePath: feedbackRef.path,
        target,
      });
      const result = await createLinearIssue(apiKey, payload.input);

      await feedbackRef.set(
        {
          linearIssueId: result.id,
          linearIdentifier: result.identifier,
          linearUrl: result.url,
          linearSyncedAtEpochMs: nowEpochMs,
          linearSyncError: "",
          linearSyncStatus: "Synced",
          updatedAtEpochMs: nowEpochMs,
        },
        { merge: true },
      );
    } catch (error) {
      await feedbackRef.set(
        {
          linearSyncAttemptedAtEpochMs: nowEpochMs,
          linearSyncError: cleanString(error?.message || String(error), 1000),
          linearSyncStatus: "Failed",
          updatedAtEpochMs: nowEpochMs,
        },
        { merge: true },
      );
    }
  },
);

exports.reconcileHouseholdNotificationsOnChoreWrite = onDocumentWritten(
  {
    document: "households/{householdId}/chores/{choreId}",
    timeoutSeconds: 120,
    memory: "512MiB",
  },
  async (event) => {
    const householdId = event.params?.householdId;
    if (!householdId) return;
    await reconcileDerivedNotificationsForHousehold(householdId);
  },
);

exports.reconcileHouseholdNotificationsOnDecisionWrite = onDocumentWritten(
  {
    document: "households/{householdId}/reassignmentDecisions/{decisionId}",
    timeoutSeconds: 120,
    memory: "512MiB",
  },
  async (event) => {
    const householdId = event.params?.householdId;
    if (!householdId) return;
    await reconcileDerivedNotificationsForHousehold(householdId);
  },
);

exports.reconcileHouseholdNotificationsOnMemberWrite = onDocumentWritten(
  {
    document: "households/{householdId}/members/{memberId}",
    timeoutSeconds: 120,
    memory: "512MiB",
  },
  async (event) => {
    const householdId = event.params?.householdId;
    if (!householdId) return;
    await reconcileDerivedNotificationsForHousehold(householdId);
  },
);

exports.reconcileHouseholdNotificationsOnSchedule = onSchedule(
  {
    schedule: "every 60 minutes",
    timeZone: "Etc/UTC",
    timeoutSeconds: 300,
    memory: "512MiB",
  },
  async () => {
    const householdsSnapshot = await firestore.collection("households").get();
    for (const householdDoc of householdsSnapshot.docs) {
      await reconcileDerivedNotificationsForHousehold(householdDoc.id);
    }
  },
);

exports.sendHouseholdNotificationPush = onDocumentWritten(
  {
    document: "households/{householdId}/notifications/{notificationId}",
    timeoutSeconds: 60,
    memory: "256MiB",
  },
  async (event) => {
    const before = event.data?.before;
    const after = event.data?.after;
    if (!after?.exists) return;

    const notification = after.data() || {};
    const previous = before?.exists ? (before.data() || {}) : null;
    if (!shouldSendNotificationPush(previous, notification)) return;

    const recipientUserIds = Array.isArray(notification.recipientUserIds)
      ? notification.recipientUserIds.filter((value) => typeof value === "string" && value.trim().length > 0)
      : [];
    if (!recipientUserIds.length) return;

    const title = typeof notification.title === "string" ? notification.title : "";
    const body = typeof notification.body === "string" ? notification.body : "";
    if (!title || !body) return;

    const tokenTargets = [];
    for (const userId of recipientUserIds) {
      const devicesSnapshot = await firestore.collection("users")
        .doc(userId)
        .collection("devices")
        .where("notificationsEnabled", "==", true)
        .get();
      devicesSnapshot.docs.forEach((deviceDoc) => {
        const device = deviceDoc.data() || {};
        if (typeof device.fcmToken === "string" && device.fcmToken.trim().length > 0) {
          tokenTargets.push({
            token: device.fcmToken.trim(),
            ref: deviceDoc.ref,
          });
        }
      });
    }

    if (!tokenTargets.length) return;

    const payloadData = {
      notificationId: after.id,
      householdId: typeof notification.householdId === "string" ? notification.householdId : "",
      choreId: typeof notification.choreId === "string" ? notification.choreId : "",
      type: typeof notification.type === "string" ? notification.type : "",
      title,
      body,
    };

    const chunks = chunkArray(tokenTargets, 500);
    for (const chunk of chunks) {
      const response = await messaging.sendEachForMulticast({
        tokens: chunk.map((item) => item.token),
        notification: { title, body },
        data: payloadData,
        android: {
          priority: "high",
          notification: {
            channelId: PUSH_NOTIFICATION_CHANNEL_ID,
            icon: ANDROID_NOTIFICATION_ICON,
            color: ANDROID_NOTIFICATION_COLOR,
          },
        },
        apns: {
          headers: {
            "apns-priority": "10",
          },
          payload: {
            aps: {
              sound: "default",
            },
            notificationId: payloadData.notificationId,
            householdId: payloadData.householdId,
            choreId: payloadData.choreId,
            type: payloadData.type,
          },
        },
      });

      const removals = response.responses.flatMap((result, index) => {
        if (result.success) return [];
        const code = result.error?.code || "";
        if (code === "messaging/registration-token-not-registered" || code === "messaging/invalid-registration-token") {
          return [chunk[index].ref.delete().catch(() => undefined)];
        }
        return [];
      });
      await Promise.all(removals);
    }
  },
);

function extractOutputText(output) {
  if (!Array.isArray(output)) return "";
  for (const item of output) {
    if (!Array.isArray(item.content)) continue;
    for (const contentItem of item.content) {
      if (typeof contentItem.text === "string" && contentItem.text.length > 0) {
        return contentItem.text;
      }
    }
  }
  return "";
}

async function removeHouseholdFromUser(userId, householdId) {
  if (!userId) return;
  const userRef = firestore.collection("users").doc(userId);
  const userSnapshot = await userRef.get();
  if (!userSnapshot.exists) return;

  const profile = userSnapshot.data() || {};
  const householdIds = Array.isArray(profile.householdIds)
    ? profile.householdIds.filter((id) => id !== householdId)
    : [];

  await userRef.set(
    {
      householdIds,
      activeHouseholdId: profile.activeHouseholdId === householdId
        ? (householdIds[0] || "")
        : (profile.activeHouseholdId || ""),
    },
    { merge: true },
  );
}

async function deleteFilesWithPrefix(prefix) {
  const bucket = storage.bucket();
  let pageToken;

  do {
    const [files, , response] = await bucket.getFiles({
      prefix,
      pageToken,
      autoPaginate: false,
    });

    await Promise.all(files.map((file) => file.delete().catch(() => undefined)));
    pageToken = response?.nextPageToken;
  } while (pageToken);
}

function chunkArray(values, size) {
  const chunks = [];
  for (let index = 0; index < values.length; index += size) {
    chunks.push(values.slice(index, index + size));
  }
  return chunks;
}

async function reconcileDerivedNotificationsForHousehold(
  householdId,
  options = {},
) {
  if (!householdId) return;

  const now = options.now instanceof Date ? new Date(options.now.getTime()) : new Date();
  const todayDateKey = options.todayDateKey || dateKeyForDate(now);
  const householdRef = firestore.collection("households").doc(householdId);
  const [membersSnapshot, choresSnapshot, decisionsSnapshot, notificationsSnapshot] = await Promise.all([
    householdRef.collection("members").get(),
    householdRef.collection("chores").get(),
    householdRef.collection("reassignmentDecisions").get(),
    householdRef.collection("notifications").get(),
  ]);

  const members = membersSnapshot.docs.map((doc) => ({ id: doc.id, ...(doc.data() || {}) }));
  const chores = choresSnapshot.docs.map((doc) => ({ id: doc.id, ...(doc.data() || {}) }));
  const decisions = decisionsSnapshot.docs.map((doc) => ({ id: doc.id, ...(doc.data() || {}) }));
  const existingNotifications = notificationsSnapshot.docs.map((doc) => ({
    id: doc.id,
    ref: doc.ref,
    ...(doc.data() || {}),
  }));

  const plan = buildDerivedNotificationPlan({
    householdId,
    members,
    chores,
    decisions,
    existingNotifications,
    todayDateKey,
    nowEpochMs: now.getTime(),
  });

  await Promise.all(plan.deletes.map((notification) => notification.ref.delete()));
  for (const document of plan.upserts) {
    await householdRef.collection("notifications").doc(document.id).set(document, { merge: true });
  }
}

function buildDerivedNotificationPlan({
  householdId,
  members,
  chores,
  decisions,
  existingNotifications,
  todayDateKey,
  nowEpochMs,
}) {
  const existingById = new Map(existingNotifications.map((notification) => [notification.id, notification]));
  const desiredById = new Map();

  chores.forEach((chore) => {
    const candidate = buildDueStatusNotification({
      householdId,
      chore,
      members,
      todayDateKey,
      nowEpochMs,
      existingById,
    });
    if (candidate) {
      desiredById.set(candidate.id, candidate);
    }
  });

  decisions.forEach((decision) => {
    buildAssignmentChangedNotifications({
      householdId,
      decision,
      chores,
      members,
      todayDateKey,
      nowEpochMs,
      existingById,
    }).forEach((notification) => {
      desiredById.set(notification.id, notification);
    });
  });

  const deletes = existingNotifications.filter((notification) => {
    if (!DERIVED_NOTIFICATION_TYPES.has(notification.type)) return false;
    return !desiredById.has(notification.id);
  });

  return {
    upserts: Array.from(desiredById.values()),
    deletes,
  };
}

function buildDueStatusNotification({
  householdId,
  chore,
  members,
  todayDateKey,
  nowEpochMs,
  existingById,
}) {
  if (!chore || chore.isActive === false || chore.pendingApproval === true) return null;
  if (!chore.assignee || !String(chore.assignee).trim()) return null;
  if (!choreAppearsOnDate(chore, todayDateKey, todayDateKey)) return null;

  const status = effectiveChoreStatusForDate(chore, todayDateKey, todayDateKey);
  if (status !== "DueSoon" && status !== "Overdue") return null;

  const recipientUserIds = dueAlertRecipientUserIds(chore, members);
  if (!recipientUserIds.length) return null;

  const dueLabel = effectiveChoreDueLabelForDate(chore, todayDateKey, todayDateKey, status);
  const prefix = status === "DueSoon" ? "due-soon" : "overdue";
  const id = `${prefix}|${householdId}|${chore.id}|${todayDateKey}`;
  const existing = existingById.get(id);
  return {
    id,
    householdId,
    choreId: chore.id,
    type: status,
    title: status === "DueSoon" ? `${chore.title} is due soon` : `${chore.title} is overdue`,
    body: status === "DueSoon"
      ? buildDueSoonBody(chore.assignee, dueLabel)
      : buildOverdueBody(chore.assignee, dueLabel),
    createdAtEpochMs: existing?.createdAtEpochMs || nowEpochMs,
    recipientUserIds,
    readReceipts: existing?.readReceipts || [],
  };
}

function effectiveChoreStatusForDate(chore, targetDateKey, todayDateKey) {
  const occurrence = chore?.occurrences?.[targetDateKey];
  if (occurrence?.status) return occurrence.status;

  const baseStatus = chore?.status || "DueSoon";
  if (baseStatus === "Completed" || baseStatus === "PendingProof") {
    const completionDateKey = dateKeyForEpochMs(chore?.completedAtEpochMs);
    if (!completionDateKey) return targetDateKey === todayDateKey ? baseStatus : "Scheduled";
    return completionDateKey === targetDateKey ? baseStatus : (targetDateKey === todayDateKey ? "DueSoon" : "Scheduled");
  }
  return targetDateKey === todayDateKey ? baseStatus : "Scheduled";
}

function effectiveChoreDueLabelForDate(chore, targetDateKey, todayDateKey, status) {
  const occurrence = chore?.occurrences?.[targetDateKey];
  if (occurrence?.dueLabel) return occurrence.dueLabel;
  if (runtimeStateAppliesToDate(chore, targetDateKey) && chore?.dueLabel) return chore.dueLabel;
  if (status === "Scheduled") return `Scheduled for ${weekdayNameFromDateKey(targetDateKey)}`;
  return targetDateKey === todayDateKey ? "Due later today" : "";
}

function runtimeStateAppliesToDate(chore, targetDateKey) {
  const baseStatus = chore?.status || "";
  if (baseStatus !== "Completed" && baseStatus !== "PendingProof") return false;
  const completionDateKey = dateKeyForEpochMs(chore?.completedAtEpochMs);
  return completionDateKey ? completionDateKey === targetDateKey : false;
}

function buildAssignmentChangedNotifications({
  householdId,
  decision,
  chores,
  members,
  todayDateKey,
  nowEpochMs,
  existingById,
}) {
  if (!decision || decision.status !== "Accepted") return [];
  const chore = chores.find((entry) => entry.id === decision.choreId);
  if (!chore || chore.isActive === false) return [];

  return dateRangeWithinWindow(
    decision.scopeStartDate,
    decision.scopeEndDate,
    todayDateKey,
    ALERT_LOOKAHEAD_DAYS,
  )
    .filter((dateKey) => choreAppearsOnDate(chore, dateKey, todayDateKey))
    .map((dateKey) => {
      const id = `assignment-changed|${householdId}|${chore.id}|${dateKey}|${normalizeNameKey(decision.originalAssignee)}|${normalizeNameKey(decision.coverMember)}`;
      const existing = existingById.get(id);
      const recipientUserIds = assignmentAlertRecipientUserIds({
        members,
        originalAssignee: decision.originalAssignee,
        newAssignee: decision.coverMember,
      });
      if (!recipientUserIds.length) return null;
      return {
        id,
        householdId,
        choreId: chore.id,
        type: "AssignmentChanged",
        title: `${decision.coverMember} is covering ${chore.title}`,
        body: `${decision.coverMember} is covering for ${decision.originalAssignee} while they're away.`,
        createdAtEpochMs: existing?.createdAtEpochMs || nowEpochMs,
        recipientUserIds,
        readReceipts: existing?.readReceipts || [],
      };
    })
    .filter(Boolean);
}

function dueAlertRecipientUserIds(chore, members) {
  const recipients = new Set(managerUserIds(members));
  const assigneeMember = memberForAssignee(chore.assigneeId, chore.assignee, members);
  if (assigneeMember?.active !== false && assigneeMember?.userId) {
    recipients.add(assigneeMember.userId);
  }
  return Array.from(recipients).sort();
}

function assignmentAlertRecipientUserIds({
  members,
  originalAssignee,
  newAssignee,
}) {
  const recipients = new Set(managerUserIds(members));
  const originalMember = memberForAssignee("", originalAssignee, members);
  const newMember = memberForAssignee("", newAssignee, members);
  if (originalMember?.active !== false && originalMember?.userId) {
    recipients.add(originalMember.userId);
  }
  if (newMember?.active !== false && newMember?.userId) {
    recipients.add(newMember.userId);
  }
  return Array.from(recipients).sort();
}

function managerUserIds(members) {
  return members
    .filter((member) => member.active !== false && (member.role === "Owner" || member.role === "Admin"))
    .map((member) => member.userId)
    .filter((userId) => typeof userId === "string" && userId.trim().length > 0);
}

function memberForAssignee(assigneeId, assigneeName, members) {
  const normalizedAssigneeId = normalizeNameKey(assigneeId);
  const normalizedAssigneeName = normalizeNameKey(assigneeName);
  return members.find((member) => {
    const memberUserId = normalizeNameKey(member.userId);
    const memberDisplayName = normalizeNameKey(member.displayName);
    return (
      (normalizedAssigneeId && memberUserId === normalizedAssigneeId) ||
      (normalizedAssigneeName && memberDisplayName === normalizedAssigneeName)
    );
  }) || null;
}

function buildDueSoonBody(assigneeName, dueLabel) {
  const prefix = assigneeName ? `${assigneeName} is up next. ` : "";
  return `${prefix}${dueLabel || "This chore needs attention today."}`;
}

function buildOverdueBody(assigneeName, dueLabel) {
  const prefix = assigneeName ? `${assigneeName} still owns this one. ` : "";
  return `${prefix}${dueLabel || "This chore has slipped past its planned window."}`;
}

function shouldSendNotificationPush(previous, current) {
  if (!current) return false;
  if (!previous) return true;
  return JSON.stringify(notificationDeliveryFingerprint(previous)) !== JSON.stringify(notificationDeliveryFingerprint(current));
}

function notificationDeliveryFingerprint(notification) {
  return {
    type: notification.type || "",
    title: notification.title || "",
    body: notification.body || "",
    recipients: Array.isArray(notification.recipientUserIds)
      ? notification.recipientUserIds.filter(Boolean).slice().sort()
      : [],
  };
}

function choreAppearsOnDate(chore, targetDateKey, todayDateKey) {
  const dayOffset = daysBetweenDateKeys(todayDateKey, targetDateKey);
  if (dayOffset < 0 || chore.isActive === false) return false;

  const startLabel = chore.repeatsEnabled ? chore.repeatStartDate : chore.selectedOneTimeDate;
  const startOffset = parseRelativeDayOffset(startLabel, weekdayNameFromDateKey(todayDateKey));
  if (dayOffset < startOffset) return false;

  if (!chore.repeatsEnabled) {
    return dayOffset === startOffset;
  }

  const weekdayName = weekdayNameFromDateKey(targetDateKey);
  switch (chore.repeatFrequency) {
    case "Daily":
      switch (chore.dailyRepeatMode) {
        case "EveryDay":
          return true;
        case "Weekdays":
          return WEEKDAY_NAMES.includes(weekdayName);
        case "SpecificDays":
          return Array.isArray(chore.selectedWeekdays) && chore.selectedWeekdays.includes(weekdayName);
        default:
          return false;
      }
    case "Weekly":
      return Array.isArray(chore.selectedWeekdays) && chore.selectedWeekdays.includes(weekdayName);
    case "Monthly":
      return dayOffset === startOffset || (dayOffset - startOffset) % 30 === 0;
    case "Custom": {
      const digits = String(chore.customPattern || "").replace(/\D/g, "");
      const interval = Number.parseInt(digits || "2", 10);
      return dayOffset === startOffset || (dayOffset - startOffset) % (interval * 7) === 0;
    }
    default:
      return false;
  }
}

function parseRelativeDayOffset(label, todayWeekdayName) {
  const normalized = String(label || "").trim();
  if (!normalized) return 0;
  if (/^today$/i.test(normalized)) return 0;
  if (/^tomorrow$/i.test(normalized)) return 1;
  if (/^this /i.test(normalized)) {
    return daysUntilWeekday(todayWeekdayName, normalized.replace(/^this /i, "").trim());
  }
  if (/^next /i.test(normalized)) {
    const days = daysUntilWeekday(todayWeekdayName, normalized.replace(/^next /i, "").trim());
    return days === 0 ? 7 : days + 7;
  }
  return 0;
}

function daysUntilWeekday(todayWeekdayName, targetWeekdayName) {
  const todayIndex = WEEKDAY_ORDER.indexOf(todayWeekdayName);
  const targetIndex = WEEKDAY_ORDER.indexOf(targetWeekdayName);
  if (todayIndex === -1 || targetIndex === -1) return 0;
  return (targetIndex - todayIndex + 7) % 7;
}

function dateRangeWithinWindow(startDateKey, endDateKey, todayDateKey, count) {
  const lowerBound = maxDateKey(startDateKey || todayDateKey, todayDateKey);
  const upperBound = minDateKey(endDateKey || addDaysToDateKey(todayDateKey, count - 1), addDaysToDateKey(todayDateKey, count - 1));
  if (!lowerBound || !upperBound || lowerBound > upperBound) return [];

  const result = [];
  let cursor = lowerBound;
  while (cursor <= upperBound) {
    result.push(cursor);
    cursor = addDaysToDateKey(cursor, 1);
  }
  return result;
}

function maxDateKey(left, right) {
  return left >= right ? left : right;
}

function minDateKey(left, right) {
  return left <= right ? left : right;
}

function dateKeyForDate(date) {
  return new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()))
    .toISOString()
    .slice(0, 10);
}

function dateKeyForEpochMs(epochMs) {
  const value = Number(epochMs || 0);
  if (!Number.isFinite(value) || value <= 0) return "";
  return dateKeyForDate(new Date(value));
}

function addDaysToDateKey(dateKey, days) {
  const date = new Date(`${dateKey}T00:00:00.000Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

function daysBetweenDateKeys(startDateKey, endDateKey) {
  const start = new Date(`${startDateKey}T00:00:00.000Z`);
  const end = new Date(`${endDateKey}T00:00:00.000Z`);
  return Math.round((end.getTime() - start.getTime()) / 86400000);
}

function weekdayNameFromDateKey(dateKey) {
  const date = new Date(`${dateKey}T00:00:00.000Z`);
  return WEEKDAY_ORDER[(date.getUTCDay() + 6) % 7];
}

function normalizeNameKey(value) {
  return String(value || "").trim().toLowerCase();
}

function cleanString(value, maxLength) {
  return String(value || "")
    .replace(/\s+/g, " ")
    .trim()
    .slice(0, maxLength);
}

function classifyFeedbackPalSubmission({
  reportedIntent,
  message,
  screenRoute,
}) {
  const normalizedIntent = cleanString(reportedIntent, 40);
  const text = `${message || ""} ${screenRoute || ""}`.toLowerCase();
  const signals = [];
  let type = normalizedIntent === "FeatureIdea" ? "Idea"
    : normalizedIntent === "ConfusingFlow" ? "ConfusingFlow"
      : normalizedIntent === "Praise" ? "Praise"
        : "Bug";
  let score = type === "Bug" ? 64 : type === "ConfusingFlow" ? 58 : type === "Idea" ? 48 : 25;
  let effortScore = type === "Idea" ? 2 : 1;

  const rules = [
    {
      pattern: /\b(crash|crashed|freez|stuck|cannot|can't|wont|won't|broken|error|failed|fail|blank|black screen)\b/,
      apply: () => {
        type = "Bug";
        score += 24;
        signals.push("blocking failure");
      },
    },
    {
      pattern: /\b(delete|lost|missing|gone|wrong household|wrong user|permission|privacy|payment|billing|subscribe|purchase)\b/,
      apply: () => {
        score += 18;
        signals.push("data, account, or billing risk");
      },
    },
    {
      pattern: /\b(notification|notify|reminder|proof|photo|upload|complete|completion|schedule|today|tomorrow|date|carousel|chores?)\b/,
      apply: () => {
        score += 10;
        signals.push("core chore workflow");
      },
    },
    {
      pattern: /\b(confusing|unclear|where|how do i|hard to|can't find|cant find|weird|looks off|polish)\b/,
      apply: () => {
        if (type !== "Bug") type = "ConfusingFlow";
        score += 9;
        signals.push("usability friction");
      },
    },
    {
      pattern: /\b(add|could|wish|idea|feature|allow|support|integrate|maybe|would be nice)\b/,
      apply: () => {
        if (type !== "Bug") type = "Idea";
        score += 5;
        effortScore += 1;
        signals.push("new capability request");
      },
    },
    {
      pattern: /\b(all users|everyone|family|household|kids|parents|admins|members)\b/,
      apply: () => {
        score += 8;
        signals.push("multi-user impact");
      },
    },
    {
      pattern: /\b(quick|small|text|copy|label|color|padding|spacing|icon)\b/,
      apply: () => {
        effortScore -= 1;
        signals.push("likely quick polish");
      },
    },
    {
      pattern: /\b(backend|cloud function|sync|database|firestore|storage|ai|openai|model|payment|subscription)\b/,
      apply: () => {
        effortScore += 2;
        signals.push("backend or integration work");
      },
    },
  ];

  rules.forEach((rule) => {
    if (rule.pattern.test(text)) rule.apply();
  });

  score = Math.max(0, Math.min(100, score));
  effortScore = Math.max(0, Math.min(4, effortScore));
  const effort = ["Quick", "Small", "Medium", "Large", "XL"][effortScore];
  const priority = score >= 85 ? "P0"
    : score >= 70 ? "P1"
      : score >= 45 ? "P2"
        : "P3";
  const dedupedSignals = Array.from(new Set(signals));
  const summary = buildFeedbackSummary({
    type,
    priority,
    effort,
    message,
    signals: dedupedSignals,
  });

  return {
    type,
    priority,
    priorityScore: score,
    effort,
    summary,
    signals: dedupedSignals,
  };
}

function buildFeedbackSummary({
  type,
  priority,
  effort,
  message,
  signals,
}) {
  const leading = cleanString(message, 96) || "Feedback captured";
  const signalText = signals.length ? ` Signals: ${signals.slice(0, 3).join(", ")}.` : "";
  return `${type} ranked ${priority}, ${effort} effort. ${leading}.${signalText}`;
}

function buildReadableFeedbackTicket({
  reportedIntent,
  message,
  screenRoute,
  userName,
  householdName,
  triage,
}) {
  const type = triage?.type || "Feedback";
  const screen = cleanString(screenRoute, 160) || "Unknown screen";
  const quote = cleanString(message, 4000);
  const normalizedIntent = cleanString(reportedIntent, 40);
  const actor = cleanString(userName, 80) || "A ChorePilot user";
  const household = cleanString(householdName, 120);
  const title = buildReadableFeedbackTitle({ type, screen, message: quote });
  const problem = [
    `${actor}${household ? ` from ${household}` : ""} reported a ${type.toLowerCase()} on ${screen}.`,
    summarizeFeedbackMessage(quote),
  ].filter(Boolean).join(" ");
  const nextStep = buildFeedbackNextStep({ type, screen, message: quote, signals: triage?.signals || [] });
  const acceptanceCriteria = buildFeedbackAcceptanceCriteria({
    type,
    screen,
    reportedIntent: normalizedIntent,
    signals: triage?.signals || [],
  });

  return {
    title,
    problem,
    nextStep,
    acceptanceCriteria,
  };
}

function buildReadableFeedbackTitle({ type, screen, message }) {
  const cleanMessage = cleanString(message, 140);
  const lowered = cleanMessage.toLowerCase();
  let focus = cleanMessage;
  if (/\b(crash\w*|freez\w*|stuck|blank|black screen)\b/.test(lowered)) {
    focus = "Investigate crash or blocked state";
  } else if (/\b(auth\w*|login|sign in|unauth\w*|permission\w*)\b/.test(lowered)) {
    focus = "Fix authentication or permission failure";
  } else if (/\b(notification|notify|reminder)\b/.test(lowered)) {
    focus = "Fix notification behavior";
  } else if (/\b(schedule|date|today|tomorrow|carousel)\b/.test(lowered)) {
    focus = "Clarify schedule and date behavior";
  } else if (/\b(padding|spacing|icon|color|text|label|looks off|weird)\b/.test(lowered)) {
    focus = "Polish visual layout";
  } else if (cleanMessage.length > 84) {
    focus = `${cleanMessage.slice(0, 81).trim()}...`;
  }
  return cleanString(`${type}: ${focus} on ${screen}`, 160);
}

function summarizeFeedbackMessage(message) {
  const cleanMessage = cleanString(message, 600);
  if (!cleanMessage) return "";
  const sentence = cleanMessage.match(/^(.+?[.!?])\s/)?.[1] || cleanMessage;
  return `The original report says: "${cleanString(sentence, 220)}"`;
}

function buildFeedbackNextStep({
  type,
  screen,
  message,
  signals,
}) {
  const text = `${message || ""} ${(signals || []).join(" ")}`.toLowerCase();
  if (type === "Bug" && /\b(crash\w*|freez\w*|blank|black screen|stuck)\b/.test(text)) {
    return `Reproduce the failure on ${screen}, capture logs, and fix the broken path before changing adjacent UI.`;
  }
  if (/\b(auth\w*|login|sign in|unauth\w*|permission\w*)\b/.test(text)) {
    return `Verify the signed-in Firebase user on ${screen}, then fix the auth/session handoff that blocks the action.`;
  }
  if (/\b(notification|notify|reminder)\b/.test(text)) {
    return `Trace the notification path end to end, then confirm the user receives the expected alert on a real device.`;
  }
  if (/\b(schedule|date|today|tomorrow|carousel|chores?)\b/.test(text)) {
    return `Audit the ${screen} schedule state, fix the confusing date/chores behavior, and verify past, today, and future dates.`;
  }
  if (type === "ConfusingFlow") {
    return `Walk through the ${screen} flow as a first-time user, remove the confusing step, and verify the intended action is obvious.`;
  }
  if (type === "Idea") {
    return "Decide whether this belongs in the first viable product; if yes, scope the smallest complete version before implementation.";
  }
  return `Review the report on ${screen}, identify the product gap, and ship the smallest complete fix.`;
}

function buildFeedbackAcceptanceCriteria({
  type,
  screen,
  reportedIntent,
  signals,
}) {
  const criteria = [
    `The reported ${type.toLowerCase()} is addressed on ${screen}.`,
    "The original message is still preserved in the issue for traceability.",
  ];
  if (type === "Bug") {
    criteria.push("The issue is reproduced or a clear root cause is documented before the fix is marked complete.");
    criteria.push("A targeted regression check passes for the affected path.");
  } else if (type === "ConfusingFlow") {
    criteria.push("The updated flow is understandable without helper text that explains the UI.");
    criteria.push("Empty, loading, and error states still read cleanly if the changed surface uses them.");
  } else if (type === "Idea") {
    criteria.push("The issue states whether this is required for first viable product or should be deferred.");
  }
  if ((signals || []).includes("core chore workflow")) {
    criteria.push("Core chore scheduling/completion behavior is not regressed.");
  }
  if (reportedIntent === "Praise") {
    criteria.push("No product change is required unless the report includes a concrete improvement.");
  }
  return criteria;
}

function shouldSyncFeedbackItemToLinear(beforeSnapshot, afterSnapshot) {
  if (!afterSnapshot?.exists) return false;
  if (beforeSnapshot?.exists) return false;

  const feedback = afterSnapshot.data() || {};
  if (feedback.source && feedback.source !== "FeedbackPal") return false;
  if (feedback.linearIssueId || feedback.linearIdentifier || feedback.linearUrl) return false;
  return cleanString(feedback.message, 4000).length >= 8;
}

async function resolveLinearFeedbackTarget(apiKey, projectSlug) {
  const response = await callLinearGraphql(apiKey, {
    query: `
      query FeedbackPalLinearProject($first: Int!) {
        projects(first: $first) {
          nodes {
            id
            name
            url
            teams {
              nodes {
                id
                key
                name
              }
            }
          }
        }
      }
    `,
    variables: { first: 100 },
  });

  const project = resolveLinearProjectFromNodes(response?.data?.projects?.nodes || [], projectSlug);
  const team = resolveLinearTeamForProject(project);
  return {
    projectId: project.id,
    projectName: project.name || "",
    projectUrl: project.url || "",
    teamId: team.id,
    teamKey: team.key || "",
    teamName: team.name || "",
  };
}

function resolveLinearProjectFromNodes(projects, projectSlug) {
  const slug = cleanString(projectSlug, 200).toLowerCase();
  const project = projects.find((candidate) => {
    const url = cleanString(candidate?.url, 500).toLowerCase();
    return slug.length > 0 && (url.endsWith(`/${slug}`) || url.includes(`/project/${slug}`));
  });

  if (!project) {
    throw new Error(`Linear project slug not found: ${projectSlug}`);
  }
  if (!project.id) {
    throw new Error(`Linear project is missing an id: ${projectSlug}`);
  }
  return project;
}

function resolveLinearTeamForProject(project) {
  const teams = Array.isArray(project?.teams?.nodes)
    ? project.teams.nodes.filter((team) => team?.id)
    : [];
  if (!teams.length) {
    throw new Error(`Linear project has no attached team: ${project?.name || project?.id || "unknown project"}`);
  }
  return teams[0];
}

function buildLinearIssuePayload({
  feedback,
  householdId,
  feedbackId,
  firestorePath,
  target,
}) {
  const type = cleanString(feedback.type, 40) || "Feedback";
  const priority = cleanString(feedback.priority, 10) || "P3";
  const summary = cleanString(feedback.optimizedTitle, 140)
    || buildReadableFeedbackTitle({
      type,
      screen: cleanString(feedback.screenRoute, 160) || "Unknown screen",
      message: cleanString(feedback.message, 4000),
    });
  return {
    input: {
      teamId: target.teamId,
      projectId: target.projectId,
      title: cleanString(`[${priority}] ${summary}`, 240),
      description: buildLinearIssueDescription({
        feedback,
        householdId,
        feedbackId,
        firestorePath,
        target,
      }),
      priority: linearPriorityValue(priority),
    },
  };
}

function buildLinearIssueDescription({
  feedback,
  householdId,
  feedbackId,
  firestorePath,
  target,
}) {
  const signals = Array.isArray(feedback.signals) ? feedback.signals.filter(Boolean) : [];
  const criteria = Array.isArray(feedback.acceptanceCriteria) ? feedback.acceptanceCriteria.filter(Boolean) : [];
  const fallbackTicket = buildReadableFeedbackTicket({
    reportedIntent: feedback.reportedIntent,
    message: feedback.message,
    screenRoute: feedback.screenRoute,
    userName: feedback.userName,
    householdName: feedback.householdName,
    triage: feedback,
  });
  const problem = cleanString(feedback.optimizedProblem, 1200) || fallbackTicket.problem;
  const nextStep = cleanString(feedback.suggestedNextStep, 800) || fallbackTicket.nextStep;
  const acceptanceCriteria = criteria.length ? criteria : fallbackTicket.acceptanceCriteria;
  const metadata = {
    source: "FeedbackPal",
    householdId,
    householdName: cleanString(feedback.householdName, 160),
    feedbackId,
    userId: cleanString(feedback.userId, 160),
    userName: cleanString(feedback.userName, 160),
    palKind: cleanString(feedback.palKind, 40),
    reportedIntent: cleanString(feedback.reportedIntent, 40),
    screenRoute: cleanString(feedback.screenRoute, 160),
    type: cleanString(feedback.type, 40),
    priority: cleanString(feedback.priority, 10),
    priorityScore: Number(feedback.priorityScore || 0),
    effort: cleanString(feedback.effort, 40),
    signals,
    firestorePath,
    linearProject: target.projectName || target.projectId,
    linearTeam: target.teamKey || target.teamName || target.teamId,
  };

  return [
    "Submitted from ChorePilot FeedbackPal.",
    "",
    "## Problem",
    problem || "No readable problem statement was generated.",
    "",
    "## Next step",
    nextStep || "Review and triage this feedback.",
    "",
    "## Acceptance criteria",
    ...acceptanceCriteria.map((item) => `- ${item}`),
    "",
    "## Triage",
    `- Type: ${metadata.type || "Feedback"}`,
    `- Priority: ${metadata.priority || "P3"} (${metadata.priorityScore}/100)`,
    `- Effort: ${metadata.effort || "Unknown"}`,
    `- Screen: ${metadata.screenRoute || "Unknown"}`,
    `- Signals: ${signals.length ? signals.join(", ") : "None"}`,
    "",
    "## Context",
    `- Household: ${metadata.householdName || householdId || "Unknown"}`,
    `- Reporter: ${metadata.userName || metadata.userId || "Unknown"}`,
    `- Firestore: ${firestorePath}`,
    "",
    "## Original message",
    cleanString(feedback.message, 4000) || "No message provided.",
    "",
    "## Machine data",
    "```json",
    JSON.stringify(metadata, null, 2),
    "```",
  ].join("\n");
}

function linearPriorityValue(priority) {
  switch (priority) {
    case "P0":
      return 1;
    case "P1":
      return 2;
    case "P2":
      return 3;
    case "P3":
    default:
      return 4;
  }
}

async function createLinearIssue(apiKey, input) {
  const response = await callLinearGraphql(apiKey, {
    query: `
      mutation FeedbackPalIssueCreate($input: IssueCreateInput!) {
        issueCreate(input: $input) {
          success
          issue {
            id
            identifier
            url
          }
        }
      }
    `,
    variables: { input },
  });

  const result = response?.data?.issueCreate;
  if (!result?.success || !result.issue?.id) {
    throw new Error("Linear issueCreate did not return a created issue.");
  }
  return result.issue;
}

async function callLinearGraphql(apiKey, body) {
  const response = await fetch(LINEAR_ENDPOINT, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: apiKey,
    },
    body: JSON.stringify(body),
  });

  const text = await response.text();
  let json;
  try {
    json = text ? JSON.parse(text) : {};
  } catch (error) {
    throw new Error(`Linear returned invalid JSON: ${error.message}`);
  }

  if (!response.ok) {
    throw new Error(`Linear request failed (${response.status}): ${cleanString(text, 800)}`);
  }
  if (Array.isArray(json.errors) && json.errors.length) {
    throw new Error(`Linear GraphQL error: ${json.errors.map((error) => error.message).join("; ")}`);
  }
  return json;
}

const ALERT_LOOKAHEAD_DAYS = 7;
const DERIVED_NOTIFICATION_TYPES = new Set(["DueSoon", "Overdue", "AssignmentChanged"]);
const WEEKDAY_ORDER = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];
const WEEKDAY_NAMES = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday"];

module.exports._private = {
  buildDerivedNotificationPlan,
  buildDueStatusNotification,
  buildAssignmentChangedNotifications,
  effectiveChoreStatusForDate,
  shouldSendNotificationPush,
  choreAppearsOnDate,
  parseRelativeDayOffset,
  dateRangeWithinWindow,
  dueAlertRecipientUserIds,
  assignmentAlertRecipientUserIds,
  classifyFeedbackPalSubmission,
  buildReadableFeedbackTicket,
  buildReadableFeedbackTitle,
  shouldSyncFeedbackItemToLinear,
  resolveLinearProjectFromNodes,
  resolveLinearTeamForProject,
  buildLinearIssuePayload,
  buildLinearIssueDescription,
  linearPriorityValue,
};
