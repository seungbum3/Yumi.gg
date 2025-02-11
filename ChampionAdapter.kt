package com.example.yumi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ChampionAdapter(private var champions: List<Champion_rotation>) :
    RecyclerView.Adapter<ChampionAdapter.ChampionViewHolder>() {

    class ChampionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.championImage)
        val nameTextView: TextView = view.findViewById(R.id.championName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChampionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_champion, parent, false)
        return ChampionViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChampionViewHolder, position: Int) {
        val champion = champions[position]
        holder.nameTextView.text = champion.name // 한글 챔피언 이름
        Glide.with(holder.itemView.context).load(champion.imageUrl).into(holder.imageView)
    }

    override fun getItemCount(): Int = champions.size

    fun updateData(newChampions: List<Champion_rotation>) {
        champions = newChampions
        notifyDataSetChanged() // 데이터 변경 시 UI 업데이트
    }
}
