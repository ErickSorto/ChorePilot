const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs/promises");
const path = require("node:path");
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const {
  doc,
  setDoc,
  updateDoc,
  deleteDoc,
  getDoc,
} = require("firebase/firestore");
const {
  ref,
  uploadBytes,
  deleteObject,
} = require("firebase/storage");

const projectId = "chorepilot-rules-test";
const firestoreRulesPath = path.resolve(__dirname, "../../firestore.rules");
const storageRulesPath = path.resolve(__dirname, "../../storage.rules");

let testEnv;

test.before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId,
    firestore: {
      host: process.env.FIRESTORE_EMULATOR_HOST?.split(":")[0] || "127.0.0.1",
      port: Number.parseInt(process.env.FIRESTORE_EMULATOR_HOST?.split(":")[1] || "8080", 10),
      rules: await fs.readFile(firestoreRulesPath, "utf8"),
    },
    storage: {
      host: process.env.FIREBASE_STORAGE_EMULATOR_HOST?.split(":")[0] || "127.0.0.1",
      port: Number.parseInt(process.env.FIREBASE_STORAGE_EMULATOR_HOST?.split(":")[1] || "9199", 10),
      rules: await fs.readFile(storageRulesPath, "utf8"),
    },
  });
});

test.after(async () => {
  await testEnv.cleanup();
});

test.afterEach(async () => {
  await testEnv.clearFirestore();
  await testEnv.clearStorage();
});

test("member cannot create chores, invites, rewards, notifications, or admin household updates", async () => {
  await seedHousehold();

  const memberDb = authedFirestore("member-1");

  await assertFails(setDoc(doc(memberDb, "households/house-1/chores/new-chore"), {
    id: "new-chore",
    title: "New chore",
    additionalComments: "",
    roomId: "room-kitchen",
    roomName: "Kitchen",
    roomIconKey: "kitchen",
    roomColorKey: "teal",
    assignee: "Emma",
    assigneeId: "member-1",
    autoAssign: false,
    smartAssignmentMode: "FairRotation",
    assignmentPool: [],
    dayAssignments: {},
    effortLevel: "Standard",
    repeatsEnabled: true,
    repeatStartDate: "Today",
    repeatFrequency: "Weekly",
    dailyRepeatMode: "Weekdays",
    selectedWeekdays: ["Monday"],
    monthlyPattern: "",
    customPattern: "",
    selectedOneTimeDate: "",
    proofRequired: false,
    durationMinutes: 10,
    status: "DueSoon",
    isActive: true,
    pendingApproval: false,
    dueLabel: "Due later today",
    proofPhotoCount: 0,
    completedByName: "",
    completedAtEpochMs: 0,
    occurrences: {},
    discoverySessionId: "",
    discoveryImageStoragePath: "",
    discoveryImageUrl: "",
    discoveryGenerated: false,
  }));

  await assertFails(setDoc(doc(memberDb, "householdInvites/ABC123XYZ"), {
    code: "ABC123XYZ",
    householdId: "house-1",
    householdName: "River House",
    role: "Member",
    allowAnonymous: true,
    active: true,
    usageCount: 0,
    usageLimit: 0,
    createdAtEpochMs: 1,
    expiresAtEpochMs: 1000,
    lifecycleStatus: "Active",
  }));

  await assertFails(setDoc(doc(memberDb, "households/house-1/rewards/state"), {
    rules: { pointsPerCompletion: 20 },
  }, { merge: true }));

  await assertFails(setDoc(doc(memberDb, "households/house-1/notifications/manual"), {
    id: "manual",
    householdId: "house-1",
    choreId: "chore-1",
    type: "DueSoon",
    title: "Dog walk is due soon",
    body: "Emma is up next.",
    createdAtEpochMs: 1,
    recipientUserIds: ["member-1"],
    readReceipts: [],
  }));

  await assertFails(updateDoc(doc(memberDb, "households/house-1"), {
    ownerUserId: "member-1",
  }));
});

