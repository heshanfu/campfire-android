package com.pandulapeter.campfire.data.persistence.access

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.pandulapeter.campfire.data.model.local.SongDetailMetadata
import com.pandulapeter.campfire.data.model.remote.Song
import com.pandulapeter.campfire.data.model.remote.SongDetail

@Dao
interface SongDetailDao {

    @Query("SELECT ${Song.ID}, ${SongDetail.VERSION} FROM ${SongDetail.TABLE_NAME}")
    fun getAllMetadata(): List<SongDetailMetadata>

    @Query("SELECT * FROM ${SongDetail.TABLE_NAME} WHERE ${Song.ID} IN(:songId) LIMIT 1")
    fun get(songId: String): SongDetail?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(songDetail: SongDetail)

    @Query("DELETE FROM ${SongDetail.TABLE_NAME} WHERE ${Song.ID} IN(:songId)")
    fun delete(songId: String)

    @Query("DELETE FROM ${SongDetail.TABLE_NAME}")
    fun deleteAll()
}