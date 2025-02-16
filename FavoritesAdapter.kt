package com.example.yumi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log


class FavoritesAdapter(
    private var favoriteList: MutableList<HashMap<String, String>>, // 🔹 변경 가능한 리스트
    private val userId: String
) : RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder>() {

    // 🔹 즐겨찾기 상태를 저장할 Map (소환사명 → 현재 상태)
    private val favoriteStatus = mutableMapOf<String, Boolean>()

    class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val favoriteIcon: ImageView = itemView.findViewById(R.id.favoriteIcon) // 별표 아이콘
        val favoriteName: TextView = itemView.findViewById(R.id.favoriteName) // 닉네임
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_favorite, parent, false)
        return FavoriteViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        val favorite = favoriteList[position]
        val summonerName = favorite["summonerName"] ?: "알 수 없음"

        holder.favoriteName.text = summonerName

        // 🔹 처음 로딩할 때 Firestore에서 가져온 상태 반영
        if (!favoriteStatus.containsKey(summonerName)) {
            favoriteStatus[summonerName] = true // 기본적으로 즐겨찾기 활성화
        }

        // 🔹 현재 상태에 따라 UI 변경
        if (favoriteStatus[summonerName] == true) {
            holder.favoriteIcon.setImageResource(R.drawable.ic_star) // ⭐ 채워진 별
        } else {
            holder.favoriteIcon.setImageResource(R.drawable.ic_star_empty) // ☆ 빈 별
        }

        // 🔹 별표 클릭 이벤트 (UI 상태만 변경)
        holder.favoriteIcon.setOnClickListener {
            val isFavorite = favoriteStatus[summonerName] ?: true
            favoriteStatus[summonerName] = !isFavorite // 상태 반전

            // UI 업데이트
            if (!isFavorite) {
                holder.favoriteIcon.setImageResource(R.drawable.ic_star) // ⭐ 즐겨찾기 추가
            } else {
                holder.favoriteIcon.setImageResource(R.drawable.ic_star_empty) // ☆ 즐겨찾기 해제
            }
        }
    }

    override fun getItemCount(): Int = favoriteList.size

    // 🔹 마이페이지를 떠날 때 Firestore에 변경 사항 반영하는 함수
    fun syncFavoritesWithFirestore(db: FirebaseFirestore, onComplete: () -> Unit) {
        val batch = db.batch()

        favoriteStatus.forEach { (summonerName, isFavorite) ->
            val docRef = db.collection("users").document(userId)
                .collection("favorites").document(summonerName)

            if (isFavorite) {
                // 🔹 즐겨찾기 유지 → Firestore에 추가
                val favoriteData = hashMapOf("summonerName" to summonerName)
                batch.set(docRef, favoriteData)
            } else {
                // 🔹 즐겨찾기 해제 → Firestore에서 삭제
                batch.delete(docRef)
            }
        }

        // 🔹 Firestore에 변경 사항 반영
        batch.commit()
            .addOnSuccessListener {
                Log.d("Firestore", "즐겨찾기 동기화 완료")
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "즐겨찾기 동기화 실패", e)
            }
    }
}


