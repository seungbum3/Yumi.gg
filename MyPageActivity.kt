package com.example.yumi

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.recyclerview.widget.GridLayoutManager
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView


class MyPageActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var friendsRecyclerView: RecyclerView
    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var favoritesAdapter: FavoritesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mypage)


        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigationView.selectedItemId = R.id.category4

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.category1 -> { // 메인화면
                    startActivity(Intent(this, MainpageActivity::class.java))
                    finish()
                    true
                }
                R.id.category2 -> {  // 커뮤니티

                    finish()
                    true
                }
                R.id.category3 -> {  // 모의밴픽

                    finish()
                    true
                }
                R.id.category4 -> {
                    // 마이페이지
                    true
                }
                else -> false
            }
            }


        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("MyPageActivity", "⚠️ 사용자가 로그인되어 있지 않음!")
            return
        }

        val userId = currentUser.uid // ✅ 현재 로그인한 사용자의 UID 가져오기
        Log.d("MyPageActivity", "로그인된 사용자 ID: $userId")

        // Firestore에서 사용자 정보 가져오기
        loadUserProfile(userId)
        loadFriendsList(userId)
        loadFavoritesList(userId)

        val btnProfileEdit = findViewById<Button>(R.id.btnProfileEdit)
        btnProfileEdit.setOnClickListener {
            val dialog = ProfileEditDialog()
            dialog.show(supportFragmentManager, "ProfileEditDialog")
        }

        // 친구 목록 RecyclerView 초기화
        friendsRecyclerView = findViewById(R.id.friendsList)
        friendsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // 즐겨찾기 목록 RecyclerView 초기화
        favoritesRecyclerView = findViewById(R.id.favoritesList)
        favoritesRecyclerView.layoutManager = LinearLayoutManager(this)

        val favoritesList = mutableListOf<HashMap<String, String>>() // 빈 리스트로 초기화
        favoritesAdapter = FavoritesAdapter(favoritesList, userId) // userId 전달
        favoritesRecyclerView.adapter = favoritesAdapter
    }

    private fun loadUserProfile(userId: String) {
        val usersRef = db.collection("users").document(userId)
        val profilesRef = db.collection("user_profiles").document(userId)

        usersRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nickname = document.getString("nickname") ?: "알 수 없음"
                    Log.d("Firestore", "✅ 닉네임: $nickname")
                    findViewById<TextView>(R.id.userName).text = nickname
                } else {
                    Log.e("Firestore", "❌ users 컬렉션에서 사용자 정보 없음!")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "❌ users 컬렉션에서 데이터 가져오기 실패!", e)
            }

        profilesRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val bio = document.getString("myinfo") ?: "자기소개 없음"
                    Log.d("Firestore", "✅ 자기소개: $bio")
                    findViewById<TextView>(R.id.userBio).text = bio
                } else {
                    Log.e("Firestore", "❌ user_profiles 컬렉션에서 사용자 정보 없음!")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "❌ user_profiles 컬렉션에서 데이터 가져오기 실패!", e)
            }
    }


    private fun loadFriendsList(userId: String) {
        val friendsList = mutableListOf<HashMap<String, String>>()

        db.collection("users").document(userId).collection("friends")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val friendName = document.getString("nickname") ?: "알 수 없음"
                    val friendProfile = document.getString("profileImageUrl") ?: ""

                    val friendMap = hashMapOf(
                        "nickname" to friendName,
                        "profileImageUrl" to friendProfile
                    )
                    friendsList.add(friendMap)
                }
                val friendsAdapter = FriendsAdapter(friendsList)
                friendsRecyclerView.adapter = friendsAdapter
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "❌ 친구 목록 가져오기 실패", e)
            }
    }

    private fun loadFavoritesList(userId: String) {
        val favoritesList = mutableListOf<HashMap<String, String>>()

        db.collection("users").document(userId).collection("favorites")
            .get()
            .addOnSuccessListener { documents ->
                val favoritesList = mutableListOf<HashMap<String, String>>()

                for (document in documents) {
                    val summonerName = document.getString("summonerName") ?: "알 수 없음"
                    val favoriteMap = hashMapOf("summonerName" to summonerName)
                    favoritesList.add(favoriteMap)
                }
                favoritesRecyclerView.layoutManager = GridLayoutManager(this, 2)
                favoritesAdapter.updateFavorites(favoritesList)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "❌ 즐겨찾기 목록 가져오기 실패", e)
            }
    }

    override fun onPause() {
        super.onPause()
        favoritesAdapter.syncFavoritesWithFirestore(db) {
            Log.d("Firestore", "✅ 마이페이지 종료 시 즐겨찾기 상태 업데이트 완료")
        }
    }

    fun refreshProfileFromFirestore(userId: String) {
        val usersRef = db.collection("users").document(userId)
        val profilesRef = db.collection("user_profiles").document(userId)

        usersRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nickname = document.getString("nickname") ?: "알 수 없음"
                    profilesRef.get()
                        .addOnSuccessListener { profileDoc ->
                            if (profileDoc.exists()) {
                                val bio = profileDoc.getString("myinfo") ?: "자기소개 없음"
                                val imageUrl = profileDoc.getString("profileImageUrl") ?: ""
                                // UI 갱신
                                refreshProfileUI(nickname, bio, imageUrl)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "❌ 프로필 정보 가져오기 실패", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "❌ 닉네임 가져오기 실패", e)
            }
    }



    fun refreshProfileUI(nickname: String, bio: String, imageUrl: String?) {
        findViewById<TextView>(R.id.userName).text = nickname
        findViewById<TextView>(R.id.userBio).text = bio

        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.default_profile)
                .error(R.drawable.default_profile)
                .circleCrop()
                .into(findViewById(R.id.profileImage))
        }
    }
}
