package com.akprojects.copyclipper.data

import android.content.Context
import androidx.room.*

@Dao
interface PresetDao {
    @Query("SELECT * FROM form_presets ORDER BY createdAt DESC")
    suspend fun getAllPresets(): List<FormPreset>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: FormPreset)

    @Delete
    suspend fun deletePreset(preset: FormPreset)

    @Query("SELECT * FROM form_presets WHERE id = :id LIMIT 1")
    suspend fun getPresetById(id: String): FormPreset?
}

@Database(entities = [FormPreset::class], version = 1, exportSchema = false)
abstract class PresetDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao

    companion object {
        @Volatile
        private var INSTANCE: PresetDatabase? = null

        fun getDatabase(context: Context): PresetDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PresetDatabase::class.java,
                    "copy_clipper_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}