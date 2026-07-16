import { applicationDefault, getApp, getApps, initializeApp } from "firebase-admin/app";
import { getAuth, type Auth } from "firebase-admin/auth";
import { getFirestore, type Firestore } from "firebase-admin/firestore";

import type { AppEnv } from "./env";

export type FirebaseAdminContext = {
  auth: Auth;
  firestore: Firestore;
};

export function initializeFirebaseAdmin(env: AppEnv): FirebaseAdminContext {
  const emulatorHost = process.env.FIRESTORE_EMULATOR_HOST;
  const credentialsPath = process.env.GOOGLE_APPLICATION_CREDENTIALS;
  const app =
    getApps().length === 0
      ? initializeApp({
          ...(emulatorHost === undefined && credentialsPath === undefined
            ? {}
            : emulatorHost !== undefined && credentialsPath === undefined
              ? {}
              : { credential: applicationDefault() }),
          projectId: env.firebaseProjectId
        })
      : getApp();

  return {
    auth: getAuth(app),
    firestore: getFirestore(app)
  };
}
