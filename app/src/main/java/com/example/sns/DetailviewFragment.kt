package com.example.sns

import ContentDTO
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sns.model.FollowDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

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
                        
                    }
                }
            }
        }
    }
}