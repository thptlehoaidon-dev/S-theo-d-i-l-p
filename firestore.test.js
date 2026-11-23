const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_monitor_uid";
const BOB_UID = "bob_teacher_uid";
const SCHOOL_ID = "thpt_main";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validRoomPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    schoolId: SCHOOL_ID,
    floor: 3,
    className: "12A1",
    roomCode: "P.301",
    memberUids: [uid],
    reportDate: "2026-11-22",
    absences: [
      { studentName: "Nguyễn Văn An", absenceType: "CO_PHEP", reason: "Bị sốt" },
    ],
    hygieneStatus: "SACH_SE",
    hygieneTimeText: "07:35",
    hygieneOnTime: true,
    hygieneNote: "",
    hygieneChecker: "Nhóm trực nhật 12A1",
    hygieneFloorClean: true,
    hygieneBoardClean: true,
    hygieneBinEmptied: true,
    devices: {
      lamp1: "GOOD",
      lamp2: "BROKEN",
      lamp3: "GOOD",
      lamp4: "GOOD",
      fan1: "GOOD",
      fan2: "GOOD",
      tv: "GOOD",
      wifi: "GOOD",
    },
    deviceNote: "Đèn 2 cháy bóng",
    deviceConfirmedBy: "Lớp trưởng 12A1",
    deviceConfirmedAtText: "07:20",
    rolePin: "1234",
    rolePattern: "1-4-7-8-9",
    updatedByUid: uid,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

async function seedSchoolAndRoom(db, uid, roomId = "12A1") {
  const now = new Date(Date.now() - 5000);
  const batch = db.batch();
  const schoolRef = db.collection("schools").doc(SCHOOL_ID);
  batch.set(schoolRef, {
    name: "Trường THPT - Dãy phòng học",
    memberUids: [uid],
    createdByUid: uid,
    createdAt: now,
    updatedAt: now,
  });
  const roomRef = schoolRef.collection("rooms").doc(roomId);
  batch.set(roomRef, validRoomPayload(uid));
  await batch.commit();
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write schools or rooms", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("schools").doc(SCHOOL_ID).get());
  await assertFails(
    unauthDb.collection("schools").doc(SCHOOL_ID).collection("rooms").get()
  );
  await assertFails(
    unauthDb.collection("schools").doc(SCHOOL_ID).collection("rooms").doc("12A1").set(
      validRoomPayload(ALICE_UID)
    )
  );
});

test("Authenticated user: atomic batch seed and array-contains query succeed; unfiltered query fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(seedSchoolAndRoom(aliceDb, ALICE_UID, "12A1"));

  // Mirrors Kotlin SchoolRepository.observeRooms(uid)
  await assertSucceeds(
    aliceDb
      .collection("schools")
      .doc(SCHOOL_ID)
      .collection("rooms")
      .where("memberUids", "array-contains", ALICE_UID)
      .get()
  );

  // Unfiltered list query must fail
  await assertFails(
    aliceDb.collection("schools").doc(SCHOOL_ID).collection("rooms").get()
  );
});

test("Cross-user isolation & safe school join flow", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(seedSchoolAndRoom(aliceDb, ALICE_UID, "12A1"));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Bob cannot read or mutate room before joining school
  await assertFails(
    bobDb.collection("schools").doc(SCHOOL_ID).collection("rooms").doc("12A1").get()
  );
  await assertFails(
    bobDb.collection("schools").doc(SCHOOL_ID).collection("rooms").doc("12A1").update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedByUid: BOB_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob joins schools/thpt_main first
  await assertSucceeds(
    bobDb.collection("schools").doc(SCHOOL_ID).update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Now Bob can add his UID to rooms/12A1 and read it
  await assertSucceeds(
    bobDb.collection("schools").doc(SCHOOL_ID).collection("rooms").doc("12A1").update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedByUid: BOB_UID,
      updatedAt: new Date(Date.now() - 400),
    })
  );
  await assertSucceeds(
    bobDb.collection("schools").doc(SCHOOL_ID).collection("rooms").doc("12A1").get()
  );
});

