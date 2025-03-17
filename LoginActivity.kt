package com.example.yumi2

<<<<<<< Updated upstream
import android.content.Context
=======
<<<<<<< HEAD
=======
import android.content.Context
>>>>>>> a870181 (3월 17일)
>>>>>>> Stashed changes
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
<<<<<<< Updated upstream
import com.google.firebase.firestore.SetOptions
import android.util.Patterns
=======
<<<<<<< HEAD
import com.google.firebase.Timestamp
=======
import com.google.firebase.firestore.SetOptions
import android.util.Patterns
>>>>>>> a870181 (3월 17일)
>>>>>>> Stashed changes

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

<<<<<<< Updated upstream
=======
<<<<<<< HEAD
        // 🔹 UI 요소 초기화
>>>>>>> Stashed changes
        val btnGoogleSignIn: SignInButton = findViewById(R.id.btnGoogleSignIn)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<TextView>(R.id.btnRegister)
        val emailInput = findViewById<EditText>(R.id.editTextEmail)
        val passwordInput = findViewById<EditText>(R.id.editTextPassword)

<<<<<<< Updated upstream
=======
        // 🔹 구글 로그인 초기화
=======
        val btnGoogleSignIn: SignInButton = findViewById(R.id.btnGoogleSignIn)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<TextView>(R.id.btnRegister)
        val emailInput = findViewById<EditText>(R.id.editTextEmail)
        val passwordInput = findViewById<EditText>(R.id.editTextPassword)

>>>>>>> a870181 (3월 17일)
>>>>>>> Stashed changes
        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            ).build()

<<<<<<< Updated upstream
        btnGoogleSignIn.setOnClickListener { signInWithGoogle() }
=======
<<<<<<< HEAD
        // 🔹 구글 로그인 버튼 클릭 이벤트
        btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }
>>>>>>> Stashed changes

        btnRegister.setOnClickListener {
            startActivity(Intent(this, JoinActivity::class.java))
        }

        btnLogin.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "올바른 이메일 주소를 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                Toast.makeText(this, "비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            val uid = user.uid

                            val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putString("loggedInUID", uid) // 🔥 UID 저장
                                apply()
                            }

                            Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                            navigateToMainPage()
                        }
                    } else {
                        Toast.makeText(this, "로그인 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        Log.e("LoginActivity", "로그인 오류", task.exception)
                    }
                }
        }
    }

<<<<<<< Updated upstream
=======
    // 🔹 구글 로그인 시작
=======
        btnGoogleSignIn.setOnClickListener { signInWithGoogle() }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, JoinActivity::class.java))
        }

        btnLogin.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "올바른 이메일 주소를 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                Toast.makeText(this, "비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            val uid = user.uid

                            val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putString("loggedInUID", uid) // 🔥 UID 저장
                                apply()
                            }

                            Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                            navigateToMainPage()
                        }
                    } else {
                        Toast.makeText(this, "로그인 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        Log.e("LoginActivity", "로그인 오류", task.exception)
                    }
                }
        }
    }

>>>>>>> Stashed changes
    private fun navigateToMainPage() {
        startActivity(Intent(this, MainpageActivity::class.java))
        finish()
    }

