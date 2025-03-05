package com.example.yumi2

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
import com.google.firebase.Timestamp

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

        // 🔹 UI 요소 초기화
        val btnGoogleSignIn: SignInButton = findViewById(R.id.btnGoogleSignIn)
        val btnLogin: Button = findViewById(R.id.btnLogin)
        val btnRegister: TextView = findViewById(R.id.btnRegister)
        val idInput = findViewById<EditText>(R.id.editTextId)
        val passwordInput = findViewById<EditText>(R.id.editTextPassword)

        // 🔹 구글 로그인 초기화
        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            ).build()

        // 🔹 구글 로그인 버튼 클릭 이벤트
        btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        // 🔹 회원가입 이동
        btnRegister.setOnClickListener {
            val intent = Intent(this, JoinActivity::class.java)
            startActivity(intent)
        }

        // 🔹 Firestore에서 아이디 & 비밀번호 체크 후 로그인
        btnLogin.setOnClickListener {
            val id = idInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (id.isNotEmpty() && password.isNotEmpty()) {
                checkLogin(id, password)
            } else {
                Toast.makeText(this, "아이디와 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🔹 Firestore에서 유저 확인 후 로그인 진행
    private fun checkLogin(id: String, password: String) {
        firestore.collection("users")
            .whereEqualTo("id", id) // Firestore에서 입력한 ID와 동일한 문서를 검색
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
                        val storedPassword = document.getString("password") // 🔹 Firestore에서 비밀번호 가져오기
                        Log.d("FirestoreLogin", "DB 저장된 비밀번호: $storedPassword") // 🔹 디버깅 로그 추가
                        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putString("loggedInUserId", id)
                            apply()
                        }
                        if (storedPassword != null) {
                            if (storedPassword == password.trim()) { // 🔹 입력한 비밀번호와 Firestore에 저장된 값 비교
                                Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                                navigateToMainPage()
                                return@addOnSuccessListener
                            } else {
                                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "비밀번호 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "존재하지 않는 아이디입니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "로그인 오류 발생: ${exception.message}", Toast.LENGTH_SHORT).show()
                Log.e("FirestoreLogin", "로그인 오류", exception)
            }
    }

    // 🔹 메인 페이지로 이동
    private fun navigateToMainPage() {
        try {
            val intent = Intent(this, MainpageActivity::class.java)
            startActivity(intent)
            finish() // 로그인 화면 종료
        } catch (e: Exception) {
            Log.e("LoginActivity", "메인 페이지 이동 오류: ${e.message}", e)
        }
    }

    // 🔹 구글 로그인 시작
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

    // 🔹 구글 로그인 결과 처리
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

                                    // 🔹 Firestore에서 이메일을 문서 ID로 변환 ('.' → '_')
                                    val sanitizedEmail = email.replace(".", "_")

                                    // 🔹 Firestore에 사용자 정보 저장 (중복 방지)
                                    val userRef = firestore.collection("users").document(sanitizedEmail)
                                    userRef.get().addOnSuccessListener { document ->
                                        if (!document.exists()) {
                                            // 🔹 Firestore에 Timestamp로 시간 저장
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
