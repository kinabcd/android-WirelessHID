package tw.lospot.kin.wirelesshid.util

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class BitBoolean(
    private val array: ByteArray,
    private val index: Int = 0,
    bitIndex: Int,
) : ReadWriteProperty<Any?, Boolean> {
    private val mask = (1 shl bitIndex).toByte()
    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean =
        array[index].hasBit(mask)

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
        array[index] = array[index].setBit(mask, value)
    }
}