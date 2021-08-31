package com.example.sns

import android.app.Activity
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Intent
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    val PICK_PROFILE_FROM_ALBUM = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<ProgressBar>(R.id.progress_bar).visibility = View.VISIBLE

        //Bottom NavigationView
        findViewById<BottomNavigationView>(R.id.bottom_navigation).setOnNavigationItemSelectedListener(this)
        findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.action_home

        //스토라지 접근 권한 요청
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)
    }
    fun setToolbarDefault() {
        findViewById<ImageView>(R.id.toolbar_title_image).visibility = View.VISIBLE
        findViewById<ImageView>(R.id.toolbar_btn_back).visibility = View.GONE
        findViewById<TextView>(R.id.toolbar_username).visibility = View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PICK_PROFILE_FROM_ALBUM && resultCode == Activity.RESULT_OK) {
            var imageUri = data?.data

            //유저 Uid
            val uid = FirebaseAuth.getInstance().currentUser!!.uid
            //파일 업로드
            FirebaseStorage
                .getInstance()
                .reference
                .child("userProfileImages")
                .child(uid)
                .putFile(imageUri!!)
                .addOnSuccessListener { task ->
                    FirebaseStorage
                        .getInstance()
                        .reference
                        .child("userProfileImages")
                        .child(uid)
                        .downloadUrl
                        .addOnSuccessListener { task ->
                            val url = task.toString()
                            val map = HashMap<String, Any>()
                            map["image"] = url
                            FirebaseFirestore.getInstance().collection("profileImages").document(uid).set(map)
                        }
                }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        setToolbarDefault()
        when (item.itemId) {
            R.id.action_home -> {
                val detailviewFragment = DetailviewFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_cotent, detailviewFragment)
                    .commit()
                return true
            }
            R.id.action_search -> {
                val grideFragment = GrideFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_cotent, grideFragment)
                    .commit()
                return true
            }
            R.id.action_add_photo -> {
                if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    startActivity(Intent(this, AddPhotoActivity::class.java))
                } else {
                    Toast.makeText(this, "스토리지 읽기 권한이 없습니다.", Toast.LENGTH_SHORT).show()
                }
                return  true
            }
            R.id.action_favorite_alarm -> {
                val alarmFragment = AlarmFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_cotent, alarmFragment)
                    .commit()
                return true
            }
            R.id.action_account -> {
                val userFragment = UserFragment()
                val uid = FirebaseAuth.getInstance().currentUser!!.uid
                val bundle = Bundle()
                bundle.putString("destinationUid", uid)
                userFragment.arguments = bundle
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_cotent, userFragment)
                    .commit()
                return true
            }
        }
        return false
    }
}