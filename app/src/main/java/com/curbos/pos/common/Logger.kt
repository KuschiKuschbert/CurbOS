package com.curbos.pos.common

import android.util.Log

object Logger {
    private const val GLOBAL_TAG_PREFIX = "CurbOS_"

    private fun formatTag(tag: String): String {
        // Truncate if necessary (Android legacy limit is 23 chars, though less strict now)
        val fullTag = "$GLOBAL_TAG_PREFIX$tag"
        return if (fullTag.length > 23) fullTag.substring(0, 23) else fullTag
    }

    fun d(tag: String, message: String) {
        if (com.curbos.pos.BuildConfig.DEBUG) {
            Log.d(formatTag(tag), message)
        }
    }

    fun i(tag: String, message: String) {
        Log.i(formatTag(tag), message)
    }

    fun w(tag: String, message: String) {
        Log.w(formatTag(tag), message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(formatTag(tag), message, throwable)
        } else {
            Log.e(formatTag(tag), message)
        }
    }
}
