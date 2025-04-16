package com.example.budguette

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.Manifest
import android.content.pm.PackageManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

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

    private val REQUEST_CODE = 1001 // Your request code for permission

    // Image picker
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            uploadImageToFirebaseStorage(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // UI Elements
        profileImage = view.findViewById(R.id.profile_image)
        nameText = view.findViewById(R.id.profile_name)
        emailText = view.findViewById(R.id.profile_email)
        dobText = view.findViewById(R.id.profile_dob)
        bioEditText = view.findViewById(R.id.profile_bio)
        saveBioBtn = view.findViewById(R.id.save_bio_btn)
        changePictureBtn = view.findViewById(R.id.change_picture_btn)
        logoutBtn = view.findViewById(R.id.logout_button)

        // Check and request permissions for external storage
        checkAndRequestPermissions()

        // Load user info
        loadUserInfo()

        saveBioBtn.setOnClickListener {
            saveBio()
        }

        changePictureBtn.setOnClickListener {
            pickImage.launch("image/*")
        }

        logoutBtn.setOnClickListener {
            auth.signOut()
            Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            activity?.finish()
        }

        return view
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE)
        }
    }

    private fun loadUserInfo() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)

        userRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                nameText.text = document.getString("name") ?: "N/A"
                emailText.text = document.getString("email") ?: auth.currentUser?.email
                dobText.text = document.getString("dob") ?: "N/A"
                bioEditText.setText(document.getString("bio") ?: "")

                val profileUrl = document.getString("profileImageUrl")
                loadProfileImage(profileUrl)
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to load user info", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "Failed to update bio", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadProfileImage(url: String?) {
        Glide.with(this)
            .load(url)
            .placeholder(R.drawable.ic_defaultprofile_background) // Optional placeholder
            .into(profileImage)
    }

    private fun uploadImageToFirebaseStorage(imageUri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        val storageRef = FirebaseStorage.getInstance().reference
            .child("profile_pictures/${userId}.jpg") // Make sure it's using UID


        Log.d("ProfileFragment", "Uploading image: $imageUri")
        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    saveImageUrlToFirestore(uri.toString())
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ProfileFragment", "Image upload failed", exception)
                Toast.makeText(context, "Image upload failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveImageUrlToFirestore(downloadUrl: String) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .update("profileImageUrl", downloadUrl)
            .addOnSuccessListener {
                Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
                loadProfileImage(downloadUrl)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to save image URL", Toast.LENGTH_SHORT).show()
            }
    }
}


