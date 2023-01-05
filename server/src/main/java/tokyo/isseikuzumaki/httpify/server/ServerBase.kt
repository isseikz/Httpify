package tokyo.isseikuzumaki.httpify.server

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import tokyo.isseikuzumaki.httpify.core.Response
import tokyo.isseikuzumaki.httpify.core.Request
import kotlin.coroutines.resume

open class ServerBase<T>(private val receiver: RequestReceiver<T>) {
    private val scope = CoroutineScope(Dispatchers.Main)

    open fun start(context: Context) {}
    open fun stop(context: Context) {}

    internal suspend fun callGet(request: Request) = suspendCancellableCoroutine<Response<T>> { cont ->
        scope.launch {
            val resp = receiver.onGet(request)
            cont.resume(resp)
        }
    }

    internal suspend fun callPost(request: Request): Response<T> = suspendCancellableCoroutine{ cont ->
        scope.launch {
            requireNotNull(request.payload)
            cont.resume(receiver.onPost(request))
        }
    }
}
