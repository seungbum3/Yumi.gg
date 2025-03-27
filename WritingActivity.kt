package com.example.opggyumi

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class WritingActivity : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 100
    private var imageUri: Uri? = null
    private lateinit var currentCategory: String
    private lateinit var selectedImageView: ImageView

    // 해시태그 관련 변수
    private lateinit var hashtagTextView: TextView
    private var hashtagList = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_writing)

        currentCategory = intent.getStringExtra("category") ?: "자유"

        val titleEditText = findViewById<EditText>(R.id.editText)
        val contentEditText = findViewById<EditText>(R.id.editTextContent)
        val saveButton = findViewById<Button>(R.id.button)
        selectedImageView = findViewById(R.id.imageView)
        hashtagTextView = findViewById(R.id.hashtagTextView)

        // 이미지 선택
        selectedImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // 해시태그 텍스트뷰 클릭 시 HashtagActivity로 이동
        hashtagTextView.setOnClickListener {
            val intent = Intent(this, HashtagActivity::class.java)
            startActivityForResult(intent, 200)  // 200은 해시태그 요청 코드
        }

        // 저장 버튼 클릭 시 해시태그 목록도 함께 Firestore에 저장
        saveButton.setOnClickListener {
            val title = titleEditText.text.toString()
            val content = contentEditText.text.toString()

            if (imageUri != null) {
                uploadImageToFirebase(imageUri!!) { imageUrl ->
                    savePostToFirestore(title, content, imageUrl)
                }
            } else {
                savePostToFirestore(title, content, null)
            }
        }

        // 뒤로 가기 버튼 처리
        val backButton = findViewById<ImageView>(R.id.imageView3)
        backButton.setOnClickListener {
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            selectedImageView.setImageURI(imageUri)
        } else if (requestCode == 200 && resultCode == Activity.RESULT_OK) {
            // 해시태그 액티비티에서 반환된 해시태그 목록 받기
            hashtagList = data?.getStringArrayListExtra("hashtags") ?: arrayListOf()
            // 해시태그들을 쉼표로 구분하여 표시
            hashtagTextView.text = hashtagList.joinToString(", ")
        }
    }

    private fun uploadImageToFirebase(uri: Uri, callback: (String) -> Unit) {
        val storageRef = FirebaseStorage.getInstance()
            .reference.child("post_images/${System.currentTimeMillis()}.jpg")

        storageRef.putFile(uri).addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                callback(downloadUri.toString())
            }
        }.addOnFailureListener {
            Toast.makeText(this, "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
        }
    }

    private fun savePostToFirestore(title: String, content: String, imageUrl: String?) {
        val db = FirebaseFirestore.getInstance()
        val postRef = db.collection("posts").document()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val uid = currentUser.uid

        val profileRef = db.collection("user_profiles").document(uid)
        profileRef.get().addOnSuccessListener { document ->
            val nickname = document.getString("nickname") ?: "닉네임 없음"
            // 해시태그도 함께 저장 (최대 5개)
            val postMap = hashMapOf(
                "title" to title,
                "content" to content,
                "category" to currentCategory,
                "timestamp" to System.currentTimeMillis(),
                "views" to 0,
                "postId" to postRef.id,
                "imageUrl" to imageUrl,
                "uid" to uid,
                "nickname" to nickname,
                "hashtags" to hashtagList  // 해시태그 필드 추가
            )
            postRef.set(postMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "게시글 저장됨", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "저장 실패", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener {
            Toast.makeText(this, "닉네임 불러오기 실패", Toast.LENGTH_SHORT).show()
        }
    }
}
