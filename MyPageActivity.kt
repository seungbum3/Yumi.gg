package com.example.yumi

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
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.*
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.storage.FirebaseStorage

class MyPageActivity : AppCompatActivity(), ProfileEditDialog.ProfileUpdateListener {

    override fun onProfileUpdated(nickname: String, bio: String, imageUrl: String?) {
        refreshProfileUI(nickname, bio, imageUrl)
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var friendsRecyclerView: RecyclerView
    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var favoritesAdapter: FavoritesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mypage)


        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            Log.d("FirebaseAuth", "현재 사용자의 uid: ${currentUser.uid}")
        } else {
            Log.d("FirebaseAuth", "로그인된 사용자가 없습니다.")
        }


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
        val userEmail = sharedPref.getString("loggedInUserId", null) ?: ""

        if (userEmail.isEmpty()) {
            Log.e("MyPageActivity", "❌ 로그인한 사용자 ID를 찾을 수 없음! 로그인 화면으로 이동")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            Log.d("MyPageActivity", "✅ 저장된 사용자 ID: $userEmail")
            CoroutineScope(Dispatchers.IO).launch {
                loadUserProfile(userEmail)
                loadFriendsList(userEmail)
                loadFavoritesList(userEmail)
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


    private fun refreshProfileUI(nickname: String, bio: String, imageUrl: String?) {
        findViewById<TextView>(R.id.userName).text = nickname
        findViewById<TextView>(R.id.userBio).text = bio

        val profileImageView = findViewById<ImageView>(R.id.profileImage)
        if (!imageUrl.isNullOrEmpty()) {
            if (imageUrl.startsWith("gs://")) {
                convertGsUrlToHttp(imageUrl) { httpUrl ->
                    loadImage(httpUrl ?: "", profileImageView)
                }
            } else {
                loadImage(imageUrl, profileImageView)
            }
        }
    }
    private fun loadImage(url: String?, imageView: ImageView) {
        if (!url.isNullOrEmpty()) {
            Glide.with(this)
                .load(url)
                .circleCrop()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(imageView)
        }
    }

    private fun loadUserProfile(userEmail: String) {

        db.collection("user_profiles").document(userEmail)
            .get()
            .addOnSuccessListener { document ->

            }

        db.collection("user_profiles").document(userEmail)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val bio = document.getString("myinfo") ?: "자기소개 없음"
                    val imageUrl = document.getString("profileImageUrl") ?: ""
                    val nickname = document.getString("nickname") ?: "알 수 없음"
                    findViewById<TextView>(R.id.userName).text = nickname
                    val profileImageView = findViewById<ImageView>(R.id.profileImage)

                    // 🔥 Firebase Storage 기본 프로필 이미지 URL 직접 처리
                    val defaultProfileUrl = "gs://yumi-5f5c0.firebasestorage.app/default_profile.jpg"

                    if (!imageUrl.isNullOrEmpty()) {
                        if (imageUrl.startsWith("gs://")) {
                            convertGsUrlToHttp(imageUrl) { httpUrl ->
                                loadImage(httpUrl ?: "", profileImageView)
                            }
                        } else {
                            loadImage(imageUrl, profileImageView)
                        }
                    } else {
                        convertGsUrlToHttp(defaultProfileUrl) { httpUrl ->
                            loadImage(httpUrl ?: "", profileImageView)
                        }
                    }

                    refreshProfileUI(nickname, bio, imageUrl)
                }
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
