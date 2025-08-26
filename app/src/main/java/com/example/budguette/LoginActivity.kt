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
import com.google.firebase.firestore.SetOptions

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager
    private val TAG = "LoginActivity"

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

        // Facebook setup
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

        // Email login
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
            LoginManager.getInstance().logInWithReadPermissions(
                this, listOf("email", "public_profile", "user_birthday")
            )
        }

        // Sign-up link
        binding.signUpLink.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)

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
                    saveUserToFirestore()
                } else {
                    Log.e(TAG, "Google auth failed", task.exception)
                    Toast.makeText(this, "Google authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    saveUserToFirestore()
                } else {
                    Log.e(TAG, "Facebook auth failed", task.exception)
                    Toast.makeText(this, "Facebook authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * Creates Firestore user if missing.
     * Only merges standard auth fields (name/email/profileImage).
     * Does NOT overwrite custom fields like DOB.
     */
    private fun saveUserToFirestore() {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val userRef = FirebaseFirestore.getInstance().collection("users").document(uid)

        userRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                // First-time signup → create doc with placeholder DOB
                val newUser = hashMapOf(
                    "uid" to uid,
                    "name" to (user.displayName ?: ""),
                    "email" to (user.email ?: ""),
                    "profileImage" to (user.photoUrl?.toString() ?: ""),
                    "dob" to null
                )
                userRef.set(newUser)
                    .addOnSuccessListener { Log.d(TAG, "User created in Firestore") }
                    .addOnFailureListener { e -> Log.e(TAG, "Error creating user", e) }
            } else {
                // Existing user → merge only safe fields
                val updateData = hashMapOf<String, Any>(
                    "name" to (user.displayName ?: ""),
                    "email" to (user.email ?: ""),
                    "profileImage" to (user.photoUrl?.toString() ?: "")
                )
                userRef.set(updateData, SetOptions.merge())
                    .addOnSuccessListener { Log.d(TAG, "User updated in Firestore (no DOB overwrite)") }
                    .addOnFailureListener { e -> Log.e(TAG, "Error updating user", e) }
            }

            // Continue into app
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}



