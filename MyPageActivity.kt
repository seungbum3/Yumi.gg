package com.example.yumi2

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.recyclerview.widget.GridLayoutManager
import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import com.example.yumi2.FavoritesAdapter
import com.example.yumi2.FriendsAdapter
import com.example.yumi2.LoginActivity
import com.example.yumi2.MainpageActivity
import com.example.yumi2.ProfileEditDialog
import com.example.yumi2.R
import com.example.yumi2.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.*
import com.google.firebase.firestore.DocumentReference
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

        val settingsText = findViewById<TextView>(R.id.settingsText)
        val settingsIcon = findViewById<ImageView>(R.id.settingsIcon)

        settingsText.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        settingsIcon.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigationView.selectedItemId = R.id.category4

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.category1 -> {
                    startActivity(Intent(this, MainpageActivity::class.java))
                    finish()
                    true
                }

                R.id.category2 -> {
                    finish()
                    true
                }

                R.id.category3 -> {
                    finish()
                    true
                }

                R.id.category4 -> true
                else -> false
            }
        }

        val btnProfileEdit = findViewById<Button>(R.id.btnProfileEdit)
        btnProfileEdit.setOnClickListener {
            val dialog = ProfileEditDialog()
            dialog.show(supportFragmentManager, "ProfileEditDialog")
        }

        friendsRecyclerView = findViewById(R.id.friendsList)
        friendsRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        favoritesRecyclerView = findViewById(R.id.favoritesList)
        favoritesRecyclerView.layoutManager = LinearLayoutManager(this)

        val favoritesList = mutableListOf<HashMap<String, String>>()
        favoritesAdapter = FavoritesAdapter(favoritesList, "")
        favoritesRecyclerView.adapter = favoritesAdapter
    }

    override fun onStart() {
        super.onStart()

        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getString("loggedInUserId", null) ?: ""

        if (userId.isEmpty()) {
            Log.e("MyPageActivity", "❌ 로그인한 사용자 ID를 찾을 수 없음! 로그인 화면으로 이동")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            Log.d("MyPageActivity", "✅ 저장된 사용자 ID: $userId")
            CoroutineScope(Dispatchers.IO).launch {
                loadUserProfile(userId)
                loadFriendsList(userId)
                loadFavoritesList(userId)
            }
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
    private fun convertGsUrlToHttp(gsUrl: String, onComplete: (String?) -> Unit) {
        FirebaseStorage.getInstance().getReferenceFromUrl(gsUrl)
            .downloadUrl
            .addOnSuccessListener { downloadUri ->
                onComplete(downloadUri.toString())
            }
            .addOnFailureListener { e ->
                Log.e("URL Conversion", "gs:// URL 변환 실패", e)
                onComplete(null)
            }
    }

    private fun refreshProfileUI(nickname: String, bio: String, imageUrl: String?) {
        findViewById<TextView>(R.id.userName).text = nickname
        findViewById<TextView>(R.id.userBio).text = bio

        val profileImageView = findViewById<ImageView>(R.id.profileImage)
        if (!imageUrl.isNullOrEmpty()) {
            // gs://로 시작하면 변환 처리
            if (imageUrl.startsWith("gs://")) {
                convertGsUrlToHttp(imageUrl) { httpUrl ->
                    val finalUrl = httpUrl ?: ""
                    loadImage(finalUrl, profileImageView)
                }
            } else {
                loadImage(imageUrl, profileImageView)
            }
        } else {
            profileImageView.setImageResource(R.drawable.default_profile)
        }
    }
    private fun loadImage(url: String, imageView: ImageView) {
        Glide.with(this)
            .load(url)
            .placeholder(R.drawable.default_profile)
            .error(R.drawable.default_profile)
            .circleCrop()
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(imageView)
    }

    private fun loadUserProfile(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nickname = document.getString("nickname") ?: "알 수 없음"
                    findViewById<TextView>(R.id.userName).text = nickname
                    Log.d("Firestore", "✅ 닉네임: $nickname")
                } else {
                    Log.e("Firestore", "❌ users 컬렉션에서 사용자 정보 없음!")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "❌ users 컬렉션에서 데이터 가져오기 실패!", e)
            }

        db.collection("user_profiles").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val bio = document.getString("myinfo") ?: "자기소개 없음"
                    findViewById<TextView>(R.id.userBio).text = bio
                    Log.d("Firestore", "✅ 자기소개: $bio")

                    // 프로필 이미지 URL 불러오기 및 Glide로 로드
                    val imageUrl = document.getString("profileImageUrl") ?: ""
                    val profileImageView = findViewById<ImageView>(R.id.profileImage)
                    if (imageUrl.isNotEmpty()) {
                        Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .circleCrop()
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(profileImageView)
                    } else {
                        profileImageView.setImageResource(R.drawable.default_profile)
                    }
                } else {
                    Log.e("Firestore", "❌ user_profiles 컬렉션에서 사용자 정보 없음!")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "❌ user_profiles 컬렉션에서 데이터 가져오기 실패!", e)
            }
    }


    private fun loadFriendsList(userId: String) {
        db.collection("users").document(userId).collection("friends")
            .get()
            .addOnSuccessListener { documents ->
                val friendsList = mutableListOf<HashMap<String, String>>()
                for (document in documents) {
                    val friendName = document.getString("nickname") ?: "알 수 없음"
                    val friendProfile = document.getString("profileImageUrl") ?: ""
                    friendsList.add(
                        hashMapOf(
                            "nickname" to friendName,
                            "profileImageUrl" to friendProfile
                        )
                    )
                }
                val friendsAdapter = FriendsAdapter(friendsList)
                friendsRecyclerView.adapter = friendsAdapter
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "❌ 친구 목록 가져오기 실패", e)
            }
    }

    private fun loadFavoritesList(userId: String) {
        db.collection("users").document(userId).collection("favorites")
            .get()
            .addOnSuccessListener { documents ->
                val favoritesList = mutableListOf<HashMap<String, String>>()
                for (document in documents) {
                    val summonerName = document.getString("summonerName") ?: "알 수 없음"
                    favoritesList.add(hashMapOf("summonerName" to summonerName))
                }
                favoritesRecyclerView.layoutManager = GridLayoutManager(this, 2)
                favoritesAdapter.updateFavorites(favoritesList)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "❌ 즐겨찾기 목록 가져오기 실패", e)
            }
    }
}
