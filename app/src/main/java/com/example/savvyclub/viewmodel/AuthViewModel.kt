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
    private val defaultAvatar = "res:${R.drawable.default_avatar}"

    private val _userState = MutableStateFlow<String?>(auth.currentUser?.email)
    val userState: StateFlow<String?> = _userState

    private val _selectedAvatar = MutableStateFlow(defaultAvatar)
    val selectedAvatar: StateFlow<String> = _selectedAvatar

    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName

    private lateinit var oneTapClient: SignInClient

    /** Загружаем данные пользователя из Firebase (имя и аватар) */
    fun loadUserData() {
        auth.currentUser?.uid?.let { uid ->
            val dbRef = FirebaseDatabase.getInstance().getReference("users/$uid")
            dbRef.get().addOnSuccessListener { snapshot ->
                _selectedAvatar.value = snapshot.child("avatar").getValue(String::class.java) ?: defaultAvatar
                _userName.value = snapshot.child("name").getValue(String::class.java) ?: ""
            }
            setupPresence()
        }
    }

    /** Сохраняем выбранный аватар */
    fun setSelectedAvatar(value: String) {
        _selectedAvatar.value = value
        auth.currentUser?.uid?.let { uid ->
            FirebaseDatabase.getInstance()
                .getReference("users/$uid/avatar")
                .setValue(value)
        }
    }

    /** Сохраняем имя пользователя */
    fun saveNameForEmailUser(name: String) {
        auth.currentUser?.let { user ->
            _userName.value = name
            val avatar = _selectedAvatar.value
            val email = user.email ?: ""
            saveUserData(user.uid, name, email, avatar, online = true)
        }
    }

    /** Записываем данные пользователя в базу */
    private fun saveUserData(
        uid: String,
        name: String,
        email: String,
        avatar: String,
        online: Boolean = true
    ) {
        val userMap = mapOf(
            "name" to name,
            "email" to email,
            "avatar" to avatar,
            "online" to online
        )
        FirebaseDatabase.getInstance()
            .getReference("users/$uid")
            .updateChildren(userMap)
    }

    /** Онлайн/оффлайн флаг */
    fun setOnlineStatus(isOnline: Boolean) {
        auth.currentUser?.uid?.let { uid ->
            FirebaseDatabase.getInstance()
                .getReference("users/$uid/online")
                .setValue(isOnline)
        }
    }

    private fun setupPresence() {
        auth.currentUser?.uid?.let { uid ->
            val userStatusRef = FirebaseDatabase.getInstance().getReference("users/$uid/online")
            userStatusRef.onDisconnect().setValue(false)
            userStatusRef.setValue(true)
        }
    }

    fun signOut() {
        setOnlineStatus(false)
        auth.signOut()
        if (::oneTapClient.isInitialized) oneTapClient.signOut()
        _selectedAvatar.value = defaultAvatar
        _userName.value = ""
    }

    /** Google Sign-In */
    fun initGoogleSignInClient(activity: Activity) {
        oneTapClient = Identity.getSignInClient(activity)
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
        if (intent == null) { onResult(false, "Google Sign-In canceled"); return }
        try {
            val credential: SignInCredential = oneTapClient.getSignInCredentialFromIntent(intent)
            val idToken = credential.googleIdToken
            if (idToken != null) firebaseAuthWithGoogle(idToken, onResult)
            else onResult(false, "No ID token found")
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
                    val avatar = user.photoUrl?.toString() ?: defaultAvatar
                    val name = user.displayName ?: user.email ?: ""
                    val email = user.email ?: ""
                    _selectedAvatar.value = avatar
                    _userName.value = name
                    saveUserData(user.uid, name, email, avatar, online = true)
                    setupPresence()
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
            .addOnFailureListener { e -> onFailure(e.localizedMessage ?: "Unknown error") }
    }

    /** Вход через email/password */
    fun signInWithEmail(
        email: String,
        password: String,
        onResult: (success: Boolean, message: String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    _userState.value = user?.email
                    _selectedAvatar.value = user?.photoUrl?.toString() ?: defaultAvatar
                    // Загружаем данные пользователя из Firebase
                    loadUserData()
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.localizedMessage)
                }
            }
    }

    /** Регистрация через email/password */
    fun signUpWithEmail(
        email: String,
        password: String,
        onResult: (success: Boolean, message: String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    _userState.value = user?.email
                    _selectedAvatar.value = defaultAvatar
                    // Сохраняем базовые данные нового пользователя в Firebase
                    user?.let {
                        saveUserData(it.uid, name = "", email = email, avatar = defaultAvatar, online = true)
                        setupPresence()
                    }
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.localizedMessage)
                }
            }
    }

}
