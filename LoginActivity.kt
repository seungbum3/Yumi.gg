package com.example.yumi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Patterns

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private val firestore = FirebaseFirestore.getInstance()

    private val GOOGLE_SIGN_IN_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        auth = FirebaseAuth.getInstance()

        // UI 요소 초기화
        val btnGoogleSignIn: SignInButton = findViewById(R.id.btnGoogleSignIn)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<TextView>(R.id.btnRegister)
        val emailInput = findViewById<EditText>(R.id.editTextEmail)
        val passwordInput = findViewById<EditText>(R.id.editTextPassword)

        // 구글 로그인 초기화
        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            ).build()

        // 구글 로그인 버튼 클릭 이벤트
        btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        // 회원가입 화면 이동
        btnRegister.setOnClickListener {
            startActivity(Intent(this, JoinActivity::class.java))
        }

        btnLogin.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // 이메일 형식 체크
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "올바른 이메일 주소를 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                Toast.makeText(this, "비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // FirebaseAuth를 통한 로그인
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            // SharedPreferences에 uid 저장 (이후 Firestore 보안 규칙에 사용)
                            val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putString("loggedInUserId", email)
                                apply()
                            }
                            Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                            navigateToMainPage()
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "로그인 실패: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e("LoginActivity", "로그인 오류", task.exception)
                    }
                }
        }
    }

    // FirebaseAuth.signInWithEmailAndPassword를 이용한 로그인 함수
    private fun loginWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // SharedPreferences에 uid 저장 (필요한 경우)
                        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putString("loggedInUserId", email)
                            apply()
                        }
                        Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                        navigateToMainPage()
                    } else {
                        Toast.makeText(this, "로그인 실패: 사용자 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(this, "로그인 실패: ${task.exception?.message}", Toast.LENGTH_SHORT)
                        .show()
                    Log.e("LoginActivity", "로그인 오류", task.exception)
                }
            }
    }

    // 메인 페이지로 이동
    private fun navigateToMainPage() {
        val intent = Intent(this, MainpageActivity::class.java)
        startActivity(intent)
        finish() // 로그인 화면 종료
    }

    // 구글 로그인 시작
    private fun signInWithGoogle() {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                try {
                    startIntentSenderForResult(
                        result.pendingIntent.intentSender, GOOGLE_SIGN_IN_REQUEST_CODE,
                        null, 0, 0, 0, null
                    )
                } catch (e: Exception) {
                    Log.e("GoogleSignIn", "Google Sign-In Failed", e)
                    Toast.makeText(this, "Google 로그인 실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("GoogleSignIn", "Sign-In Request Failed", e)
                Toast.makeText(this, "Google 로그인 요청 실패: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // 구글 로그인 결과 처리
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_SIGN_IN_REQUEST_CODE) {
            try {
                val credential = oneTapClient.getSignInCredentialFromIntent(data)
                val idToken = credential.googleIdToken
                if (idToken != null) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                if (user != null) {
                                    val email = user.email ?: "unknown"
                                    val uid = user.uid

                                    // Firestore에 사용자 정보 저장 (이미 존재하지 않는 경우)
                                    val sanitizedEmail = email.replace(".", "_")
                                    val userRef = firestore.collection("users").document(sanitizedEmail)
                                    userRef.get().addOnSuccessListener { document ->
                                        if (!document.exists()) {
                                            val userData = hashMapOf(
                                                "email" to email,
                                                "uid" to uid,
                                                "createdAt" to com.google.firebase.Timestamp.now()
                                            )
                                            userRef.set(userData)
                                                .addOnSuccessListener {
                                                    Log.d("GoogleSignIn", "Firestore에 사용자 정보 저장 완료: $email")
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("GoogleSignIn", "Firestore 저장 실패", e)
                                                }
                                        } else {
                                            Log.d("GoogleSignIn", "사용자 정보가 이미 존재함: $email")
                                        }
                                    }

                                    Toast.makeText(this, "Google 간편 로그인 성공", Toast.LENGTH_SHORT).show()
                                    navigateToMainPage()
                                }
                            } else {
                                Log.e("GoogleSignIn", "Google 로그인 실패", task.exception)
                                Toast.makeText(this, "Google 로그인 실패", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            } catch (e: ApiException) {
                Log.e("GoogleSignIn", "Sign-in failed: ${e.message}")
                Toast.makeText(this, "Google 로그인 오류: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