test("Kotlin-mirrored partial updates (absences, hygiene, devices, role credentials) succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(seedSchoolAndRoom(aliceDb, ALICE_UID, "12A1"));

  const roomRef = aliceDb.collection("schools").doc(SCHOOL_ID).collection("rooms").doc("12A1");

  // 1. updateAbsences
  await assertSucceeds(
    roomRef.update({
      reportDate: "2026-11-23",
      absences: [
        { studentName: "Trần Bình", absenceType: "KHONG_PHEP", reason: "Nghỉ không lý do" },
      ],
      updatedByUid: ALICE_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // 2. submitHygiene
  await assertSucceeds(
    roomRef.update({
      reportDate: "2026-11-23",
      hygieneStatus: "CAN_NHAC_NHO",
      hygieneTimeText: "08:10",
      hygieneOnTime: true,
      hygieneNote: "Còn rác dưới bàn",
      hygieneChecker: "Nhóm trực nhật 12A1",
      hygieneFloorClean: false,
      hygieneBoardClean: true,
      hygieneBinEmptied: true,
      updatedByUid: ALICE_UID,
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // 3. updateDevices
  await assertSucceeds(
    roomRef.update({
      devices: {
        lamp1: "GOOD",
        lamp2: "GOOD",
        lamp3: "GOOD",
        lamp4: "GOOD",
        fan1: "GOOD",
        fan2: "BROKEN",
        tv: "GOOD",
        wifi: "GOOD",
      },
      deviceNote: "Quạt 2 quay chậm",
      deviceConfirmedBy: "Lớp trưởng 12A1",
      deviceConfirmedAtText: "08:12",
      updatedByUid: ALICE_UID,
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // 4. updateRoleCredentials
  await assertSucceeds(
    roomRef.update({
      rolePin: "5678",
      rolePattern: "1-2-3-6-9",
      updatedByUid: ALICE_UID,
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("Security & schema validation rejects invalid payloads, ghost fields, and immutable mutations", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(seedSchoolAndRoom(aliceDb, ALICE_UID, "12A1"));

  const roomsCol = aliceDb.collection("schools").doc(SCHOOL_ID).collection("rooms");
  const roomRef = roomsCol.doc("12A1");

  // Reject extra @DocumentId `id` field on create
  await assertFails(
    roomsCol.doc("12A2").set(validRoomPayload(ALICE_UID, { id: "12A2", className: "12A2" }))
  );

  // Reject ghost field on update
  await assertFails(
    roomRef.update({
      deviceNote: "Test",
      ghostAdminFlag: true,
      updatedByUid: ALICE_UID,
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Reject tampering with immutable floor or createdAt
  await assertFails(
    roomRef.update({
      floor: 2,
      updatedByUid: ALICE_UID,
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertFails(
    roomRef.update({
      createdAt: new Date(Date.now() - 100),
      updatedByUid: ALICE_UID,
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Reject invalid device map (missing wifi key)
  await assertFails(
    roomRef.update({
      devices: {
        lamp1: "GOOD",
        lamp2: "GOOD",
        lamp3: "GOOD",
        lamp4: "GOOD",
        fan1: "GOOD",
        fan2: "GOOD",
        tv: "GOOD",
      },
      deviceNote: "",
      deviceConfirmedBy: "Lớp trưởng 12A1",
      deviceConfirmedAtText: "08:00",
      updatedByUid: ALICE_UID,
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Reject invalid PIN format (3 digits)
  await assertFails(
    roomRef.update({
      rolePin: "123",
      rolePattern: "1-4-7-8-9",
      updatedByUid: ALICE_UID,
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Reject future timestamp
  await assertFails(
    roomRef.update({
      rolePin: "9999",
      rolePattern: "1-4-7-8-9",
      updatedByUid: ALICE_UID,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});
