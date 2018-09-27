package com.pandulapeter.campfire.feature.main.home.onboarding.page.songAppearance

import android.databinding.ObservableBoolean
import com.pandulapeter.campfire.data.persistence.PreferenceDatabase
import com.pandulapeter.campfire.feature.shared.CampfireViewModel
import com.pandulapeter.campfire.util.generateNotationExample
import com.pandulapeter.campfire.util.onPropertyChanged

class SongAppearanceViewModel(private val preferenceDatabase: PreferenceDatabase) : CampfireViewModel() {
    private var areListenersSet = false
    val englishNotationExample = generateNotationExample(false)
    val germanNotationExample = generateNotationExample(true)
    val isFirstOptionSelected = ObservableBoolean()
    val isSecondOptionSelected = ObservableBoolean()
    val isThirdOptionSelected = ObservableBoolean()

    fun initialize() {
        isFirstOptionSelected.set(preferenceDatabase.shouldShowChords && !preferenceDatabase.shouldUseGermanNotation)
        isSecondOptionSelected.set(preferenceDatabase.shouldShowChords && preferenceDatabase.shouldUseGermanNotation)
        isThirdOptionSelected.set(!preferenceDatabase.shouldShowChords)
        if (!areListenersSet) {
            areListenersSet = true
            isFirstOptionSelected.onPropertyChanged {
                if (it) {
                    isSecondOptionSelected.set(false)
                    isThirdOptionSelected.set(false)
                    preferenceDatabase.shouldShowChords = true
                    preferenceDatabase.shouldUseGermanNotation = false
                }
            }
            isSecondOptionSelected.onPropertyChanged {
                if (it) {
                    isFirstOptionSelected.set(false)
                    isThirdOptionSelected.set(false)
                    preferenceDatabase.shouldShowChords = true
                    preferenceDatabase.shouldUseGermanNotation = true
                }
            }
            isThirdOptionSelected.onPropertyChanged {
                if (it) {
                    isFirstOptionSelected.set(false)
                    isSecondOptionSelected.set(false)
                    preferenceDatabase.shouldShowChords = false
                }
            }
        }
    }
}