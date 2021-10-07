package xyz.aprildown.tools.helper

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * This file is just for backward-compatibility.
 */

val Context.safeSharedPreference: SharedPreferences
    get() = PreferenceManager.getDefaultSharedPreferences(this)
