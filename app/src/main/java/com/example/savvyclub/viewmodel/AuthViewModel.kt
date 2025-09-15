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

    // Экземпляр Firebase Authentication
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // ------------------ Состояние пользователя ------------------
    // Приватный поток для хранения email текущего пользователя
    private val _userState = MutableStateFlow<String?>(auth.currentUser?.email)
    // Публичная версия StateFlow для UI, чтобы можно было подписываться и получать обновления
    val userState: StateFlow<String?> = _userState

    // Google One Tap клиент (инициализируем позже через initGoogleSignInClient)
    private lateinit var oneTapClient: SignInClient

    // ------------------ Инициализация Google One Tap ------------------
    fun initGoogleSignInClient(activity: Activity) {
        // Инициализируем клиент Google One Tap для данного Activity
        oneTapClient = Identity.getSignInClient(activity)
    }

    // ------------------ Email/Password методы ------------------
    fun signUpWithEmail(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                // Обновляем email в StateFlow
                _userState.value = auth.currentUser?.email
                // Возвращаем результат в UI через callback
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
        auth.signOut() // Выход из Firebase
        _userState.value = null // Обнуляем состояние пользователя
        if (::oneTapClient.isInitialized) {
            oneTapClient.signOut() // Выход из One Tap
        }
    }

    // ------------------ Google One Tap Sign-In ------------------
    // Генерируем запрос для начала One Tap Sign-In
    fun getSignInRequest(clientId: String): BeginSignInRequest {
        return BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true) // включаем поддержку Google ID Token
                    .setServerClientId(clientId) // обязательно web-client-id из Firebase
                    .setFilterByAuthorizedAccounts(false) // показывать все аккаунты, не только ранее авторизованные
                    .build()
            )
            .setAutoSelectEnabled(false) // отключаем авто-выбор аккаунта
            .build()
    }

    // Обрабатываем результат One Tap Sign-In
    fun handleGoogleSignInResult(intent: Intent?, onResult: (Boolean, String?) -> Unit) {
        if (intent == null) {
            onResult(false, "Google Sign-In canceled")
            return
        }

        try {
            // Получаем credential из intent
            val credential: SignInCredential = oneTapClient.getSignInCredentialFromIntent(intent)
            val idToken = credential.googleIdToken
            if (idToken != null) {
                // Авторизация через Firebase с Google токеном
                firebaseAuthWithGoogle(idToken, onResult)
            } else {
                onResult(false, "No ID token found")
            }
        } catch (e: Exception) {
            onResult(false, e.localizedMessage)
        }
    }

    // Авторизация в Firebase через Google ID Token
    private fun firebaseAuthWithGoogle(idToken: String, onResult: (Boolean, String?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                _userState.value = auth.currentUser?.email // обновляем email
                onResult(task.isSuccessful, task.exception?.localizedMessage)
            }
    }

    // Запускаем Google One Tap Sign-In
    fun startGoogleSignIn(
        request: BeginSignInRequest,
        onSuccess: (IntentSenderRequest) -> Unit,
        onFailure: (String) -> Unit
    ) {
        oneTapClient.beginSignIn(request)
            .addOnSuccessListener { result ->
                // Отправляем IntentSender в UI для запуска
                onSuccess(
                    IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                )
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Unknown error")
            }
    }
}
