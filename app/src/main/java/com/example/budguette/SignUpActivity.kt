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
import com.google.firebase.firestore.SetOptions
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

        // ✅ Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // ✅ Facebook setup
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

        // ✅ UI setup
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

        // ✅ Email Sign Up
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
                        val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                        val userData = hashMapOf(
                            "uid" to uid,
                            "name" to name,
                            "email" to email,
                            "dob" to dob
                        )
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .set(userData, SetOptions.merge())
                            .addOnSuccessListener {
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                    } else {
                        Toast.makeText(this, "Sign Up Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // ✅ Google Login
        googleLoginButton.setOnClickListener {
            startActivityForResult(googleSignInClient.signInIntent, 9001)
        }

        // ✅ Facebook Login
        facebookLoginButton.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
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

    // ✅ Facebook Login Handler
    private fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser ?: return@addOnCompleteListener
                    val userRef = FirebaseFirestore.getInstance().collection("users").document(user.uid)
                    val updateData = hashMapOf(
                        "name" to (user.displayName ?: ""),
                        "email" to (user.email ?: ""),
                        "profileImage" to (user.photoUrl?.toString() ?: "")
                    )
                    userRef.set(updateData, SetOptions.merge())
                        .addOnSuccessListener {
                            goToMainActivity()
                        }
                } else {
                    Toast.makeText(this, "Facebook authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // ✅ Google Login Handler
    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser ?: return@addOnCompleteListener
                    val userRef = FirebaseFirestore.getInstance().collection("users").document(user.uid)
                    val updateData = hashMapOf(
                        "name" to (user.displayName ?: ""),
                        "email" to (user.email ?: ""),
                        "profileImage" to (user.photoUrl?.toString() ?: "")
                    )
                    userRef.set(updateData, SetOptions.merge())
                        .addOnSuccessListener {
                            goToMainActivity()
                        }
                } else {
                    Toast.makeText(this, "Google authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun goToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
