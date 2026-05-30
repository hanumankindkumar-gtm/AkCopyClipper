package com.akprojects.copyclipper.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Embedded

@Entity(tableName = "form_presets")
data class FormPreset(
    @PrimaryKey val id: String,
    val presetName: String,
    val iconName: String,
    val colorTheme: String,
    @Embedded val personal: PersonalDetails,
    @Embedded val contact: ContactDetails,
    val customFieldsJson: String, // Stringified custom attributes
    val createdAt: String
)