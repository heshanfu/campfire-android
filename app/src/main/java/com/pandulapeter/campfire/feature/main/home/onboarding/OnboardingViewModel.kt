package com.pandulapeter.campfire.feature.main.home.onboarding

import android.databinding.ObservableBoolean
import android.databinding.ObservableFloat
import com.pandulapeter.campfire.feature.shared.CampfireViewModel

class OnboardingViewModel : CampfireViewModel() {

    val doneButtonOffset = ObservableFloat()
    val canSkip = ObservableBoolean()
    val shouldShowLegalDocuments = ObservableBoolean()
    lateinit var skip: () -> Unit
    lateinit var navigateToNextPage: () -> Unit

    fun onSkipButtonClicked() = skip()

    fun onNextButtonClicked() = navigateToNextPage()

    fun onLegalDocumentsClicked() = shouldShowLegalDocuments.set(true)
}