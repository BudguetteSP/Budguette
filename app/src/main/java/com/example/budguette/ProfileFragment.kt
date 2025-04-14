package com.example.budguette

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream
import android.util.Base64

class ProfileFragment : Fragment() {

    private lateinit var profileImage: ImageView
    private lateinit var nameText: TextView
    private lateinit var emailText: TextView
    private lateinit var dobText: TextView
    private lateinit var bioEditText: EditText
    private lateinit var saveBioBtn: Button
    private lateinit var changePictureBtn: Button
    private lateinit var logoutBtn: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Register image picker result
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val base64String = convertImageToBase64(it)
            if (base64String != null) {
                storeBase64ImageToFirestore(base64String)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        profileImage = view.findViewById(R.id.profile_image)
        nameText = view.findViewById(R.id.profile_name)
        emailText = view.findViewById(R.id.profile_email)
        dobText = view.findViewById(R.id.profile_dob)
        bioEditText = view.findViewById(R.id.profile_bio)
        saveBioBtn = view.findViewById(R.id.save_bio_btn)
        changePictureBtn = view.findViewById(R.id.change_picture_btn)
        logoutBtn = view.findViewById(R.id.logout_button)

        loadUserInfo()

        saveBioBtn.setOnClickListener {
            saveBio()
        }

        changePictureBtn.setOnClickListener {
            pickImage.launch("image/*")
        }

        logoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            activity?.finish()
        }

        return view
    }

    private fun loadUserInfo() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)

        userRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                nameText.text = document.getString("name") ?: "N/A"
                emailText.text = document.getString("email") ?: auth.currentUser?.email
                dobText.text = document.getString("dob") ?: "N/A"
                bioEditText.setText(document.getString("bio") ?: "")

                // Load Base64 image from Firestore
                loadBase64ImageFromFirestore()
            }
        }
    }

    private fun saveBio() {
        val userId = auth.currentUser?.uid ?: return
        val bio = bioEditText.text.toString()

        db.collection("users").document(userId)
            .update("bio", bio)
            .addOnSuccessListener {
                Toast.makeText(context, "Bio updated!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to update bio.", Toast.LENGTH_SHORT).show()
            }
    }

    // Convert image to Base64 string
    private fun convertImageToBase64(uri: Uri): String? {
        try {
            val inputStream: InputStream = requireContext().contentResolver.openInputStream(uri)!!
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    // Store Base64 string in Firestore
    private fun storeBase64ImageToFirestore(base64String: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId)
            .update("profileImageBase64", base64String)
            .addOnSuccessListener {
                Toast.makeText(context, "Image saved to Firestore!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error saving image", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
    }

    // Load Base64 image from Firestore and decode
    private fun loadBase64ImageFromFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val base64String = document.getString("profileImageBase64")
                if (!base64String.isNullOrEmpty()) {
                    val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                    val decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    profileImage.setImageBitmap(decodedBitmap)
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }
}

