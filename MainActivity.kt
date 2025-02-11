package com.example.yumi

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.FirebaseApp


class MainActivity : AppCompatActivity() {
    private val championViewModel: ChampionViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChampionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_main)


        recyclerView = findViewById(R.id.recyclerView) // RecyclerView 연결
        adapter = ChampionAdapter(emptyList()) // 초기 빈 리스트
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val apiKey = "RGAPI-c4844fef-eeb6-40e5-9369-9d9153341e6b" // 🔑 Riot API Key (필수)

        // 데이터 불러오기
        championViewModel.fetchWeeklyRotation(apiKey)

        // 데이터 변경 감지 및 UI 업데이트
        championViewModel.champions.observe(this, Observer { champions ->
            adapter.updateData(champions) // RecyclerView 업데이트
        })
    }
}