test("member can update self profile, complete own chore, submit proof doc, and upload own proof image", async () => {
  await seedHousehold();

  const memberDb = authedFirestore("member-1");
  const memberStorage = authedStorage("member-1");

  await assertSucceeds(updateDoc(doc(memberDb, "households/house-1/members/member-1"), {
    displayName: "Emma Stone",
    availabilityMode: "DayOff",
    availabilityStartDate: "2026-04-13",
    availabilityEndDate: "2026-04-13",
    availabilityStatusLabel: "Day off scheduled",
    availabilitySummary: "Emma is away today.",
  }));

  await assertSucceeds(updateDoc(doc(memberDb, "households/house-1/chores/chore-1"), {
    status: "Completed",
    dueLabel: "Completed",
    completedByName: "",
    completedAtEpochMs: 10,
    proofPhotoCount: 0,
  }));

  await assertSucceeds(setDoc(doc(memberDb, "households/house-1/proofs/proof-1"), {
    id: "proof-1",
    choreId: "chore-1",
    dateKey: "2026-04-13",
    choreTitle: "Dog walk",
    roomName: "Kitchen",
    uploadedByUserId: "member-1",
    uploadedByName: "Emma Stone",
    storagePath: "households/house-1/images/chores/chore-1/proofs/member-1/proof-1.jpg",
    downloadUrl: "https://example.com/proof-1.jpg",
    contentType: "image/jpeg",
    createdAtEpochMs: 11,
  }));

  await assertSucceeds(
    uploadBytes(
      ref(memberStorage, "households/house-1/images/chores/chore-1/proofs/member-1/proof-1.jpg"),
      new TextEncoder().encode("proof-image"),
      {
        contentType: "image/jpeg",
        customMetadata: {
          householdId: "house-1",
          choreId: "chore-1",
          userId: "member-1",
        },
      },
    ),
  );
});

test("member cannot approve proof, change another member, or write reassignment decisions", async () => {
  await seedHousehold();

  const memberDb = authedFirestore("member-1");

  await assertFails(updateDoc(doc(memberDb, "households/house-1/chores/chore-proof"), {
    status: "Completed",
    dueLabel: "Approved",
  }));

  await assertFails(updateDoc(doc(memberDb, "households/house-1/members/admin-1"), {
    role: "Member",
  }));

  await assertFails(setDoc(doc(memberDb, "households/house-1/reassignmentDecisions/decision-1"), {
    suggestionId: "decision-1",
    choreId: "chore-1",
    originalAssignee: "Emma",
    coverMember: "Mila",
    scopeStartDate: "2026-04-14",
    scopeEndDate: "2026-04-15",
    status: "Accepted",
    createdAtEpochMs: 12,
  }));
});

test("admin can perform admin writes and member can only update notification read state", async () => {
  await seedHousehold();

  const adminDb = authedFirestore("admin-1");
  const memberDb = authedFirestore("member-1");

  await assertSucceeds(updateDoc(doc(adminDb, "households/house-1/chores/chore-proof"), {
    status: "Completed",
    dueLabel: "Approved",
  }));

  await assertSucceeds(setDoc(doc(adminDb, "households/house-1/reassignmentDecisions/decision-1"), {
    suggestionId: "decision-1",
    choreId: "chore-1",
    originalAssignee: "Emma",
    coverMember: "Mila",
    scopeStartDate: "2026-04-14",
    scopeEndDate: "2026-04-15",
    status: "Accepted",
    createdAtEpochMs: 12,
  }));

  await assertSucceeds(updateDoc(doc(memberDb, "households/house-1/notifications/note-1"), {
    readReceipts: [{ userId: "member-1", readAtEpochMs: 20 }],
  }));

  await assertFails(updateDoc(doc(memberDb, "households/house-1/notifications/note-1"), {
    title: "Hacked",
  }));
});

test("member cannot delete someone else's proof image from storage", async () => {
  await seedHousehold();
  await seedStorageObject("households/house-1/images/chores/chore-1/proofs/admin-1/proof-admin.jpg");

  const memberStorage = authedStorage("member-1");
  await assertFails(deleteObject(ref(memberStorage, "households/house-1/images/chores/chore-1/proofs/admin-1/proof-admin.jpg")));
});

function authedFirestore(uid) {
  const context = testEnv.authenticatedContext(uid);
  return context.firestore();
}

function authedStorage(uid) {
  const context = testEnv.authenticatedContext(uid);
  return context.storage();
}

