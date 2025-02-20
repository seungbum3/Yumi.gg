package com.example.yumi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

class FriendsAdapter(private val friendsList: List<Map<String, String>>) :
    RecyclerView.Adapter<FriendsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val friendProfileImage: ImageView = itemView.findViewById(R.id.friendProfileImage)
        val friendName: TextView = itemView.findViewById(R.id.friendName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val friend = friendsList[position]
        val name = friend["nickname"] ?: "알 수 없음"
        val imageUrl = friend["profileImageUrl"] ?: ""
        val friendId = friend["id"] ?: "" // 친구 ID 가져오기

        holder.friendName.text = name

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.default_profile)
            .error(R.drawable.default_profile)
            .circleCrop()
            .into(holder.friendProfileImage)

        // ✅ 친구 목록에서 클릭 시 채팅방 이동 (닉네임이 정확히 전달되도록 수정)
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener

            getOrCreateChatRoom(currentUserId, friendId) { chatId ->
                Log.d("FriendsAdapter", "🔹 친구 선택됨 - ID: $friendId, 닉네임: $name")  // ✅ 로그 추가

                val intent = Intent(context, ChatActivity::class.java).apply {
                    putExtra("chatId", chatId)
                    putExtra("friendId", friendId)
                    putExtra("friendNickname", name)  // ✅ 닉네임 전달 확인
                }
                context.startActivity(intent)
            }
        }
    }
    override fun getItemCount(): Int = friendsList.size

    private fun getOrCreateChatRoom(userA: String, userB: String, callback: (String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val chatsRef = db.collection("chats")

        chatsRef.whereArrayContains("users", userA)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val users = document.get("users") as List<String>
                    if (users.contains(userB)) {
                        // ✅ 기존 채팅방이 존재하면 해당 chatId 반환
                        callback(document.id)
                        return@addOnSuccessListener
                    }
                }
                // 채팅방이 없으면 새로 생성
                val newChatRef = chatsRef.document()
                val chatData = hashMapOf(
                    "users" to listOf(userA, userB),
                    "lastMessage" to "",
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
                newChatRef.set(chatData).addOnSuccessListener {
                    callback(newChatRef.id) // ✅ 새 chatId 반환
                }
            }
    }
}
