package com.example.nexusandroid.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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

class AuthViewModel : ViewModel() {

    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()
    val userRef: DatabaseReference = db.getReference("users")

    private val storageRef = Firebase.storage.reference
    private val imageRef = storageRef.child("users/${UUID.randomUUID()}.jpg")

    private val _firebaseUser = MutableLiveData<FirebaseUser?>()
    val firebaseUser: LiveData<FirebaseUser?> = _firebaseUser

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    init {
        _firebaseUser.value = auth.currentUser
    }

    fun login(email: String, password: String, context: Context) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    _firebaseUser.value = auth.currentUser
                    _firebaseUser.value?.let { user ->
                        userRef.child(user.uid).setValue(user)

                        getData(auth.currentUser!!.uid, context)
                    }
                } else {
                    _error.postValue("Something went wrong.")
                }
            }
    }

    private fun getData(uid: String, context: Context){
        userRef.child(uid).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val userData = snapshot.getValue(UserModel::class.java )
                SharedPref.storeDate(userData!!.email,userData!!.name,userData!!.userName,userData!!.bio,userData!!.imageUrl,context)

            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    fun register(email: String, password: String, name:String, bio:String, userName: String, imageUri: Uri, context: Context) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _firebaseUser.postValue(auth.currentUser)
                    saveImage(email,password,userName,name,bio, imageUri, auth.currentUser?.uid, context)

                } else {
                    _error.postValue("Something went wrong.")
                }
            }
    }

   private fun saveImage(email: String,password: String,name: String,bio: String,userName: String, imageUri: Uri,uid: String?,context: Context) {
       val uploadTask = imageRef.putFile(imageUri)
       uploadTask.addOnSuccessListener {
           imageRef.downloadUrl.addOnSuccessListener {
               saveData(email, password, name, userName, bio, it.toString(), uid, context )
           }
       }
   }

    private fun saveData(email: String, password: String, name: String, userName: String, bio: String, toString: String, uid: String?, context: Context) {
        val userData = UserModel(email, password, name, userName, bio, toString,uid!!)

        userRef.child(uid!!).setValue(userData).addOnSuccessListener {
            SharedPref.storeDate(email,name,userName,bio,toString,context)

        }.addOnFailureListener{

        }



    }
    fun logout(){
        auth.signOut()
        _firebaseUser.postValue(null)
    }


}