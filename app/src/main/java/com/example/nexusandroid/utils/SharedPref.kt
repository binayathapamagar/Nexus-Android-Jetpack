package com.example.nexusandroid.utils

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.google.api.Context


object SharedPref {
    fun storeDate(name:String, email:String, bio:String, userName:String,imageUrl:String, context: android.content.Context ){
        val sharedPreferences = context.getSharedPreferences("users",MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("name",name)
        editor.putString("email",email)
        editor.putString("bio",bio)
        editor.putString("userName",userName)
        editor.putString("imageUrl",imageUrl)
        editor.apply()
    }
    fun getUserName(context: android.content.Context):String{
        val sharedPreferences = context.getSharedPreferences("users",MODE_PRIVATE)
        return sharedPreferences.getString("username","")!!
    }
    fun getName(context: android.content.Context):String{
        val sharedPreferences = context.getSharedPreferences("users",MODE_PRIVATE)
        return sharedPreferences.getString("name","")!!
    }
    fun getBio(context: android.content.Context):String{
        val sharedPreferences = context.getSharedPreferences("users",MODE_PRIVATE)
        return sharedPreferences.getString("bio","")!!
    }
    fun getEmail(context: android.content.Context):String{
        val sharedPreferences = context.getSharedPreferences("users",MODE_PRIVATE)
        return sharedPreferences.getString("email","")!!
    }
    fun getImage(context: android.content.Context):String{
        val sharedPreferences = context.getSharedPreferences("users",MODE_PRIVATE)
        return sharedPreferences.getString("image","")!!
    }
}