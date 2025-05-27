package com.example.budguette

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.budguette.databinding.ActivityLoginBinding
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Google Sign-In configuration
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Facebook Sign-In setup
        callbackManager = CallbackManager.Factory.create()

        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    handleFacebookAccessToken(result.accessToken)
                }

                override fun onCancel() {
                    Toast.makeText(this@LoginActivity, "Facebook sign in canceled.", Toast.LENGTH_SHORT).show()
                }

                override fun onError(error: FacebookException) {
                    Toast.makeText(this@LoginActivity, "Facebook sign in failed: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })

        // Email/password login
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Login failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        binding.forgotPasswordButton.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        // Google login
        binding.googleSignInButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, 1001)
        }

        // Facebook login
        binding.facebookSignInButton.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile", "user_birthday"))
        }

        // Sign-up link
        binding.signUpLink.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Facebook callback
        callbackManager.onActivityResult(requestCode, resultCode, data)

        // Google Sign-In result
        if (requestCode == 1001) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid
                    val name = user?.displayName
                    val email = user?.email
                    val profileUrl = user?.photoUrl?.toString()

                    if (uid != null && email != null) {
                        val db = FirebaseFirestore.getInstance()
                        val userRef = db.collection("users").document(uid)

                        userRef.get().addOnSuccessListener { document ->
                            if (!document.exists()) {
                                val userMap = hashMapOf(
                                    "uid" to uid,
                                    "name" to name,
                                    "email" to email,
                                    "profileUrl" to profileUrl,
                                    "dob" to null
                                )
                                userRef.set(userMap)
                                    .addOnSuccessListener {
                                        Log.d("LOGIN", "New user added to Firestore")
                                    }
                                    .addOnFailureListener {
                                        Log.e("LOGIN", "Failed to save user", it)
                                    }
                            } else {
                                Log.d("LOGIN", "User already exists in Firestore")
                            }

                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                    }
                } else {
                    Log.e("GOOGLE_AUTH", "Failure", task.exception)
                    Toast.makeText(this, "Google authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid
                    val profileUrl = user?.photoUrl?.toString()

                    val request = GraphRequest.newMeRequest(token) { obj, _ ->
                        try {
                            val email = obj?.getString("email")
                            val name = obj?.getString("name")
                            val birthday = obj?.optString("birthday")

                            if (uid != null && email != null) {
                                val db = FirebaseFirestore.getInstance()
                                val userRef = db.collection("users").document(uid)

                                userRef.get().addOnSuccessListener { document ->
                                    if (!document.exists()) {
                                        val userMap = hashMapOf(
                                            "uid" to uid,
                                            "name" to name,
                                            "email" to email,
                                            "profileUrl" to profileUrl,
                                            "dob" to birthday
                                        )
                                        userRef.set(userMap)
                                            .addOnSuccessListener {
                                                Log.d("FACEBOOK", "User added to Firestore")
                                            }
                                            .addOnFailureListener {
                                                Log.e("FACEBOOK", "Failed to save user", it)
                                            }
                                    }
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("FACEBOOK_GRAPH", "Error parsing Facebook data", e)
                        }
                    }

                    val parameters = Bundle()
                    parameters.putString("fields", "id,name,email,birthday")
                    request.parameters = parameters
                    request.executeAsync()
                } else {
                    Toast.makeText(this, "Facebook authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}



