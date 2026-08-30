package com.bustracker.ui.main

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bustracker.data.model.DriverModel
import com.karroh.bussathi.databinding.ActivityLeaderboardBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLeaderboardBinding
    private lateinit var adapter: LeaderboardAdapter

    private val PREFS_NAME = "LeaderboardPrefs"
    private val KEY_LAST_FETCH = "last_fetch_time"
    private val KEY_CACHED_DATA = "cached_leaderboard"
    private val TWENTY_FOUR_HOURS_IN_MS = 24 * 60 * 60 * 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityLeaderboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = LeaderboardAdapter(emptyList())
        binding.rvLeaderboard.layoutManager = LinearLayoutManager(this)
        binding.rvLeaderboard.adapter = adapter

        checkAndFetchLeaderboard()
    }

    private fun checkAndFetchLeaderboard() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastFetchTime = prefs.getLong(KEY_LAST_FETCH, 0L)
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastFetchTime > TWENTY_FOUR_HOURS_IN_MS) {
            fetchFromFirebase()
        } else {
            loadCachedData()
        }
    }

    private fun fetchFromFirebase() {
        val db = FirebaseFirestore.getInstance()

        db.collection("drivers")
            .orderBy("totalKm", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { snapshot ->
                val drivers = snapshot.toObjects(DriverModel::class.java)
                adapter.updateData(drivers)
                cacheDataLocally(drivers)
            }
            .addOnFailureListener { e ->
                Log.e("Leaderboard", "Error fetching", e)
                Toast.makeText(this, "Failed to load latest. Showing cache.", Toast.LENGTH_SHORT).show()
                loadCachedData()
            }
    }

    private fun cacheDataLocally(drivers: List<DriverModel>) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = Gson().toJson(drivers)

        prefs.edit()
            .putString(KEY_CACHED_DATA, jsonString)
            .putLong(KEY_LAST_FETCH, System.currentTimeMillis())
            .apply()
    }

    private fun loadCachedData() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_CACHED_DATA, null)

        if (jsonString != null) {
            val type = object : TypeToken<List<DriverModel>>() {}.type
            val cachedDrivers: List<DriverModel> = Gson().fromJson(jsonString, type)
            adapter.updateData(cachedDrivers)
        } else {
            fetchFromFirebase()
        }
    }
}