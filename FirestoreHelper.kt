package com.example.yumi

import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

object FirestoreHelper {
    private val db = FirebaseFirestore.getInstance()

    fun saveChampionRotation(rotationDate: String, championList: List<Champion_rotation>) {
        val docRef = db.collection("champion_rotation").document(rotationDate)
        val championMap = hashMapOf("champion_list" to championList)

        docRef.set(championMap)
            .addOnSuccessListener {
                Log.d("Firestore", "로테이션 저장 성공: $rotationDate")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "저장 실패: ${e.message}")
            }
    }
}
