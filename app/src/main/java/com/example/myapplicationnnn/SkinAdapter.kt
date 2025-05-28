package com.example.myapplicationnnn

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast // <--- ADD THIS IMPORT
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class SkinAdapter(
    private var skins: List<Skin>, // Make it var
    private var userCoins: Int,    // Make it var
    private val onSkinSelected: (Skin) -> Unit
) : RecyclerView.Adapter<SkinAdapter.SkinViewHolder>() {

    private var selectedPosition = -1 // Initialize to -1 or find first selectable

    init {
        // Automatically select the first available (unlocked or free) skin if none selected
        selectedPosition = skins.indexOfFirst { it.isUnlocked || it.price == 0 }
        if (selectedPosition != -1 && selectedPosition < skins.size) { // Added boundary check
            onSkinSelected(skins[selectedPosition])
        }
    }


    // Method to update the adapter's data
    fun updateSkins(newSkins: List<Skin>, newUserCoins: Int) {
        this.skins = newSkins
        this.userCoins = newUserCoins
        // If selected skin is no longer valid or affordable, reset selection
        val currentSelectedSkin = if (selectedPosition != -1 && selectedPosition < skins.size) skins[selectedPosition] else null

        if (currentSelectedSkin == null ||
            (!currentSelectedSkin.isUnlocked && currentSelectedSkin.price > 0 && userCoins < currentSelectedSkin.price)) {
            selectedPosition = skins.indexOfFirst { it.isUnlocked || it.price == 0 }
        }
        notifyDataSetChanged()
    }

    // Method to programmatically set the selected skin
    fun setSelectedSkin(position: Int) {
        if (position >= 0 && position < skins.size) {
            val skin = skins[position]
            if (skin.isUnlocked || skin.price == 0 || userCoins >= skin.price) {
                val oldPosition = selectedPosition
                selectedPosition = position
                if (oldPosition != -1 && oldPosition < skins.size) notifyItemChanged(oldPosition) // Boundary check
                if (selectedPosition < skins.size) notifyItemChanged(selectedPosition) // Boundary check
                onSkinSelected(skin)
            }
        }
    }


    class SkinViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val skinImage: ImageView = view.findViewById(R.id.skinImage)
        val skinPrice: TextView = view.findViewById(R.id.skinPrice)
        val selectedIndicator: View = view.findViewById(R.id.selectedIndicator)
        val cardView: CardView = view.findViewById(R.id.skinItemCardView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkinViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.skin_item, parent, false)
        return SkinViewHolder(view)
    }

    override fun onBindViewHolder(holder: SkinViewHolder, position: Int) {
        if (position < 0 || position >= skins.size) return // Graceful exit if position is out of bounds
        val skin = skins[position]
        val context = holder.itemView.context

        holder.skinImage.setImageResource(skin.imageResource)

        when {
            skin.isUnlocked || skin.price == 0 -> {
                holder.skinPrice.text = if (skin.price == 0 && !skin.isUnlocked) "FREE" else "OWNED"
                holder.skinPrice.setTextColor(ContextCompat.getColor(context, R.color.accent_green))
            }
            userCoins >= skin.price -> {
                holder.skinPrice.text = "${skin.price}🪙"
                holder.skinPrice.setTextColor(ContextCompat.getColor(context, R.color.coin_gold))
            }
            else -> {
                holder.skinPrice.text = "${skin.price}🪙"
                holder.skinPrice.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
        }

        holder.selectedIndicator.visibility = if (position == selectedPosition) View.VISIBLE else View.GONE

        val isAffordableOrOwned = skin.isUnlocked || skin.price == 0 || userCoins >= skin.price
        val cardBackgroundColor = when {
            position == selectedPosition -> R.color.selected_skin
            !isAffordableOrOwned -> R.color.primary_medium
            else -> R.color.primary_light
        }
        holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, cardBackgroundColor))
        holder.itemView.alpha = if (isAffordableOrOwned || position == selectedPosition) 1.0f else 0.6f


        holder.itemView.setOnClickListener {
            // Ensure position is still valid (can change due to adapter updates)
            val currentPosition = holder.adapterPosition
            if (currentPosition == RecyclerView.NO_POSITION || currentPosition >= skins.size) return@setOnClickListener

            val clickedSkin = skins[currentPosition] // Use current skin
            val currentIsAffordableOrOwned = clickedSkin.isUnlocked || clickedSkin.price == 0 || userCoins >= clickedSkin.price

            if (currentIsAffordableOrOwned) {
                val oldPosition = selectedPosition
                selectedPosition = currentPosition
                if (oldPosition != -1 && oldPosition < skins.size) notifyItemChanged(oldPosition)
                if (selectedPosition < skins.size) notifyItemChanged(selectedPosition)
                onSkinSelected(clickedSkin)
            } else {
                Toast.makeText(context, "Not enough coins or skin locked!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount() = skins.size
}