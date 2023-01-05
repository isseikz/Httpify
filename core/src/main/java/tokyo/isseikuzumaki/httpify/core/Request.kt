package tokyo.isseikuzumaki.httpify.core


data class Request(
    val params: Map<String, String>? = null,
    val payload: String? = null
)
