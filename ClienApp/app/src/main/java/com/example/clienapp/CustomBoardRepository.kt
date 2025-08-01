package com.example.clienapp

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object CustomBoardRepository {
    private const val PREFS_NAME = "custom_boards_prefs"
    private const val KEY_CUSTOM_BOARDS = "custom_boards_json"

    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getCustomBoards(): List<MenuItem> {
        val json = sharedPreferences.getString(KEY_CUSTOM_BOARDS, null)
        return if (json != null) {
            val type = object : TypeToken<List<MenuItem>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun addCustomBoard(menuItem: MenuItem) {
        val currentBoards = getCustomBoards().toMutableList()
        currentBoards.add(menuItem)
        val json = gson.toJson(currentBoards)
        sharedPreferences.edit().putString(KEY_CUSTOM_BOARDS, json).apply()
    }

    fun deleteCustomBoard(menuItem: MenuItem) {
        val currentBoards = getCustomBoards().toMutableList()
        currentBoards.removeAll { it.url == menuItem.url && it.title == menuItem.title }
        val json = gson.toJson(currentBoards)
        sharedPreferences.edit().putString(KEY_CUSTOM_BOARDS, json).apply()
    }
}
