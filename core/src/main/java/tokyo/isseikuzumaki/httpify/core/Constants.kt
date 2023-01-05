package tokyo.isseikuzumaki.httpify.core

import android.bluetooth.BluetoothGatt
import java.util.*

object Constants {
    val UUID_DEFAULT_SERVICE: UUID = UUID.fromString("ac356cb2-1fb1-220c-c362-a6a22a7557bf")
    val UUID_DEFAULT_CHARACTERISTIC: UUID = UUID.fromString("d6ad5e96-b2d5-fda9-b658-48a39bf42c38")
    val UUID_DEFAULT_CHARACTERISTIC_WRITE: UUID = UUID.fromString("195afd05-9f2f-dff3-e7c0-44fe18bd5586")
    val RESPONSE_CODE = mapOf<Int, Int>(
        200 to BluetoothGatt.GATT_SUCCESS,
        404 to BluetoothGatt.GATT_FAILURE,
        403 to BluetoothGatt.GATT_FAILURE
    )
}