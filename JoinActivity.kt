package com.example.yumi

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.yumi.viewmodel.RegisterViewModel
import androidx.activity.viewModels


class JoinActivity : AppCompatActivity() {
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.join)

        val PageBack: TextView = findViewById(R.id.PageBack)
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
        val phoneInput = findViewById<EditText>(R.id.editTextPhoneNumber)
        val registerButton = findViewById<Button>(R.id.btnLogin)

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

        // 🔹 회원가입 버튼 클릭
        registerButton.setOnClickListener {
            val id = idInput.text.toString()
            val password = passwordInput.text.toString()
            val nickname = nicknameInput.text.toString()
            val phone = phoneInput.text.toString()

            if (id.isEmpty() || password.isEmpty() || nickname.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "모든 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.registerUser(id, password, nickname, phone) { success, error ->
                if (success) {
                    Toast.makeText(this, "회원가입 성공!", Toast.LENGTH_SHORT).show()
                    finish() // 회원가입 완료 후 화면 종료
                } else {
                    Toast.makeText(this, "회원가입 실패: $error", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 중복확인 버튼 기능 아직 안 만듦
        // 가입하기 버튼 누르면 "가입 완료 되었습니다" 는 아직 안 만듦

    }
}