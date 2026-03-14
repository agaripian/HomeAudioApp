package com.example.homeaudiogem.activities

import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.homeaudiogem.R
import com.example.homeaudiogem.api.ZoneRepository
import com.example.homeaudiogem.models.Zone
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ControlAllActivity : AppCompatActivity() {

    private lateinit var repository: ZoneRepository
    private lateinit var masterZone: Zone

    private lateinit var toolbar: Toolbar
    private lateinit var btnPower: Button
    private lateinit var btnMute: Button
    private lateinit var seekVolume: SeekBar
    private lateinit var seekTreble: SeekBar
    private lateinit var seekBass: SeekBar
    private lateinit var spinnerSource: Spinner
    private lateinit var txtVolume: TextView
    private lateinit var txtTreble: TextView
    private lateinit var txtBass: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var txtError: TextView
    private lateinit var toolbarTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control_all)

        // Get master zone from intent using the parcelable extra
        masterZone = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 and higher
            intent.getParcelableExtra("MASTER_ZONE", Zone::class.java) ?: Zone.createMasterZone()
        } else {
            // For older Android versions
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("MASTER_ZONE") ?: Zone.createMasterZone()
        }

        // Initialize repository
        repository = ZoneRepository(this)

        // Initialize views
        toolbar = findViewById(R.id.toolbar)
        toolbarTitle = findViewById(R.id.toolbarTitle)
        setSupportActionBar(toolbar)
        // Don't show the title in the ActionBar since we're using our custom TextView
        supportActionBar?.setDisplayShowTitleEnabled(false)
        // Enable the back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        btnPower = findViewById(R.id.btnPower)
        btnMute = findViewById(R.id.btnMute)
        seekVolume = findViewById(R.id.seekVolume)
        seekTreble = findViewById(R.id.seekTreble)
        seekBass = findViewById(R.id.seekBass)
        spinnerSource = findViewById(R.id.spinnerSource)
        txtVolume = findViewById(R.id.txtVolume)
        txtTreble = findViewById(R.id.txtTreble)
        txtBass = findViewById(R.id.txtBass)
        progressBar = findViewById(R.id.progressBar)
        txtError = findViewById(R.id.txtError)

        // Setup source spinner
        setupSourceSpinner()

        // Setup click listeners
        setupClickListeners()

        // Load current state
        loadCurrentState()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupSourceSpinner() {
        val sources = arrayOf("Source 1", "Source 2", "Source 3", "Source 4", "Source 5", "Source 6")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sources)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSource.adapter = adapter

        // Set current source
        val sourceIndex = masterZone.ch.toIntOrNull()?.minus(1) ?: 0
        if (sourceIndex in 0..5) {
            spinnerSource.setSelection(sourceIndex)
        }

        spinnerSource.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val sourceValue = (position + 1).toString().padStart(2, '0')
                if (sourceValue != masterZone.ch) {
                    setAllZonesAttribute("ch", sourceValue)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupClickListeners() {
        btnPower.setOnClickListener {
            val newValue = if (masterZone.isOn()) "00" else "01"
            setAllZonesAttribute("pr", newValue)
            val newState = !masterZone.isOn()
            updatePowerButton(newState)
            
            // Apply styles to seekbars based on new power state
            applySeekBarStyle(seekVolume, newState)
            applySeekBarStyle(seekTreble, newState)
            applySeekBarStyle(seekBass, newState)
            
            // Update text alpha
            txtVolume.alpha = if (newState) 1.0f else 0.5f
            txtTreble.alpha = if (newState) 1.0f else 0.5f
            txtBass.alpha = if (newState) 1.0f else 0.5f
        }

        btnMute.setOnClickListener {
            val newValue = if (masterZone.isMuted()) "00" else "01"
            setAllZonesAttribute("mu", newValue)
            updateMuteButton(!masterZone.isMuted())
        }

        seekVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                txtVolume.text = getString(R.string.volume, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    val paddedVolume = repository.pad2(it.progress.toString())
                    setAllZonesAttribute("vo", paddedVolume)
                }
            }
        })

        seekTreble.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                txtTreble.text = getString(R.string.treble, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    val paddedTreble = repository.pad2(it.progress.toString())
                    setAllZonesAttribute("tr", paddedTreble)
                }
            }
        })

        seekBass.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                txtBass.text = getString(R.string.bass, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    val paddedBass = repository.pad2(it.progress.toString())
                    setAllZonesAttribute("bs", paddedBass)
                }
            }
        })
    }

    private fun loadCurrentState() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val zones = repository.getZones()
                if (zones.isNotEmpty()) {
                    // Update UI with first zone's values (assuming all zones have same settings)
                    val firstZone = zones.firstOrNull() ?: return@launch

                    // Update volume
                    val volume = firstZone.vo.toIntOrNull() ?: 15
                    seekVolume.progress = volume
                    txtVolume.text = getString(R.string.volume, volume)

                    // Update treble
                    val treble = firstZone.tr.toIntOrNull() ?: 7
                    seekTreble.progress = treble
                    txtTreble.text = getString(R.string.treble, treble)

                    // Update bass
                    val bass = firstZone.bs.toIntOrNull() ?: 7
                    seekBass.progress = bass
                    txtBass.text = getString(R.string.bass, bass)

                    // Update source
                    val sourceIndex = firstZone.ch.toIntOrNull()?.minus(1) ?: 0
                    if (sourceIndex in 0..5) {
                        spinnerSource.setSelection(sourceIndex)
                    }

                    // Update power state and apply styles
                    val isOn = firstZone.isOn()
                    updatePowerButton(isOn)
                    
                    // Apply styles to seekbars based on power state
                    applySeekBarStyle(seekVolume, isOn)
                    applySeekBarStyle(seekTreble, isOn)
                    applySeekBarStyle(seekBass, isOn)
                    
                    // Update text alpha
                    txtVolume.alpha = if (isOn) 1.0f else 0.5f
                    txtTreble.alpha = if (isOn) 1.0f else 0.5f
                    txtBass.alpha = if (isOn) 1.0f else 0.5f

                    // Update mute button
                    updateMuteButton(firstZone.isMuted())

                    // Update master zone
                    masterZone = firstZone.copy(zone = "all")
                }
            } catch (e: Exception) {
                showError(e.message ?: getString(R.string.error_loading_zones))
            } finally {
                showLoading(false)
            }
        }
    }

    private fun setAllZonesAttribute(attribute: String, value: String) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val result = repository.setAllZonesAttribute(attribute, value)
                if (result.isNotEmpty()) {
                    // Update master zone with the new value
                    when (attribute) {
                        "pr" -> masterZone.pr = value
                        "mu" -> masterZone.mu = value
                        "vo" -> masterZone.vo = value
                        "tr" -> masterZone.tr = value
                        "bs" -> masterZone.bs = value
                        "ch" -> masterZone.ch = value
                    }
                }
            } catch (e: Exception) {
                showError(e.message ?: getString(R.string.error_updating_zone))
            } finally {
                showLoading(false)
            }
        }
    }

    private fun updatePowerButton(isOn: Boolean) {
        btnPower.text = if (isOn) getString(R.string.power_off) else getString(R.string.power_on)
    }

    private fun updateMuteButton(isMuted: Boolean) {
        btnMute.text = if (isMuted) getString(R.string.unmute) else getString(R.string.mute)
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        txtError.visibility = View.VISIBLE
        txtError.text = message

        // Auto-hide error after 3 seconds
        lifecycleScope.launch {
            delay(3000)
            txtError.visibility = View.GONE
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
                ContextCompat.getColor(this, R.color.slider_thumb_dark))
            seekBar.progressTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.slider_progress_dark))
            seekBar.progressBackgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.slider_track_dark))
        } else {
            seekBar.thumbTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.slider_thumb_dark_disabled))
            seekBar.progressTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.slider_progress_dark_disabled))
            seekBar.progressBackgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.slider_track_dark_disabled))
        }
        
        // Force a redraw
        seekBar.invalidate()
    }
    
    // Helper to determine if device is in night mode
    private fun isNightMode(): Boolean {
        return resources.configuration.uiMode and 
            android.content.res.Configuration.UI_MODE_NIGHT_MASK == 
            android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    override fun onResume() {
        super.onResume()
        
        // Ensure styles are correctly applied when returning to the activity
        val isOn = masterZone.isOn()
        applySeekBarStyle(seekVolume, isOn)
        applySeekBarStyle(seekTreble, isOn)
        applySeekBarStyle(seekBass, isOn)
        
        // Update text alpha
        txtVolume.alpha = if (isOn) 1.0f else 0.5f
        txtTreble.alpha = if (isOn) 1.0f else 0.5f
        txtBass.alpha = if (isOn) 1.0f else 0.5f
    }
}