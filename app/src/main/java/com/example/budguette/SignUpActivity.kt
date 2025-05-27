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

        auth = FirebaseAuth.getInstance()

        // Google Sign-In setup
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Facebook setup
        callbackManager = CallbackManager.Factory.create()
        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    handleFacebookAccessToken(result.accessToken)
                }

                override fun onCancel() {
                    Toast.makeText(this@SignUpActivity, "Facebook sign in canceled.", Toast.LENGTH_SHORT).show()
                }

                override fun onError(error: FacebookException) {
                    Toast.makeText(this@SignUpActivity, "Facebook sign in failed: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })

        // UI setup
        val dobEditText: EditText = findViewById(R.id.dob_edit_text)
        val signUpButton: Button = findViewById(R.id.sign_up_button)
        val googleLoginButton: Button = findViewById(R.id.google_login_button)
        val facebookLoginButton: Button = findViewById(R.id.facebook_login_button)

        dobEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                this,
                { _, y, m, d -> dobEditText.setText("${m + 1}/$d/$y") },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

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

        googleLoginButton.setOnClickListener {
            startActivityForResult(googleSignInClient.signInIntent, 9001)
        }

        facebookLoginButton.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(
                this,
                listOf("email", "public_profile", "user_birthday")
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 9001) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    val uid = firebaseUser?.uid

                    // Fetch additional profile data using GraphRequest
                    val request = GraphRequest.newMeRequest(token) { obj, _ ->
                        try {
                            val name = obj?.getString("name")
                            val fbEmail = obj?.optString("email") // May be null if not granted
                            val birthday = obj?.optString("birthday")
                            val profileUrl = "https://graph.facebook.com/${token.userId}/picture?type=large"

                            // Fallback to Firebase's email if Facebook email is null
                            val email = fbEmail ?: firebaseUser?.email

                            Log.d("FacebookData", "name=$name, email=$email, birthday=$birthday")

                            if (uid != null) {
                                saveUserToFirestore(name, email, uid, profileUrl, birthday)
                                startActivity(Intent(this@SignUpActivity, MainActivity::class.java))
                                finish()
                            }

                        } catch (e: JSONException) {
                            Log.e(TAG, "JSON parsing error", e)
                        }
                    }

                    val parameters = Bundle()
                    parameters.putString("fields", "id,name,email,birthday")
                    request.parameters = parameters
                    request.executeAsync()

                } else {
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
                    Toast.makeText(this, "Google authentication failed", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveUserToFirestore(name: String?, email: String?, uid: String?, profileUrl: String?, dob: String?) {
        if (uid == null) return
        val db = FirebaseFirestore.getInstance()
        val userMap = hashMapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "profileUrl" to profileUrl,
            "dob" to dob
        )
        db.collection("users").document(uid).set(userMap)
            .addOnSuccessListener {
                Log.d(TAG, "User added to Firestore")
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to save user", it)
            }
    }
}




