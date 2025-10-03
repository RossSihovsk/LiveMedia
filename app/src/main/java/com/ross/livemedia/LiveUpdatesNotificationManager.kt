//package com.ross.livemedia
//
//import android.app.Activity
//import android.app.Notification
//import android.app.Notification.ProgressStyle
//import android.app.Notification.ProgressStyle.Point
//import android.app.Notification.ProgressStyle.Segment
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.NotificationManager.IMPORTANCE_DEFAULT
//import android.content.Context
//import android.content.Intent
//import android.graphics.Color
//import android.os.Build
//import android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT
//import com.ross.livemedia.OrderState.*
//import android.util.Log
//import androidx.annotation.RequiresApi
//import androidx.core.graphics.drawable.IconCompat
//import androidx.core.net.toUri
//import com.ross.livemedia.OrderState
//
//object LiveUpdatesNotificationManager {
//    private lateinit var notificationManager: NotificationManager
//    private lateinit var appContext: Context
//    const val CHANNEL_ID = "live_updates_16_channel_id"
//    private const val CHANNEL_NAME = "live_updates_16_channel_name"
//    private const val NOTIFICATION_ID = 4321
//
//
//    fun initialize(context: Context, notifManager: NotificationManager) {
//        notificationManager = notifManager
//        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, IMPORTANCE_DEFAULT)
//        appContext = context
//        notificationManager.createNotificationChannel(channel)
//    }
//
//    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
//    fun checkInitialization(context: Context) {
//        val canPostLiveUpdates = notificationManager.canPostPromotedNotifications()
//        if (!canPostLiveUpdates) {
//            val intent = Intent(ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).setData(
//                "package:${appContext.packageName}".toUri(),
//            )
//            (context as Activity).startActivityForResult(intent, 0)
//        }
//    }
//
//    fun buildBaseNotification(
//        appContext: Context,
//    ): Notification.Builder {
//        return Notification.Builder(appContext, CHANNEL_ID)
//            .setSmallIcon(R.drawable.ic_launcher_foreground)
//            .setOngoing(true)
//            .setColorized(true)
//    }
//
//    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
//    fun buildBaseProgressStyle(orderState: OrderState): ProgressStyle {
//        val progressStyle = ProgressStyle()
//            .setProgressPoints(
//                listOf(
//                    Point(25).setColor(Color.MAGENTA),
//                    Point(50).setColor(Color.RED),
//                    Point(75).setColor(Color.LTGRAY),
//                    Point(100).setColor(Color.GRAY)
//                )
//            ).setProgressSegments(
//                listOf(
//                    Segment(25).setColor(Color.MAGENTA),
//                    Segment(25).setColor(Color.RED),
//                    Segment(25).setColor(Color.LTGRAY),
//                    Segment(25).setColor(Color.GRAY)
//
//                )
//            )
//        when (orderState) {
//
//            Confirmed -> {}
//            Preparing -> {}
//
//            Enroute -> {
//                progressStyle.setProgressPoints(
//                    listOf(
//                        Point(25).setColor(Color.MAGENTA),
//                    ),
//                )
//            }
//
//            Arriving -> {
//                progressStyle.setProgressPoints(
//                    listOf(
//                        Point(25).setColor(Color.MAGENTA),
//                        Point(50).setColor(Color.RED),
//                    ),
//                )
//            }
//
//            Delivered -> {
//                progressStyle.setProgressPoints(
//                    listOf(
//                        Point(25).setColor(Color.MAGENTA),
//                        Point(50).setColor(Color.RED),
//                        Point(75).setColor(Color.LTGRAY),
//                    ),
//                )
//            }
//
//
//        }
//        return progressStyle
//    }
//
//    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
//    fun updateNotificationStatus(orderState: OrderState) {
//        val notificationBuilder = when (orderState) {
//            Confirmed -> {
//                buildBaseNotification(appContext)
//                    .setContentTitle("You order is being placed")
//                    .setContentText("Confirming with Spicy7...")
//                    .setStyle(buildBaseProgressStyle(orderState)/*.setProgressIndeterminate(true)*/)
////                Chip status - 7 char limit
//                    .setShortCriticalText("Spot").setWhen(System.currentTimeMillis() + ( 1000L)) // 5 minutes from now
//                    .setShowWhen(true)
//            }
//
//            Preparing -> {
//                buildBaseNotification(
//                    appContext
//                )
//                    .setContentTitle("Your order is being prepared")
//                    .setContentText("Next step will be delivery")
//                    .setLargeIcon(
//                        IconCompat.createWithResource(
//                            appContext, R.drawable.ic_launcher_background,
//                        ).toIcon(appContext),
//                    )
//                    .setStyle(buildBaseProgressStyle(orderState).setProgress(25))
//            }
//
//            Enroute -> {
//                buildBaseNotification(appContext)
//                    .setContentTitle("Your order is on its way")
//                    .setContentText("Enroute to destination")
//                    .setStyle(
//                        buildBaseProgressStyle(orderState)
//                            .setProgressTrackerIcon(
//                                IconCompat.createWithResource(
//                                    appContext, R.drawable.ic_launcher_background,
//                                ).toIcon(appContext),
//                            )
//                            .setProgress(50),
//                    )
//                    .setLargeIcon(
//                        IconCompat.createWithResource(
//                            appContext,
//                            R.drawable.ic_launcher_background,
//                        )
//                            .toIcon(appContext),
//                    )
//            }
//
//
//            Arriving -> {
//                buildBaseNotification(
//                    appContext
//                )
//                    .setContentTitle("Your order is arriving...")
//                    .setContentText("Enjoy 😋")
//                    .setStyle(
//                        buildBaseProgressStyle(orderState)
//                            .setProgressTrackerIcon(
//                                IconCompat.createWithResource(
//                                    appContext, R.drawable.ic_launcher_background,
//                                ).toIcon(appContext),
//                            )
//                            .setProgress(75),
//                    )
//                    .setLargeIcon(
//                        IconCompat.createWithResource(
//                            appContext, R.drawable.ic_launcher_background,
//                        ).toIcon(appContext),
//                    )
//            }
//
//            Delivered -> {
//                buildBaseNotification(appContext)
//                    .setContentTitle("Your order is complete.")
//                    .setContentText("Thank you for using our app.")
//                    .setStyle(
//                        buildBaseProgressStyle(orderState)
//                            .setProgressTrackerIcon(
//                                IconCompat.createWithResource(
//                                    appContext, R.drawable.ic_launcher_background,
//                                ).toIcon(appContext),
//                            )
//                            .setProgress(100),
//                    )
//                    .setLargeIcon(
//                        IconCompat.createWithResource(
//                            appContext, R.drawable.ic_launcher_foreground,
//                        ).toIcon(appContext),
//                    )
//
//            }
//        }
//        val notification = notificationBuilder.build()
//        notificationManager.notify(NOTIFICATION_ID, notification)
//    }
//
//    private const val TAG = "LiveUpdatesNotificationManager"
//}