package com.example.opggyumi

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class WritingActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var currentCategory: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_writing)

        currentCategory = intent.getStringExtra("category") ?: "자유"

        val titleEditText = findViewById<EditText>(R.id.editText)
        val contentEditText = findViewById<EditText>(R.id.editTextContent)

        val saveButton = findViewById<Button>(R.id.button)
        val saveDraftButton = findViewById<Button>(R.id.button2)

        saveButton.setOnClickListener {
            val title = titleEditText.text.toString()
            val content = contentEditText.text.toString()
            val timestamp = System.currentTimeMillis()

            val post = hashMapOf(
                "title" to title,
                "content" to content,
                "category" to currentCategory,
                "timestamp" to timestamp
            )

            // Firestore에 게시글 저장
            db.collection("posts")
                .add(post)
                .addOnSuccessListener { documentReference ->
                    val intent = Intent()
                    intent.putExtra("title", title)
                    intent.putExtra("content", content)
                    intent.putExtra("category", currentCategory)
                    intent.putExtra("timestamp", timestamp)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e("WritingActivity", "게시글 저장 오류", e)
                }
        }

        saveDraftButton.setOnClickListener {
            Log.d("WritingActivity", "임시 저장 버튼 클릭됨.")
        }

        val backButton = findViewById<ImageView>(R.id.imageView3)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
