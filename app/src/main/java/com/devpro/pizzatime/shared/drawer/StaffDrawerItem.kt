package com.devpro.pizzatime.shared.drawer

enum class StaffDrawerItem {
    ORDER_HISTORY,
    INVENTORY,
    STAFF_SCHEDULE,
    SUPPORT,
    LOGOUT,
    ;

    companion object {
        fun fromAction(action: String?): StaffDrawerItem? {
            return entries.firstOrNull { item ->
                item.name == action
            }
        }
    }
}