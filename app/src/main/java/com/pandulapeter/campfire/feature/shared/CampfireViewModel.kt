package com.pandulapeter.campfire.feature.shared

import android.arch.lifecycle.ViewModel

abstract class CampfireViewModel : ViewModel() {

    open fun subscribe() = Unit

    open fun unsubscribe() = Unit
}