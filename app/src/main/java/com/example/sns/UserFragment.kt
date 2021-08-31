package com.example.sns

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class UserFragment : Fragment() {

    val PiCK_PROFILE_FROM_ALBUM = 10

    //firebase
    var auth: FirebaseAuth? = null
    var firestore: FirebaseFirestore? = null
    //private string destinationUid
    var uid: String? = null
    var currentUserUid: String? = null
    var fragmentView: View? = null
    var followListenerRegistration: ListenerRegistration? = null
    var followingListenerRegistration: ListenerRegistration? = null
    var imageprofileListenerRegistration:ListenerRegistration? = null
    var recyclerListenerRegistration: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView = inflater.inflate(R.layout.fragment_user, container, false)
        //firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        currentUserUid = auth?.currentUser?.uid
        if (arguments != null) {
            uid = arguments!!.getString("destinationUid")
            //본인 계정인 경우 로그아웃, Toolbar 기본으로 설정
            if(uid != null && uid == currentUserUid) {
                fragmentView!!.findViewById<Button>(R.id.account_btn_follow_signout).text = getString(R.string.signout)
                fragmentView?.findViewById<Button>(R.id.account_btn_follow_signout)?.setOnClickListener {
                    activity?.finish()
                    startActivity(Intent(activity, LoginActivity::class.java))
                    auth?.signOut()
                }
            }else {
                fragmentView!!.findViewById<Button>(R.id.account_btn_follow_signout).text = getString(R.string.follow)
                //View.account_btn_follow_signout.setOnClickListenet{ requestFollow() }
                var mainActivity = (activity as MainActivity)
                mainActivity.findViewById<ImageView>(R.id.toolbar_title_image).visibility = View.GONE
                mainActivity.findViewById<ImageView>(R.id.toolbar_btn_back).visibility = View.VISIBLE
                mainActivity.findViewById<TextView>(R.id.toolbar_username).visibility = View.VISIBLE
                mainActivity.findViewById<TextView>(R.id.toolbar_username).text = arguments!!.getString("userId")
                mainActivity.findViewById<ImageView>(R.id.toolbar_btn_back).setOnClickListener {
                    mainActivity.findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.action_home
                }
                fragmentView?.findViewById<Button>(R.id.account_btn_follow_signout)?.setOnClickListener {
                    requestFollow()
                }
            }
        }
        //profile image click listener
        fragmentView?.findViewById<ImageView>(R.id.account_iv_profile)?.setOnClickListener {
            if(ContextCompat.checkSelfPermission(activity!!, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                //앨범오픈
                var photoPickerIntent = Intent(Intent.ACTION_PICK)
                photoPickerIntent.type = "image/*"
                activity!!.startActivityForResult(photoPickerIntent, PiCK_PROFILE_FROM_ALBUM)
           }
        }
        getFollowing()
        getFollower()
        fragmentView?.findViewById<RecyclerView>(R.id.account_recyclerview)?.layoutManager = GridLayoutManager(activity!!, 3)
        fragmentView?.findViewById<RecyclerView>(R.id.account_recyclerview)?.adapter = UserFragmentRecyclerViewAdapter()
        return fragmentView
    }

    override fun onResume() {
        super.onResume()
        getProfileImage()
    }
    fun getProfileImage() {
        imageprofileListenerRegistration = firestore?.collection("profileImages")?.document(uid!!)
            ?.addSnapshotListener { documentsnapshot, firebasefirestoreException ->
                if(documentsnapshot?.data != null) {
                    val url = documentsnapshot?.data!!["image"]
                    Glide.with(activity!!)
                        .load(url)
                        .apply(RequestOptions().circleCrop())
                        .into(fragmentView!!.findViewById(R.id.account_iv_profile))
                }
            }
    }
    fun getFollowing() {
        followingListenerRegistration = firestore?.collection("users")?.document(uid!!)
            ?.addSnapshotListener { documentsnapshot, firebasefirestoreException ->
                val followDTO = documentsnapshot?.toObject(FollowDTO::class.java)
                if (followDTO == null) return@addSnapshotListener
                fragmentView!!.findViewById<TextView>(R.id.account_tv_following_count).text = followDTO?.followingCount.toString()
            }
    }
    fun getFollower() {
        followListenerRegistration = firestore?.collection("users")?.document(uid!!)
            ?.addSnapshotListener { documentsnapshot, firebasefirestoreException ->
                val followDTO = documentsnapshot?.toObject(FollowDTO::class.java)
                if (followDTO == null) return@addSnapshotListener
                fragmentView?.findViewById<TextView>(R.id.account_tv_follower_count)?.text = followDTO?.followerCount.toString()
                if (followDTO?.followers?.containsKey(currentUserUid)!!) {
                    fragmentView?.findViewById<Button>(R.id.account_btn_follow_signout)?.text = getString(R.string.follow_cancel)
                    fragmentView?.findViewById<Button>(R.id.account_btn_follow_signout)
                        ?.background
                        ?.setColorFilter(ContextCompat.getColor(activity!!, R.color.colorLightGray), PorterDuff.Mode.MULTIPLY)
                } else {
                    if (uid != currentUserUid) {
                        fragmentView?.findViewById<Button>(R.id.account_btn_follow_signout)?.text = getString(R.string.follow)
                        fragmentView?.findViewById<Button>(R.id.account_btn_follow_signout)?.background?.colorFilter = null
                    }
                }
            }
    }
    fun requestFollow() {
        var tsDocFollowing = firestore!!.collection("users").document(currentUserUid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing).toObject(FollowDTO::class.java)
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO.followingCount = 1
                followDTO.followings[uid!!] = true
                transaction.set(tsDocFollowing, followDTO)
                return@runTransaction
            }
            //unstar the post and remove self from stars
            if (followDTO?.followings?.containsKey(uid)!!) {
                followDTO?.followingCount = followDTO?.followingCount - 1
                followDTO?.followings.remove(uid)
            } else {
                followDTO?.followingCount = followDTO?.followingCount + 1
                followDTO?.followings[uid!!] = true
            }
            transaction.set(tsDocFollowing, followDTO)
            return@runTransaction
        }
        var tsDocFollower = firestore!!.collection("users").document(uid!!)
        firestore?.runTransaction { transation ->
            var followDTO = transation.get(tsDocFollower).toObject(FollowDTO::class.java)
            if(followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid!!] = true
                transation.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }
            if (followDTO?.followers?.containsKey(currentUserUid!!)!!) {
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid!!)
            } else {
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserUid!!] = true
            } //star the post and add self to stars
            transation.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }
    }
    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        val contentDTOs: ArrayList<ContentDTO>
        init {
            contentDTOs = ArrayList()
            //나의 사진만 찾기
            recyclerListenerRegistration = firestore?.collection("images")?.whereEqualTo("uid", uid)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                contentDTOs.clear()
                if(querySnapshot == null) return@addSnapshotListener
                for (snapshot in querySnapshot?.documents!!) {
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }
                fragmentView?.findViewById<TextView>(R.id.account_tv_post_count)?.text = contentDTOs.size.toString()
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val width = resources.displayMetrics.widthPixels / 3
            val imageView = ImageView(parent.context)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width)
            return CustomViewHolder(imageView)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            println(contentDTOs[position].imageUrl)
            var imageView = (holder as CustomViewHolder).imageView
            Glide.with(holder.itemView.context)
                .load(contentDTOs[position].imageUrl)
                .apply(RequestOptions().centerCrop())
                .into(imageView)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }
        inner class CustomViewHolder(var imageView: ImageView): RecyclerView.ViewHolder(imageView)
    }

    override fun onStop() {
        super.onStop()
        followListenerRegistration?.remove()
        followingListenerRegistration?.remove()
        imageprofileListenerRegistration?.remove()
        recyclerListenerRegistration?.remove()
    }
}