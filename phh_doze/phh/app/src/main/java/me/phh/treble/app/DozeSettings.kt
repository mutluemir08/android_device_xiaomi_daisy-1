package me.phh.treble.app

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.os.UserHandle
import android.preference.PreferenceCategory
import android.preference.PreferenceFragment
import android.preference.SwitchPreference
import android.provider.Settings
import com.android.internal.hardware.AmbientDisplayConfiguration
import me.phh.treble.app.DozeSettings.handwaveKey
import me.phh.treble.app.DozeSettings.pocketKey
import me.phh.treble.app.DozeSettings.proxyKey

object DozeSettings {
    val handwaveKey = "key_doze_handwave"
    val pocketKey = "key_doze_pocket"
    val proxyKey = "proximity_sensor"

    fun enabled(): Boolean {
        //TODO: Check if sensors are available and respond to interrupts
        return true
    }
}

class DozeSettingsFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.pref_doze)
        
        val dozeEnabled = isDozeEnabled(getActivity())

        val proximitySensorCategory = getPreferenceScreen().findPreference(proxyKey) as PreferenceCategory

        val mHandwavePreference = findPreference(handwaveKey) as SwitchPreference
        mHandwavePreference.setEnabled(dozeEnabled)

        val mPocketPreference = findPreference(pocketKey) as SwitchPreference
        mPocketPreference.setEnabled(dozeEnabled)

        // Hide proximity sensor related features if the device doesn't support them
        if (!getProxCheckBeforePulse(getActivity())) {
            getPreferenceScreen().removePreference(proximitySensorCategory)
        }

        // Hide AOD if not supported and set all its dependents otherwise
        if (!alwaysOnDisplayAvailable(getActivity())) {
            getPreferenceScreen().removePreference(proximitySensorCategory)
        } else {
            proximitySensorCategory.setEnabled(!isAlwaysOnEnabled(getActivity()))
        }
    }

    private fun isDozeEnabled(context: Context): Boolean {
        return Settings.Secure.getInt(context.contentResolver,
                android.provider.Settings.Secure.DOZE_ENABLED, 1) != 0
    }

    internal fun getProxCheckBeforePulse(context: Context): Boolean {
        try {
            val con = context.createPackageContext("com.android.systemui", 0)
            val id = con.resources.getIdentifier("doze_proximity_check_before_pulse",
                    "bool", "com.android.systemui")
            return con.resources.getBoolean(id)
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }
    }

    internal fun alwaysOnDisplayAvailable(context: Context): Boolean {
        return AmbientDisplayConfiguration(context).alwaysOnAvailable()
    }

    internal fun isAlwaysOnEnabled(context: Context): Boolean {
        val enabledByDefault = context.resources
                .getBoolean(Resources.getSystem().getIdentifier("config_dozeAlwaysOnEnabled", "bool", "android"))

        return Settings.Secure.getIntForUser(context.contentResolver,
                android.provider.Settings.Secure.DOZE_ALWAYS_ON,
                if (alwaysOnDisplayAvailable(context) && enabledByDefault) 1 else 0,
                UserHandle.USER_CURRENT) !== 0
    }
}
