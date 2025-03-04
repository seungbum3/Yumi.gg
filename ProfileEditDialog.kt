package com.example.yumi

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*

class ProfileEditDialog : DialogFragment() {

    private lateinit var profileImageView: ImageView
    private lateinit var editNickname: EditText
    private lateinit var editBio: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnCheckNickname: Button

    private var imageUri: Uri? = null

    private var originalNickname: String = ""
    private var isNicknameChecked = false
    private var isNicknameAvailable = false
    private var userId: String = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_profile_edit, null)
        dialog.setContentView(view)

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Log.e("FirebaseAuth", "❌ 로그인 필요! 로그인 화면으로 이동")
            val signInIntent = Intent(context, LoginActivity::class.java)
            startActivity(signInIntent)
        } else {
            Log.d("FirebaseAuth", "✅ 로그인된 사용자 UID: ${currentUser.uid}")
        }

        // 로그인한 사용자의 ID 가져오기 (사용자 지정 아이디, 예: "tpcks571")
        val sharedPref = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        userId = sharedPref.getString("loggedInUserId", null) ?: ""

        if (userId.isEmpty()) {
            Toast.makeText(context, "로그인 정보가 없습니다!", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        editNickname = view.findViewById(R.id.editNickname)
        editBio = view.findViewById(R.id.editBio)
        profileImageView = view.findViewById(R.id.profileImageView)
        btnSave = view.findViewById(R.id.btnSave)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnCheckNickname = view.findViewById(R.id.btnCheckNickname)

        loadUserProfile() // 사용자 정보 불러오기

        btnCheckNickname.setOnClickListener {
            checkNicknameDuplicate()
        }

        profileImageView.setOnClickListener {
            selectImage()
        }

        btnSave.setOnClickListener {
            saveProfileChanges()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        return dialog
    }

    private fun loadUserProfile() {
        val db = FirebaseFirestore.getInstance()
        val sharedPref = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getString("loggedInUserId", null) ?: ""

        if (userId.isEmpty()) {
            Toast.makeText(context, "로그인 정보가 없습니다!", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }
        Log.d("Firestore", "🔍 Firestore에서 사용자 정보 불러오기 시작: $userId")

        // UID 기반 쿼리는 제거하고, 바로 사용자 정보 불러오기
        fetchUserProfileData(userId)
    }

    private fun fetchUserProfileData(userId: String) {
        val db = FirebaseFirestore.getInstance()

        // users 컬렉션에서 닉네임 가져오기
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nickname = document.getString("nickname") ?: "알 수 없음"
                    originalNickname = nickname
                    editNickname.setText(nickname)
                    Log.d("Firestore", "✅ 닉네임 로드 성공: $nickname")
                } else {
                    Log.e("Firestore", "❌ users 컬렉션에서 사용자 정보 없음!")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "❌ users 컬렉션 데이터 가져오기 실패", e)
            }

        // user_profiles 컬렉션에서 자기소개(myinfo)와 프로필 이미지 URL(profileImageUrl) 가져오기
        db.collection("user_profiles").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val bio = document.getString("myinfo") ?: "자기소개 없음"
                    editBio.setText(bio)
                    Log.d("Firestore", "✅ 자기소개 로드 성공: $bio")
                    // 프로필 이미지 URL이 존재하면 불러오기 (Glide 라이브러리 사용 예시)
                    val imageUrl = document.getString("profileImageUrl")
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.default_profile) // 기본 이미지 리소스
                            .error(R.drawable.default_profile)
                            .circleCrop()
                            .into(profileImageView)
                    }
                } else {
                    Log.e("Firestore", "❌ user_profiles 컬렉션에서 사용자 정보 없음!")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "❌ user_profiles 컬렉션 데이터 가져오기 실패", e)
            }
    }


    private fun saveProfileChanges() {
        val newNickname = editNickname.text.toString().trim()
        val newBio = editBio.text.toString().trim().take(20)

        if (newNickname.isEmpty()) {
            Toast.makeText(context, "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // 닉네임이 변경되지 않았다면 중복 체크 없이 저장
        if (newNickname == originalNickname) {
            saveProfileToFirestore(newNickname, newBio, imageUri)
            return
        }

        // 닉네임이 변경된 경우에만 중복 확인
        if (!isNicknameChecked) {
            Toast.makeText(context, "닉네임 중복 확인을 해주세요!", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isNicknameAvailable) {
            Toast.makeText(context, "이미 사용 중인 닉네임입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 중복 확인 통과한 경우에만 저장 진행
        saveProfileToFirestore(newNickname, newBio, imageUri)
    }

    private fun uploadProfileImage(imageUri: Uri, onComplete: (String?) -> Unit) {
        // 현재 로그인한 사용자의 Firebase uid 사용
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e("ProfileImage", "로그인된 사용자가 없습니다!")
            onComplete(null)
            return
        }
        val firebaseUid = currentUser.uid
        // 파일 경로를 "profile_images/uid/profile.jpg" 형태로 설정합니다.
        val storageRef = FirebaseStorage.getInstance().reference.child("profile_images/$firebaseUid/profile.jpg")
        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    onComplete(downloadUri.toString())
                }.addOnFailureListener { e ->
                    Log.e("ProfileImage", "다운로드 URL 받기 실패", e)
                    onComplete(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileImage", "이미지 업로드 실패", e)
                onComplete(null)
            }
    }


    private fun saveProfileToFirestore(nickname: String, bio: String, imageUri: Uri?) {
        val db = FirebaseFirestore.getInstance()
        val sharedPref = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        // 사용자 지정 아이디(예: "tpcks571")를 Firestore 문서 ID로 사용
        val userId = sharedPref.getString("loggedInUserId", null)

        if (userId == null) {
            Log.e("Firestore", "❌ SharedPreferences에서 사용자 ID를 찾을 수 없음!")
            return
        }

        val userRef = db.collection("users").document(userId)
        val userProfileRef = db.collection("user_profiles").document(userId)
        val profileUpdate = mutableMapOf<String, Any>("myinfo" to bio)

        if (nickname.isNotEmpty()) {
            userRef.update(mapOf("nickname" to nickname))
                .addOnSuccessListener {
                    Log.d("Firestore", "✅ 닉네임 업데이트 성공!")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "❌ 닉네임 업데이트 실패", e)
                }
        }

        if (imageUri != null) {
            // 이미지가 선택된 경우 Firebase Storage에 업로드 진행
            uploadProfileImage(imageUri) { downloadUrl ->
                if (downloadUrl != null) {
                    profileUpdate["profileImageUrl"] = downloadUrl
                }
                updateUserProfile(userProfileRef, profileUpdate, userId)
            }
        } else {
            updateUserProfile(userProfileRef, profileUpdate, userId)
        }
    }

    private fun updateUserProfile(
        userProfileRef: DocumentReference,
        profileUpdate: MutableMap<String, Any>,
        userId: String
    ) {
        userProfileRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    userProfileRef.update(profileUpdate)
                        .addOnSuccessListener {
                            Log.d("Firestore", "✅ 프로필 업데이트 성공! (문서 존재)")
                            (activity as? MyPageActivity)?.refreshProfileFromFirestore(userId)
                            dismiss()
                        }
                        .addOnFailureListener {
                            Log.e("Firestore", "❌ 프로필 업데이트 실패!", it)
                        }
                } else {
                    profileUpdate["profileImageUrl"] = profileUpdate["profileImageUrl"] ?: ""
                    userProfileRef.set(profileUpdate)
                        .addOnSuccessListener {
                            Log.d("Firestore", "✅ 새 프로필 문서 생성 후 업데이트 성공!")
                            (activity as? MyPageActivity)?.refreshProfileFromFirestore(userId)
                            dismiss()
                        }
                        .addOnFailureListener {
                            Log.e("Firestore", "❌ 새 프로필 문서 생성 실패!", it)
                        }
                }
            }
            .addOnFailureListener {
                Log.e("Firestore", "❌ Firestore에서 프로필 문서 조회 실패!", it)
            }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            profileImageView.setImageURI(imageUri) // 선택한 이미지를 즉시 UI에 반영
        }
    }

    private fun checkNicknameDuplicate() {
        val nickname = editNickname.text.toString().trim()
        if (nickname.isEmpty()) {
            Toast.makeText(context, "닉네임을 입력하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // 현재 닉네임과 동일하면 중복검사 필요 없음
        if (nickname == originalNickname) {
            isNicknameChecked = true
            isNicknameAvailable = true
            Toast.makeText(context, "기존 닉네임입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .whereEqualTo("nickname", nickname)
            .get()
            .addOnSuccessListener { documents ->
                isNicknameChecked = true
                isNicknameAvailable = documents.isEmpty
                val message = if (isNicknameAvailable) "사용 가능한 닉네임입니다!" else "이미 사용 중인 닉네임입니다."
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        private const val IMAGE_PICK_CODE = 1000
    }
}
