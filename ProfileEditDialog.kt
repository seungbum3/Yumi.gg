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
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.text.Editable
import android.text.TextWatcher
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.util.Log


class ProfileEditDialog : DialogFragment() {

    private lateinit var profileImageView: ImageView
    private lateinit var editNickname: EditText
    private lateinit var editBio: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnCheckNickname: Button

    private var imageUri: Uri? = null
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // 닉네임 중복확인
    private var originalNickname: String = ""
    private var isNicknameChecked = false
    private var isNicknameAvailable = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_profile_edit, null)
        dialog.setContentView(view)

        editNickname = view.findViewById(R.id.editNickname)
        editBio = view.findViewById(R.id.editBio)
        profileImageView = view.findViewById(R.id.profileImageView)
        btnSave = view.findViewById(R.id.btnSave)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnCheckNickname = view.findViewById(R.id.btnCheckNickname)

        // 닉네임 길이 제한 (최대 8자)
        editNickname.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.length > 8) {
                    s.delete(8, s.length)
                    Toast.makeText(context, "닉네임은 최대 8자까지 입력 가능합니다.", Toast.LENGTH_SHORT).show()
                }
            }
        })

        // 자기소개 길이 제한 (최대 20자)
        editBio.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.length > 20) {
                    s.delete(20, s.length)
                    Toast.makeText(context, "자기소개는 최대 20자까지 입력 가능합니다.", Toast.LENGTH_SHORT).show()
                }
            }
        })

        loadUserProfile()

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

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    originalNickname = document.getString("nickname") ?: ""
                    editNickname.setText(originalNickname)
                }
            }

        db.collection("user_profiles").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    editBio.setText(document.getString("myinfo") ?: "")

                    val profileImageUrl = document.getString("profileImageUrl") ?: ""
                    if (profileImageUrl.isNotEmpty()) {
                        Glide.with(this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .circleCrop()
                            .into(profileImageView)
                    }
                }
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
            profileImageView.setImageURI(imageUri)
        }
    }

    private fun hideKeyboard() {
        val inputMethodManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        inputMethodManager?.hideSoftInputFromWindow(view?.windowToken, 0)
    }


    private fun saveProfileChanges() {
        hideKeyboard()

        val newNickname = editNickname.text.toString().trim()
        val newBio = editBio.text.toString().trim().take(20)

        if (newNickname.isEmpty()) {
            Toast.makeText(context, "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ 닉네임이 변경된 경우만 중복 확인을 강제
        if (newNickname != originalNickname) {
            if (!isNicknameChecked) {
                Toast.makeText(context, "닉네임 중복 확인을 해주세요!", Toast.LENGTH_SHORT).show()
                return
            }
            if (!isNicknameAvailable) {
                Toast.makeText(context, "이미 사용 중인 닉네임입니다. 다른 닉네임을 입력하세요!", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(userId)
        val userProfileRef = db.collection("user_profiles").document(userId)

        val profileUpdate = mutableMapOf<String, Any>("myinfo" to newBio)

        if (newNickname != originalNickname) {
            userRef.update(mapOf("nickname" to newNickname))
                .addOnSuccessListener {
                    Log.d("Firestore", "✅ 닉네임 업데이트 성공!")
                }.addOnFailureListener { e ->
                    Log.e("Firestore", "❌ 닉네임 업데이트 실패", e)
                }
        }

        if (imageUri != null) {
            val storageRef = FirebaseStorage.getInstance().reference.child("profile_images/$userId.jpg")
            storageRef.putFile(imageUri!!)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        profileUpdate["profileImageUrl"] = uri.toString()
                        userProfileRef.update(profileUpdate)
                            .addOnSuccessListener {
                                Log.d("Firestore", "✅ 프로필 업데이트 성공! (이미지 포함)")
                                (activity as? MyPageActivity)?.refreshProfileFromFirestore(userId)
                                dismiss()
                            }
                            .addOnFailureListener {
                                Log.e("Firestore", "❌ 프로필 업데이트 실패!", it)
                            }
                    }
                }
                .addOnFailureListener {
                    Log.e("Storage", "❌ 이미지 업로드 실패!", it)
                }
        } else {
            userProfileRef.update(profileUpdate)
                .addOnSuccessListener {
                    Log.d("Firestore", "✅ 프로필 업데이트 성공! (이미지 없음)")
                    (activity as? MyPageActivity)?.refreshProfileFromFirestore(userId)
                    dismiss()
                }
                .addOnFailureListener {
                    Log.e("Firestore", "❌ 프로필 업데이트 실패!", it)
                }
        }
    }

    private fun checkNicknameDuplicate() {
        val nickname = editNickname.text.toString().trim()
        if (nickname.isEmpty()) {
            Toast.makeText(context, "닉네임을 입력하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (nickname == originalNickname) {
            Toast.makeText(context, "현재 닉네임과 동일합니다.", Toast.LENGTH_SHORT).show()
            isNicknameChecked = true
            isNicknameAvailable = true
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .whereEqualTo("nickname", nickname)
            .get()
            .addOnSuccessListener { documents ->
                isNicknameChecked = true
                if (documents.isEmpty) {
                    isNicknameAvailable = true
                    Toast.makeText(context, "사용 가능한 닉네임입니다!", Toast.LENGTH_SHORT).show()
                } else {
                    isNicknameAvailable = false
                    Toast.makeText(context, "이미 사용 중인 닉네임입니다.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    companion object {
        private const val IMAGE_PICK_CODE = 1000
    }
}
