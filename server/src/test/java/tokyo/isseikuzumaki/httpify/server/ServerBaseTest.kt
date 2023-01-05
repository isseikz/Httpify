package tokyo.isseikuzumaki.httpify.server

import kotlinx.coroutines.*
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import tokyo.isseikuzumaki.httpify.core.Request
import tokyo.isseikuzumaki.httpify.core.Response

@RunWith(JUnit4::class)
class ServerBaseTest {
    @OptIn(DelicateCoroutinesApi::class)
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset the main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    @Test
    fun notify_() {
        var receivedCnt = 0
        var receivedValue = "Not received"
        val requestReceiver = object : RequestReceiver<String> {
            override suspend fun onGet(request: Request): Response<String> {
                receivedCnt += 1
                receivedValue = request.payload!!
                return Response(status = 200, payload = receivedValue)
            }

            override suspend fun onPost(request: Request): Response<String> {
                TODO("Not yet implemented")
            }
        }
        val target = ServerBase(requestReceiver)

        var cnt = 0
        val test = { value: String ->
            cnt += 1
            runBlocking {
                launch(Dispatchers.Main) {
                    target.callGet(Request(payload = value))
                }
            }
            assert(cnt == receivedCnt && value == receivedValue)
        }

        test("test")
        test("test") // notification with same value is received
        test("testtest") // notification with the other value is received
    }
}
