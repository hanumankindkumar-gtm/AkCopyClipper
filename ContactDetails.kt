package com.akprojects.copyclipper.data

data class ContactDetails(
    val email: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val city: String = "",
    val zipCode: String = "",
    val country: String = "United States"
)