package com.example.nexusandroid.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.nexusandroid.model.ThreadModel
import com.example.nexusandroid.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class HomeViewModel : ViewModel() {

    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()
    val thread: DatabaseReference = db.getReference("threads")


    private val _threadsAndUsers = MutableLiveData<List<Pair<ThreadModel, UserModel>>>()
    val threadsAndUsers: LiveData<List<Pair<ThreadModel, UserModel>>> = _threadsAndUsers

    init {

        fetchThreadsAndUsers {
            _threadsAndUsers.value = it
        }
    }


    private fun fetchThreadsAndUsers(onResult: (List<Pair<ThreadModel, UserModel>>)->Unit){
        thread.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val result = mutableListOf<Pair<ThreadModel, UserModel>>()
                for (threadSnapshot in snapshot.children){
                    val thread= threadSnapshot.getValue(ThreadModel::class.java)
                    thread.let{
                        fetchUserFromThread(it!!){
                                user ->
                            result.add(0,it to user)
                            if(result.size == snapshot.childrenCount.toInt()){
                                onResult(result)
                            }
                        }
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    fun fetchUserFromThread(thread: ThreadModel, onResult:(UserModel)-> Unit){
        db.getReference("users").child(thread.userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user=snapshot.getValue(UserModel::class.java)
                    user?.let(onResult)
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }









}