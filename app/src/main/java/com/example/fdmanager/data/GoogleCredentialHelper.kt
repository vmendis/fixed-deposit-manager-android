package com.example.fdmanager.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * #9 Firebase Auth — Google Sign-In via the Credential Manager API (the current,
 * non-deprecated path; the legacy GoogleSignInClient is not used).
 *
 * The server client id is resolved AT RUNTIME from the google-services-generated
 * resource `default_web_client_id`. Builds without the json simply don't have the
 * resource → the helper reports "not configured" instead of failing to compile.
 * (Compile-time R.string reference would break CI's json-less build.)
 */
object GoogleCredentialHelper {

    /** @param activityContext MUST be an Activity context — Credential Manager shows UI. */
    suspend fun fetchGoogleIdToken(activityContext: Context): Result<String> {
        val res = activityContext.resources.getIdentifier(
            "default_web_client_id", "string", activityContext.packageName
        )
        if (res == 0) {
            return Result.failure(IllegalStateException("Google Sign-In is not configured on this build"))
        }
        return try {
            val option = GetGoogleIdOption.Builder()
                .setServerClientId(activityContext.getString(res))
                .setFilterByAuthorizedAccounts(false)
                .build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val result = CredentialManager.create(activityContext)
                .getCredential(activityContext, request)
            val cred = result.credential
            if (cred is CustomCredential &&
                cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                Result.success(GoogleIdTokenCredential.createFrom(cred.data).idToken)
            } else {
                Result.failure(IllegalStateException("Unexpected credential type"))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(IllegalStateException("Sign-in cancelled", e))
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.message ?: "Google Sign-In failed", e))
        }
    }
}
