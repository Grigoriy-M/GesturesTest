package com.example.gesturestest.util

import android.util.Log
import java.lang.StringBuilder

const val LOG_TAG = "----"

fun String.toLog() {
    Log.d(LOG_TAG, this)
}

fun Any?.toLog() {
    this.info().toLog()
}

fun Any?.toLog(vararg msg: Any?) {
    val sb = StringBuilder()
    msg.forEach { sb.append(it).append(" ") }
    sb.toString().toLog()
}

fun Any?.getClassName(): String {
    if (this == null) return "null"
    val name = this::class.simpleName
    return if (toString().contains("@")) {
        "$name@${toString().substringAfterLast("@")}"
    } else {
        toString()
    }
}

fun Any?.getClassId(): String {
    if (this == null) return "null"
    return toString().substringAfterLast("@")
}

fun Any?.toInfo(): String = info()

private fun Any?.info(): String {
    if (this == null) return "null"
    return "${getClassName()} ${Thread.currentThread().stackTrace[4].methodName}"
}

