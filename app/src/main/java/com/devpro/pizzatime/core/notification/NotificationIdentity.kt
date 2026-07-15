package com.devpro.pizzatime.core.notification

import java.security.MessageDigest

internal fun systemNotificationId(
    scope: NotificationScope,
    dedupeKey: String,
): Int {
    val digest = notificationIdentityDigest(scope, dedupeKey)
    val raw = ((digest[0].toInt() and 0x7f) shl 24) or
        ((digest[1].toInt() and 0xff) shl 16) or
        ((digest[2].toInt() and 0xff) shl 8) or
        (digest[3].toInt() and 0xff)
    return raw.takeIf { it != 0 } ?: 1
}

internal fun systemNotificationGroupKey(scope: NotificationScope): String {
    val digest = notificationIdentityDigest(scope, "group")
    return "pizzatime.${scope.role.name.lowercase()}.${digest.toHex(length = 8)}"
}

internal fun systemNotificationIntentToken(
    scope: NotificationScope,
    dedupeKey: String,
): String {
    return notificationIdentityDigest(scope, dedupeKey).toHex(length = 16)
}

private fun ByteArray.toHex(length: Int): String {
    return take(length).joinToString(separator = "") { byte ->
        "%02x".format(byte.toInt() and 0xff)
    }
}

private fun notificationIdentityDigest(
    scope: NotificationScope,
    value: String,
): ByteArray {
    val identity = "${scope.applicationId}|${scope.userId}|${scope.role.name}|$value"
    return MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
}
