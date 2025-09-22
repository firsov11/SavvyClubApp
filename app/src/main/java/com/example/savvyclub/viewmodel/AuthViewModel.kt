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
    private val database = FirebaseDatabase.getInstance().reference

    private val defaultAvatar = "res:${R.drawable.default_avatar}"

    private val _selectedAvatar = MutableStateFlow(defaultAvatar)
    val selectedAvatar: StateFlow<String> = _selectedAvatar

    private val _userState = MutableStateFlow<String?>(auth.currentUser?.email)
    val userState: StateFlow<String?> = _userState

    private lateinit var oneTapClient: SignInClient

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _userState.value = user?.email
        // При изменении авторизации загружаем аватар из Firebase
        loadAvatarFromFirebase(user?.uid)
    }

    init {
        auth.addAuthStateListener(authListener)
        viewModelScope.launch {
            auth.currentUser?.uid?.let { uid ->
                FirebaseDatabase.getInstance()
                    .getReference("users/$uid/avatar")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val avatarFromDb = snapshot.getValue(String::class.java)
                        if (!avatarFromDb.isNullOrEmpty()) {
                            _selectedAvatar.value = avatarFromDb
                        } else {
                            // Если в базе пусто, проверяем Google avatar
                            val googleAvatar = auth.currentUser?.photoUrl?.toString()
                            _selectedAvatar.value = googleAvatar ?: defaultAvatar
                            // Сохраняем в базе, чтобы потом не тянуть снова
                            googleAvatar?.let {
                                FirebaseDatabase.getInstance()
                                    .getReference("users/$uid/avatar")
                                    .setValue(it)
                            }
                        }
                    }
            }
        }
    }

    /**
     * Устанавливаем аватар локально и сразу сохраняем в Firebase
     */
    fun setSelectedAvatar(value: String) {
        _selectedAvatar.value = value
        auth.currentUser?.uid?.let { uid ->
            database.child("users/$uid/avatar").setValue(value)
        }
    }

    /**
     * Загружаем аватар из Firebase, только если он отличается от локального
     */
    private fun loadAvatarFromFirebase(uid: String?) {
        if (uid == null) return
        database.child("users/$uid/avatar")
            .get()
            .addOnSuccessListener { snapshot ->
                val remoteAvatar = snapshot.getValue(String::class.java)
                if (!remoteAvatar.isNullOrEmpty() && remoteAvatar != _selectedAvatar.value) {
                    _selectedAvatar.value = remoteAvatar
                }
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
        if (::oneTapClient.isInitialized) {
            oneTapClient.signOut()
        }
        // Сбрасываем аватар на дефолтный
        _selectedAvatar.value = defaultAvatar
    }


    // --- Google One Tap ---
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
            }
    }

    fun startGoogleSignIn(
        request: BeginSignInRequest,
        onSuccess: (androidx.activity.result.IntentSenderRequest) -> Unit,
        onFailure: (String) -> Unit
    ) {
        oneTapClient.beginSignIn(request)
            .addOnSuccessListener { result ->
                onSuccess(
                    androidx.activity.result.IntentSenderRequest.Builder(
                        result.pendingIntent.intentSender
                    ).build()
                )
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
