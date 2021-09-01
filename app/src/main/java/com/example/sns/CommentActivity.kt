package com.example.sns

import android.os.Bundle
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.sns.modelpackage.AlarmDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import org.w3c.dom.Text

class CommentActivity : AppCompatActivity() {

    var contentUid: String? = null
    var user: FirebaseUser? = null
    var destinationUid: String? = null
    var commentSnapshot: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)
        user = FirebaseAuth.getInstance().currentUser
        destinationUid = intent.getStringExtra("destinationUid")
        contentUid = intent.getStringExtra("contentUid")
        findViewById<Button>(R.id.comment_btn_send).setOnClickListener {
            val comment = ContentDTO.Comment()
            comment.userId = FirebaseAuth.getInstance().currentUser!!.email
            comment.comment = findViewById<EditText>(R.id.comment_edit_message).text.toString()
            comment.uid = FirebaseAuth.getInstance().currentUser!!.uid
            comment.timestamp = System.currentTimeMillis()
            FirebaseFirestore.getInstance()
                    .collection("images")
                    .document(contentUid!!)
                    .collection("comments")
                    .document()
                    .set(comment)
            commentAlarm(destinationUid!!, findViewById<EditText>(R.id.comment_edit_message).text.toString())
            findViewById<EditText>(R.id.comment_edit_message).setText("")
        }
        findViewById<RecyclerView>(R.id.comment_recyclerview).adapter = CommentRecyclerViewAdapter()
        findViewById<RecyclerView>(R.id.comment_recyclerview).layoutManager = LinearLayoutManager(this)
    }

    override fun onStop() {
        super.onStop()
        commentSnapshot?.remove()
    }
    fun commentAlarm(destinationUid:String, message: String) {
        val alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = user?.email
        alarmDTO.uid = user?.uid
        alarmDTO.kind = 1
        alarmDTO.message = message
        alarmDTO.timestamp = System.currentTimeMillis()

        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        var message = user?.email + getString(R.string.alarm_who) + message + getString(R.string.alarm_comment)
    }
    inner class CommentRecyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        val comments: ArrayList<ContentDTO.Comment>
        init {
            comments = ArrayList()
            commentSnapshot = FirebaseFirestore
                    .getInstance()
                    .collection("images")
                    .document(contentUid!!)
                    .collection("comments")
                    .addSnapshotListener { quertSnapshot, firebaseFirestoreException ->
                        comments.clear()
                        if(quertSnapshot == null) return@addSnapshotListener
                        for(snapshot in quertSnapshot?.documents!!) {
                            comments.add(snapshot.toObject(ContentDTO.Comment::class.java)!!)
                        }
                        notifyDataSetChanged()
                    }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
            return CustomViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var view = holder.itemView
            //profile image
            FirebaseFirestore.getInstance()
                    .collection("profileImages")
                    .document(comments[position].uid!!)
                    .addSnapshotListener { documentSnapshot, firebasefirestoreEception ->
                        if(documentSnapshot?.data != null) {
                            val url = documentSnapshot?.data!!["image"]
                            Glide.with(holder.itemView.context)
                                    .load(url)
                                    .apply(RequestOptions().circleCrop())
                                    .into(view.findViewById(R.id.commentviewitem_imageview_profile))
                        }
                    }
            view.findViewById<TextView>(R.id.commentviewitem_textview_profile).text = comments[position].userId
            view.findViewById<TextView>(R.id.commentviewitem_textview_comment).text = comments[position].comment
        }

        override fun getItemCount(): Int {
            return comments.size
        }
        inner class CustomViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)
    }
}