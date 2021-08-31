package com.example.sns

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class GrideFragment : Fragment() {

    var imageSnapshot:ListenerRegistration? = null
    var mainView: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        mainView = inflater.inflate(R.layout.fragment_gride, container, false)
        return mainView
    }

    override fun onResume() {
        super.onResume()
        mainView?.findViewById<RecyclerView>(R.id.gridfragment_recyclerview)?.adapter = GrideFragmentRecyclerViewAdatper()
        mainView?.findViewById<RecyclerView>(R.id.gridfragment_recyclerview)?.layoutManager = GridLayoutManager(activity, 3)
    }

    override fun onStop() {
        super.onStop()
        imageSnapshot?.remove()
    }
    inner class GrideFragmentRecyclerViewAdatper : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO>

        init {
            contentDTOs = ArrayList()
            imageSnapshot = FirebaseFirestore
                .getInstance().collection("images").orderBy("timestamp")
                ?.addSnapshotListener { querySnapshot, firebasefirestoreException ->
                    contentDTOs.clear()
                    for (snapshot in querySnapshot!!.documents) {
                        contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                    }
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            //현재 사이즈 뷰 화면 가로 크기의 1/3값을 가지고 오기
            val width = resources.displayMetrics.widthPixels / 3

            val imageView = ImageView(parent.context)
            imageView.layoutParams = LinearLayout.LayoutParams(width, width)
            return CustomViewHolder(imageView)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageView = (holder as CustomViewHolder).imageView
            Glide.with(holder.itemView.context)
                .load(contentDTOs[position].imageUrl)
                .apply(RequestOptions().centerCrop())
                .into(imageView)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        inner class CustomViewHolder(var imageView: ImageView) : RecyclerView.ViewHolder(imageView)
    }
}