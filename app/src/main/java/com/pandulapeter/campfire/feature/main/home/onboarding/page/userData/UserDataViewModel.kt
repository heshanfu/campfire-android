package com.pandulapeter.campfire.feature.main.home.onboarding.page.userData

import android.databinding.ObservableBoolean
import com.pandulapeter.campfire.data.persistence.PreferenceDatabase
import com.pandulapeter.campfire.feature.shared.CampfireViewModel
import com.pandulapeter.campfire.integration.AnalyticsManager
import com.pandulapeter.campfire.util.onPropertyChanged

class UserDataViewModel(
    private val preferenceDatabase: PreferenceDatabase,
    private val analyticsManager: AnalyticsManager
) : CampfireViewModel() {

    val isAnalyticsEnabled = ObservableBoolean(preferenceDatabase.shouldShareUsageData)
    val isCrashReportingEnabled = ObservableBoolean(preferenceDatabase.shouldShareCrashReports)

    init {
        isAnalyticsEnabled.onPropertyChanged {
            preferenceDatabase.shouldShareUsageData = it
            analyticsManager.updateCollectionEnabledState()
            if (it) {
                analyticsManager.onConsentGiven(System.currentTimeMillis())
            }
        }
        isCrashReportingEnabled.onPropertyChanged { preferenceDatabase.shouldShareCrashReports = it }
    }
}