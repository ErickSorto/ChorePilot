const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");

const OPENAI_ENDPOINT = "https://api.openai.com/v1/responses";
const OPENAI_MODEL = "gpt-4.1-mini";

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
            channelId: "proof_lifecycle_updates",
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
  if (chore.status !== "DueSoon" && chore.status !== "Overdue") return null;

  const recipientUserIds = dueAlertRecipientUserIds(chore, members);
  if (!recipientUserIds.length) return null;

  const prefix = chore.status === "DueSoon" ? "due-soon" : "overdue";
  const id = `${prefix}|${householdId}|${chore.id}|${todayDateKey}`;
  const existing = existingById.get(id);
  return {
    id,
    householdId,
    choreId: chore.id,
    type: chore.status,
    title: chore.status === "DueSoon" ? `${chore.title} is due soon` : `${chore.title} is overdue`,
    body: chore.status === "DueSoon"
      ? buildDueSoonBody(chore.assignee, chore.dueLabel)
      : buildOverdueBody(chore.assignee, chore.dueLabel),
    createdAtEpochMs: existing?.createdAtEpochMs || nowEpochMs,
    recipientUserIds,
    readReceipts: existing?.readReceipts || [],
  };
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

const ALERT_LOOKAHEAD_DAYS = 7;
const DERIVED_NOTIFICATION_TYPES = new Set(["DueSoon", "Overdue", "AssignmentChanged"]);
const WEEKDAY_ORDER = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];
const WEEKDAY_NAMES = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday"];

module.exports._private = {
  buildDerivedNotificationPlan,
  buildDueStatusNotification,
  buildAssignmentChangedNotifications,
  shouldSendNotificationPush,
  choreAppearsOnDate,
  parseRelativeDayOffset,
  dateRangeWithinWindow,
  dueAlertRecipientUserIds,
  assignmentAlertRecipientUserIds,
};
