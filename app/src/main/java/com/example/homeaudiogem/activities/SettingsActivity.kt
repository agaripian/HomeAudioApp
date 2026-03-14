package com.example.homeaudiogem.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.example.homeaudiogem.R
import com.example.homeaudiogem.api.ZoneRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var repository: ZoneRepository
    
    private lateinit var toolbar: Toolbar
    private lateinit var editServerUrl: EditText
    private lateinit var editAmpCount: EditText
    private lateinit var btnSaveAmpCount: Button
    private lateinit var btnSaveSettings: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var txtError: TextView
    private lateinit var txtSuccess: TextView
    private lateinit var toolbarTitle: TextView
    private lateinit var radioGroupTheme: RadioGroup
    private lateinit var radioThemeSystem: RadioButton
    private lateinit var radioThemeLight: RadioButton
    private lateinit var radioThemeDark: RadioButton
    
    // Theme constants
    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        // Initialize repository
        repository = ZoneRepository(this)
        
        // Initialize views
        toolbar = findViewById(R.id.toolbar)
        toolbarTitle = findViewById(R.id.toolbarTitle)
        editServerUrl = findViewById(R.id.editServerUrl)
        editAmpCount = findViewById(R.id.editAmpCount)
        btnSaveAmpCount = findViewById(R.id.btnSaveAmpCount)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)
        progressBar = findViewById(R.id.progressBar)
        txtError = findViewById(R.id.txtError)
        txtSuccess = findViewById(R.id.txtSuccess)
        radioGroupTheme = findViewById(R.id.radioGroupTheme)
        radioThemeSystem = findViewById(R.id.radioThemeSystem)
        radioThemeLight = findViewById(R.id.radioThemeLight)
        radioThemeDark = findViewById(R.id.radioThemeDark)
        
        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Load current settings
        loadCurrentSettings()
        
        // Setup click listeners
        setupClickListeners()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    private fun loadCurrentSettings() {
        // Load server URL from preferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val serverUrl = sharedPreferences.getString("server_url", "http://192.168.1.23:3000/api/")
        editServerUrl.setText(serverUrl)
        
        // Load theme preference
        val currentTheme = sharedPreferences.getString(getString(R.string.theme_preference_key), THEME_SYSTEM)
        when (currentTheme) {
            THEME_LIGHT -> radioThemeLight.isChecked = true
            THEME_DARK -> radioThemeDark.isChecked = true
            else -> radioThemeSystem.isChecked = true
        }
        
        // Load amp count from API
        loadAmpCount()
    }
    
    private fun loadAmpCount() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val ampCount = repository.getAmpCount()
                editAmpCount.setText(ampCount.toString())
            } catch (e: Exception) {
                showError(e.message ?: getString(R.string.error_loading_zones))
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun setupClickListeners() {
        btnSaveAmpCount.setOnClickListener {
            saveAmpCount()
        }
        
        btnSaveSettings.setOnClickListener {
            saveServerUrl()
            saveThemePreference()
        }
    }
    
    private fun saveAmpCount() {
        val ampCountStr = editAmpCount.text.toString()
        if (ampCountStr.isBlank()) {
            showError(getString(R.string.error_saving_settings))
            return
        }
        
        val ampCount = ampCountStr.toIntOrNull() ?: 1
        
        showLoading(true)
        lifecycleScope.launch {
            try {
                val success = repository.setAmpCount(ampCount)
                if (success) {
                    showSuccess(getString(R.string.amp_count_saved))
                } else {
                    showError(getString(R.string.error_saving_settings))
                }
            } catch (e: Exception) {
                showError(e.message ?: getString(R.string.error_saving_settings))
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun saveServerUrl() {
        val serverUrl = editServerUrl.text.toString()
        if (serverUrl.isBlank()) {
            showError(getString(R.string.error_saving_settings))
            return
        }
        
        // Save to preferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.edit().putString("server_url", serverUrl).apply()
        
        showSuccess(getString(R.string.settings_saved))
    }
    
    private fun saveThemePreference() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val themePreferenceKey = getString(R.string.theme_preference_key)
        
        val newTheme = when (radioGroupTheme.checkedRadioButtonId) {
            R.id.radioThemeLight -> THEME_LIGHT
            R.id.radioThemeDark -> THEME_DARK
            else -> THEME_SYSTEM
        }
        
        // Get the current theme to check if it changed
        val currentTheme = sharedPreferences.getString(themePreferenceKey, THEME_SYSTEM)
        
        // Only apply if the theme changed
        if (currentTheme != newTheme) {
            // Save the new theme preference
            sharedPreferences.edit().putString(themePreferenceKey, newTheme).apply()
            
            // Apply the theme change
            when (newTheme) {
                THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            
            // Recreate the activity to apply the theme
            recreate()
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
    
    private fun showError(message: String) {
        txtError.visibility = View.VISIBLE
        txtError.text = message
        txtSuccess.visibility = View.GONE
        
        // Auto-hide error after 3 seconds
        lifecycleScope.launch {
            delay(3000)
            txtError.visibility = View.GONE
        }
    }
    
    private fun showSuccess(message: String) {
        txtSuccess.visibility = View.VISIBLE
        txtSuccess.text = message
        txtError.visibility = View.GONE
        
        // Auto-hide success message after 3 seconds
        lifecycleScope.launch {
            delay(3000)
            txtSuccess.visibility = View.GONE
        }
    }
} 