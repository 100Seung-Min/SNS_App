package com.example.sns

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity : AppCompatActivity() {
    val PICK_IMAGE_FROM_ALBUM = 0
    var photoUri: Uri? = null
    var storage: FirebaseStorage? = null
    var firebasestore: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)
        //firebasestorage
        storage = FirebaseStorage.getInstance()
        //firebasedatabase
        firebasestore = FirebaseFirestore.getInstance()
        //firebaseauth
        auth = FirebaseAuth.getInstance()

        val photoPickerItent = Intent(Intent.ACTION_PICK)
        photoPickerItent.type = "image/*"
        startActivityForResult(photoPickerItent, PICK_IMAGE_FROM_ALBUM)
        findViewById<ImageView>(R.id.addphoto_image).setOnClickListener {
            val photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)
        }
        findViewById<Button>(R.id.addphoto_btn_upload).setOnClickListener { contentUpload() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PICK_IMAGE_FROM_ALBUM) {
            //이미지 선택시
            if (resultCode == Activity.RESULT_OK) {
                //이미지뷰에 이미지 세팅
                photoUri = data?.data
                findViewById<ImageView>(R.id.addphoto_image).setImageURI(data?.data)
            }
            //선택 안할시 액티비티 종료
            else {
                finish()
            }

        }
    }
    fun contentUpload() {
        findViewById<ProgressBar>(R.id.progress_bar).visibility = View.VISIBLE
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_.png"
        val storageref = storage?.reference?.child("images")?.child(imageFileName)
        storageref?.putFile(photoUri!!)?.addOnSuccessListener { taskSnapshot ->
            findViewById<ProgressBar>(R.id.progress_bar).visibility = View.GONE
            Toast.makeText(this, getString(R.string.upload_success), Toast.LENGTH_SHORT).show()
            //디비에 바인딩 할 위치 생성 및 컬렉션(테이블)에 데이터 집합 생성
            storageref?.downloadUrl?.addOnSuccessListener { uri ->
                //시간 생성
                val contentDTO = ContentDTO()
                //이미지주소
                contentDTO.imageUrl = uri!!.toString()
                println(contentDTO.imageUrl + "두 번쨰 1")
                //유저의 UID
                contentDTO.uid = auth?.currentUser?.uid
                //게시물의 설명
                contentDTO.explain = findViewById<TextView>(R.id.addphoto_edit_explain).text.toString()
                //유저의 아이디
                contentDTO.userId = auth?.currentUser?.email
                //게시물 업로드 시간
                contentDTO.timestamp = System.currentTimeMillis()
                //게시물에 데이터 생성및 액티비티 종료
                firebasestore?.collection("images")?.document()?.set(contentDTO)
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
                ?.addOnFailureListener {
                    findViewById<ProgressBar>(R.id.progress_bar).visibility = View.GONE
                    Toast.makeText(this, getString(R.string.upload_fail), Toast.LENGTH_SHORT).show()
                }
    }
}