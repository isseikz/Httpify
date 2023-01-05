package tokyo.isseikuzumaki.httpify.serverdemo

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
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
import tokyo.isseikuzumaki.httpify.serverdemo.ui.theme.ServerDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Your device does not support Bluetooth Low Energy", LENGTH_SHORT).show()
            return
        }

        setContent {
            ServerDemoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    ServerController(context = this)
                }
            }
        }
    }
}

@Composable
fun ServerController(context: Activity?) {
    Column {
        Text(text = "Server Controller")

        Row {
            Button(onClick = {
                context?.let {
                    val intent = Intent(context, BleService::class.java)
                    context.startForegroundService(intent)
                }
            }) {
                Text(text = "Start")
            }

            Button(onClick = {
                context?.let {
                    context.stopService(Intent(context, BleService::class.java))
                }
            }) {
                Text(text = "Stop")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ServerDemoTheme {
        ServerController(context = null)
    }
}
