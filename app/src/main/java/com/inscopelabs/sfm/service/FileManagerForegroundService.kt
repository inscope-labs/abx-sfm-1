package com.inscopelabs.sfm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.inscopelabs.sfm.mcp.security.MCPSecurityGuard
import com.inscopelabs.sfm.mcp.server.MCPServer
import com.inscopelabs.sfm.mcp.server.MCPServerConfig
import com.inscopelabs.sfm.security.audit.AuditLogger
import com.inscopelabs.sfm.security.policy.PolicyEngine
import com.inscopelabs.sfm.security.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service for MCP server lifecycle.
 */
class FileManagerForegroundService : Service() {

    private var mcpServer: MCPServer? = null
    private lateinit var sessionManager: SessionManager
    private lateinit var policyEngine: PolicyEngine
    private lateinit var auditLogger: AuditLogger
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        sessionManager = SessionManager()
        auditLogger = AuditLogger()
        policyEngine = PolicyEngine(auditLogger)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startServer()
            ACTION_STOP -> stopServer()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startServer() {
        val notification = createNotification("Starting MCP server...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Initialize MCP server
        mcpServer = MCPServer(
            context = applicationContext,
            config = getServerConfig(),
            sessionManager = sessionManager,
            policyEngine = policyEngine,
            auditLogger = auditLogger,
            securityGuard = MCPSecurityGuard()
        )

        // Start server in background
        serviceScope.launch {
            mcpServer?.start()
            updateNotification("MCP server running")
        }
    }

    private fun stopServer() {
        serviceScope.launch {
            mcpServer?.stop()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "File Manager Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "MCP server status notifications"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(message: String): Notification {
        val stopIntent = Intent(this, FileManagerForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("File Manager")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(message: String) {
        val notification = createNotification(message)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun getServerConfig(): MCPServerConfig {
        return MCPServerConfig(
            serverName = "FileManagerMCP",
            useRelay = false,
            enableAuditLogging = true
        )
    }

    override fun onDestroy() {
        mcpServer?.let {
            serviceScope.launch {
                it.stop()
            }
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "filemanager_service"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_START = "start"
        private const val ACTION_STOP = "stop"

        fun start(context: Context) {
            val intent = Intent(context, FileManagerForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FileManagerForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
