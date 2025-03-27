package com.example.opggyumi

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import coil.load
import java.text.SimpleDateFormat
import java.util.*

data class Comment(
    val text: String,
    val timestamp: Long,
    val uid: String? = null,
    val nickname: String? = null
)

class PostDetailFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private var postId: String = ""
    // 게시글 작성자의 uid를 저장 (삭제 버튼 보임 여부 결정)
    private var postAuthorUid: String = ""

    // UI 요소들
    private lateinit var detailPostTitle: TextView
    private lateinit var detailPostContent: TextView
    private lateinit var detailPostCategory: TextView
    private lateinit var detailPostTimestamp: TextView
    private lateinit var detailPostViewCount: TextView
    private lateinit var detailPostImage: ImageView
    private lateinit var detailPostNickname: TextView
    // 해시태그 표시 TextView
    private lateinit var hashtagTextView: TextView

    // 댓글 관련 UI
    private lateinit var commentEditText: EditText
    private lateinit var commentSendButton: Button
    private lateinit var commentRecyclerView: RecyclerView
    private lateinit var commentAdapter: CommentAdapter
    private var comments = mutableListOf<Comment>()

    // 삭제 기능을 위한 "삭제" 텍스트뷰
    private lateinit var deleteTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore = FirebaseFirestore.getInstance()
        postId = arguments?.getString("postId") ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_post_detail, container, false)
        detailPostTitle = view.findViewById(R.id.detail_post_title)
        detailPostContent = view.findViewById(R.id.detail_post_content)
        detailPostCategory = view.findViewById(R.id.detail_post_category)
        detailPostTimestamp = view.findViewById(R.id.detail_post_timestamp)
        detailPostViewCount = view.findViewById(R.id.detail_post_view_count)
        detailPostImage = view.findViewById(R.id.detail_post_image)
        detailPostNickname = view.findViewById(R.id.detail_post_nickname)
        hashtagTextView = view.findViewById(R.id.hashtagTextView)

        commentEditText = view.findViewById(R.id.commentEditText)
        commentSendButton = view.findViewById(R.id.commentSendButton)
        commentRecyclerView = view.findViewById(R.id.commentRecyclerView)
        commentRecyclerView.layoutManager = LinearLayoutManager(context)
        commentAdapter = CommentAdapter(comments)
        commentRecyclerView.adapter = commentAdapter

        // 삭제 텍스트뷰 초기화 및 클릭 리스너 설정
        deleteTextView = view.findViewById(R.id.deleteTextView)
        deleteTextView.setOnClickListener { showDeleteConfirmation() }

        commentSendButton.setOnClickListener {
            val commentText = commentEditText.text.toString().trim()
            if (commentText.isNotEmpty()) {
                postComment(commentText)
            } else {
                Toast.makeText(context, "댓글을 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (postId.isNotEmpty()) {
            val postRef = firestore.collection("posts").document(postId)
            postRef.update("views", FieldValue.increment(1))
                .addOnSuccessListener {
                    postRef.get().addOnSuccessListener { document ->
                        val views = document.getLong("views") ?: 0
                        view.findViewById<TextView>(R.id.detail_post_view_count).text = "조회수: $views"
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("FirestoreError", "조회수 증가 실패: ${exception.message}")
                }
            loadPostDetails(postId)
            loadComments(postId)
        } else {
            Toast.makeText(context, "Invalid post ID", Toast.LENGTH_SHORT).show()
        }

        val backButton: ImageView = view.findViewById(R.id.backButton)
        backButton.setOnClickListener { requireActivity().onBackPressed() }

        val toggleCommentText: TextView = view.findViewById(R.id.toggleCommentText)
        val commentInputLayout: View = view.findViewById(R.id.commentInputContainer)
        val commentRecyclerView: View = view.findViewById(R.id.commentRecyclerView)
        commentInputLayout.visibility = View.GONE
        commentRecyclerView.visibility = View.GONE
        var isCommentVisible = false
        toggleCommentText.setOnClickListener {
            isCommentVisible = !isCommentVisible
            if (isCommentVisible) {
                commentInputLayout.visibility = View.VISIBLE
                commentRecyclerView.visibility = View.VISIBLE
            } else {
                commentInputLayout.visibility = View.GONE
                commentRecyclerView.visibility = View.GONE
            }
        }
    }

    // 게시글 상세 정보를 불러오는 함수 (작성자 uid 및 해시태그 포함)
    private fun loadPostDetails(postId: String) {
        val postRef = firestore.collection("posts").document(postId)
        postRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val title = document.getString("title") ?: "제목 없음"
                    val content = document.getString("content") ?: "내용 없음"
                    val category = document.getString("category") ?: "카테고리 없음"
                    val timestamp = document.getLong("timestamp") ?: 0L
                    val imageUrl = document.getString("imageUrl") ?: ""
                    val nickname = document.getString("nickname") ?: "닉네임 없음"
                    // 게시글 작성자 uid와 해시태그 읽기
                    postAuthorUid = document.getString("uid") ?: ""
                    val hashtags = document.get("hashtags") as? List<String> ?: emptyList()

                    detailPostTitle.text = title
                    detailPostContent.text = content
                    detailPostCategory.text = category
                    detailPostNickname.text = nickname

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    detailPostTimestamp.text = dateFormat.format(Date(timestamp))

                    if (imageUrl.isNotEmpty()) {
                        detailPostImage.visibility = View.VISIBLE
                        detailPostImage.load(imageUrl) { crossfade(true) }
                    } else {
                        detailPostImage.visibility = View.GONE
                    }

                    // 현재 로그인한 사용자의 uid와 비교해서 "삭제" 버튼 보임 여부 설정
                    val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
                    if (currentUserUid != null && currentUserUid == postAuthorUid) {
                        deleteTextView.visibility = View.VISIBLE
                    } else {
                        deleteTextView.visibility = View.GONE
                    }
                    // 해시태그 TextView에 표시
                    hashtagTextView.text = hashtags.joinToString(", ")
                } else {
                    Toast.makeText(context, "게시글을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "게시글 로드 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun postComment(commentText: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val uid = currentUser.uid
        firestore.collection("user_profiles").document(uid)
            .get()
            .addOnSuccessListener { document ->
                val nickname = document.getString("nickname") ?: "닉네임 없음"
                val commentData = hashMapOf(
                    "text" to commentText,
                    "timestamp" to System.currentTimeMillis(),
                    "uid" to uid,
                    "nickname" to nickname
                )
                firestore.collection("posts")
                    .document(postId)
                    .collection("comments")
                    .add(commentData)
                    .addOnSuccessListener {
                        Toast.makeText(context, "댓글 저장 성공!", Toast.LENGTH_SHORT).show()
                        commentEditText.text.clear()
                        loadComments(postId)
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(context, "댓글 저장 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "닉네임 불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadComments(postId: String) {
        firestore.collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { querySnapshot ->
                comments.clear()
                for (document in querySnapshot.documents) {
                    val commentText = document.getString("text") ?: ""
                    val timestamp = document.getLong("timestamp") ?: 0L
                    val uid = document.getString("uid") ?: ""
                    val nickname = document.getString("nickname") ?: "닉네임 없음"
                    comments.add(Comment(commentText, timestamp, uid, nickname))
                }
                commentAdapter.notifyDataSetChanged()
                val toggleCommentText: TextView? = view?.findViewById(R.id.toggleCommentText)
                toggleCommentText?.text = "댓글[${comments.size}]"
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "댓글 불러오기 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("게시글 삭제")
            .setMessage("게시글을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ -> deletePost() }
            .setNegativeButton("취소") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun deletePost() {
        firestore.collection("posts").document(postId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "게시글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressed()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "게시글 삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showReplyDialog(comment: Comment) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("답글 입력")
        val editText = EditText(requireContext())
        builder.setView(editText)
        builder.setPositiveButton("전송") { _, _ ->
            val replyText = editText.text.toString().trim()
            if (replyText.isNotEmpty()) {
                postReply(comment, replyText)
            } else {
                Toast.makeText(requireContext(), "답글을 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("취소") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    // 참고: 현재 Comment 데이터 클래스에는 id 필드가 없으므로, 실제 구현 시 id를 포함해야 합니다.
    private fun postReply(comment: Comment, replyText: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(context, "로그인이 필요합니다", Toast.LENGTH_SHORT).show()
            return
        }
        val uid = currentUser.uid

        firestore.collection("posts")
            .document(postId)
            .collection("comments")
            .document("댓글문서ID")  // 실제 댓글 문서 ID로 수정 필요
            .collection("replies")
            .add(hashMapOf(
                "text" to replyText,
                "timestamp" to System.currentTimeMillis(),
                "uid" to uid,
                "nickname" to (comment.nickname ?: "닉네임 없음")
            ))
            .addOnSuccessListener {
                Toast.makeText(context, "답글 저장 성공", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "답글 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
