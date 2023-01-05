package tokyo.isseikuzumaki.httpify.core

data class Response<S>(
    val status: Int,
    val payload: S
)
