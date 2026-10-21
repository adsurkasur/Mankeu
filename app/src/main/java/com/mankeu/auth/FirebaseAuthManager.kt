package com.mankeu.auth

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { exception -> cont.resumeWithException(exception) }
    addOnCanceledListener { cont.cancel() }
}

class FirebaseAuthManager(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private val _currentUser = MutableStateFlow<AuthUser?>(auth.currentUser?.toAuthUser())
    val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _currentUser.value = firebaseAuth.currentUser?.toAuthUser()
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    suspend fun signInWithCredential(credential: AuthCredential): Result<AuthUser> {
        return runCatching {
            val authResult = auth.signInWithCredential(credential).awaitTask()
            val user = authResult.user?.toAuthUser() ?: error("Sign in succeeded but user is null.")
            _currentUser.value = user
            user
        }
    }

    suspend fun signInWithGoogleIdToken(idToken: String): Result<AuthUser> {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return signInWithCredential(credential)
    }

    fun setCustomUser(user: AuthUser?) {
        _currentUser.value = user
    }

    fun signOut() {
        auth.signOut()
        _currentUser.value = null
    }

    /**
     * "Hapus semua data" (DEC-020): removes the Firebase user record, then signs out. Firebase may
     * refuse with "requires recent login"; the caller is then told, and the user is still signed out.
     * Returns whether the record was deleted (true when there was none).
     */
    suspend fun deleteAccount(): Boolean {
        val deleted = runCatching { auth.currentUser?.delete()?.awaitTask() }.isSuccess
        signOut()
        return deleted
    }

    private fun FirebaseUser.toAuthUser(): AuthUser = AuthUser(
        uid = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl?.toString(),
    )
}
