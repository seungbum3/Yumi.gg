package com.example.opggyumi

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

data class Post( val title: String, val content: String, val category: String, val timestamp: Long )
@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private lateinit var freeBoardAdapter: FreeBoardAdapter
    private lateinit var rankBoardAdapter: RankBoardAdapter
    private lateinit var normalBoardAdapter: NormalBoardAdapter
    private lateinit var championBoardAdapter: ChampionBoardAdapter
    private lateinit var categoryTitle: TextView
    private lateinit var currentCategory: String
    private val freeBoardList = mutableListOf<Post>()
    private val rankBoardList = mutableListOf<Post>()
    private val normalBoardList = mutableListOf<Post>()
    private val championBoardList = mutableListOf<Post>()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        categoryTitle = findViewById(R.id.categoryTitle)

        currentCategory = "자유"

        freeBoardAdapter = FreeBoardAdapter(this, freeBoardList)
        rankBoardAdapter = RankBoardAdapter(this, rankBoardList)
        normalBoardAdapter = NormalBoardAdapter(this, normalBoardList)
        championBoardAdapter = ChampionBoardAdapter(this, championBoardList)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = freeBoardAdapter

        val textFree = findViewById<TextView>(R.id.text_free)
        val textRank = findViewById<TextView>(R.id.text_rank)
        val textNormal = findViewById<TextView>(R.id.text_normal)
        val textChampion = findViewById<TextView>(R.id.text_champion)

        textFree.setOnClickListener { changeCategory("자유") }
        textRank.setOnClickListener { changeCategory("랭크") }
        textNormal.setOnClickListener { changeCategory("일반") }
        textChampion.setOnClickListener { changeCategory("챔피언 빌드") }

        val writeButton = findViewById<Button>(R.id.button3)
        writeButton.setOnClickListener {
            val intent = Intent(this, WritingActivity::class.java)
            intent.putExtra("category", currentCategory) // 현재 카테고리 전달
            startActivityForResult(intent, 1)
        }

        // Firestore에서 게시글 가져오기
        loadPosts()
    }

    private fun loadPosts() {
        firestore.collection("posts")
            .whereEqualTo("category", currentCategory) // 🔥 카테고리 필터링
            .orderBy("timestamp") // 🔥 최신순 정렬
            .get()
            .addOnSuccessListener { result ->
                Log.d("FirestoreDebug", "Firestore 데이터 가져오기 성공! 개수: ${result.size()}")

                // 카테고리에 해당하는 리스트를 비워줌 (기존 데이터를 초기화)
                when (currentCategory) {
                    "자유" -> freeBoardList.clear()
                    "랭크" -> rankBoardList.clear()
                    "일반" -> normalBoardList.clear()
                    "챔피언 빌드" -> championBoardList.clear()
                }

                // Firestore에서 데이터를 추가
                for (document in result) {
                    Log.d("FirestoreDebug", "불러온 데이터: ${document.data}") // ✅ 가져온 데이터 로그 출력

                    val title = document.getString("title") ?: ""
                    val content = document.getString("content") ?: ""
                    val category = document.getString("category") ?: ""
                    val timestamp = document.getLong("timestamp") ?: 0L
                    val post = Post(title, content, category, timestamp)

                    // 카테고리별로 게시글 리스트에 추가
                    when (currentCategory) {
                        "자유" -> freeBoardList.add(post)
                        "랭크" -> rankBoardList.add(post)
                        "일반" -> normalBoardList.add(post)
                        "챔피언 빌드" -> championBoardList.add(post)
                    }
                }

                Log.d("FirestoreDebug", "현재 카테고리: $currentCategory, 가져온 게시글 개수: ${freeBoardList.size}")

                // RecyclerView 갱신
                (findViewById<RecyclerView>(R.id.recyclerView).adapter as RecyclerView.Adapter<*>).notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreError", "게시글 불러오기 실패: ${exception.message}") // ✅ 오류 메시지 확인
            }
    }


    private fun changeCategory(category: String) {
        currentCategory = category
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        // 카테고리에 맞는 어댑터 설정
        when (category) {
            "자유" -> {
                recyclerView.adapter = freeBoardAdapter
                categoryTitle.text = "자유 게시판"
            }
            "랭크" -> {
                recyclerView.adapter = rankBoardAdapter
                categoryTitle.text = "랭크 게시판"
            }
            "일반" -> {
                recyclerView.adapter = normalBoardAdapter
                categoryTitle.text = "일반 게시판"
            }
            "챔피언 빌드" -> {
                recyclerView.adapter = championBoardAdapter
                categoryTitle.text = "챔피언 s빌드 게시판"
            }
        }
        loadPosts() // 변경된 카테고리의 게시글 불러오기
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // WritingActivity에서 돌아왔을 때
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            val title = data?.getStringExtra("title") ?: ""
            val content = data?.getStringExtra("content") ?: ""
            val timestamp = System.currentTimeMillis() // 현재 시간 저장

            // Firestore에서 동일한 게시글이 있는지 확인
            val query = firestore.collection("posts")
                .whereEqualTo("title", title)
                .whereEqualTo("timestamp", timestamp)

            query.get()
                .addOnSuccessListener { result ->
                    if (result.isEmpty) {
                        // 중복되지 않는 경우에만 추가
                        val post = Post(title, content, currentCategory, timestamp)

                        // Firestore에 게시글 저장
                        val postData = hashMapOf(
                            "title" to post.title,
                            "content" to post.content,
                            "category" to post.category,
                            "timestamp" to post.timestamp
                        )

                        // Firestore에 새 게시글 추가
                        firestore.collection("posts")
                            .add(postData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "게시글이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                                loadPosts() // 현재 카테고리의 게시글만 다시 불러오기
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(this, "게시글 저장 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        // 이미 동일한 게시글이 있는 경우
                        Toast.makeText(this, "이 게시글은 이미 존재합니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "게시글 확인 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

}
