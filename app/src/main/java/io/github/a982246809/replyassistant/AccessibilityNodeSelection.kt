package io.github.a982246809.replyassistant

internal fun <T> List<T>?.getOrNullAt(index: Int): T? {
    return this?.getOrNull(index)
}
