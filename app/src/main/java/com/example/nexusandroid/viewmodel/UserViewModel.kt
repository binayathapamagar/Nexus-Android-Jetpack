package com.example.nexusandroid.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.nexusandroid.model.ThreadModel
import com.example.nexusandroid.model.UserModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.auth.User

class UserViewModel : ViewModel() {

    private val db = FirebaseDatabase.getInstance()
    val threadRef = db.getReference("threads")
    val userRef = db.getReference("users")

    private val _threads = MutableLiveData(listOf<ThreadModel>())
    val threads: LiveData<List<ThreadModel>> get() = _threads

    private val _users = MutableLiveData(listOf<UserModel>())
    val users: LiveData<List<UserModel>> get() = _users

    fun fetchUser(uid:String) {
        userRef.child(uid).addListenerForSingleValueEvent(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(UserModel::class.java)
                user?.let {
                    _users.postValue(listOf(it))
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    fun fetchThreads(uid:String) {
        threadRef.orderByChild("userId").equalTo(uid).addListenerForSingleValueEvent(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val threadList = snapshot.children.mapNotNull {
                    it.getValue(ThreadModel::class.java)
                }
                _threads.postValue(threadList)
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
}