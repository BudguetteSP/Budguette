package com.example.budguette

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONException
import java.util.*

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager
    private val TAG = "SignUpActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Set up Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Set up Facebook Callback Manager
        callbackManager = CallbackManager.Factory.create()

        // Initialize UI Elements
        val dobEditText: EditText = findViewById(R.id.dob_edit_text)
        val signUpButton: Button = findViewById(R.id.sign_up_button)
        val googleLoginButton: Button = findViewById(R.id.google_login_button)
        val facebookLoginButton: Button = findViewById(R.id.facebook_login_button)

        // Date picker for DOB field
        dobEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val datePickerDialog = DatePickerDialog(this, { _, y, m, d ->
                dobEditText.setText("${m + 1}/$d/$y")
            }, year, month, day)
            datePickerDialog.show()
        }

        // Manual sign-up (not using social logins)
        signUpButton.setOnClickListener {
            val name = findViewById<EditText>(R.id.full_name_edit_text).text.toString()
            val email = findViewById<EditText>(R.id.email_edit_text).text.toString()
            val dob = dobEditText.text.toString()
            val password = findViewById<EditText>(R.id.password_edit_text).text.toString()
            val confirmPassword = findViewById<EditText>(R.id.confirm_password_edit_text).text.toString()

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid
                        saveUserToFirestore(name, email, uid, null, dob)
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Sign Up Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Google Sign-In
        googleLoginButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, 9001)
        }

        // Facebook Sign-In
        facebookLoginButton.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(
                this,
                listOf("email", "public_profile", "user_birthday")
            )
            LoginManager.getInstance().registerCallback(callbackManager,
                object : FacebookCallback<LoginResult> {
                    override fun onSuccess(result: LoginResult) {
                        handleFacebookAccessToken(result.accessToken)

                        val request = GraphRequest.newMeRequest(result.accessToken) { obj, _ ->
                            try {
                                obj?.let {
                                    val name = it.getString("name")
                                    val email = it.optString("email")
                                    val birthday = it.optString("birthday")
                                    val profileUrl = "https://graph.facebook.com/${result.accessToken.userId}/picture?type=large"
                                    val uid = auth.currentUser?.uid
                                    if (uid != null) {
                                        saveUserToFirestore(name, email, uid, profileUrl, birthday)
                                    }
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                        val parameters = Bundle()
                        parameters.putString("fields", "id,name,email,birthday")
                        request.parameters = parameters
                        request.executeAsync()
                    }

                    override fun onCancel() {
                        Toast.makeText(this@SignUpActivity, "Facebook sign in canceled.", Toast.LENGTH_SHORT).show()
                    }

                    override fun onError(error: FacebookException) {
                        Toast.makeText(this@SignUpActivity, "Facebook sign in failed: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                })
        }
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (!task.isSuccessful) {
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid
                    val name = user?.displayName
                    val email = user?.email
                    val profileUrl = user?.photoUrl?.toString()
                    val dob = findViewById<EditText>(R.id.dob_edit_text).text.toString()
                    saveUserToFirestore(name, email, uid, profileUrl, dob)
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Google sign-in failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveUserToFirestore(name: String?, email: String?, uid: String?, profileUrl: String?, dobFromProvider: String? = null) {
        val dob = dobFromProvider ?: findViewById<EditText>(R.id.dob_edit_text).text.toString()
        val db = FirebaseFirestore.getInstance()

        val userMap = hashMapOf(
            "name" to name,
            "email" to email,
            "dob" to dob,
            "profileUrl" to profileUrl
        )

        uid?.let {
            db.collection("users").document(it)
                .set(userMap)
                .addOnSuccessListener {
                    Log.d(TAG, "User added to Firestore")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error adding user to Firestore", e)
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Facebook
        callbackManager.onActivityResult(requestCode, resultCode, data)

        // Google
        if (requestCode == 9001) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account)
                }
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }
}


