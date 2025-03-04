package com.example.yumi

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)





        val settingsContainer = findViewById<LinearLayout>(R.id.settingsContainer)

        val settingTexts = listOf(
            "로그인,로그아웃",
            "친구목록",
            "나만의 아이템 즐겨찾기",
            "게시글 임시저장",
            "알림 설정",
            "테마 설정",
            "이용약관",
            "개인정보 처리방침",
        )

        for (i in settingTexts.indices) {
            val itemView = layoutInflater.inflate(R.layout.item_setting, settingsContainer, false)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = dpToPx(6)
            itemView.layoutParams = params

            val itemText = itemView.findViewById<TextView>(R.id.itemText)
            itemText.text = settingTexts[i]

            if (settingTexts[i] == "나만의 아이템 즐겨찾기") {
                itemView.setOnClickListener {
                    val intent = Intent(this, ItemSelectionActivity::class.java)
                    startActivity(intent)
                }
            }
            settingsContainer.addView(itemView)
        }
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            val intent = Intent(this, MyPageActivity::class.java)
            startActivity(intent)
            finish()
        }
    }


    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
