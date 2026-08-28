package com.wangxq.consumable

import android.app.Application
import android.util.Log
import com.wangxq.consumable.data.AppDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConsumableApplication : Application() {
    val database by lazy { AppDatabase.build(this) }

    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
    }

    /** 把未捕获异常写到应用专属外部目录，便于不连电脑时定位闪退 */
    private fun setupCrashHandler() {
        val dir = getExternalFilesDir(null) ?: filesDir
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val f = File(dir, "crash_log.txt")
                val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(Date())
                f.appendText("==== crash @ $time ====\n")
                f.appendText(Log.getStackTraceString(throwable))
                f.appendText("\n")
            } catch (_: Exception) {
                // 忽略写日志本身的异常
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
