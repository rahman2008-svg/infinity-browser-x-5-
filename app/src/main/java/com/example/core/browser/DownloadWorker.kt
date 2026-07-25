package com.example.core.browser

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.core.data.db.BrowserDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * DownloadWorker.kt
 * 
 * WorkManager ভিত্তিক ব্যাকগ্রাউন্ড/ফোরগ্রাউন্ড ডাউনলোড সার্ভিস।
 * ফাইল ডাউনলোড, নোটিফিকেশন প্রোগ্রেস আপডেট, এবং MIME টাইপ অনুযায়ী ফোল্ডারে সেভ করে।
 */
class DownloadWorker(
    private val context: Context,
    private val workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = BrowserDatabase.getDatabase(context)
    private val downloadDao = db.downloadDao()
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val KEY_DOWNLOAD_ID = "KEY_DOWNLOAD_ID"
        const val KEY_URL = "KEY_URL"
        const val KEY_FILE_NAME = "KEY_FILE_NAME"
        const val KEY_MIME_TYPE = "KEY_MIME_TYPE"
        const val CHANNEL_ID = "infinity_downloads_channel"
        private const val NOTIFICATION_ID_BASE = 1000
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadId = inputData.getLong(KEY_DOWNLOAD_ID, -1)
        val urlStr = inputData.getString(KEY_URL) ?: return@withContext Result.failure()
        val fileName = inputData.getString(KEY_FILE_NAME) ?: "downloaded_file"
        val mimeType = inputData.getString(KEY_MIME_TYPE) ?: "*/*"

        if (downloadId == -1L) return@withContext Result.failure()

        createNotificationChannel()

        val notifId = (NOTIFICATION_ID_BASE + downloadId).toInt()
        setForeground(createForegroundInfo(notifId, fileName, 0, true))
        downloadDao.updateProgressAndStatus(downloadId, "DOWNLOADING", 0)

        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlStr)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                downloadDao.updateProgressAndStatus(downloadId, "FAILED", 0)
                showCompletionNotification(notifId, fileName, false, null, mimeType)
                return@withContext Result.failure()
            }

            val fileLength = connection.contentLengthLong

            // Determine target directory inside public Downloads folder
            val subFolder = getSubfolderByMimeType(mimeType, fileName)
            val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val targetDir = File(baseDir, subFolder)
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val targetFile = File(targetDir, getUniqueFileName(targetDir, fileName))
            val outputStream = FileOutputStream(targetFile)
            val inputStream = connection.inputStream

            val buffer = ByteArray(4096)
            var totalBytesRead = 0L
            var bytesRead: Int
            var lastProgress = 0

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (isStopped) {
                    outputStream.close()
                    inputStream.close()
                    targetFile.delete()
                    downloadDao.updateProgressAndStatus(downloadId, "PAUSED", lastProgress)
                    return@withContext Result.success()
                }

                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                if (fileLength > 0) {
                    val progress = ((totalBytesRead * 100) / fileLength).toInt()
                    if (progress - lastProgress >= 5 || progress == 100) {
                        lastProgress = progress
                        downloadDao.updateProgressAndStatus(downloadId, "DOWNLOADING", progress, targetFile.absolutePath)
                        notificationManager.notify(notifId, getNotificationBuilder(fileName, progress, true).build())
                    }
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            // Success
            val savedPath = targetFile.absolutePath
            downloadDao.updateProgressAndStatus(downloadId, "COMPLETED", 100, savedPath)
            showCompletionNotification(notifId, fileName, true, savedPath, mimeType)

            Result.success(workDataOf("savedPath" to savedPath))

        } catch (e: Exception) {
            e.printStackTrace()
            downloadDao.updateProgressAndStatus(downloadId, "FAILED", 0)
            showCompletionNotification(notifId, fileName, false, null, mimeType)
            Result.failure()
        } finally {
            connection?.disconnect()
        }
    }

    private fun getSubfolderByMimeType(mimeType: String, fileName: String): String {
        val lowerMime = mimeType.lowercase()
        val lowerName = fileName.lowercase()
        return when {
            lowerMime.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".png") || lowerName.endsWith(".webp") -> "Images"
            lowerMime.startsWith("video/") || lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") -> "Videos"
            lowerMime.contains("pdf") || lowerMime.startsWith("text/") || lowerName.endsWith(".pdf") || lowerName.endsWith(".doc") || lowerName.endsWith(".txt") -> "Documents"
            lowerMime.contains("package-archive") || lowerName.endsWith(".apk") -> "APK"
            else -> "Others"
        }
    }

    private fun getUniqueFileName(dir: File, fileName: String): String {
        var file = File(dir, fileName)
        if (!file.exists()) return fileName

        val nameWithoutExt = file.nameWithoutExtension
        val ext = file.extension
        var counter = 1
        while (file.exists()) {
            val newName = if (ext.isNotEmpty()) "$nameWithoutExt ($counter).$ext" else "$nameWithoutExt ($counter)"
            file = File(dir, newName)
            counter++
        }
        return file.name
    }

    private fun createForegroundInfo(notifId: Int, fileName: String, progress: Int, indeterminate: Boolean): ForegroundInfo {
        val notification = getNotificationBuilder(fileName, progress, indeterminate).build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notifId, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notifId, notification)
        }
    }

    private fun getNotificationBuilder(fileName: String, progress: Int, isDownloading: Boolean): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(if (isDownloading) "Downloading: $fileName" else fileName)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(isDownloading)

        if (isDownloading) {
            builder.setProgress(100, progress, progress == 0)
            builder.setContentText("$progress%")
        }
        return builder
    }

    private fun showCompletionNotification(notifId: Int, fileName: String, success: Boolean, filePath: String?, mimeType: String) {
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(if (success) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
            .setContentTitle(if (success) "Download Completed" else "Download Failed")
            .setContentText(fileName)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (success && filePath != null) {
            try {
                val file = File(filePath)
                val uri = Uri.fromFile(file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val pendingIntent = PendingIntent.getActivity(
                    applicationContext, notifId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.setContentIntent(pendingIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        notificationManager.notify(notifId, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Infinity Browser Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active and completed download notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
