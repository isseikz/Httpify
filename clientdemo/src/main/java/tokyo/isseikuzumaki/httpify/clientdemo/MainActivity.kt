package tokyo.isseikuzumaki.httpify.clientdemo

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tokyo.isseikuzumaki.httpify.client.clients.BleClient
import tokyo.isseikuzumaki.httpify.clientdemo.ui.theme.HttpifyTheme
import tokyo.isseikuzumaki.httpify.core.Request

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HttpifyTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    ClientTouchPoint(context = this)
                }
            }
        }
    }
}

@Composable
fun ClientTouchPoint(context: Activity?) {
    Column {
        Text(text = "Client")
        Row {
            Button(onClick = {
                if (context == null) return@Button

                CoroutineScope(Dispatchers.IO).launch {
                    val client = BleClient(context)
                    val buf = client.get(Request())
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, String(buf.payload), Toast.LENGTH_LONG).show()
                    }
                }
            }) {
                Text(text = "Read")
            }

            Button(onClick = {
                if (context == null) return@Button

                CoroutineScope(Dispatchers.IO).launch {
                    val client = BleClient(context)
                    val buf = client.post(Request(payload = "Taro"))
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, String(buf.payload), Toast.LENGTH_LONG).show()
                    }
                }
            }) {
                Text(text = "Write")
            }

            Button(onClick = {
                if (context == null) return@Button

                CoroutineScope(Dispatchers.IO).launch {
                    val client = BleClient(context)
                    val buf = client.post(Request(payload = "Taro"))
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, String(buf.payload), Toast.LENGTH_LONG).show()
                    }
                }
            }) {
                Text(text = "Permission")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    HttpifyTheme {
        ClientTouchPoint(context = null)
    }
}
