package tokyo.isseikuzumaki.httpify.serverdemo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.httpify.core.Request
import tokyo.isseikuzumaki.httpify.core.Response
import tokyo.isseikuzumaki.httpify.server.RequestReceiver
import tokyo.isseikuzumaki.httpify.server.servers.BleServer

class BleService : Service() {
    companion object {
        const val TAG = "BleService"
        const val CHANNEL_ID = "tokyo.isseikuzumaki.httpify.serverdemo.BleService"
    }
    private lateinit var server: BleServer
    private val mainScope = CoroutineScope(Dispatchers.Main)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        server = BleServer(
            object : RequestReceiver<ByteArray> {
                override suspend fun onGet(request: Request): Response<ByteArray> {
                    return Response(200, "Hello! ${request.payload}".toByteArray())
                }

                override suspend fun onPost(request: Request): Response<ByteArray> {
                    val resp = "Hi! ${request.payload}"
                    mainScope.launch {
                        Toast.makeText(this@BleService, resp, Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "onPost [${request.payload}]")
                    }
                    return Response(200, "Hi! ${request.payload}".toByteArray())
                }
            }
        )

        server.start(this)

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Httpify Server Demo",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val intentToLaunchMain: PendingIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Httpify Demo Service")
            .setContentText("Server is running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(intentToLaunchMain)
            .setTicker("Ticker text")
            .build()
        startForeground(1, notification)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onDestroy() {
        super.onDestroy()
        server.stop(context = this)
    }
}
