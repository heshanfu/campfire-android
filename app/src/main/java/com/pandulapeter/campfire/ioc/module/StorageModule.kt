package com.pandulapeter.campfire.ioc.module

import android.content.Context
import com.google.gson.Gson
import com.pandulapeter.campfire.data.storage.DataStorageManager
import com.pandulapeter.campfire.data.storage.PreferenceStorageManager
import com.pandulapeter.campfire.ioc.app.AppContext
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object StorageModule {

    @Provides
    @Singleton
    @JvmStatic
    fun providePreferenceStorageManager(@AppContext context: Context) = PreferenceStorageManager(context)

    @Provides
    @Singleton
    @JvmStatic
    fun provideDataStorageManager(@AppContext context: Context, gson: Gson) = DataStorageManager(context, gson)
}