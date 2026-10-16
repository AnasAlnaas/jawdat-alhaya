package ly.jawdat.alhaya.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import android.app.Notification
import ly.jawdat.alhaya.MainActivity
import ly.jawdat.alhaya.R
import ly.jawdat.alhaya.data.BatchStatus
import ly.jawdat.alhaya.data.Prefs
import ly.jawdat.alhaya.data.Repo
import ly.jawdat.alhaya.data.StageStatus
import ly.jawdat.alhaya.ui.label
import ly.jawdat.alhaya.ui.t
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * One alarm a day (between [Prefs.reminderHour] and an hour later) checks every production stage. Each unfinished stage
 * whose "check on" date has come gets a notification, and keeps getting one each day until it is
 * marked done or its date is moved. No exact-alarm permission is needed for this.
 */
object Reminders {
    const val CHANNEL = "stages"

    fun createChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, t("مراحل الإنتاج", "Production stages"), NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun schedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(Prefs.reminderHour, 0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // A one-hour window keeps it near the chosen hour (an inexact repeating alarm may slip by
        // up to 18 hours). It fires once; the receiver sets the next day's alarm.
        am.setWindow(AlarmManager.RTC_WAKEUP, millis, 60 * 60 * 1000L, alarmIntent(context))
    }

    private fun alarmIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, 0, Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** Stages that need attention today: their check date is today or already passed. */
    fun dueStages(today: LocalDate = LocalDate.now()) =
        Repo.batchSummaries()
            .filter { it.batch.status == BatchStatus.ACTIVE }
            .flatMap { s -> s.stages.map { s.batch to it } }
            .filter { (_, st) ->
                st.remind && st.checkDate != null && !st.checkDate.isAfter(today) &&
                    (st.status == StageStatus.ACTIVE || st.status == StageStatus.PENDING)
            }

    fun notifyDue(context: Context) {
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val nm = context.getSystemService(NotificationManager::class.java)
        dueStages().forEach { (batch, stage) ->
            val open = PendingIntent.getActivity(
                context, stage.id.toInt(),
                Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_BATCH, batch.id)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val title = batch.label + ": " + stage.name
            val text = t("حان وقت متابعة هذه المرحلة", "Time to check on this stage")
            val n = Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            nm.notify(stage.id.toInt(), n)
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.notifyDue(context)
        Reminders.schedule(context)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.schedule(context)
    }
}
