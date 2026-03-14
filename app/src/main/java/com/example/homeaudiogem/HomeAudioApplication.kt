package com.example.homeaudiogem

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.example.homeaudiogem.activities.SettingsActivity

class HomeAudioApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Set the theme based on saved preferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val themePreferenceKey = getString(R.string.theme_preference_key)
        val savedTheme = sharedPreferences.getString(themePreferenceKey, SettingsActivity.THEME_SYSTEM)
        
        when (savedTheme) {
            SettingsActivity.THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            SettingsActivity.THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
} 