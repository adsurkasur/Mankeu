package com.mankeu.auth

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * An access token for the app's own Drive folder (`drive.appdata`), for the signed-in Google
 * account. One place for what backup, "Hapus semua data" and sync each used to repeat.
 */
class DriveTokenProvider(private val context: Context) {
    fun signedIn(): Boolean = GoogleSignIn.getLastSignedInAccount(context)?.account != null

    suspend fun token(fallbackEmail: String? = null): String = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context)?.account
            ?: fallbackEmail?.let { Account(it, "com.google") }
            ?: error("Belum masuk akun Google.")
        GoogleAuthUtil.getToken(context, account, SCOPE)
    }

    private companion object {
        const val SCOPE = "oauth2:https://www.googleapis.com/auth/drive.appdata"
    }
}
