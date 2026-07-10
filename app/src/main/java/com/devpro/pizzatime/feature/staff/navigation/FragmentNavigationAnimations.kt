package com.devpro.pizzatime.feature.staff.navigation

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.devpro.pizzatime.R

enum class NavigationDirection {
    FORWARD,
    BACKWARD,
}

fun FragmentManager.replaceForward(
    containerId: Int,
    fragment: Fragment,
    tag: String? = fragment::class.java.name,
    addToBackStack: Boolean = true,
) {
    if (isStateSaved || isSameDestination(containerId, fragment)) return

    beginTransaction()
        .setReorderingAllowed(true)
        .setCustomAnimations(
            R.anim.slide_in_right,
            R.anim.slide_out_left,
            R.anim.slide_in_left,
            R.anim.slide_out_right,
        )
        .replace(containerId, fragment, tag)
        .apply {
            if (addToBackStack) {
                addToBackStack(tag)
            }
        }
        .commit()
}

fun FragmentManager.replaceWithDirection(
    containerId: Int,
    fragment: Fragment,
    moveForward: Boolean,
    tag: String? = fragment::class.java.name,
) {
    if (isStateSaved || isSameDestination(containerId, fragment)) return

    beginTransaction()
        .setReorderingAllowed(true)
        .apply {
        if (moveForward) {
            setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
        } else {
            setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        }
        .replace(containerId, fragment, tag)
        .commit()
}

fun FragmentManager.replaceWithoutAnimation(
    containerId: Int,
    fragment: Fragment,
    tag: String? = fragment::class.java.name,
    addToBackStack: Boolean = false,
) {
    if (isStateSaved || isSameDestination(containerId, fragment)) return

    beginTransaction()
        .setReorderingAllowed(true)
        .replace(containerId, fragment, tag)
        .apply {
            if (addToBackStack) {
                addToBackStack(tag)
            }
        }
        .commit()
}

private fun FragmentManager.isSameDestination(
    containerId: Int,
    fragment: Fragment,
): Boolean {
    val current = findFragmentById(containerId) ?: return false
    return current::class.java == fragment::class.java &&
        current.arguments.isSameArguments(fragment.arguments)
}

private fun Bundle?.isSameArguments(other: Bundle?): Boolean {
    if (this == null || isEmpty) return other == null || other.isEmpty
    if (other == null || size() != other.size()) return false
    return keySet().all { key -> get(key) == other.get(key) }
}
