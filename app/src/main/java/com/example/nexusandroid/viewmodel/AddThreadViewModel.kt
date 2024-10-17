package com.example.nexusandroid.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.nexusandroid.model.ThreadModel
import com.example.nexusandroid.model.UserModel
import com.example.nexusandroid.utils.SharedPref
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.core.UserData
import com.google.firebase.storage.storage
import java.util.UUID

class AddThreadViewModel : ViewModel() {

    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()
    val userRef: DatabaseReference = db.getReference("users")

    private val storageRef = Firebase.storage.reference
    private val imageRef = storageRef.child("users/${UUID.randomUUID()}.jpg")

    private val _isPosted = MutableLiveData<Boolean>()
    val isPosted: LiveData<Boolean> = _isPosted

    // LiveData to observe the current FirebaseUser (null if logged out)
    private val _firebaseUser = MutableLiveData<FirebaseUser?>()
    val firebaseUser: LiveData<FirebaseUser?> = _firebaseUser

    init {
        // Initialize _firebaseUser with the current user
        _firebaseUser.value = auth.currentUser
    }



    fun saveImage(
       thread: String,
       userId : String,
       imageUri: Uri,
   ) {
       val uploadTask = imageRef.putFile(imageUri)
       uploadTask.addOnSuccessListener {
           imageRef.downloadUrl.addOnSuccessListener {
               saveData(thread, userId, it.toString())
           }
       }
   }
    fun saveData(
        thread: String,
        userId : String,
        image: String,
        ) {
        val threadData = ThreadModel(thread, image, userId, System.currentTimeMillis().toString())

        userRef.child(userRef.push().key!!).setValue(threadData)
            .addOnSuccessListener {
                _isPosted.postValue(true)
            }.addOnFailureListener{
                _isPosted.postValue(false)
    }



    }
    fun logout(){
        auth.signOut()
        _firebaseUser.postValue(null)

    }


}