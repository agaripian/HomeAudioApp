package com.example.homeaudiogem.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.homeaudiogem.R
import com.example.homeaudiogem.models.Zone

class ZoneAdapter(
    private val context: Context,
    private val zones: List<Zone>,
    private val listener: ZoneAdapterListener
) : RecyclerView.Adapter<ZoneAdapter.ZoneViewHolder>() {

    // Map to store expanded state for each zone by its zone ID
    private val expandedStates = mutableMapOf<String, Boolean>()

    interface ZoneAdapterListener {
        fun onPowerClicked(zone: Zone, position: Int)
        fun onVolumeChanged(zone: Zone, volume: String, position: Int)
        fun onSourceSelected(zone: Zone, source: String, position: Int)
        fun onNameChanged(zone: Zone, name: String, position: Int)
        fun onTrebleChanged(zone: Zone, treble: String, position: Int)
        fun onBassChanged(zone: Zone, bass: String, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ZoneViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_zone, parent, false)
        return ZoneViewHolder(view)
    }

    override fun onBindViewHolder(holder: ZoneViewHolder, position: Int) {
        val zone = zones[position]
        holder.bind(zone, position)
    }

    override fun onBindViewHolder(
        holder: ZoneViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty() && payloads[0] == "DATA_UPDATE_ONLY") {
            // Partial update - only update data values, not expanded state
            val zone = zones[position]
            holder.updateData(zone, position)
        } else {
            // Full update
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun getItemCount(): Int = zones.size

    inner class ZoneViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtZoneName: TextView = itemView.findViewById(R.id.txtZoneName)
        private val editZoneName: EditText = itemView.findViewById(R.id.editZoneName)
        private val btnPower: ImageButton = itemView.findViewById(R.id.btnPower)
        private val seekVolume: SeekBar = itemView.findViewById(R.id.seekVolume)
        private val spinnerSource: Spinner = itemView.findViewById(R.id.spinnerSource)
        private val seekTreble: SeekBar = itemView.findViewById(R.id.seekTreble)
        private val seekBass: SeekBar = itemView.findViewById(R.id.seekBass)
        private val txtVolume: TextView = itemView.findViewById(R.id.txtVolume)
        private val txtTreble: TextView = itemView.findViewById(R.id.txtTreble)
        private val txtBass: TextView = itemView.findViewById(R.id.txtBass)
        private val btnExpandCollapse: ImageButton = itemView.findViewById(R.id.btnExpandCollapse)
        private val expandableControls: LinearLayout = itemView.findViewById(R.id.expandableControls)
        
        // Track expanded state
        private var isExpanded = false

        fun bind(zone: Zone, position: Int) {
            // Update all the data values first
            updateData(zone, position)
            
            // Get previous expanded state or default to false
            isExpanded = expandedStates[zone.zone] ?: false
            
            // Set expanded state based on stored value
            expandableControls.visibility = if (isExpanded) View.VISIBLE else View.GONE
            btnExpandCollapse.setImageResource(
                if (isExpanded) R.drawable.ic_collapse else R.drawable.ic_expand
            )
            
            // Setup click listeners
            btnPower.setOnClickListener {
                // Get the current zone state from the actual zones array (which might have been updated)
                val currentZone = zones[position]
                // Toggle based on the current actual state
                val newState = !currentZone.isOn()
                
                // Set the visual state directly for snappier response
                updatePowerButton(newState)
                
                // Update slider styles immediately
                applySeekBarStyle(seekVolume, newState)
                applySeekBarStyle(seekTreble, newState)
                applySeekBarStyle(seekBass, newState)
                
                // Update text alphas
                txtVolume.alpha = if (newState) 1.0f else 0.5f
                txtTreble.alpha = if (newState) 1.0f else 0.5f
                txtBass.alpha = if (newState) 1.0f else 0.5f
                
                // Notify listener about the change (which will make the API call)
                listener.onPowerClicked(currentZone, position)
            }
            
            btnExpandCollapse.setOnClickListener {
                isExpanded = !isExpanded
                // Store the new expanded state
                expandedStates[zone.zone] = isExpanded
                
                expandableControls.visibility = if (isExpanded) View.VISIBLE else View.GONE
                btnExpandCollapse.setImageResource(
                    if (isExpanded) R.drawable.ic_collapse else R.drawable.ic_expand
                )
            }
            
            // Handle zone name editing in expandable section
            editZoneName.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val newName = editZoneName.text.toString()
                    if (newName.isNotEmpty() && newName != zone.name) {
                        listener.onNameChanged(zone, newName, position)
                        // Update the display name as well
                        txtZoneName.text = newName
                    }
                }
            }
            
            seekVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    txtVolume.text = "Vol: $progress"
                }
                
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    seekBar?.let {
                        listener.onVolumeChanged(zone, it.progress.toString(), position)
                    }
                }
            })
            
            seekTreble.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    txtTreble.text = "Treb: $progress"
                }
                
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    seekBar?.let {
                        listener.onTrebleChanged(zone, it.progress.toString(), position)
                    }
                }
            })
            
            seekBass.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    txtBass.text = "Bass: $progress"
                }
                
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    seekBar?.let {
                        listener.onBassChanged(zone, it.progress.toString(), position)
                    }
                }
            })
        }
        
        // Method for partial updates - only updates data values, not UI state
        fun updateData(zone: Zone, position: Int) {
            // Update displayed values without touching expanded state
            txtZoneName.text = zone.name ?: "Zone ${zone.zone}"
            editZoneName.setText(zone.name ?: "Zone ${zone.zone}")
            
            // Get the current power state
            val isOn = zone.isOn()
            
            // Only update power button if needed
            updatePowerButton(isOn)
            
            // Set volume without causing redraw
            val volume = zone.vo.toIntOrNull() ?: 15
            if (seekVolume.progress != volume) {
                seekVolume.progress = volume
            }
            txtVolume.text = "Vol: $volume"
            
            // Set treble without causing redraw
            val treble = zone.tr.toIntOrNull() ?: 7
            if (seekTreble.progress != treble) {
                seekTreble.progress = treble
            }
            txtTreble.text = "Treb: $treble"
            
            // Set bass without causing redraw
            val bass = zone.bs.toIntOrNull() ?: 7
            if (seekBass.progress != bass) {
                seekBass.progress = bass
            }
            txtBass.text = "Bass: $bass"
            
            // Always apply SeekBar styles to ensure consistency
            applySeekBarStyle(seekVolume, isOn)
            applySeekBarStyle(seekTreble, isOn)
            applySeekBarStyle(seekBass, isOn)
            
            // Update text alpha
            txtVolume.alpha = if (isOn) 1.0f else 0.5f
            txtTreble.alpha = if (isOn) 1.0f else 0.5f
            txtBass.alpha = if (isOn) 1.0f else 0.5f
            
            // Setup source spinner
            setupSourceSpinner(zone)
        }
        
        private fun updatePowerButton(isOn: Boolean) {
            btnPower.setBackgroundResource(
                if (isOn) R.drawable.button_on_background
                else R.drawable.button_off_background
            )
        }
        
        private fun setupSourceSpinner(zone: Zone) {
            val sources = arrayOf("Source 1", "Source 2", "Source 3", "Source 4", "Source 5", "Source 6")
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, sources)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSource.adapter = adapter
            
            // Set current source
            val sourceIndex = zone.ch.toIntOrNull()?.minus(1) ?: 0
            if (sourceIndex in 0..5) {
                spinnerSource.setSelection(sourceIndex)
            }
            
            spinnerSource.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val sourceValue = (position + 1).toString().padStart(2, '0')
                    if (sourceValue != zone.ch) {
                        listener.onSourceSelected(zone, sourceValue, adapterPosition)
                    }
                }
                
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
        
        // Helper method to apply the correct style to a SeekBar based on enabled state
        private fun applySeekBarStyle(seekBar: SeekBar, enabled: Boolean) {
            // Reset enabled state before applying styles
            seekBar.isEnabled = false
            seekBar.isEnabled = enabled
            
            // Force apply the tints based on current state - more reliable across devices
            applyTintImmediately(seekBar, enabled)
            
            // Ensure the tint change propagates through the UI thread properly
            seekBar.post {
                // Reapply the enabled state to ensure it sticks
                seekBar.isEnabled = enabled
                applyTintImmediately(seekBar, enabled)
            }
        }
        
        // Helper to apply tints immediately
        private fun applyTintImmediately(seekBar: SeekBar, enabled: Boolean) {
            if (enabled) {
                seekBar.thumbTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.slider_thumb_dark))
                seekBar.progressTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.slider_progress_dark))
                seekBar.progressBackgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.slider_track_dark))
            } else {
                seekBar.thumbTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.slider_thumb_dark_disabled))
                seekBar.progressTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.slider_progress_dark_disabled))
                seekBar.progressBackgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.slider_track_dark_disabled))
            }
            
            // Force a redraw
            seekBar.invalidate()
        }
        
        // Helper to determine if device is in night mode
        private fun isNightMode(): Boolean {
            return context.resources.configuration.uiMode and 
                android.content.res.Configuration.UI_MODE_NIGHT_MASK == 
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
    }
} 