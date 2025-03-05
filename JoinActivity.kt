package com.example.yumi2

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.yumi2.viewmodel.RegisterViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class JoinActivity : AppCompatActivity() {
    private val viewModel: RegisterViewModel by viewModels()
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    private var isIdAvailable = false  // 아이디 중복 검사 결과 저장
    private var isNicknameAvailable = false  // 닉네임 중복 검사 결과 저장
    private var isEmailVerified = false  // 이메일 인증 여부 저장

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.join)

        auth = FirebaseAuth.getInstance()

        val PageBack: Button = findViewById(R.id.PageBack)
        PageBack.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        val btnRegister: TextView = findViewById(R.id.btnRegister)
        btnRegister.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        val idInput = findViewById<EditText>(R.id.editTextId)
        val passwordInput = findViewById<EditText>(R.id.editTextPassword)
        val nicknameInput = findViewById<EditText>(R.id.editTextNickname)
        val emailInput = findViewById<EditText>(R.id.editTextEmail)
        val registerButton = findViewById<Button>(R.id.btnLogin)
        val verifyEmailButton = findViewById<Button>(R.id.VerifyEmailbtn)

// 🔹 이메일 형식이 맞는지 확인 후 `VerifyEmailbtn` 활성화
        emailInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val email = s.toString().trim()
                verifyEmailButton.isEnabled = Patterns.EMAIL_ADDRESS.matcher(email).matches()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 🔹 아이디 중복 확인 버튼 클릭
        findViewById<Button>(R.id.Checkbtn).setOnClickListener {
            val id = idInput.text.toString()
            if (id.isEmpty()) {
                Toast.makeText(this, "아이디를 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.checkId(id) { isAvailable ->
                isIdAvailable = isAvailable
                if (isAvailable) {
                    Toast.makeText(this, "사용 가능한 아이디입니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "이미 사용 중인 아이디입니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 🔹 닉네임 중복 확인 버튼 클릭
        findViewById<Button>(R.id.NameCheckbtn).setOnClickListener {
            val nickname = nicknameInput.text.toString()
            if (nickname.isEmpty()) {
                Toast.makeText(this, "닉네임을 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.checkNickname(nickname) { isAvailable ->
                isNicknameAvailable = isAvailable
                if (isAvailable) {
                    Toast.makeText(this, "사용 가능한 닉네임입니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "이미 사용 중인 닉네임입니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 🔹 이메일 인증 버튼 클릭
        verifyEmailButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "이메일을 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendEmailVerification(email)
        }

        // 🔹 회원가입 버튼 클릭 (Firestore에 저장)
        registerButton.setOnClickListener {
            val id = idInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val nickname = nicknameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()

            // 필수 입력 항목 확인
            if (id.isEmpty() || password.isEmpty() || nickname.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "모든 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔹 이메일 형식 검사 추가
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "올바른 이메일을 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔹 아이디 중복 검사 확인
            if (!isIdAvailable) {
                Toast.makeText(this, "아이디 중복 검사를 완료해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔹 닉네임 중복 검사 확인
            if (!isNicknameAvailable) {
                Toast.makeText(this, "닉네임 중복 검사를 완료해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔹 비밀번호 최소 6자리 검사
            if (password.length < 6) {
                Toast.makeText(this, "비밀번호는 최소 6자리 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔹 이메일 인증 확인 (Firebase Auth에서 최신 상태 확인)
            auth.currentUser?.reload()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user == null || !user.isEmailVerified) {
                        Toast.makeText(this, "이메일 인증을 완료하세요.", Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }

                    // 🔹 모든 검사를 통과한 경우 회원가입 진행
                    registerUser(id, password, nickname, email)
                } else {
                    Toast.makeText(this, "이메일 인증을 완료하세요.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 🔹 이메일 인증 메일 보내기
    private fun sendEmailVerification(email: String) {
        auth.createUserWithEmailAndPassword(email, "tempPassword123")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.sendEmailVerification()
                        ?.addOnCompleteListener { verifyTask ->
                            if (verifyTask.isSuccessful) {
                                Toast.makeText(this, "이메일 인증 링크를 보냈습니다. 이메일에서 확인 후 회원가입하세요.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this, "인증 이메일 전송 실패: ${verifyTask.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "계정 생성 실패: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // 🔹 Firestore에 사용자 정보 저장
    private fun registerUser(id: String, password: String, nickname: String, email: String) {

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentTime = dateFormat.format(System.currentTimeMillis())

        val  currentUser = auth.currentUser
        val uid = currentUser?.uid ?: ""

        if (uid.isEmpty()){
            Toast.makeText(this, "Firebase UID를 가져올 수 없습니다!", Toast.LENGTH_SHORT).show()
            return
        }

        val user = hashMapOf(
            "id" to id,
            "password" to password,
            "nickname" to nickname,
            "email" to email,
            "myinfo" to "",
            "uid" to uid,
            "createdAt" to currentTime
        )

        // 🔹 users 컬렉션에 저장
        db.collection("users").document(id)
            .set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "회원가입 성공!", Toast.LENGTH_SHORT).show()

                // 🔹 user_profiles 컬렉션에도 같은 데이터 저장
                db.collection("user_profiles").document(id)
                    .set(user)
                    .addOnSuccessListener {
                        Toast.makeText(this, "user_profiles에도 저장 완료!", Toast.LENGTH_SHORT).show()
                        finish()  // ✅ 모든 저장이 끝나면 액티비티 종료
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "user_profiles 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "회원가입 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