<<<<<<< Updated upstream
=======
>>>>>>> a870181 (3월 17일)
>>>>>>> Stashed changes
    private fun signInWithGoogle() {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                try {
                    startIntentSenderForResult(
                        result.pendingIntent.intentSender, GOOGLE_SIGN_IN_REQUEST_CODE,
                        null, 0, 0, 0, null
                    )
                } catch (e: Exception) {
<<<<<<< Updated upstream
                    Log.e("GoogleSignIn", "Google 로그인 실패", e)
=======
<<<<<<< HEAD
                    Log.e("GoogleSignIn", "Google Sign-In Failed", e)
=======
                    Log.e("GoogleSignIn", "Google 로그인 실패", e)
>>>>>>> a870181 (3월 17일)
>>>>>>> Stashed changes
                    Toast.makeText(this, "Google 로그인 실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
<<<<<<< Updated upstream
                Log.e("GoogleSignIn", "로그인 요청 실패", e)
=======
<<<<<<< HEAD
                Log.e("GoogleSignIn", "Sign-In Request Failed", e)
=======
                Log.e("GoogleSignIn", "로그인 요청 실패", e)
>>>>>>> a870181 (3월 17일)
>>>>>>> Stashed changes
                Toast.makeText(this, "Google 로그인 요청 실패: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

<<<<<<< Updated upstream
=======
<<<<<<< HEAD
    // 🔹 구글 로그인 결과 처리
=======
>>>>>>> a870181 (3월 17일)
>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
                                    val userRef = firestore.collection("users").document(uid)
=======
<<<<<<< HEAD
                                    // 🔹 Firestore에서 이메일을 문서 ID로 변환 ('.' → '_')
                                    val sanitizedEmail = email.replace(".", "_")
>>>>>>> Stashed changes

                                    val userData = hashMapOf(
                                        "email" to email,
                                        "uid" to uid,
                                        "createdAt" to com.google.firebase.Timestamp.now()
                                    )
                                    userRef.set(userData, SetOptions.merge())

                                    val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                                    with(sharedPref.edit()) {
                                        putString("loggedInUID", uid) // 🔥 UID 저장
                                        apply()
                                    }

                                    val userProfileRef = firestore.collection("user_profiles").document(uid)
                                    userProfileRef.get().addOnSuccessListener { document ->
                                        if (!document.exists()) {
                                            val defaultProfile = hashMapOf(
                                                "nickname" to (user.displayName ?: "닉네임 없음"),
                                                "myinfo" to "아직 자기소개가 없습니다.",
                                                "theme" to "default",
                                                "profileImageUrl" to "gs://yumi-5f5c0.firebasestorage.app/default_profile.jpg"
                                            )
<<<<<<< Updated upstream
                                            userProfileRef.set(defaultProfile)
=======
                                            userRef.set(userData)
                                                .addOnSuccessListener {
                                                    Log.d("GoogleSignIn", "Firestore에 사용자 정보 저장 완료: $email")
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("GoogleSignIn", "Firestore 저장 실패", e)
                                                }
                                        } else {
                                            Log.d("GoogleSignIn", "사용자 정보가 이미 존재함: $email")
=======
                                    val userRef = firestore.collection("users").document(uid)

                                    val userData = hashMapOf(
                                        "email" to email,
                                        "uid" to uid,
                                        "createdAt" to com.google.firebase.Timestamp.now()
                                    )
                                    userRef.set(userData, SetOptions.merge())

                                    val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                                    with(sharedPref.edit()) {
                                        putString("loggedInUID", uid) // 🔥 UID 저장
                                        apply()
                                    }

                                    val userProfileRef = firestore.collection("user_profiles").document(uid)
                                    userProfileRef.get().addOnSuccessListener { document ->
                                        if (!document.exists()) {
                                            val defaultProfile = hashMapOf(
                                                "nickname" to (user.displayName ?: "닉네임 없음"),
                                                "myinfo" to "아직 자기소개가 없습니다.",
                                                "theme" to "default",
                                                "profileImageUrl" to "gs://yumi-5f5c0.firebasestorage.app/default_profile.jpg"
                                            )
                                            userProfileRef.set(defaultProfile)
>>>>>>> a870181 (3월 17일)
>>>>>>> Stashed changes
                                        }
                                    }

                                    Toast.makeText(this, "Google 간편 로그인 성공", Toast.LENGTH_SHORT).show()
                                    navigateToMainPage()
                                }
                            } else {
<<<<<<< Updated upstream
                                Log.e("GoogleSignIn", "로그인 실패", task.exception)
=======
<<<<<<< HEAD
                                Log.e("GoogleSignIn", "Google 로그인 실패", task.exception)
=======
                                Log.e("GoogleSignIn", "로그인 실패", task.exception)
>>>>>>> a870181 (3월 17일)
>>>>>>> Stashed changes
                                Toast.makeText(this, "Google 로그인 실패", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            } catch (e: ApiException) {
<<<<<<< Updated upstream
                Log.e("GoogleSignIn", "로그인 오류: ${e.message}")
=======
<<<<<<< HEAD
                Log.e("GoogleSignIn", "Sign-in failed: ${e.message}")
=======
                Log.e("GoogleSignIn", "로그인 오류: ${e.message}")
>>>>>>> a870181 (3월 17일)
>>>>>>> Stashed changes
                Toast.makeText(this, "Google 로그인 오류: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
<<<<<<< Updated upstream
=======
<<<<<<< HEAD

=======
>>>>>>> a870181 (3월 17일)
>>>>>>> Stashed changes
}
