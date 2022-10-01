package tw.lospot.kin.wirelesshid.util

import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun Byte.setBit(byte: Byte, value: Boolean) = if (value) this.or(byte) else this.and(byte.inv())
fun Byte.hasBit(byte: Byte) = this.and(byte) != 0.toByte()

class ByteInArray(
    private val array: ByteArray,
    private val index: Int = 0,
) : ReadWriteProperty<Any?, Byte> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Byte = array[index]
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Byte) {
        array[index] = value
    }
}
