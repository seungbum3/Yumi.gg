package com.example.yumi

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.yumi.adapters.ItemAdapter
import com.example.yumi.models.Item
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan

class ItemSelectionActivity : AppCompatActivity() {

    private lateinit var itemRecyclerView: RecyclerView
    private lateinit var itemAdapter: ItemAdapter
    private lateinit var slotRecyclerView: RecyclerView
    lateinit var firestore: FirebaseFirestore

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
        btnBack.setOnClickListener { finish() }

        // 상단 6칸 슬롯 RecyclerView 설정
        setupSlotRecyclerView()

        // 전체 아이템 목록 RecyclerView 설정
        itemRecyclerView.layoutManager = GridLayoutManager(this, 5)
        fetchItemsFromFirestore()

        // 저장, 불러오기 버튼 참조
        val btnSaveSlots = findViewById<Button>(R.id.btnSaveSlots)
        val btnLoadSlots = findViewById<Button>(R.id.btnLoadSlots)

        btnSaveSlots.setOnClickListener { saveConfiguration() }
        btnLoadSlots.setOnClickListener { showLoadConfigurationsDialog() }
    }

    // 저장된 구성 데이터 클래스 (6칸 슬롯)
    data class SavedConfiguration(
        val configName: String = "",  // 구성 이름(문서 ID로 활용)
        val slots: List<Item?> = List(6) { null }  // 6칸 슬롯 데이터
    )

    // 저장 기능: 최소 1개 이상의 아이템이 선택되어야 저장
    private fun saveConfiguration() {
        val adapter = slotRecyclerView.adapter as? SlotAdapter ?: return
        val currentSlots = adapter.getSlotItems()

        // 모든 슬롯이 null이면 저장 불가
        if (currentSlots.all { it == null }) {
            Toast.makeText(this, "최소 하나 이상의 아이템을 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = getUserId()  // 로그인한 유저 ID 가져오기
        if (userId.isEmpty()) {
            Log.e("SaveConfig", "저장할 유저 ID가 없습니다.")
            return
        }

        // custom_save_dialog.xml 파일을 inflate하여 커스텀 뷰로 사용
        val customView = layoutInflater.inflate(R.layout.custom_save_dialog, null)
        val editText = customView.findViewById<EditText>(R.id.editTextDialogName)
        val btnSave = customView.findViewById<Button>(R.id.btnSave)
        val btnCancel = customView.findViewById<Button>(R.id.btnCancel)

        // 타이틀 텍스트 색상은 XML에서 이미 설정했다고 가정
        val dialog = AlertDialog.Builder(this)
            .setView(customView)
            .create()

        // 저장 버튼 클릭 이벤트 처리
        btnSave.setOnClickListener {
            val customName = editText.text.toString().trim()
            if (customName.isEmpty()) {
                Toast.makeText(this, "구성 이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val config = SavedConfiguration(
                configName = customName,
                slots = currentSlots
            )
            firestore.collection("users")
                .document(userId)
                .collection("savedConfigurations")
                .document(customName)
                .set(config)
                .addOnSuccessListener {
                    Toast.makeText(this, "구성 저장 성공", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener { e ->
                    Log.e("SaveConfig", "구성 저장 실패", e)
                    Toast.makeText(this, "구성 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // 취소 버튼 클릭 이벤트 처리
        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    inner class SavedBuildAdapter(
        private val context: ItemSelectionActivity,
        private val configList: MutableList<SavedConfiguration>
    ) : BaseAdapter() {
        override fun getCount(): Int = configList.size
        override fun getItem(position: Int): Any = configList[position]
        override fun getItemId(position: Int): Long = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view: View = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_saved_build, parent, false)
            val tvBuildName = view.findViewById<TextView>(R.id.tvBuildName)
            val ivDelete = view.findViewById<ImageView>(R.id.ivDelete)

            val config = configList[position]
            tvBuildName.text = config.configName

            // 리스트 아이템 클릭 시 구성 불러오기
            view.setOnClickListener {
                val adapter = slotRecyclerView.adapter as? SlotAdapter
                adapter?.setSlots(config.slots)
                Toast.makeText(context, "구성 불러오기 성공", Toast.LENGTH_SHORT).show()
                // 다이얼로그 닫기
                (parent as? ListView)?.let { listView ->
                    (listView.parent as? AlertDialog)?.dismiss()
                }
            }

            // 삭제 버튼 클릭 시 해당 구성 삭제
            ivDelete.setOnClickListener {
                val userId = context.getUserId()
                if (userId.isNotEmpty()) {
                    firestore.collection("users")
                        .document(userId)
                        .collection("savedConfigurations")
                        .document(config.configName)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(context, "구성 삭제 성공", Toast.LENGTH_SHORT).show()
                            configList.removeAt(position)
                            notifyDataSetChanged()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "구성 삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            return view
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
                    document.toObject(SavedConfiguration::class.java)?.copy(configName = document.id)
                }.toMutableList()

                if (configList.isEmpty()) {
                    Toast.makeText(this, "저장된 구성이 없습니다.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val listView = ListView(this)
                val adapter = SavedBuildAdapter(this, configList)
                listView.adapter = adapter

                // 타이틀 텍스트에 색상 변경 (80929F)
                val titleText = SpannableString("불러올 구성을 선택하세요")
                titleText.setSpan(
                    ForegroundColorSpan(Color.parseColor("#80929F")),
                    0,
                    titleText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                val dialog = AlertDialog.Builder(this)
                    .setTitle(titleText)
                    .setView(listView)
                    .setNegativeButton("취소", null)
                    .create()

                dialog.show()

                // 다이얼로그 전체 백그라운드 색상을 E7EBED로 변경
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#E7EBED")))

                // 취소 버튼 텍스트 색상을 80929F로 변경
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    ?.setTextColor(Color.parseColor("#80929F"))
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
        val userId = getUserId()  // 로그인한 유저 ID 가져오기
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

    // 불러오기 기능: Firestore에서 "savedItems" 데이터를 불러와 슬롯 상태 업데이트
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
                val loadedSlots = MutableList<Item?>(6) { null }
                for (document in querySnapshot.documents) {
                    val slotIndex = document.getLong("slotIndex")?.toInt() ?: continue
                    if (slotIndex in 0..5) {
                        val item = document.toObject(Item::class.java)
                        loadedSlots[slotIndex] = item
                    }
                }
                val adapter = slotRecyclerView.adapter as? SlotAdapter
                adapter?.setSlots(loadedSlots)
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
