package com.qifan.pixelcallanimation

import android.util.Log

fun Any.debug(
    message: String,
    vararg args: String
) {
    Log.d(this::class.java.simpleName, message.format(args))
}


inline infix fun <T> T?.guard(block: () -> Nothing): T {
    return this ?: block()
}