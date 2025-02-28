package com.example.yumi2


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import com.example.yumi2.viewmodel.RegisterViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class JoinActivity : AppCompatActivity() {
    private val viewModel: RegisterViewModel by viewModels()
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    private var generatedCode: String? = null // 생성된 인증 코드 저장

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.join)

        auth = FirebaseAuth.getInstance() //  Firebase 인증 인스턴스 초기화

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

        // 🔹 아이디 중복 확인
        findViewById<Button>(R.id.Checkbtn).setOnClickListener {
            val id = idInput.text.toString()
            viewModel.checkId(id) { isAvailable ->
                if (isAvailable) {
                    Toast.makeText(this, "사용 가능한 아이디입니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "이미 사용 중인 아이디입니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 🔹 닉네임 중복 확인
        findViewById<Button>(R.id.NameCheckbtn).setOnClickListener {
            val nickname = nicknameInput.text.toString()
            viewModel.checkNickname(nickname) { isAvailable ->
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

            if (id.isEmpty() || password.isEmpty() || nickname.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "모든 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            //  비밀번호 최소 6자리 검사
            if (password.length < 6) {
                Toast.makeText(this, "비밀번호는 최소 6자리 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Firebase Auth에서 이메일 인증 여부 확인 후 회원가입 진행
            auth.currentUser?.reload()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        registerUser(id, password, nickname, email)
                    } else {
                        Toast.makeText(this, "이메일 인증을 완료하세요.", Toast.LENGTH_SHORT).show()
                    }
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
        val user = hashMapOf(
            "id" to id,
            "password" to password,
            "nickname" to nickname,
            "email" to email,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(id)
            .set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "회원가입 성공!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "회원가입 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}