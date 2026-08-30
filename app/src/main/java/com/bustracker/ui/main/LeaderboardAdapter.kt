package com.bustracker.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bustracker.data.model.DriverModel
import com.karroh.bussathi.databinding.ItemLeaderboardBinding

class LeaderboardAdapter(private var driverList: List<DriverModel>) :
    RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>() {

    inner class LeaderboardViewHolder(val binding: ItemLeaderboardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val binding = ItemLeaderboardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LeaderboardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        val driver = driverList[position]

        with(holder.binding) {
            tvRank.text = (position + 1).toString()
            tvDriverName.text = driver.name
            tvLicense.text = driver.licenseNumber
            tvDistance.text = String.format("%.2f km", driver.totalKm)
        }
    }

    override fun getItemCount(): Int = driverList.size

    fun updateData(newList: List<DriverModel>) {
        driverList = newList
        notifyDataSetChanged()
    }
}