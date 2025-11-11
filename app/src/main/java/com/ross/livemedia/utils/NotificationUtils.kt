package com.ross.livemedia.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import com.ross.livemedia.media.MusicState

fun buildBaseProgressStyle(duration: Long, position: Long): NotificationCompat.ProgressStyle {
    val playedColor = Color.valueOf(236f / 255f, 183f / 255f, 255f / 255f, 1f).toArgb()
    val remainingColor = Color.LTGRAY
    val pointColor = Color.GRAY

    val clampedPosition = position.coerceIn(0, duration)
    val playedPercent =
        if (duration > 0) ((clampedPosition.toFloat() / duration) * 100).coerceIn(
            0f,
            100f
        ) else 0f
    val remainingPercent = 100 - playedPercent

    val segments = listOf(
        NotificationCompat.ProgressStyle.Segment(playedPercent.toInt()).setColor(playedColor),
        NotificationCompat.ProgressStyle.Segment(remainingPercent.toInt())
            .setColor(remainingColor)
    )

    val points = listOf(
        NotificationCompat.ProgressStyle.Point(playedPercent.toInt()).setColor(pointColor)
    )

    return NotificationCompat.ProgressStyle()
        .setProgressSegments(segments)
        .setProgressPoints(points)
}

fun buildBaseBigTextStyle(musicState: MusicState) = NotificationCompat.BigTextStyle()
    .setBigContentTitle(musicState.title)
//    .bigText(buildArtisAlbumTitle(musicState))

fun <T> createAction(
    icon: Int,
    title: String,
    action: String,
    requestCode: Int,
    packageContext: Context,
    cls: Class<T>,
): NotificationCompat.Action {
    val intent = Intent(packageContext, cls).setAction(action)
    val pendingIntent = PendingIntent.getService(
        packageContext,
        requestCode,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    return NotificationCompat.Action(icon, title, pendingIntent)
}