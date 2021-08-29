package com.example.sns

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.sns.model.FollowDTO
import com.example.sns.modelpackage.AlarmDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import org.w3c.dom.Text

class DetailviewFragment : Fragment() {
    var user: FirebaseUser? = null
    var firestore: FirebaseFirestore? = null
    var imageSnapshot: ListenerRegistration? = null
    var mainView: View? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle? ): View? {
        user = FirebaseAuth.getInstance().currentUser
        firestore = FirebaseFirestore.getInstance()

        //RecyclerVuew 와 어댑터 연결
        mainView = inflater.inflate(R.layout.fragment_detailview, container, false)
        return mainView
    }

    override fun onResume() {
        super.onResume()
        mainView?.findViewById<RecyclerView>(R.id.detailviewfragment_recyclerview)?.layoutManager =
                LinearLayoutManager(activity)
        mainView?.findViewById<RecyclerView>(R.id.detailviewfragment_recyclerview)?.adapter =
                DetailRecyclerViewAdapter()
        var mainActivity = activity as MainActivity
        mainActivity.findViewById<ProgressBar>(R.id.progress_bar).visibility = View.INVISIBLE
    }

    override fun onStop() {
        super.onStop()
        imageSnapshot?.remove()
    }
    inner class DetailRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        val contentDTOs: ArrayList<ContentDTO>
        val contentUidList: ArrayList<String>

        init {
            contentDTOs = ArrayList()
            contentUidList = ArrayList()
            var uid = FirebaseAuth.getInstance().currentUser?.uid
            firestore?.collection("users")?.document(uid!!)?.get()?.addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    var userDTO = task.result.toObject(FollowDTO::class.java)
                    if(userDTO?.followings != null) {
                        getContents(userDTO?.followings)
                    }
                }
            }
        }
        fun getContents(followers: MutableMap<String, Boolean>) {
            imageSnapshot = firestore?.collection("images")?.orderBy("timestamp")?.
                    addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                        contentDTOs.clear()
                        contentUidList.clear()
                        if(querySnapshot == null) return@addSnapshotListener
                        for (snapshot in querySnapshot!!.documents) {
                            var item = snapshot.toObject(ContentDTO::class.java)!!
                            println(item.uid)
                            if(followers?.keys?.contains(item.uid)!!) {
                                contentDTOs.add(item)
                                contentUidList.add(snapshot.id)
                            }
                        }
                        notifyDataSetChanged()
                    }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail, parent, false)
            return CustomViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val viewHolder = (holder as CustomViewHolder).itemView
            //profile image 가져오기
            firestore?.collection("profileImages")?.document(contentDTOs[position].uid!!)
                    ?.get()?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {

                            val url = task.result["image"]
                            Glide.with(holder.itemView.context)
                                    .load(url)
                                    .apply(RequestOptions().circleCrop()).into(viewHolder.findViewById(R.id.detailviewitem_profile_image))

                        }
                    }
            //userfragment로 이동
            viewHolder.findViewById<RecyclerView>(R.id.detailviewitem_profile_image).setOnClickListener {
                val fragment = UserFragment()
                val bundle = Bundle()
                bundle.putString("destinationUID", contentDTOs[position].uid)
                bundle.putString("userId", contentDTOs[position].userId)
                fragment.arguments = bundle
                activity!!.supportFragmentManager.beginTransaction()
                        .replace(R.id.main_cotent, fragment)
                        .commit()
            }
            //유저 아이디
            viewHolder.findViewById<TextView>(R.id.detailviewitem_profile_textview).text = contentDTOs[position].userId
            //가운데 이미지
            Glide.with(holder.itemView.context)
                    .load(contentDTOs[position].imageUrl)
                    .into(viewHolder.findViewById(R.id.detailviewitem_imageview_content))
            //설명 텍스트
            viewHolder.findViewById<TextView>(R.id.detailviewitem_explain_textview).text = contentDTOs[position].explain
            //좋아요 이벤트
            viewHolder.findViewById<ImageView>(R.id.detailviewitem_favorite_imageview).setOnClickListener {
                favoriteEvent(position)
            }
            //좋아요 버튼 설정
            if(contentDTOs[position].favorites.containsKey(FirebaseAuth.getInstance().currentUser!!.uid)) {
                viewHolder.findViewById<ImageView>(R.id.detailviewitem_favorite_imageview).setImageResource(R.drawable.ic_favorite)
            } else {
                viewHolder.findViewById<ImageView>(R.id.detailviewitem_favorite_imageview).setImageResource(R.drawable.ic_favorite_border)
            }
            //좋아요 카운터 설정
            viewHolder.findViewById<TextView>(R.id.detailviewitem_favoritecounter_textview).text = "좋아요 " + contentDTOs[position].favoriteCount + "개"
            viewHolder.findViewById<ImageView>(R.id.detailviewitem_comment_imageview).setOnClickListener {
                val intent = Intent(activity, CommentActivity::class.java)
                intent.putExtra("contentUid", contentUidList[position])
                intent.putExtra("destinationUid", contentDTOs[position].uid)
                startActivity(intent)
            }
        }

        fun favoriteAlarm(destinationUid: String) {

            val alarmDTO = AlarmDTO()
            alarmDTO.destinationUid = destinationUid
            alarmDTO.userId = user?.email
            alarmDTO.uid = user?.uid
            alarmDTO.kind = 0
            alarmDTO.timestamp = System.currentTimeMillis()

            FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
            var message = user?.email + getString(R.string.alarm_favorite)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        //좋아요 이벤트 기능
        private fun favoriteEvent(position: Int) {
            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])
            firestore?.runTransaction { transaction ->
                val uid = FirebaseAuth.getInstance().currentUser!!.uid
                val contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                if (contentDTO!!.favorites.containsKey(uid)) {
                    // Unstar the post and remove self from stars
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount!! - 1
                    contentDTO?.favorites.remove(uid)

                } else {
                    // Star the post and add self to stars
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount!! + 1
                    contentDTO?.favorites[uid] = true
                    favoriteAlarm(contentDTOs[position].uid!!)
                }
                transaction.set(tsDoc, contentDTO)
            }
        }
    }

    inner class CustomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}