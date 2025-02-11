package com.example.yumi

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ChampionViewModel : ViewModel() {
    private val _champions = MutableLiveData<List<Champion_rotation>>()
    val champions: LiveData<List<Champion_rotation>> get() = _champions

    private val db = FirebaseFirestore.getInstance()
    private val apiService = RetrofitClient.instance.create(RiotApiService::class.java)
    private val client = OkHttpClient()

    fun fetchWeeklyRotation(apiKey: String) {
        val rotationDate = getRotationWeekDate()

        db.collection("champion_rotation").document(rotationDate)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // 📌 문서는 존재하지만 "champion_list" 필드가 없을 경우, 새로 데이터를 가져오도록 수정
                    if (!document.contains("champion_list")) {
                        Log.d("Firestore", "문서는 있지만 champion_list 필드가 없음, 새로 데이터 가져오기")
                        fetchChampionRotation(apiKey, rotationDate)
                    } else {
                        // 📌 "champion_list"가 존재하면 데이터를 불러옴
                        val champions = document["champion_list"] as? List<HashMap<String, Any>>
                        val championList = champions?.map { champ ->
                            Champion_rotation(
                                (champ["id"] as Long).toInt(),
                                champ["name"] as String,
                                champ["imageUrl"] as String
                            )
                        } ?: emptyList()

                        Log.d("Firestore", "Firestore에서 기존 데이터 불러오기 성공: $championList")
                        _champions.postValue(championList)
                    }
                } else {
                    Log.d("Firestore", "문서 자체가 존재하지 않음, 새로 생성")
                    fetchChampionRotation(apiKey, rotationDate)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Firestore 가져오기 실패: ${e.message}")
            }
    }


    private fun fetchChampionRotation(apiKey: String, rotationDate: String) {
        val call = apiService.getChampionRotation(apiKey)

        call.enqueue(object : retrofit2.Callback<ChampionRotationResponse> {
            override fun onResponse(call: retrofit2.Call<ChampionRotationResponse>, response: retrofit2.Response<ChampionRotationResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { rotationResponse ->
                        fetchChampionNames(rotationResponse.freeChampionIds, rotationDate)
                    }
                }
            }

            override fun onFailure(call: retrofit2.Call<ChampionRotationResponse>, t: Throwable) {
                Log.e("Retrofit", "로테이션 데이터 받아오기 실패")
            }
        })
    }

    private fun fetchChampionNames(championIds: List<Int>, rotationDate: String) {
        val url = "https://ddragon.leagueoflegends.com/cdn/15.2.1/data/ko_KR/champion.json"

        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("OkHttp", "챔피언 데이터 받아오기 실패")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (!response.isSuccessful) return

                response.body?.let { responseBody ->
                    val jsonString = responseBody.string()
                    val championList = parseChampionData(jsonString, championIds)

                    val docRef = db.collection("champion_rotation").document(rotationDate)
                    val championMap = hashMapOf("champion_list" to championList)

                    docRef.set(championMap)
                        .addOnSuccessListener {
                            Log.d("Firebase", "로테이션 저장 성공: $rotationDate")
                            _champions.postValue(championList)
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firebase", "Firestore 저장 실패: ${e.message}")
                        }
                }
            }
        })
    }

    private fun parseChampionData(jsonString: String, championIds: List<Int>): List<Champion_rotation> {
        val championList = mutableListOf<Champion_rotation>()
        val jsonObject = JSONObject(jsonString).getJSONObject("data")

        for (key in jsonObject.keys()) {
            val champData = jsonObject.getJSONObject(key)
            val champId = champData.getInt("key")
            if (champId in championIds) {
                val name = champData.getString("name") // ✅ 챔피언 한글 이름 가져오기
                val engName = champData.getString("id") // ✅ 영어 이름 (이미지 URL용)
                val imageUrl = "https://ddragon.leagueoflegends.com/cdn/15.2.1/img/champion/$engName.png"
                championList.add(Champion_rotation(champId, name, imageUrl))
            }
        }
        return championList
    }

    private fun getRotationWeekDate(): String {
        val calendar = Calendar.getInstance()

        // 현재 날짜가 화요일 00시를 지나면 이번 주 화요일 날짜를 사용
        // 아직 화요일 00시 이전이면 지난주 화요일을 사용
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
        if (calendar.timeInMillis > System.currentTimeMillis()) {
            calendar.add(Calendar.WEEK_OF_YEAR, -1)
        }

        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(calendar.time)
    }
}
