package com.example.fdmanager.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source

/**
 * Roadmap #8 acceptance probe — DEBUG BUILDS ONLY, zero user-facing behavior.
 *
 * Proves Firestore connectivity via Logcat (tag "FirebaseProbe"):
 * - `google-services.json` ABSENT  → logs "NOT configured … local-only mode" and does nothing.
 *   The app must remain fully functional in this state (opt-in sync principle, roadmap #23).
 * - json PRESENT → forces a SERVER read of a dummy document. Under the dev project's
 *   deny-all rules the expected (= PASSING) outcome is PERMISSION_DENIED: the server
 *   answering "denied" proves reachability. A successful read would mean the rules are
 *   NOT locked down and is logged as a warning.
 *
 * No data is written. The probed document path never exists. Reads of this kind are
 * billed as 1 read op at most — irrelevant on dev, and the probe never ships in release.
 */
object FirebaseProbe {
    private const val TAG = "FirebaseProbe"

    fun run(context: Context) {
        val debuggable =
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!debuggable) return

        if (FirebaseApp.getApps(context).isEmpty()) {
            Log.i(TAG, "Firebase NOT configured (google-services.json absent) — local-only mode, nothing to probe")
            return
        }

        val projectId = FirebaseApp.getInstance().options.projectId
        Log.i(TAG, "Firebase configured (project=$projectId) — probing Firestore (server read)...")

        FirebaseFirestore.getInstance()
            .collection("connectivity-probe").document("ping")
            .get(Source.SERVER)
            .addOnSuccessListener {
                Log.w(TAG, "Firestore REACHABLE — but the read SUCCEEDED, which is UNEXPECTED under deny-all rules. CHECK THE RULES TAB.")
            }
            .addOnFailureListener { e ->
                when ((e as? FirebaseFirestoreException)?.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                        Log.i(TAG, "Firestore REACHABLE — server answered PERMISSION_DENIED (EXPECTED under locked deny-all rules). Roadmap-8 acceptance: PASS")
                    FirebaseFirestoreException.Code.UNAVAILABLE ->
                        Log.w(TAG, "Firestore NOT reachable — UNAVAILABLE (no network, or server unreachable)")
                    else ->
                        Log.w(TAG, "Firestore probe failed — ${(e as? FirebaseFirestoreException)?.code}: $e")
                }
            }
    }
}
