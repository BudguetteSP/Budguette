package com.example.budguette

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class ProfileFragment : Fragment() {

    private lateinit var profileImage: ImageView
    private lateinit var nameText: TextView
    private lateinit var emailText: TextView
    private lateinit var dobText: TextView
    private lateinit var bioText: TextView
    private lateinit var changePictureBtn: Button
    private lateinit var logoutBtn: Button
    private lateinit var editProfileBtn: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val REQUEST_CODE = 1001

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadImageToFirebaseStorage(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        profileImage = view.findViewById(R.id.profile_image)
        nameText = view.findViewById(R.id.profile_name)
        emailText = view.findViewById(R.id.profile_email)
        dobText = view.findViewById(R.id.profile_dob)
        bioText = view.findViewById(R.id.profile_bio)
        changePictureBtn = view.findViewById(R.id.change_picture_btn)
        logoutBtn = view.findViewById(R.id.logout_button)
        editProfileBtn = view.findViewById(R.id.edit_profile_btn)

        checkAndRequestPermissions()
        loadUserInfo()

        changePictureBtn.setOnClickListener { pickImage.launch("image/*") }
        logoutBtn.setOnClickListener { showLogoutConfirmation() }
        editProfileBtn.setOnClickListener { showEditProfileDialog() }

        return view
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE)
        }
    }

    private fun loadUserInfo() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    nameText.text = document.getString("name") ?: "N/A"
                    emailText.text = document.getString("email") ?: auth.currentUser?.email
                    dobText.text = document.getString("dob") ?: "N/A"
                    bioText.text = document.getString("bio") ?: ""

                    loadProfileImage(document.getString("profileImageUrl"))
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load user info", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadProfileImage(url: String?) {
        Glide.with(this)
            .load(url)
            .placeholder(R.drawable.ic_defaultprofile_background)
            .into(profileImage)
    }

    private fun uploadImageToFirebaseStorage(imageUri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        val storageRef = storage.reference.child("profile_pictures/${userId}.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    saveImageUrlToFirestore(uri.toString())
                }
            }
            .addOnFailureListener {
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

    private fun showEditProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        val nameField = dialogView.findViewById<EditText>(R.id.edit_name)
        val dobField = dialogView.findViewById<EditText>(R.id.edit_dob)
        val bioField = dialogView.findViewById<EditText>(R.id.edit_bio)

        nameField.setText(nameText.text.toString())
        dobField.setText(dobText.text.toString())
        bioField.setText(bioText.text.toString())

        dobField.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(requireContext(),
                { _, year, month, day -> dobField.setText("${month + 1}/$day/$year") },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        dialog.setButton(android.app.AlertDialog.BUTTON_POSITIVE, "Save") { _, _ -> }

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val newName = nameField.text.toString().trim()
                val newDob = dobField.text.toString().trim()
                val newBio = bioField.text.toString().trim()

                if (newName.isEmpty() || newDob.isEmpty()) {
                    Toast.makeText(context, "Name and DOB are required", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                updateProfile(newName, newDob, newBio)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun updateProfile(name: String, dob: String, bio: String) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .update(mapOf(
                "name" to name,
                "dob" to dob,
                "bio" to bio
            ))
            .addOnSuccessListener {
                Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                loadUserInfo()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLogoutConfirmation() {
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Confirm Logout")
        builder.setMessage("Are you sure you want to log out?")
        builder.setPositiveButton("Yes") { _, _ ->
            auth.signOut()
            Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            activity?.finish()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }
}





