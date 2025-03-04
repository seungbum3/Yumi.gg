package com.example.yumi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.yumi.R
import com.example.yumi.models.Item

class ItemAdapter(private val itemList: List<Item>, private val itemClickListener: (Item) -> Unit) :
    RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemImage: ImageView = view.findViewById(R.id.itemImage)
        val itemName: TextView = view.findViewById(R.id.itemName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)

        // 부모 RecyclerView의 너비를 가져와서 아이템 크기 조정
        val parentWidth = parent.width
        val itemWidth = (parentWidth / 5) - 10  // 🔹 6칸 배치 + 간격 확보 (여백 포함)

        val layoutParams = view.layoutParams
        layoutParams.width = itemWidth
        layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT // 🔹 높이 자동 조정
        view.layoutParams = layoutParams

        return ItemViewHolder(view)
    }


    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = itemList[position]
        holder.itemName.text = item.name

        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.error_image)
            .into(holder.itemImage)

        holder.itemView.setBackgroundResource(R.drawable.item_slot_background)

        holder.itemView.setOnClickListener {
            itemClickListener(item)
        }
    }



    override fun getItemCount() = itemList.size
}
