package com.example.yumi2

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.yumi2.adapter.ChampionAdapter
import com.example.yumi2.viewmodel.ChampionViewModel

class MainpageActivity : AppCompatActivity() {
    private val viewModel: ChampionViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mainpage)


        recyclerView = findViewById(R.id.championRecyclerView)
        // 🔹 2줄로 가로로 배치 + 스크롤 방향을 가로로 설정
        recyclerView.layoutManager = GridLayoutManager(this, 2, GridLayoutManager.HORIZONTAL, false)

        viewModel.championList.observe(this) { championList ->
            Log.d("RecyclerView", "챔피언 리스트 업데이트됨: $championList") // 🔹 로그 추가

            recyclerView.adapter = ChampionAdapter(championList) // 🔹 올바른 데이터 전달
        }

        viewModel.fetchChampionRotations()
    }
}
