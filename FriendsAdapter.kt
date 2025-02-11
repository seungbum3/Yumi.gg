package com.example.yumi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

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
        val name = friend["name"] ?: "알 수 없음"
        val imageUrl = friend["imageResId"] ?: ""

        holder.friendName.text = name

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.default_profile) // 기본 이미지
            .error(R.drawable.default_profile) // 에러 발생 시 기본 이미지
            .circleCrop() // ✅ 원형 이미지 적용
            .into(holder.friendProfileImage)
    }

    override fun getItemCount(): Int = friendsList.size
}
