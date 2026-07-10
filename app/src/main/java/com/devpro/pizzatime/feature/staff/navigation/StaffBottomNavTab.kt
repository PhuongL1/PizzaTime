package com.devpro.pizzatime.feature.staff.navigation

enum class StaffBottomNavTab {
    DASHBOARD,
    KITCHEN,
    DELIVERY,
    PROFILE,
}

fun StaffBottomNavTab.directionTo(target: StaffBottomNavTab): NavigationDirection {
    return if (target.ordinal > ordinal) NavigationDirection.FORWARD else NavigationDirection.BACKWARD
}
