package tokyo.isseikuzumaki.httpify.client.clients

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.httpify.core.Request
import tokyo.isseikuzumaki.httpify.core.Response
import tokyo.isseikuzumaki.httpify.client.ClientBase
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BleClient(context: Context): ClientBase<ByteArray> {
    companion object {
        const val TAG = "BleClient"
    }
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val resumeScope = CoroutineScope(Dispatchers.Main)
    private val communication = BleCommunicationProcess.Builder().build(context)

    override suspend fun get(request: Request): Response<ByteArray> = suspendCoroutine { continuation ->
        coroutineScope.launch {
            communication.connect()
            val response = communication.read()
            communication.disconnect()
            resumeScope.launch {
                continuation.resume(response)
            }
        }
    }

    override suspend fun post(request: Request): Response<ByteArray> = suspendCoroutine { continuation ->
        coroutineScope.launch {
            communication.connect()
            val response = communication.write(request.payload?.toByteArray()?: ByteArray(0))
            communication.disconnect()
            resumeScope.launch {
                continuation.resume(response)
            }
        }
    }
}
