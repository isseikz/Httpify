package tokyo.isseikuzumaki.httpify.server.servers

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.BluetoothGatt.GATT_FAILURE
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.httpify.core.Constants.UUID_DEFAULT_CHARACTERISTIC
import tokyo.isseikuzumaki.httpify.core.Constants.UUID_DEFAULT_CHARACTERISTIC_WRITE
import tokyo.isseikuzumaki.httpify.core.Constants.UUID_DEFAULT_SERVICE
import tokyo.isseikuzumaki.httpify.core.Request
import tokyo.isseikuzumaki.httpify.server.RequestReceiver
import tokyo.isseikuzumaki.httpify.server.ServerBase

class BleServer(
    requestReceiver: RequestReceiver<ByteArray>
) : ServerBase<ByteArray>(requestReceiver) {
    companion object {
        const val TAG = "BleServer"
    }

    private lateinit var manager: BluetoothManager
    private lateinit var server: BluetoothGattServer
    private var advertiseCallback: AdvertiseCallback? = null

    private val service = BluetoothGattService(UUID_DEFAULT_SERVICE, SERVICE_TYPE_PRIMARY).apply {
        addCharacteristic(
            BluetoothGattCharacteristic(
                UUID_DEFAULT_CHARACTERISTIC_WRITE,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )
        )
        addCharacteristic(
            BluetoothGattCharacteristic(
                UUID_DEFAULT_CHARACTERISTIC,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PROPERTY_READ
            )
        )
    }

    override fun start(context: Context) {
        manager = context.getSystemService(BluetoothManager::class.java)
        startGattServer(context)?.also { server = it }?.also { startAdvertisement(context) }
    }

    @SuppressLint("MissingPermission")
    override fun stop(context: Context) {
        if (resolvePermission(context)) {
            server.close()
        }
    }

    private fun resolvePermission(context: Context): Boolean {
        val requiredPermissions = mutableListOf<String>()
        val permissionToRequest = { candidate: String ->
            if (ActivityCompat.checkSelfPermission(context, candidate) != PackageManager.PERMISSION_GRANTED) {
                candidate
            } else null
        }
        if (Build.VERSION.SDK_INT >= 31) {
            permissionToRequest(Manifest.permission.BLUETOOTH_CONNECT)?.let{
                requiredPermissions.add(it)
            }
            permissionToRequest(Manifest.permission.BLUETOOTH_ADVERTISE)?.let{
                requiredPermissions.add(it)
            }
        }
        if (requiredPermissions.isNotEmpty()) {
            if (context is Activity) {
                ActivityCompat.requestPermissions(context, requiredPermissions.toTypedArray(), 123)
            } else {
                Log.e(TAG, "Permission request failed since context is Activity")
            }
            return false
        }
        return true
    }

    @SuppressLint("MissingPermission") // see resolvePermission
    private fun startGattServer(context: Context): BluetoothGattServer? {
        if (!resolvePermission(context)) {
            return null
        }
        val server = manager.openGattServer(context, Callback(context))
        server.addService(service)
        return server
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertisement(context: Context) {
        if (!resolvePermission(context)) {
            Toast.makeText(context, "Advertisement failed due to missing permissions", Toast.LENGTH_LONG).show()
            return
        }

        if (Build.VERSION.SDK_INT >= 31 && context is Activity) {
            val requestCode = 1
            val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
            startActivityForResult(context, discoverableIntent, requestCode, null)
        }

        val advertiseData = AdvertiseData.Builder().apply {
            setIncludeTxPowerLevel(true)
            addServiceUuid(ParcelUuid(UUID_DEFAULT_SERVICE))
        }.build()
        val advertiseSettings = AdvertiseSettings.Builder().apply {
            setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            setTimeout(0)
            setConnectable(true)
        }.build()
        val advertiseResponse = AdvertiseData.Builder().apply {
            setIncludeDeviceName(true)
        }.build()
        val callback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
                Log.d(TAG, "Advertising started successfully")
            }

            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
                Log.w(TAG, "Advertising failed to start")
            }
        }
        manager.adapter.bluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, advertiseResponse, callback)
        advertiseCallback = callback
    }

    private fun gattStatus(responseStatus: Int): Int {
        return if (responseStatus == 200) {
            GATT_SUCCESS
        } else {
            GATT_FAILURE
        }
    }

    inner class Callback(private val context: Context) : BluetoothGattServerCallback() {

        private val callbackScope = CoroutineScope(Dispatchers.IO)
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)

            when (status) {
                BluetoothGatt.STATE_CONNECTING -> {
                    advertiseCallback?.let { manager.adapter.bluetoothLeAdvertiser.stopAdvertising(it) }
                }
                BluetoothGatt.STATE_DISCONNECTED -> {
                    startAdvertisement(context)
                }
            }
        }

        override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
            super.onServiceAdded(status, service)
        }

        @SuppressLint("MissingPermission") // @see doAfterPermissionCheck
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            val request = Request(
                mapOf(
                    "requestId" to requestId.toString()
                )
            )
            callbackScope.launch {
                val response = callGet(request)
                server.sendResponse(device, requestId, gattStatus(response.status), 0, response.payload)
            }
        }

        @SuppressLint("MissingPermission") // @see doAfterPermissionCheck
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            val request = Request(
                params = mapOf(
                    "requestId" to requestId.toString()
                ),
                payload = value?.let { String(it) } ?: ""
            )
            callbackScope.launch {
                val response = callPost(request)
                if (responseNeeded) {
                    server.sendResponse(device, requestId, gattStatus(response.status), 0, response.payload)
                }
            }
        }

        override fun onDescriptorReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor?
        ) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor)
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            descriptor: BluetoothGattDescriptor?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onDescriptorWriteRequest(
                device,
                requestId,
                descriptor,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )
        }

        override fun onExecuteWrite(device: BluetoothDevice?, requestId: Int, execute: Boolean) {
            super.onExecuteWrite(device, requestId, execute)
        }

        override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
            super.onNotificationSent(device, status)
        }

        override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
            super.onMtuChanged(device, mtu)
        }

        override fun onPhyUpdate(device: BluetoothDevice?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyUpdate(device, txPhy, rxPhy, status)
        }

        override fun onPhyRead(device: BluetoothDevice?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyRead(device, txPhy, rxPhy, status)
        }
    }
}
