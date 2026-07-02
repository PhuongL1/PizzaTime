package com.devpro.pizzatime.core.session

enum class UserRole {
    GUEST,
    CUSTOMER,
    STAFF,
    KITCHEN,
    SHIPPER,
    ADMIN;

    companion object {
        fun fromString(value: String?): UserRole? = when (value?.uppercase()) {
            "CUSTOMER" -> CUSTOMER
            "STAFF" -> STAFF
            "KITCHEN" -> KITCHEN
            "SHIPPER" -> SHIPPER
            "ADMIN" -> ADMIN
            else -> null
        }
    }
}