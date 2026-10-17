package me.rerere.rikkahub.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import me.rerere.rikkahub.R
import me.rerere.rikkahub.RouteActivity
import me.rerere.rikkahub.SUB_AGENT_NOTIFICATION_CHANNEL_ID
import me.rerere.rikkahub.SUB_AGENT_NOTIFICATION_ID
import me.rerere.rikkahub.data.ai.subagent.SubAgentManager
import me.rerere.rikkahub.data.model.SubAgentStatus
import org.koin.java.KoinJavaComponent.getKoin

/**
 * 子 Agent 前台服务：有子 Agent 运行时保持进程存活（保活），
 * 并展示「N 个子 Agent 运行中」的常驻通知。
 */
class SubAgentForegroundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                SUB_AGENT_NOTIFICATION_ID,
                buildNotification(this),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(SUB_AGENT_NOTIFICATION_ID, buildNotification(this))
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        /** 启动前台服务（保活） */
        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, SubAgentForegroundService::class.java),
            )
        }

        /** 停止前台服务 */
        fun stop(context: Context) {
            context.stopService(Intent(context, SubAgentForegroundService::class.java))
        }

        /** 刷新常驻通知内容（任务列表变化时调用） */
        fun refresh(context: Context) {
            NotificationManagerCompat.from(context)
                .notify(SUB_AGENT_NOTIFICATION_ID, buildNotification(context))
        }

        private fun buildNotification(context: Context): Notification {
            val manager: SubAgentManager = getKoin().get()
            val running = manager.tasks.value.filter { it.status == SubAgentStatus.RUNNING }
            val names = running.map { it.name }
            val title = context.getString(R.string.sub_agent_notification_title, running.size)
            val text = names.joinToString("、")
                .ifEmpty { context.getString(R.string.sub_agent_notification_empty) }
            return NotificationCompat.Builder(context, SUB_AGENT_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_rikkahub)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        0,
                        Intent(context, RouteActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                )
                .build()
        }
    }
}