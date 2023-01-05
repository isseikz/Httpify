package tokyo.isseikuzumaki.httpify.server

import tokyo.isseikuzumaki.httpify.core.Request
import tokyo.isseikuzumaki.httpify.core.Response

interface RequestReceiver<T> {
    suspend fun onGet(request: Request): Response<T>
    suspend fun onPost(request: Request): Response<T>
}
