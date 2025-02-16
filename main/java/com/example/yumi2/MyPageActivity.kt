package com.example.yumi2

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyPageActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var friendsRecyclerView: RecyclerView
    private lateinit var friendsAdapter: FriendsAdapter
    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var favoritesAdapter: FavoritesAdapter

    // 테스트용 userId; 실제 로그인한 사용자의 UID와 일치해야 합니다.
    private val testUserId = "bMT5crP5APSZX5pu6wShSnWOfWs2"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mypage)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        friendsRecyclerView = findViewById(R.id.friendsList)
        favoritesRecyclerView = findViewById(R.id.favoritesList)

        friendsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        favoritesRecyclerView.layoutManager = GridLayoutManager(this, 2)

        // Bottom Navigation View 설정
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.category4
        bottomNavigation.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.category1 -> {
                    Log.d("BottomNav", "메인화면 선택")
                    true
                }
                R.id.category2 -> {
                    Log.d("BottomNav", "커뮤니티 선택")
                    true
                }
                R.id.category3 -> {
                    Log.d("BottomNav", "모의밴픽 선택")
                    true
                }
                R.id.category4 -> {
                    Log.d("BottomNav", "마이페이지 선택")
                    true
                }
                else -> false
            }
        }

        // Firestore 데이터 로드
        loadUserProfile(testUserId)
        loadFriendsList(testUserId)
        loadFavoritesList(testUserId)
    }

    private fun loadUserProfile(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nickname = document.getString("nickname") ?: "닉네임 없음"
                    val profileImageUrl = document.getString("profileImageUrl")
                        ?: "https://firebasestorage.googleapis.com/v0/b/your_project_id.appspot.com/o/default_profile.jpg?alt=media"
                    // Firestore에서 자기소개 글은 "myinfo" 필드로 불러옵니다.
                    val bio = document.getString("myinfo") ?: "한 줄 자기소개가 없습니다."
                    // 25자 이상이면 한 줄로 제한하고 "..." 추가
                    val oneLineBio = if (bio.length > 25) bio.substring(0, 25) + "..." else bio

                    findViewById<TextView>(R.id.userName).text = nickname
                    findViewById<TextView>(R.id.userBio).text = oneLineBio

                    Glide.with(this)
                        .load(profileImageUrl)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .circleCrop()
                        .into(findViewById(R.id.profileImage))
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "프로필 정보 불러오기 실패", e)
            }
    }


    private fun loadFriendsList(userId: String) {
        // Friend 데이터를 HashMap으로 처리 (Friend.kt 없이)
        val friendsList = mutableListOf<HashMap<String, String>>()
        db.collection("users").document(userId).collection("friends")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("Firestore", "친구 목록이 비어 있음!")
                }
                for (document in documents) {
                    val nickname = document.getString("nickname") ?: "알 수 없음"
                    val imageUrl = document.getString("profileImageUrl") ?: ""
                    val friendMap = hashMapOf("name" to nickname, "imageResId" to imageUrl)
                    friendsList.add(friendMap)
                }
                friendsAdapter = FriendsAdapter(friendsList)
                friendsRecyclerView.adapter = friendsAdapter
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "친구 목록 가져오기 실패", e)
            }
    }

    private fun loadFavoritesList(userId: String) {
        Log.d("Firestore", "🔥 loadFavoritesList() 함수 실행됨!")
        val favoritesList = mutableListOf<HashMap<String, String>>()
        db.collection("users").document(userId).collection("favorites")
            .limit(10) // 최대 10개까지만 가져옴
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("Firestore", "즐겨찾기 목록이 비어 있음!")
                }
                for (document in documents) {
                    val nickname = document.getString("nickname") ?: "알 수 없음"
                    Log.d("Firestore", "Firestore에서 불러온 즐겨찾기 유저: $nickname")
                    val favoriteMap = hashMapOf("nickname" to nickname)
                    favoritesList.add(favoriteMap)
                }
                favoritesAdapter = FavoritesAdapter(favoritesList)
                favoritesRecyclerView.adapter = favoritesAdapter
                favoritesAdapter.notifyDataSetChanged()
                Log.d("Firestore", "RecyclerView 어댑터 설정 완료! 목록 개수: ${favoritesList.size}")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "즐겨찾기 목록 가져오기 실패", e)
            }
    }
}
