package xyz.aprildown.timer.flavor.google.utils

internal fun Throwable.causeFirstMessage(): String {
    fun Throwable.getCurrentMessage(): String? {
        return localizedMessage ?: message
    }

    return cause?.getCurrentMessage() ?: getCurrentMessage().toString()
}
