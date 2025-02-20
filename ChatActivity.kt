package com.example.yumi

import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var chatTitle: TextView
    private lateinit var backButton: ImageButton

    private lateinit var chatId: String
    private lateinit var friendId: String
    private lateinit var friendNickname: String
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)












        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        chatTitle = findViewById(R.id.chatTitle)
        backButton = findViewById(R.id.backButton)


        // 현재 로그인한 유저 ID 가져오기
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        if (currentUserId.isEmpty()) {
            finish()
            return
        }


        friendId = intent.getStringExtra("friendId") ?: ""
        friendNickname = intent.getStringExtra("friendNickname") ?: "알 수 없는 사용자"


        // 채팅방 상단 제목 친구 이름으로
        chatTitle.text = friendNickname

        // 뒤로가기 버튼 동작
        backButton.setOnClickListener {
            finish()
        }

        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatAdapter = ChatAdapter(mutableListOf(), currentUserId)
        chatRecyclerView.adapter = chatAdapter

        if (friendId.isNotEmpty()) {
            getOrCreateChatRoom(currentUserId, friendId) { chatRoomId ->
                chatId = chatRoomId
                loadMessages(chatId)
            }
        }

        sendButton.setOnClickListener {
            val message = messageInput.text.toString().trim()
            if (message.isNotEmpty() && ::chatId.isInitialized) {
                sendMessage(chatId, currentUserId, message)
                messageInput.setText("") // 입력창 초기화
            }
            if (!::chatId.isInitialized) {
                Log.e("ChatActivity", "⚠️ chatId가 초기화되지 않았습니다!")
                return@setOnClickListener
            }

        }

        val rootView = window.decorView.rootView
        rootView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val rect = Rect()
                rootView.getWindowVisibleDisplayFrame(rect)
                val screenHeight = rootView.height
                val keypadHeight = screenHeight - rect.bottom

                if (keypadHeight > screenHeight * 0.15) { // 키보드가 올라온 경우
                    chatRecyclerView.postDelayed({
                        chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                    }, 100)
                }
            }
        })

        // 여러 줄 입력 시 높이 제한하여 버튼 위치 고정
        messageInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                messageInput.post {
                    val lineCount = messageInput.lineCount

                    if (lineCount in 1..5) { // 1~5줄까지 자연스럽게 크기 확장
                        messageInput.layoutParams.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    } else if (lineCount > 5) { // 5줄 이상 입력하면 크기 유지 & 내부 스크롤
                        messageInput.layoutParams.height = messageInput.lineHeight * 5 + 20 // 크기를 약간 키워 4번째 줄 기준으로 맞춤
                        messageInput.scrollTo(0, messageInput.bottom)
                    }
                    messageInput.requestLayout() // 크기 변경 후 적용
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })




        messageInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                chatRecyclerView.postDelayed({
                    chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                }, 300)
            }
        }

        sendButton.setOnClickListener {
            val message = messageInput.text.toString().trim()
            if (message.isNotEmpty() && ::chatId.isInitialized) {
                sendMessage(chatId, currentUserId, message)
                messageInput.setText("") // 입력창 초기화
            }
        }
    }

    // 채팅방을 찾거나 생성하는 함수
    private fun getOrCreateChatRoom(userA: String, userB: String, callback: (String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val chatsRef = db.collection("chats")

        chatsRef.whereArrayContainsAny("users", listOf(userA, userB))
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val users = document.get("users") as List<String>
                    if (users.contains(userA) && users.contains(userB)) {
                        callback(document.id)
                        return@addOnSuccessListener
                    }
                }
                // 채팅방이 없으면 새로 생성
                val newChatRef = chatsRef.document()
                val chatData = hashMapOf(
                    "users" to listOf(userA, userB),
                    "lastMessage" to "",
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
                newChatRef.set(chatData).addOnSuccessListener {
                    callback(newChatRef.id)
                }
            }

    }

    // 메시지 전송 함수
    private fun sendMessage(chatId: String, senderId: String, message: String) {
        val db = FirebaseFirestore.getInstance()
        val messageRef = db.collection("chats").document(chatId).collection("messages").document()

        val messageData = hashMapOf(
            "senderId" to senderId,
            "text" to message,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        messageRef.set(messageData)

        // 채팅방의 마지막 메시지 업데이트
        db.collection("chats").document(chatId)
            .update(mapOf(
                "lastMessage" to message,
                "updatedAt" to com.google.firebase.Timestamp.now()
            ))
    }

    // 채팅 내역 불러오기 함수
    private fun loadMessages(chatId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener

                val messages = mutableListOf<ChatMessage>() // 올바른 타입 사용

                for (doc in snapshots.documents) {
                    val message = doc.toObject(ChatMessage::class.java) // 변환
                    if (message != null) {
                        messages.add(message)
                    }
                }

                // currentUserId를 추가하여 ChatAdapter 생성
                chatAdapter = ChatAdapter(messages, currentUserId)
                chatRecyclerView.adapter = chatAdapter
            }
    }
}
