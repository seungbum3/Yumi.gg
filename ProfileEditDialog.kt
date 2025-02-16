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

class ProfileEditDialog : DialogFragment() {

    private lateinit var profileImageView: ImageView
    private lateinit var editNickname: EditText
    private lateinit var editBio: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnCheckNickname: Button

    private var imageUri: Uri? = null
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_profile_edit, null)
        dialog.setContentView(view)

        editNickname = view.findViewById(R.id.editNickname)
        editBio = view.findViewById(R.id.editBio)


        editNickname.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null && s.length > 8) {
                    editNickname.setText(s.subSequence(0, 8))
                    editNickname.setSelection(8)
                    Toast.makeText(context, "닉네임은 최대 8자까지 입력 가능합니다.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        editBio.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.length > 20) {
                    editBio.setText(s.subSequence(0, 20))
                    editBio.setSelection(20)
                    Toast.makeText(context, "자기소개는 최대 19자까지 입력 가능합니다.", Toast.LENGTH_SHORT).show()
                }
            }
        })

        btnCheckNickname = view.findViewById(R.id.btnCheckNickname)

        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        profileImageView = view.findViewById(R.id.profileImageView)
        editNickname = view.findViewById(R.id.editNickname)
        editBio = view.findViewById(R.id.editBio)
        btnSave = view.findViewById(R.id.btnSave)
        btnCancel = view.findViewById(R.id.btnCancel)

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

        return dialog
    }

    private fun loadUserProfile() {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    editNickname.setText(document.getString("nickname"))
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
            .addOnFailureListener {
                Toast.makeText(context, "프로필 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun selectImage() {
        Toast.makeText(context, "갤러리에서 이미지를 선택하세요!", Toast.LENGTH_SHORT).show()

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

    private fun saveProfileChanges() {
        val newNickname = editNickname.text.toString().trim()
        val newBio = editBio.text.toString().trim().take(25) // 25자 제한

        if (newNickname.isEmpty()) {
            Toast.makeText(context, "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(userId)

        val updates = hashMapOf(
            "nickname" to newNickname,
            "myinfo" to newBio
        )

        userRef.update(updates as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(context, "프로필이 변경되었습니다!", Toast.LENGTH_SHORT).show()

                // 변경된 정보를 MyPageActivity로 전달
                (activity as? MyPageActivity)?.updateUserProfile(newNickname, newBio, imageUri)

                dismiss() // 다이얼로그 닫기
            }
            .addOnFailureListener {
                Toast.makeText(context, "프로필 변경 실패!", Toast.LENGTH_SHORT).show()
            }

        // 프로필 이미지가 변경되었을 경우 Firebase Storage에 업로드
        imageUri?.let { uri ->
            uploadImageToFirebase(uri)
        }
    }


    private fun uploadImageToFirebase(imageUri: Uri) {
        val storageRef = FirebaseStorage.getInstance().reference.child("profile_images/$userId.jpg")
        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val db = FirebaseFirestore.getInstance()
                    db.collection("users").document(userId)
                        .update("profileImageUrl", uri.toString())
                        .addOnSuccessListener {
                            Toast.makeText(context, "프로필 사진이 변경되었습니다!", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "프로필 사진 업로드 실패!", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        private const val IMAGE_PICK_CODE = 1000
    }
    private fun checkNicknameDuplicate() {
        val nickname = editNickname.text.toString().trim()
        if (nickname.isEmpty()) {
            Toast.makeText(context, "닉네임을 입력하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .whereEqualTo("nickname", nickname)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(context, "사용 가능한 닉네임입니다!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "이미 사용 중인 닉네임입니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "닉네임 확인 실패!", Toast.LENGTH_SHORT).show()
            }
    }
}
