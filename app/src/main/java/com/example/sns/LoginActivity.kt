package com.example.sns

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.*
import com.twitter.sdk.android.core.*
import com.twitter.sdk.android.core.identity.TwitterAuthClient
import java.util.*

class LoginActivity : AppCompatActivity() {

    // firebase authentication 관리 클래스
    var auth: FirebaseAuth? = null

    // googlelogin 관리 클래스
    var googleSignInClient: GoogleSignInClient? = null

    //facebook 로그인 처리 결과 관리 클래스
    var callbackManager: CallbackManager? = null

    //googlelogin
    var GOOGLE_LOGIN_CODE = 9001 // intent request ID

    //twitterlogin
    var twitterAuthClient: TwitterAuthClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Twitter.initialize(this)
        setContentView(R.layout.activity_login)

        //firebase 로그인 통합 관리하는 Object만들기
        auth = FirebaseAuth.getInstance()

        //구글 로그인 옵션
        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        //구글 로그인 클래스를 만듬
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        callbackManager = CallbackManager.Factory.create()

        //트위터 세팅
        twitterAuthClient = TwitterAuthClient()

        //구글 로그인 버튼 생성
        findViewById<Button>(R.id.google_signin_button).setOnClickListener {googleLogin()}

        //페이스북 로그인 버튼 생성
        findViewById<Button>(R.id.facebook_login_button).setOnClickListener {facebookLogin()}

        //이메일 로그인 버튼 생성
        findViewById<Button>(R.id.email_login_button).setOnClickListener {emailLogin()}

        //트위터 로그인 버튼 생성
        findViewById<Button>(R.id.twitter_login_button).setOnClickListener {twitterLogin()}
    }

    fun moveMainPage(user : FirebaseUser?){
        //User is signed in
        if (user != null) {
            Toast.makeText(this, getString(R.string.signin_complete),Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    fun googleLogin() {
        findViewById<ProgressBar>(R.id.progress_bar).visibility = View.VISIBLE
        var signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }

    fun facebookLogin() {
        findViewById<ProgressBar>(R.id.progress_bar).visibility = View.VISIBLE

        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "email"))
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                handleFacebookAccessToken(loginResult.accessToken)
            }

            override fun onCancel() {
                findViewById<ProgressBar>(R.id.progress_bar).visibility = View.GONE
            }

            override fun onError(error: FacebookException) {
                findViewById<ProgressBar>(R.id.progress_bar).visibility = View.GONE
            }
        })
    }

    fun twitterLogin() {
        findViewById<ProgressBar>(R.id.progress_bar).visibility = View.VISIBLE
        twitterAuthClient?.authorize(this, object : Callback<TwitterSession>() {
            override fun success(result: Result<TwitterSession>?) {
                val credential = TwitterAuthProvider.getCredential(
                    result?.data?.authToken?.token!!,
                    result?.data?.authToken?.secret!!)
                auth?.signInWithCredential(credential)?.addOnCompleteListener { task ->
                    findViewById<ProgressBar>(R.id.progress_bar).visibility = View.GONE
                    //다음페이지 이동
                    if (task.isSuccessful) {
                        moveMainPage(auth?.currentUser)
                    }
                }
            }

            override fun failure(exception: TwitterException?) {

            }
        })
    }

    //facebook 토큰을 firebase로 넘겨주는 코드
    fun handleFacebookAccessToken(token : AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener { task ->
                findViewById<ProgressBar>(R.id.progress_bar).visibility = View.GONE
                //다음 페이지 이동
                if (task.isSuccessful) {
                    moveMainPage(auth?.currentUser)
                }
            }
    }

    //이메일 회원가입 및 로그인 메소드
    fun createAndLoginEmail() {
        auth?.createUserWithEmailAndPassword(findViewById<EditText>(R.id.email_edittext).text.toString(),
            findViewById<EditText>(R.id.password_edittext).text.toString())
            ?.addOnCompleteListener { task ->
                findViewById<ProgressBar>(R.id.progress_bar).visibility = View.GONE
                if (task.isSuccessful) {
                    //아이디 생성이 성공했을 경우
                    Toast.makeText(this, getString(R.string.signup_complete), Toast.LENGTH_SHORT).show()
                    //다음 페이지 호출
                    moveMainPage(auth?.currentUser)
                } else if (task.exception?.message.isNullOrEmpty()) {
                    //회원가입 에러가 발생했을 경우
                    Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
                } else {
                    signinEmail()
                }
            }
    }

    fun emailLogin() {
        if (findViewById<EditText>(R.id.email_edittext).text.toString().isNullOrEmpty() || findViewById<EditText>(R.id.password_edittext).text.toString().isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.signout_fail_null), Toast.LENGTH_SHORT).show()
        } else {
            findViewById<ProgressBar>(R.id.progress_bar).visibility = View.VISIBLE
            createAndLoginEmail()
        }
    }

    //로그인 메소드
    fun signinEmail() {
        auth?.signInWithEmailAndPassword(findViewById<EditText>(R.id.email_edittext).text.toString(), findViewById<EditText>(R.id.password_edittext).text.toString())
            ?.addOnCompleteListener { task ->
                findViewById<ProgressBar>(R.id.progress_bar).visibility = View.GONE
                if (task.isSuccessful) {
                    //로그인 성공 및 다음 페이지 호출
                    moveMainPage(auth?.currentUser)
                } else {
                    //로그인 실패
                    Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //facebook sdk로 값 넘겨주기
        callbackManager?.onActivityResult(requestCode, resultCode, data)

        //twitter sdk로 값 넘겨주기
        twitterAuthClient?.onActivityResult(requestCode, resultCode, data)

        //구글에서 승인된 정보를 가지고 오기
        if (requestCode == GOOGLE_LOGIN_CODE && resultCode == Activity.RESULT_OK) {
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)

            if (result.isSuccess) {
                var account = result.signInAccount
                firebaseAuthWithGoogle(account!!)
            } else {
                findViewById<ProgressBar>(R.id.progress_bar).visibility = View.GONE
            }
        }
    }

    fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener { task ->
                findViewById<ProgressBar>(R.id.progress_bar).visibility = View.GONE
                if (task.isSuccessful) {
                    //다음페이지 호출
                    moveMainPage(auth?.currentUser)
                }
            }
    }

    override fun onStart() {
        super.onStart()

        //자동 로그인 설정
        moveMainPage(auth?.currentUser)
    }
}