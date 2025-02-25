package com.example.iblogg.data

import android.app.ProgressDialog
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation.NavController
import com.example.iblogg.model.Topics
import com.example.iblogg.navigation.ROUTE_HOME
import com.example.iblogg.navigation.ROUTE_LOGIN
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class TopicsViewModel(var navController: NavController,
                      var context: Context) {
    var authRepository: AuthViewModel

    init {
        authRepository=AuthViewModel(navController,context)
        if (!authRepository.isloggedin()){
            navController.navigate(ROUTE_LOGIN)
        }
    }

    fun saveTopics(
        filePath: Uri, firstname: String, lastname: String, gender: String, age:String,
        bio: String){
        var id = System.currentTimeMillis().toString()
        var storageReference = FirebaseStorage.getInstance().getReference().child("Passport/$id")

        storageReference.putFile(filePath).addOnCompleteListener{
            if (it.isSuccessful){
                storageReference.downloadUrl.addOnSuccessListener{
                    var imageUrl = it.toString()
                    var houseData = Topics(imageUrl,firstname,lastname,gender,age,bio,id)
                    var dbRef = FirebaseDatabase.getInstance().getReference().child("Topics/$id")
                    dbRef.setValue(houseData)
                    Toast.makeText(context,"Topic Added successfully",Toast.LENGTH_LONG).show()
                }
            }else{
                Toast.makeText(context,"${it.exception!!.message}",Toast.LENGTH_LONG).show()
            }
        }
    }

    fun viewTopics(client:MutableState<Topics>, Topics: SnapshotStateList<Topics>): SnapshotStateList<Topics> {
        var ref = FirebaseDatabase.getInstance().getReference().child("Topic")


        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                Topics.clear()
                for (snap in snapshot.children){
                    val value = snap.getValue(Topics::class.java)
                    Topics.value = value!!
                    Topics.add(value)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
            }
        })
        return Topics
    }
    fun updateTopics(
        context: Context,  // Pass context from the Composable or ViewModel
        navController: NavController,  // Pass navController from the Composable or ViewModel
        filePath: Uri,
        firstname: String,
        lastname: String,
        gender: String,
        age: String,
        bio: String,
        id: String,
        currentImageUrl: String // Pass the current image URL from the database
    ) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("Client/$id")

        if (filePath != Uri.EMPTY) {
            val storageReference = FirebaseStorage.getInstance().reference
            val imageRef = storageReference.child("Passport/${UUID.randomUUID()}.jpg")

            imageRef.putFile(filePath)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        val updatedClient = Topics(imageUrl, firstname, lastname, gender, age, bio, id)

                        databaseReference.setValue(updatedClient)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "Update successful", Toast.LENGTH_SHORT).show()
                                    navController.navigate(ROUTE_HOME)
                                } else {
                                    Toast.makeText(context, "Update failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "Image upload failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Keep the current image URL if no new image is selected
            val updatedTopics = Topics(currentImageUrl, firstname, lastname, gender, age, bio, id)
            databaseReference.setValue(updatedTopics)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Update successful", Toast.LENGTH_SHORT).show()
                        navController.navigate(ROUTE_HOME)
                    } else {
                        Toast.makeText(context, "Update failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }


    fun deleteClient(context: Context, id: String, navController: NavController) {
        // Initialize a ProgressDialog (if desired, or use another indicator)
        val progressDialog = ProgressDialog(context).apply {
            setMessage("Deleting Topic...")
            setCancelable(false)
            show()
        }

        // Reference to Firebase Realtime Database for the specific client
        val delRef = FirebaseDatabase.getInstance().getReference("Topic/$id")

        // Perform the delete operation
        delRef.removeValue().addOnCompleteListener { task ->
            // Dismiss the progress dialog
            progressDialog.dismiss()

            if (task.isSuccessful) {
                // If deletion was successful, show success message
                Toast.makeText(context, "Topic deleted successfully", Toast.LENGTH_SHORT).show()

                // Navigate to a different screen after deletion
                navController.navigate(ROUTE_HOME)
            } else {
                // If deletion failed, show error message
                Toast.makeText(context, task.exception?.message ?: "Deletion failed", Toast.LENGTH_SHORT).show()
            }
        }
    }


}