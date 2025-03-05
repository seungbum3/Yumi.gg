package com.example.yumi2

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.yumi2.adapters.ItemAdapter
import com.example.yumi2.models.Item
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth


class ItemSelectionActivity : AppCompatActivity() {

    private lateinit var itemRecyclerView: RecyclerView
    private lateinit var itemAdapter: ItemAdapter
    private lateinit var slotRecyclerView: RecyclerView
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_selection)
        // 현재 로그인한 사용자 UID 출력 예시
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val uid = user.uid
            Log.d("UserUID", "현재 로그인한 사용자의 UID: $uid")
        } else {
            Log.d("UserUID", "사용자가 로그인되어 있지 않습니다.")
        }
        itemRecyclerView = findViewById(R.id.itemRecyclerView)
        slotRecyclerView = findViewById(R.id.slotRecyclerView)
        firestore = FirebaseFirestore.getInstance()

        val btnBack: ImageView = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        // 상단 6칸 슬롯 RecyclerView 설정
        setupSlotRecyclerView()

        // 전체 아이템 목록 RecyclerView 설정
        itemRecyclerView.layoutManager = GridLayoutManager(this, 5)
        fetchItemsFromFirestore()

        // 저장, 불러오기 버튼 참조 (activity_item_selection.xml에 추가되어 있어야 함)
        val btnSaveSlots = findViewById<Button>(R.id.btnSaveSlots)
        val btnLoadSlots = findViewById<Button>(R.id.btnLoadSlots)

        btnSaveSlots.setOnClickListener {
            saveConfiguration()
        }

        btnLoadSlots.setOnClickListener {
            showLoadConfigurationsDialog()
        }
    }



    data class SavedConfiguration(
        val configName: String = "",  // 구성 이름(문서 ID로도 활용 가능)
        val slots: List<Item?> = List(6) { null }  // 6칸 슬롯 데이터
    )

    private fun saveConfiguration() {
        val adapter = slotRecyclerView.adapter as? SlotAdapter ?: return
        val currentSlots = adapter.getSlotItems()

        val userId = getUserId()  // SharedPreferences에서 커스텀 ID 가져오기
        if (userId.isEmpty()) {
            Log.e("SaveConfig", "저장할 유저 ID가 없습니다.")
            return
        }

        val configName = "MyBuild_${System.currentTimeMillis()}"

        val config = SavedConfiguration(
            configName = configName,
            slots = currentSlots
        )

        val configCollection = firestore.collection("users")
            .document(userId)
            .collection("savedConfigurations")

        configCollection.document(configName).set(config)
            .addOnSuccessListener {
                Toast.makeText(this, "구성 저장 성공", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("SaveConfig", "구성 저장 실패", e)
                Toast.makeText(this, "구성 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun showLoadConfigurationsDialog() {
        val userId = getUserId()
        if (userId.isEmpty()) {
            Log.e("LoadConfig", "User ID가 없습니다.")
            return
        }

        val configCollection = firestore.collection("users")
            .document(userId)
            .collection("savedConfigurations")

        configCollection.get()
            .addOnSuccessListener { snapshot ->
                val configList = snapshot.documents.mapNotNull { document ->
                    // 구성 이름은 문서 ID를 사용하거나, configName 필드를 활용
                    document.toObject(SavedConfiguration::class.java)?.copy(configName = document.id)
                }
                if (configList.isEmpty()) {
                    Toast.makeText(this, "저장된 구성이 없습니다.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // 구성 이름 배열 만들기
                val configNames = configList.map { it.configName }.toTypedArray()

                AlertDialog.Builder(this)
                    .setTitle("불러올 구성을 선택하세요")
                    .setItems(configNames) { dialog, which ->
                        val selectedConfig = configList[which]
                        // 선택된 구성의 슬롯 데이터를 SlotAdapter에 적용
                        val adapter = slotRecyclerView.adapter as? SlotAdapter
                        adapter?.setSlots(selectedConfig.slots)
                        Toast.makeText(this, "구성 불러오기 성공", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
            .addOnFailureListener { e ->
                Log.e("LoadConfig", "구성 불러오기 실패", e)
                Toast.makeText(this, "구성 불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun setupSlotRecyclerView() {
        slotRecyclerView.layoutManager = GridLayoutManager(this, 6)
        slotRecyclerView.adapter = SlotAdapter() // 6칸 빈 슬롯 초기화
    }

    private fun fetchItemsFromFirestore() {
        firestore.collection("items")
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { documents ->
                val itemList = mutableListOf<Item>()
                for (document in documents) {
                    val id = document.getString("id") ?: ""
                    val name = document.getString("name") ?: "알 수 없음"
                    val imageUrl = document.getString("imageUrl") ?: ""
                    itemList.add(Item(id, name, imageUrl))
                }
                itemAdapter = ItemAdapter(itemList) { item ->
                    addItemToSlot(item)
                }
                itemRecyclerView.adapter = itemAdapter
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "아이템 가져오기 실패", e)
            }
    }

    private fun addItemToSlot(item: Item) {
        val adapter = slotRecyclerView.adapter as? SlotAdapter
        adapter?.addItemToSlot(item)
    }

    // 저장 기능: 현재 슬롯에 들어있는 아이템들을 Firestore의 "savedItems" 하위 컬렉션에 저장
    private fun saveSlots() {
        val adapter = slotRecyclerView.adapter as? SlotAdapter ?: return
        val slots = adapter.getSlotItems() // SlotAdapter에서 추가할 메소드
        val userId = getUserId()  // SharedPreferences에서 로그인한 유저 ID 가져오기
        if (userId.isEmpty()) {
            Log.e("SaveSlots", "저장할 유저 ID가 없습니다.")
            return
        }
        val savedItemsCollection = firestore.collection("users")
            .document(userId)
            .collection("savedItems")

        // 각 슬롯 인덱스에 대해 저장 (빈 슬롯은 삭제)
        for (index in slots.indices) {
            val slotItem = slots[index]
            val docId = "slot$index"
            if (slotItem != null) {
                val data = hashMapOf(
                    "id" to slotItem.id,
                    "name" to slotItem.name,
                    "imageUrl" to slotItem.imageUrl,
                    "slotIndex" to index
                )
                savedItemsCollection.document(docId).set(data)
                    .addOnSuccessListener {
                        Log.d("SaveSlots", "Slot $index 저장 성공")
                    }
                    .addOnFailureListener { e ->
                        Log.e("SaveSlots", "Slot $index 저장 실패", e)
                    }
            } else {
                // 빈 슬롯이면 기존 문서가 있다면 삭제
                savedItemsCollection.document(docId).delete()
                    .addOnSuccessListener {
                        Log.d("SaveSlots", "Slot $index 삭제 성공")
                    }
                    .addOnFailureListener { e ->
                        Log.e("SaveSlots", "Slot $index 삭제 실패", e)
                    }
            }
        }
    }

    // 불러오기 기능: Firestore에서 "savedItems" 하위 컬렉션 데이터를 불러와 슬롯 상태 업데이트
    private fun loadSlots() {
        val userId = getUserId()
        if (userId.isEmpty()) {
            Log.e("LoadSlots", "불러올 유저 ID가 없습니다.")
            return
        }
        val savedItemsCollection = firestore.collection("users")
            .document(userId)
            .collection("savedItems")
        savedItemsCollection.get()
            .addOnSuccessListener { querySnapshot ->
                // 6칸 배열로 초기화 후, 불러온 데이터의 slotIndex에 맞게 채움
                val loadedSlots = MutableList<Item?>(6) { null }
                for (document in querySnapshot.documents) {
                    val slotIndex = document.getLong("slotIndex")?.toInt() ?: continue
                    if (slotIndex in 0..5) {
                        val item = document.toObject(Item::class.java)
                        loadedSlots[slotIndex] = item
                    }
                }
                val adapter = slotRecyclerView.adapter as? SlotAdapter
                adapter?.setSlots(loadedSlots) // SlotAdapter에 새 메소드 추가
            }
            .addOnFailureListener { e ->
                Log.e("LoadSlots", "저장된 슬롯 불러오기 실패", e)
            }
    }

    // SharedPreferences에서 로그인한 유저 ID를 가져오는 함수
    private fun getUserId(): String {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        return sharedPref.getString("loggedInUserId", "") ?: ""
    }
}
