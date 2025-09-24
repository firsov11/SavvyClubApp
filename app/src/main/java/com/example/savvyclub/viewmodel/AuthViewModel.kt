package com.example.savvyclub.viewmodel

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savvyclub.R
import com.example.savvyclub.data.UserPreferences
import com.example.savvyclub.data.model.Comrade
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

class AuthViewModel(private val userPreferences: UserPreferences) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val defaultAvatar = "res:${R.drawable.default_avatar}"

    private val _userState = MutableStateFlow<Comrade?>(null)
    val userState: StateFlow<Comrade?> = _userState

    private lateinit var oneTapClient: SignInClient

    init {
        // Загружаем локальные данные
        loadLocalUserData()

        // Если есть Firebase-пользователь, подтягиваем данные из Firebase
        auth.currentUser?.uid?.let { loadUserDataFromFirebase(it) }
    }

    /** Сохраняем пользователя в Firebase и локально */
    private fun saveUserData(comrade: Comrade) {
        comrade.uid?.let { uid ->
            val userMap = mapOf(
                "name" to comrade.name,
                "email" to comrade.email,
                "avatar" to comrade.avatarUrl,
                "online" to comrade.isOnline
            )
            FirebaseDatabase.getInstance().getReference("users/$uid")
                .updateChildren(userMap)
        }
        saveLocally(comrade)
    }

    /** Сохраняем локально */
    fun saveLocally(comrade: Comrade) {
        viewModelScope.launch {
            userPreferences.saveUser(comrade)
            _userState.value = comrade
        }
    }

    /** Загружаем локальные данные */
    private fun loadLocalUserData() {
        viewModelScope.launch {
            userPreferences.userFlow.collect { comrade ->
                comrade?.let { _userState.value = it }
            }
        }
    }

    /** Загружаем данные пользователя из Firebase */
    fun loadUserDataFromFirebase(uid: String) {
        val dbRef = FirebaseDatabase.getInstance().getReference("users/$uid")
        dbRef.get().addOnSuccessListener { snapshot ->
            val comrade = Comrade(
                uid = uid,
                name = snapshot.child("name").getValue(String::class.java) ?: "",
                email = snapshot.child("email").getValue(String::class.java) ?: "",
                avatarUrl = snapshot.child("avatar").getValue(String::class.java) ?: defaultAvatar,
                isOnline = snapshot.child("online").getValue(Boolean::class.java) ?: false
            )
            saveUserData(comrade)
            setupPresence(uid)
        }
    }

    /** Онлайн/оффлайн */
    fun setOnlineStatus(isOnline: Boolean) {
        _userState.value?.uid?.let { uid ->
            FirebaseDatabase.getInstance().getReference("users/$uid/online")
                .setValue(isOnline)
        }
    }

    /** Настройка online/offline при отключении */
    private fun setupPresence(uid: String) {
        val userStatusRef = FirebaseDatabase.getInstance().getReference("users/$uid/online")
        userStatusRef.onDisconnect().setValue(false)
        userStatusRef.setValue(true)
    }

    /** Выход из аккаунта */
    fun signOut() {
        _userState.value?.uid?.let { setOnlineStatus(false) }
        auth.signOut()
        if (::oneTapClient.isInitialized) oneTapClient.signOut()
        _userState.value = null
    }

    /** Инициализация Google Sign-In */
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
                val user = task.result?.user
                if (task.isSuccessful && user != null) {
                    val comrade = Comrade(
                        uid = user.uid,
                        name = user.displayName ?: "",
                        email = user.email ?: "",
                        avatarUrl = user.photoUrl?.toString() ?: defaultAvatar,
                        isOnline = true
                    )
                    saveUserData(comrade)
                }
                onResult(task.isSuccessful, task.exception?.localizedMessage)
            }
    }

    /** Вход через Email/Password */
    fun signInWithEmail(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                val user = auth.currentUser
                if (task.isSuccessful && user != null) {
                    loadUserDataFromFirebase(user.uid)
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.localizedMessage)
                }
            }
    }

    /** Регистрация через Email/Password */
    fun signUpWithEmail(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                val user = auth.currentUser
                if (task.isSuccessful && user != null) {
                    val comrade = Comrade(
                        uid = user.uid,
                        name = "",
                        email = email,
                        avatarUrl = defaultAvatar,
                        isOnline = true
                    )
                    saveUserData(comrade)
                    setupPresence(user.uid)
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.localizedMessage)
                }
            }
    }

    fun startGoogleSignIn(
        request: BeginSignInRequest,
        onSuccess: (androidx.activity.result.IntentSenderRequest) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (!::oneTapClient.isInitialized) {
            onFailure("Google Sign-In client не инициализирован")
            return
        }

        oneTapClient.beginSignIn(request)
            .addOnSuccessListener { result ->
                val intentSenderRequest = androidx.activity.result.IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                onSuccess(intentSenderRequest)
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Unknown error")
            }
    }

}
