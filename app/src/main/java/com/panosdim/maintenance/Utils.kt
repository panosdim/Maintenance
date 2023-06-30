package com.panosdim.maintenance

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.pm.PackageInfoCompat
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.panosdim.maintenance.model.FileMetadata
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.Duration

var refId: Long = -1

@Suppress("DEPRECATION")
fun checkForNewVersion(context: Context) {
    val storage = Firebase.storage
    val metadataFileName = "output-metadata.json"
    val apkFileName = "app-release.apk"

    // Create a storage reference from our app
    val storageRef = storage.reference

    // Create a metadata reference
    val metadataRef: StorageReference = storageRef.child(metadataFileName)

    metadataRef.getBytes(Long.MAX_VALUE).addOnSuccessListener {
        // Use the bytes to display the image
        val data = String(it)
        val fileMetadata = Json.decodeFromString<FileMetadata>(data)
        val version = fileMetadata.elements[0].versionCode

        val appVersion = PackageInfoCompat.getLongVersionCode(
            context.packageManager.getPackageInfo(
                context.packageName,
                0
            )
        )

        if (version > appVersion) {
            Toast.makeText(
                context,
                context.getString(R.string.new_version),
                Toast.LENGTH_LONG
            ).show()

            val versionName = fileMetadata.elements[0].versionName

            // Create an apk reference
            val apkRef = storageRef.child(apkFileName)

            apkRef.downloadUrl.addOnSuccessListener { uri ->
                downloadNewVersion(context, uri, versionName)
            }.addOnFailureListener {
                // Handle any errors
                Log.w(TAG, "Fail to download file $apkFileName")
            }
        }
    }.addOnFailureListener {
        // Handle any errors
        Log.w(TAG, "Fail to retrieve $metadataFileName")
    }
}

private fun downloadNewVersion(context: Context, downloadUrl: Uri, version: String) {
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val request =
        DownloadManager.Request(downloadUrl)
    request.setDescription("Downloading new version of Maintenance.")
    request.setTitle("New Maintenance Version: $version")
    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
    request.setDestinationInExternalPublicDir(
        Environment.DIRECTORY_DOWNLOADS,
        "Maintenance-${version}.apk"
    )
    refId = manager.enqueue(request)
}

fun createNotificationChannel(context: Context) {
    // Create the NotificationChannel, but only on API 26+ because
    // the NotificationChannel class is new and not in the support library
    val name = context.getString(R.string.channel_name)
    val importance = NotificationManager.IMPORTANCE_DEFAULT

    val channel = NotificationChannel(CHANNEL_ID, name, importance)
    // Register the channel with the system
    val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
}

fun convertMonthsToYearsAndMonths(months: Int): Pair<Int, Int> {
    val years = months / 12
    val remainingMonths = months % 12
    return Pair(years, remainingMonths)
}

fun getPeriodicity(periodicity: Float, resources: Resources): String {
    val itemPeriodicity = convertMonthsToYearsAndMonths(periodicity.toInt())
    return when (itemPeriodicity.first) {
        0 -> resources.getString(
            R.string.months_periodicity,
            itemPeriodicity.second
        )

        else -> {
            when (itemPeriodicity.second) {
                0 -> resources.getQuantityString(
                    R.plurals.periodicity,
                    itemPeriodicity.first,
                    itemPeriodicity.first
                )

                else -> resources.getQuantityString(
                    R.plurals.periodicity_years_and_months,
                    itemPeriodicity.first,
                    itemPeriodicity.first,
                    itemPeriodicity.second
                )
            }
        }
    }
}

fun formatDuration(duration: Duration, resources: Resources): String {
    val years = duration.toDays() / 365
    val months = (duration.toDays() % 365) / 30
    val days = (duration.toDays() % 365) % 30

    val sb = StringBuilder()
    if (years > 0) sb.append(
        resources.getQuantityString(
            R.plurals.years,
            years.toInt(),
            years.toInt()
        )
    ).append(" ")
    if (months > 0) sb.append(
        resources.getQuantityString(
            R.plurals.months,
            months.toInt(),
            months.toInt()
        )
    ).append(" ")
    if (days > 0) sb.append(
        resources.getQuantityString(
            R.plurals.days,
            days.toInt(),
            days.toInt()
        )
    ).append(" ")

    return sb.toString().trim()
}