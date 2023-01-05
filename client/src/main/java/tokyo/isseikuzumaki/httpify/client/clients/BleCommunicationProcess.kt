package tokyo.isseikuzumaki.httpify.client.clients

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import tokyo.isseikuzumaki.httpify.core.Constants
import tokyo.isseikuzumaki.httpify.core.Response
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BleCommunicationProcess(
    private val context: Context,
    private val adapter: BluetoothAdapter,
    private val filter: List<ScanFilter>,
    private val settings: ScanSettings
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val callbackScope = CoroutineScope(Dispatchers.Default)
    private val gattCallback = object : BluetoothGattCallback() {
        var onConnected: ((BluetoothGatt) -> Unit)? = null
        var onService: ((Connection) -> Unit)? = null
        var onReadData: ((BluetoothResponse) -> Unit)? = null
        var onWriteData: ((BluetoothResponse) -> Unit)? = null

        override fun onConnectionStateChange(btGatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    btGatt?.let { onConnected?.invoke(it) }
                }
                BluetoothGatt.STATE_DISCONNECTING, BluetoothGatt.STATE_DISCONNECTED -> {
                    connection = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            gatt?.services?.first()?.let {
                connection = Connection(gatt, it).also { conn ->
                    onService?.invoke(conn)
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            characteristic?.let { onReadData?.invoke(BluetoothResponse(status, it)) }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            characteristic?.let { onWriteData?.invoke(BluetoothResponse(status, it)) }
        }
    }
    private var connection: Connection? = null
    private var currentScanCallback: ScanCallback? = null

    suspend fun connect() = suspendCancellableCoroutine<Connection> { cont ->
        coroutineScope.launch {
            cont.resume(
                findService( findDevice() )
            )
        }
    }

    fun disconnect() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            connection?.gatt?.disconnect()
        }
    }

    suspend fun read() = sendCharacteristic(null)

    suspend fun write(data: ByteArray) = sendCharacteristic(data)

    private suspend fun sendCharacteristic(data: ByteArray?) = suspendCancellableCoroutine<Response<ByteArray>> { cont ->
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            cont.resumeWithException(SecurityException())
        }

        val returnResponse = { response: BluetoothResponse ->
            val httpStatus = if (response.status == BluetoothGatt.GATT_SUCCESS && response.characteristic.value != null) {
                200
            } else {
                500
            }
            callbackScope.launch {
                cont.resume(Response(httpStatus, response.characteristic.value))
                response.characteristic.value = ByteArray(0)
            }
        }

        val useWrite = data != null
        if (useWrite) {
            gattCallback.onWriteData = { response ->
                returnResponse(response)
                gattCallback.onWriteData = null
            }
        } else {
            gattCallback.onReadData = { response ->
                returnResponse(response)
                gattCallback.onReadData = null
            }
        }

        val characteristic = if (useWrite) {
            Constants.UUID_DEFAULT_CHARACTERISTIC_WRITE
        } else {
            Constants.UUID_DEFAULT_CHARACTERISTIC
        }

        connection?.characteristics
            ?.firstOrNull { it.uuid == characteristic }
            ?.let {
                it.value = data
                if ( useWrite ) {
                    connection?.gatt?.writeCharacteristic(it)
                } else {
                    connection?.gatt?.readCharacteristic(it)
                }
            } ?: cont.resumeWithException(IllegalStateException())
    }

    private suspend fun findDevice() = suspendCancellableCoroutine<BluetoothDevice> { cont ->
        var onScanFinished: ((BluetoothDevice) -> Unit)? = null
        var onScanFailed: ((Int) -> Unit)? = null
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.device?.let { onScanFinished?.invoke(it) }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                super.onBatchScanResults(results)
            }

            override fun onScanFailed(errorCode: Int) {
                onScanFailed?.invoke(errorCode)
            }
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val scanner = adapter.bluetoothLeScanner

            onScanFinished = {
                scanner.stopScan(callback)
                callbackScope.launch {
                    cont.resume(it)
                }
                onScanFinished = null
            }
            onScanFailed = {
                callbackScope.launch {
                    cont.resumeWithException(IllegalStateException("errorCode: $it"))
                }
                onScanFailed = null
            }

            currentScanCallback?.let {
                scanner.stopScan(it)
            }
            scanner.startScan(filter, settings, callback)
            currentScanCallback = callback
        }
    }

    private suspend fun findService(device: BluetoothDevice) = suspendCancellableCoroutine<Connection> { cont ->
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            gattCallback.onConnected = { gatt ->
                gatt.discoverServices()
            }
            gattCallback.onService = { connection ->
                gattCallback.onService = null
                cont.resume(connection)
            }
            device.connectGatt(context, true, gattCallback)
        }
    }

    inner class Connection(
        val gatt: BluetoothGatt,
        val service: BluetoothGattService
    ) {
        val characteristics = service.characteristics.toList()
    }

    inner class BluetoothResponse(
        val status: Int,
        val characteristic: BluetoothGattCharacteristic
    )

    class Builder {
        var serviceUuid: UUID = Constants.UUID_DEFAULT_SERVICE
        var filter: List<ScanFilter> = listOf(
            ScanFilter.Builder().apply {
                setServiceUuid(ParcelUuid(serviceUuid))
            }.build()
        )
        var settings: ScanSettings = ScanSettings.Builder().apply {
            setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
            setMatchMode(ScanSettings.MATCH_MODE_STICKY)
            setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        }.build()
        fun build(context: Context): BleCommunicationProcess {
            val adapter = context.getSystemService(BluetoothManager::class.java).adapter
            return BleCommunicationProcess(
                context,
                adapter,
                filter,
                settings
            )
        }
    }
}
