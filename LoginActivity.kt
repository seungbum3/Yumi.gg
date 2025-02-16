package com.example.yumi

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.login)

        val btnRegister: TextView = findViewById(R.id.btnRegister)
        btnRegister.setOnClickListener {
            val intent = Intent(this, JoinActivity::class.java)
            startActivity(intent)
        }

        val btnLogin: TextView = findViewById(R.id.btnLogin)
        btnLogin.setOnClickListener {
            val intent = Intent(this, MyPageActivity::class.java)
            startActivity(intent)
        }

        // 로그인 누르면 로그인 완료 메시지가 떠야함 < 아직 미완성
    }
}