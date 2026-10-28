package com.mankeu.auth

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
)

/** The signed-in Google account, read straight from Google Sign-In (DEC-025: no Firebase). */
class GoogleIdentity(private val context: Context) {
    private val _currentUser = MutableStateFlow(lastAccount())
    val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    fun refresh() { _currentUser.value = lastAccount() }

    fun signOut() {
        GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
        _currentUser.value = null
    }

    private fun lastAccount(): AuthUser? = GoogleSignIn.getLastSignedInAccount(context)?.let {
        AuthUser(uid = it.id ?: it.email ?: "google-user", email = it.email, displayName = it.displayName, photoUrl = it.photoUrl?.toString())
    }
}
