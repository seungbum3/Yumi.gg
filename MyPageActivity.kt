package com.example.yumi

import android.net.Uri
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
import com.google.firebase.storage.FirebaseStorage


class MyPageActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var friendsRecyclerView: RecyclerView
    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var favoritesAdapter: FavoritesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mypage)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val btnProfileEdit = findViewById<Button>(R.id.btnProfileEdit)
        btnProfileEdit.setOnClickListener {
            val dialog = ProfileEditDialog()
            dialog.show(supportFragmentManager, "ProfileEditDialog")
        }
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("MyPageActivity", "사용자가 로그인되어 있지 않음")
            return
        }
        val userId = currentUser.uid // 현재 로그인한 사용자의 ID 가져오기

        if (userId != null) {
            loadUserProfile(userId) // Firestore에서 유저 정보 가져오기
        } else {
            Log.e("Firestore", "현재 로그인한 사용자 ID를 가져올 수 없음!")
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

        loadFriendsList(userId) // Firestore에서 친구 목록 불러오기
        loadFavoritesList(userId) // Firestore에서 즐겨찾기 목록 불러오기
    }
    private fun loadUserProfile(userId: String) {
        val usersRef = db.collection("users").document(userId) // 🔹 users 컬렉션 참조
        val profilesRef = db.collection("user_profiles").document(userId) // 🔹 user_profiles 컬렉션 참조

        // users 컬렉션에서 닉네임 가져오기
        usersRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nickname = document.getString("nickname") ?: "알 수 없음"
                    Log.d("Firestore", "닉네임 불러오기 성공! 닉네임: $nickname")

                    // UI 업데이트 (닉네임)
                    findViewById<TextView>(R.id.userName).text = nickname
                } else {
                    Log.e("Firestore", "Firestore에 해당 사용자 정보(users 컬렉션)가 없음!")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "닉네임 불러오기 실패", e)
            }

        // user_profiles 컬렉션에서 자기소개(myinfo) 가져오기
        profilesRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val bio = document.getString("myinfo") ?: "자기소개 없음"
                    Log.d("Firestore", "자기소개 불러오기 성공! 자기소개: $bio")

                    // UI 업데이트 (자기소개)
                    findViewById<TextView>(R.id.userBio).text = bio
                } else {
                    Log.e("Firestore", "Firestore에 해당 사용자 프로필 정보(user_profiles 컬렉션)가 없음!")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "자기소개 불러오기 실패", e)
            }
    }


    private fun loadFriendsList(userId: String) {
        val friendsList = mutableListOf<HashMap<String, String>>()
        db.collection("users").document(userId).collection("friends") // Firestore에서 친구 목록 가져오기
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
                val friendsAdapter = FriendsAdapter(friendsList) // 친구 목록 어댑터 설정
                friendsRecyclerView.adapter = friendsAdapter
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "친구 목록 가져오기 실패", e)
            }
    }

    private fun loadFavoritesList(userId: String) {
        val favoritesList = mutableListOf<HashMap<String, String>>()
        db.collection("users").document(userId).collection("favorites")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val summonerName = document.getString("summonerName") ?: "알 수 없음"
                    val favoriteMap = hashMapOf("summonerName" to summonerName)
                    favoritesList.add(favoriteMap)
                }
                favoritesAdapter = FavoritesAdapter(favoritesList, userId) // 수정된 리스트로 어댑터 업데이트
                favoritesRecyclerView.adapter = favoritesAdapter
                favoritesAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "즐겨찾기 목록 가져오기 실패", e)
            }
    }

    override fun onPause() {
        super.onPause()
        favoritesAdapter.syncFavoritesWithFirestore(db) {
            Log.d("Firestore", "마이페이지 종료 시 즐겨찾기 상태 업데이트 완료")
        }
    }
    fun updateUserProfile(nickname: String, bio: String, imageUri: Uri?) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId) // 닉네임 수정용 (users 컬렉션)
        val userProfileRef = db.collection("user_profiles").document(userId) // 프로필 정보 수정용 (user_profiles 컬렉션)

        val nicknameUpdate = mutableMapOf<String, Any>(
            "nickname" to nickname // 닉네임은 users 컬렉션에서 수정
        )

        val profileUpdate = mutableMapOf<String, Any>(
            "myinfo" to bio, // 자기소개는 user_profiles에서 수정
        )

        if (imageUri != null) {
            val storageRef = FirebaseStorage.getInstance().reference.child("profile_images/$userId.jpg")
            storageRef.putFile(imageUri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        profileUpdate["profileImageUrl"] = uri.toString()

                        userProfileRef.update(profileUpdate)
                            .addOnSuccessListener {
                                Log.d("Firestore", "✅ 프로필 업데이트 성공! (이미지 포함)")
                                refreshProfileUI(nickname, bio, uri.toString())
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "프로필 업데이트 실패", e)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Storage", "이미지 업로드 실패", e)
                }
        } else {
            userProfileRef.update(profileUpdate)
                .addOnSuccessListener {
                    Log.d("Firestore", "✅ 프로필 업데이트 성공!")
                    refreshProfileUI(nickname, bio, null)
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "프로필 업데이트 실패", e)
                }
        }

        // 닉네임 업데이트 실행 (users 컬렉션)
        userRef.update(nicknameUpdate)
            .addOnSuccessListener {
                Log.d("Firestore", "닉네임 업데이트 성공!")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "닉네임 업데이트 실패", e)
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