async function seedHousehold() {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();

    await setDoc(doc(db, "households/house-1"), {
      id: "house-1",
      name: "River House",
      sizeLabel: "4 people",
      ownerUserId: "owner-1",
      memberCount: 3,
      activeInviteCode: "RIVER123",
      lastInviteCode: "RIVER123",
      hasChorePilotPro: false,
      premiumPurchaserIds: [],
      premiumLastSyncedAtEpochMs: 0,
    });

    await Promise.all([
      setDoc(doc(db, "households/house-1/members/owner-1"), memberDoc("owner-1", "Erick", "Owner")),
      setDoc(doc(db, "households/house-1/members/admin-1"), memberDoc("admin-1", "Mila", "Admin")),
      setDoc(doc(db, "households/house-1/members/member-1"), memberDoc("member-1", "Emma", "Member")),
    ]);

    await Promise.all([
      setDoc(doc(db, "households/house-1/chores/chore-1"), choreDoc({
        id: "chore-1",
        title: "Dog walk",
        assignee: "Emma",
        assigneeId: "member-1",
        status: "DueSoon",
        dueLabel: "Due before dinner",
      })),
      setDoc(doc(db, "households/house-1/chores/chore-proof"), choreDoc({
        id: "chore-proof",
        title: "Kitchen reset",
        assignee: "Emma",
        assigneeId: "member-1",
        status: "PendingProof",
        dueLabel: "1 photo uploaded",
        proofRequired: true,
        proofPhotoCount: 1,
      })),
    ]);

    await setDoc(doc(db, "households/house-1/rewards/state"), {
      rules: {
        pointsPerCompletion: 12,
        proofSubmissionBonus: 4,
        proofApprovalBonus: 6,
        redemptionRequiresApproval: false,
      },
      rewardDefinitions: [],
      pointEvents: [],
      redemptionHistory: [],
      celebration: null,
    });

    await setDoc(doc(db, "households/house-1/notifications/note-1"), {
      id: "note-1",
      householdId: "house-1",
      choreId: "chore-1",
      type: "DueSoon",
      title: "Dog walk is due soon",
      body: "Emma is up next.",
      createdAtEpochMs: 1,
      recipientUserIds: ["member-1", "owner-1"],
      readReceipts: [],
    });

    await setDoc(doc(db, "householdInvites/RIVER123"), {
      code: "RIVER123",
      householdId: "house-1",
      householdName: "River House",
      role: "Member",
      allowAnonymous: true,
      active: true,
      usageCount: 0,
      usageLimit: 0,
      createdAtEpochMs: 1,
      expiresAtEpochMs: 9999999999999,
      lifecycleStatus: "Active",
    });
  });
}

async function seedStorageObject(storagePath) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const storage = context.storage();
    await uploadBytes(ref(storage, storagePath), new TextEncoder().encode("seed"), {
      contentType: "image/jpeg",
      customMetadata: {
        householdId: "house-1",
        choreId: "chore-1",
        userId: "admin-1",
      },
    });
  });
}

function memberDoc(userId, displayName, role) {
  return {
    userId,
    displayName,
    role,
    active: true,
    weeklyEffortScore: 0,
    lastAssignedCycleKey: "",
    availabilityMode: "Available",
    availabilityStartDate: "",
    availabilityEndDate: "",
    availabilityStatusLabel: "",
    availabilitySummary: "",
  };
}

function choreDoc(overrides) {
  return {
    id: overrides.id,
    title: overrides.title,
    additionalComments: "",
    roomId: "room-kitchen",
    roomName: "Kitchen",
    roomIconKey: "kitchen",
    roomColorKey: "teal",
    assignee: overrides.assignee,
    assigneeId: overrides.assigneeId,
    autoAssign: false,
    smartAssignmentMode: "FairRotation",
    assignmentPool: [],
    dayAssignments: {},
    effortLevel: "Standard",
    repeatsEnabled: true,
    repeatStartDate: "Today",
    repeatFrequency: "Weekly",
    dailyRepeatMode: "Weekdays",
    selectedWeekdays: ["Monday"],
    monthlyPattern: "",
    customPattern: "",
    selectedOneTimeDate: "",
    proofRequired: overrides.proofRequired || false,
    durationMinutes: 10,
    status: overrides.status,
    isActive: true,
    pendingApproval: false,
    dueLabel: overrides.dueLabel,
    proofPhotoCount: overrides.proofPhotoCount || 0,
    completedByName: "",
    completedAtEpochMs: 0,
    occurrences: {},
    discoverySessionId: "",
    discoveryImageStoragePath: "",
    discoveryImageUrl: "",
    discoveryGenerated: false,
  };
}
