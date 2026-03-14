package com.example.homeaudiogem

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import android.graphics.Rect
import com.example.homeaudiogem.activities.ControlAllActivity
import com.example.homeaudiogem.activities.SettingsActivity
import com.example.homeaudiogem.adapters.ZoneAdapter
import com.example.homeaudiogem.api.ZoneRepository
import com.example.homeaudiogem.models.Zone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), ZoneAdapter.ZoneAdapterListener {

    private lateinit var repository: ZoneRepository
    private lateinit var adapter: ZoneAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var txtError: TextView
    private lateinit var btnSettings: ImageButton
    private lateinit var btnControlAll: Button
    private lateinit var btnAllOff: Button
    private lateinit var toolbar: Toolbar
    private lateinit var toolbarTitle: TextView

    private val zones = mutableListOf<Zone>()
    private val masterZone = Zone.createMasterZone()
    
    // Flag to prevent duplicate API calls
    private var isLoadingZones = false
    
    // Reference to periodic refresh job
    private var refreshJob: Job? = null

    // Add this class inside MainActivity
    class BottomSpacingItemDecoration(private val spacing: Int) : ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            // Add space only to the last item
            if (parent.getChildAdapterPosition(view) == parent.adapter?.itemCount?.minus(1)) {
                outRect.bottom = spacing
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize repository
        repository = ZoneRepository(this)

        // Initialize views
        recyclerView = findViewById(R.id.zonesRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        txtError = findViewById(R.id.txtError)
        btnSettings = findViewById(R.id.btnSettings)
        btnControlAll = findViewById(R.id.btnControlAll)
        btnAllOff = findViewById(R.id.btnAllOff)
        
        // Set up toolbar
        toolbar = findViewById(R.id.toolbar)
        toolbarTitle = findViewById(R.id.toolbarTitle)
        toolbarTitle.text = getString(R.string.home_audio_control)
        setSupportActionBar(toolbar)
        // Don't show the title in the ActionBar since we're using our custom TextView
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ZoneAdapter(this, zones, this)
        recyclerView.adapter = adapter
        // Add bottom spacing decoration
        recyclerView.addItemDecoration(BottomSpacingItemDecoration(resources.getDimensionPixelSize(R.dimen.bottom_space)))

        // Setup click listeners
        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        btnControlAll.setOnClickListener {
            val intent = Intent(this, ControlAllActivity::class.java)
            intent.putExtra("MASTER_ZONE", masterZone)
            startActivity(intent)
        }

        btnAllOff.setOnClickListener {
            turnAllZonesOff()
        }

        // Load data
        loadZones()

        // Start periodic refresh
        startPeriodicRefresh()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cancel the refresh job when the activity is destroyed
        refreshJob?.cancel()
    }

    override fun onResume() {
        super.onResume()
        // Only refresh zones if we're not already loading them
        if (!isLoadingZones) {
            loadZones()
        }
    }

    private fun loadZones() {
        // If already loading, don't make another call
        if (isLoadingZones) return
        
        isLoadingZones = true
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val result = repository.getZones()
                if (result.isNotEmpty()) {
                    updateZones(result)
                    showError(false)
                } else {
                    showError(true, getString(R.string.error_no_zones))
                }
            } catch (e: Exception) {
                showError(true, e.message ?: getString(R.string.error_loading_zones))
            } finally {
                showLoading(false)
                isLoadingZones = false
            }
        }
    }

    private fun startPeriodicRefresh() {
        // Cancel any existing job first
        refreshJob?.cancel()
        
        refreshJob = lifecycleScope.launch {
            // Initial delay to prevent conflict with the first loadZones call
            delay(10000)
            
            while (true) {
                // Only make API call if not already loading
                if (!isLoadingZones) {
                    try {
                        val result = repository.getZones()
                        if (result.isNotEmpty()) {
                            updateZones(result)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                delay(10000) // 10 seconds between each refresh
            }
        }
    }

    private suspend fun updateZones(newZones: List<Zone>) = withContext(Dispatchers.Main) {
        if (zones.isEmpty()) {
            // First load - just add all zones
            zones.addAll(newZones)
            adapter.notifyDataSetChanged()
        } else {
            // Update existing zones without triggering a full refresh
            // This preserves expanded states and other UI states
            for (i in newZones.indices) {
                if (i < zones.size) {
                    // Update existing zone data while preserving position
                    zones[i] = newZones[i]
                } else {
                    // New zone appeared - add it
                    zones.add(newZones[i])
                }
            }
            
            // If we had more zones before than now, remove the extra ones
            if (zones.size > newZones.size) {
                zones.subList(newZones.size, zones.size).clear()
            }
            
            // Using notifyItemRangeChanged instead of notifyDataSetChanged to avoid full rebind
            // This helps preserve UI states like expanded controls
            adapter.notifyItemRangeChanged(0, zones.size, "DATA_UPDATE_ONLY")
        }
    }

    private fun turnAllZonesOff() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val result = repository.setAllZonesAttribute("pr", "00")
                if (result.isNotEmpty()) {
                    updateZones(result)
                }
            } catch (e: Exception) {
                showError(true, e.message ?: getString(R.string.error_turning_off))
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showError(isError: Boolean, message: String = "") {
        if (isError) {
            txtError.visibility = View.VISIBLE
            txtError.text = message

            // Auto-hide error after 3 seconds
            lifecycleScope.launch {
                delay(3000)
                txtError.visibility = View.GONE
            }
        } else {
            txtError.visibility = View.GONE
        }
    }

    // ZoneAdapterListener implementations
    override fun onPowerClicked(zone: Zone, position: Int) {
        // Determine new power state
        val newValue = if (zone.isOn()) "00" else "01"
        
        // Update local model immediately to avoid visual flickering
        zones[position] = zones[position].copy(pr = newValue)
        
        // Notify adapter of the change with a payload to prevent full rebind
        adapter.notifyItemChanged(position, "DATA_UPDATE_ONLY")
        
        // Make API call in background without triggering UI update
        lifecycleScope.launch {
            try {
                repository.setZoneAttribute(zone.zone, "pr", newValue)
                // No need to update UI again
            } catch (e: Exception) {
                // Only show error and revert if API call fails
                showError(true, e.message ?: getString(R.string.error_updating_zone))
                
                // Revert to original power state if API call failed
                lifecycleScope.launch(Dispatchers.Main) {
                    val originalZone = repository.getZone(zone.zone)
                    originalZone?.let {
                        zones[position] = it
                        adapter.notifyItemChanged(position)
                    }
                }
            }
        }
    }

    override fun onVolumeChanged(zone: Zone, volume: String, position: Int) {
        // Update local model immediately to avoid visual flickering
        val paddedVolume = repository.pad2(volume)
        zones[position] = zones[position].copy(vo = paddedVolume)
        
        // Make API call in background without triggering UI update
        lifecycleScope.launch {
            try {
                repository.setZoneAttribute(zone.zone, "vo", paddedVolume)
                // No need to update UI again or call notifyItemChanged
            } catch (e: Exception) {
                // Only show error and revert if API call fails
                showError(true, e.message ?: getString(R.string.error_updating_zone))
                
                // Revert to original volume if API call failed
                lifecycleScope.launch(Dispatchers.Main) {
                    val originalZone = repository.getZone(zone.zone)
                    originalZone?.let {
                        zones[position] = it
                        adapter.notifyItemChanged(position)
                    }
                }
            }
        }
    }

    override fun onSourceSelected(zone: Zone, source: String, position: Int) {
        lifecycleScope.launch {
            try {
                val updatedZone = repository.setZoneAttribute(zone.zone, "ch", source)
                updatedZone?.let {
                    zones[position] = it
                    adapter.notifyItemChanged(position)
                }
            } catch (e: Exception) {
                showError(true, e.message ?: getString(R.string.error_updating_zone))
            }
        }
    }

    override fun onNameChanged(zone: Zone, name: String, position: Int) {
        // Update local model immediately to avoid visual flickering
        zones[position] = zones[position].copy(name = name)
        
        // Make API call in background without triggering UI update
        lifecycleScope.launch {
            try {
                repository.setZoneAttribute(zone.zone, "nm", name)
                // No need to update UI again or call notifyItemChanged
            } catch (e: Exception) {
                // Only show error and revert if API call fails
                showError(true, e.message ?: getString(R.string.error_updating_zone))
                
                // Revert to original name if API call failed
                lifecycleScope.launch(Dispatchers.Main) {
                    val originalZone = repository.getZone(zone.zone)
                    originalZone?.let {
                        zones[position] = it
                        adapter.notifyItemChanged(position)
                    }
                }
            }
        }
    }

    override fun onTrebleChanged(zone: Zone, treble: String, position: Int) {
        // Update local model immediately to avoid visual flickering
        val paddedTreble = repository.pad2(treble)
        zones[position] = zones[position].copy(tr = paddedTreble)
        
        // Make API call in background without triggering UI update
        lifecycleScope.launch {
            try {
                repository.setZoneAttribute(zone.zone, "tr", paddedTreble)
                // No need to update UI again or call notifyItemChanged
            } catch (e: Exception) {
                // Only show error and revert if API call fails
                showError(true, e.message ?: getString(R.string.error_updating_zone))
                
                // Revert to original treble if API call failed
                lifecycleScope.launch(Dispatchers.Main) {
                    val originalZone = repository.getZone(zone.zone)
                    originalZone?.let {
                        zones[position] = it
                        adapter.notifyItemChanged(position)
                    }
                }
            }
        }
    }

    override fun onBassChanged(zone: Zone, bass: String, position: Int) {
        // Update local model immediately to avoid visual flickering
        val paddedBass = repository.pad2(bass)
        zones[position] = zones[position].copy(bs = paddedBass)
        
        // Make API call in background without triggering UI update
        lifecycleScope.launch {
            try {
                repository.setZoneAttribute(zone.zone, "bs", paddedBass)
                // No need to update UI again or call notifyItemChanged
            } catch (e: Exception) {
                // Only show error and revert if API call fails
                showError(true, e.message ?: getString(R.string.error_updating_zone))
                
                // Revert to original bass if API call failed
                lifecycleScope.launch(Dispatchers.Main) {
                    val originalZone = repository.getZone(zone.zone)
                    originalZone?.let {
                        zones[position] = it
                        adapter.notifyItemChanged(position)
                    }
                }
            }
        }
    }
}