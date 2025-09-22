package com.example.savvyclub.viewmodel

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savvyclub.R
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _userState = MutableStateFlow<String?>(auth.currentUser?.email)
    val userState: StateFlow<String?> = _userState

    private val defaultAvatar = "res:${R.drawable.default_avatar}"

    private val _selectedAvatar = MutableStateFlow(defaultAvatar)
    val selectedAvatar: StateFlow<String> = _selectedAvatar

    private lateinit var oneTapClient: SignInClient

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _userState.value = user?.email

        if (user != null) {
            loadAvatarForUser(user.uid, user.photoUrl?.toString())
        } else {
            _selectedAvatar.value = defaultAvatar
        }
    }

    init {
        auth.addAuthStateListener(authListener)
        auth.currentUser?.let { user ->
            loadAvatarForUser(user.uid, user.photoUrl?.toString())
        }
    }

    private fun loadAvatarForUser(uid: String, googleAvatarUrl: String?) {
        viewModelScope.launch {
            val dbRef = FirebaseDatabase.getInstance().getReference("users/$uid/avatar")
            dbRef.get().addOnSuccessListener { snapshot ->
                val avatarFromDb = snapshot.getValue(String::class.java)
                if (!avatarFromDb.isNullOrEmpty()) {
                    _selectedAvatar.value = avatarFromDb
                } else {
                    // Если в базе пусто, используем Google avatar или дефолт
                    val avatar = googleAvatarUrl ?: defaultAvatar
                    _selectedAvatar.value = avatar
                    // Сохраняем в базу
                    dbRef.setValue(avatar)
                }
            }.addOnFailureListener {
                // на случай ошибки, используем Google или дефолт
                _selectedAvatar.value = googleAvatarUrl ?: defaultAvatar
            }
        }
    }

    fun setSelectedAvatar(value: String) {
        _selectedAvatar.value = value
        auth.currentUser?.uid?.let { uid ->
            FirebaseDatabase.getInstance()
                .getReference("users/$uid/avatar")
                .setValue(value)
        }
    }

    fun initGoogleSignInClient(activity: Activity) {
        oneTapClient = Identity.getSignInClient(activity)
    }

    fun signUpWithEmail(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful, task.exception?.localizedMessage)
            }
    }

    fun signInWithEmail(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful, task.exception?.localizedMessage)
            }
    }

    fun signOut() {
        auth.signOut()
        if (::oneTapClient.isInitialized) oneTapClient.signOut()
        _selectedAvatar.value = defaultAvatar
    }

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
                onResult(task.isSuccessful, task.exception?.localizedMessage)
                task.result?.user?.let { user ->
                    loadAvatarForUser(user.uid, user.photoUrl?.toString())
                }
            }
    }

    fun startGoogleSignIn(
        request: BeginSignInRequest,
        onSuccess: (androidx.activity.result.IntentSenderRequest) -> Unit,
        onFailure: (String) -> Unit
    ) {
        oneTapClient.beginSignIn(request)
            .addOnSuccessListener { result ->
                onSuccess(androidx.activity.result.IntentSenderRequest.Builder(result.pendingIntent.intentSender).build())
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Unknown error")
            }
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
    }
}
