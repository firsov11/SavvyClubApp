package com.example.savvyclub.viewmodel

import android.app.Activity
import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Состояние текущего пользователя
    private val _userState = MutableStateFlow<String?>(auth.currentUser?.email)
    val userState: StateFlow<String?> = _userState

    private lateinit var oneTapClient: SignInClient

    // Инициализация Google One Tap клиента
    fun initGoogleSignInClient(activity: Activity) {
        oneTapClient = Identity.getSignInClient(activity)
    }

    // ----------------- Email/Password -----------------
    fun signUpWithEmail(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _userState.value = auth.currentUser?.email
                onResult(task.isSuccessful, task.exception?.localizedMessage)
            }
    }

    fun signInWithEmail(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _userState.value = auth.currentUser?.email
                onResult(task.isSuccessful, task.exception?.localizedMessage)
            }
    }

    fun signOut() {
        auth.signOut()
        _userState.value = null
        if (::oneTapClient.isInitialized) {
            oneTapClient.signOut()
        }
    }

    // ----------------- Google One Tap Sign-In -----------------
    fun getSignInRequest(clientId: String): BeginSignInRequest {
        return BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(clientId)
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .setAutoSelectEnabled(false)
            .build()
    }

    fun handleGoogleSignInResult(intent: Intent?, onResult: (Boolean, String?) -> Unit) {
        if (intent == null) {
            onResult(false, "Google Sign-In canceled")
            return
        }

        try {
            val credential: SignInCredential = oneTapClient.getSignInCredentialFromIntent(intent)
            val idToken = credential.googleIdToken
            if (idToken != null) {
                firebaseAuthWithGoogle(idToken, onResult)
            } else {
                onResult(false, "No ID token found")
            }
        } catch (e: Exception) {
            onResult(false, e.localizedMessage)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String, onResult: (Boolean, String?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                _userState.value = auth.currentUser?.email
                onResult(task.isSuccessful, task.exception?.localizedMessage)
            }
    }

    fun startGoogleSignIn(
        request: BeginSignInRequest,
        onSuccess: (IntentSenderRequest) -> Unit,
        onFailure: (String) -> Unit
    ) {
        oneTapClient.beginSignIn(request)
            .addOnSuccessListener { result ->
                onSuccess(
                    IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                )
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Unknown error")
            }
    }

}
