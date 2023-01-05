package tokyo.isseikuzumaki.httpify.client

import tokyo.isseikuzumaki.httpify.core.Request
import tokyo.isseikuzumaki.httpify.core.Response

interface ClientBase<S> {
    suspend fun get(request: Request): Response<S>
    suspend fun post(request: Request): Response<S>
}
