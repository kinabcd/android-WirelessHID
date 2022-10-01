package tw.lospot.kin.wirelesshid.bluetooth.report

import android.view.KeyEvent
import tw.lospot.kin.wirelesshid.util.BitBoolean

class KeyboardReport(val bytes: ByteArray = ByteArray(8)) {
    var leftControl by BitBoolean(bytes, 0, 0)
    var leftShift by BitBoolean(bytes, 0, 1)
    var leftAlt by BitBoolean(bytes, 0, 2)
    var leftMeta by BitBoolean(bytes, 0, 3)
    var rightControl by BitBoolean(bytes, 0, 4)
    var rightShift by BitBoolean(bytes, 0, 5)
    var rightAlt by BitBoolean(bytes, 0, 6)
    var rightMeta by BitBoolean(bytes, 0, 7)

    fun setKeys(keys: ByteArray) {
        val oldKeys = bytes.filterIndexed { index, _ -> index in 2..7 }
        for (i in 2..7) {
            if (bytes[i] !in keys) bytes[i] = 0
        }
        val newKeys = keys.filter { it !in oldKeys }
        newKeys.forEach {
            for (i in 2..7) {
                if (bytes[i] == 0.toByte()) {
                    bytes[i] = it
                    break
                }
            }
        }
    }

    companion object {
        const val ID = 2

//        const val KEYCODE_SHIFT_NOT = 30
//        const val KEYCODE_SHIFT_DOLLAR = 33
//        const val KEYCODE_SHIFT_PERCENT = 34
//        const val KEYCODE_SHIFT_CARET = 35
//        const val KEYCODE_SHIFT_AMPERSAND = 36
//        const val KEYCODE_SHIFT_OPEN_PARENTHESIS = 38
//        const val KEYCODE_SHIFT_CLOSED_PARENTHESIS = 39
//        const val KEYCODE_SHIFT_UNDERSCORE = 45
//        const val KEYCODE_SHIFT_PLUS = 46
//        const val KEYCODE_SHIFT_OPEN_CURLY_BRACKET = 47
//        const val KEYCODE_SHIFT_CLOSED_CURLY_BRACKET = 48
//        const val KEYCODE_SHIFT_PIPE = 49
//        const val KEYCODE_SHIFT_COLON = 51
//        const val KEYCODE_SHIFT_QUOTE = 52
//        const val KEYCODE_SHIFT_TILDE = 53
//        const val KEYCODE_SHIFT_LESS_THAN = 54
//        const val KEYCODE_SHIFT_GREATER_THAN = 55
//        const val KEYCODE_SHIFT_QUESTION_MARK = 56

        val KeyEventMap = mapOf(
            KeyEvent.KEYCODE_A to 4,
            KeyEvent.KEYCODE_B to 5,
            KeyEvent.KEYCODE_C to 6,
            KeyEvent.KEYCODE_D to 7,
            KeyEvent.KEYCODE_E to 8,
            KeyEvent.KEYCODE_F to 9,
            KeyEvent.KEYCODE_G to 10,
            KeyEvent.KEYCODE_H to 11,
            KeyEvent.KEYCODE_I to 12,
            KeyEvent.KEYCODE_J to 13,
            KeyEvent.KEYCODE_K to 14,
            KeyEvent.KEYCODE_L to 15,
            KeyEvent.KEYCODE_M to 16,
            KeyEvent.KEYCODE_N to 17,
            KeyEvent.KEYCODE_O to 18,
            KeyEvent.KEYCODE_P to 19,
            KeyEvent.KEYCODE_Q to 20,
            KeyEvent.KEYCODE_R to 21,
            KeyEvent.KEYCODE_S to 22,
            KeyEvent.KEYCODE_T to 23,
            KeyEvent.KEYCODE_U to 24,
            KeyEvent.KEYCODE_V to 25,
            KeyEvent.KEYCODE_W to 26,
            KeyEvent.KEYCODE_X to 27,
            KeyEvent.KEYCODE_Y to 28,
            KeyEvent.KEYCODE_Z to 29,

            KeyEvent.KEYCODE_1 to 30,
            KeyEvent.KEYCODE_2 to 31,
            KeyEvent.KEYCODE_3 to 32,
            KeyEvent.KEYCODE_4 to 33,
            KeyEvent.KEYCODE_5 to 34,
            KeyEvent.KEYCODE_6 to 35,
            KeyEvent.KEYCODE_7 to 36,
            KeyEvent.KEYCODE_8 to 37,
            KeyEvent.KEYCODE_9 to 38,
            KeyEvent.KEYCODE_0 to 39,

            KeyEvent.KEYCODE_ENTER to 40,
            KeyEvent.KEYCODE_ESCAPE to 41,
            KeyEvent.KEYCODE_DEL to 42,
            KeyEvent.KEYCODE_TAB to 43,
            KeyEvent.KEYCODE_SPACE to 44,
            KeyEvent.KEYCODE_MINUS to 45,
            KeyEvent.KEYCODE_EQUALS to 46,
            KeyEvent.KEYCODE_LEFT_BRACKET to 47,
            KeyEvent.KEYCODE_RIGHT_BRACKET to 48,
            KeyEvent.KEYCODE_BACKSLASH to 49,
            KeyEvent.KEYCODE_SEMICOLON to 51,
            KeyEvent.KEYCODE_APOSTROPHE to 52,
            KeyEvent.KEYCODE_GRAVE to 53,
            KeyEvent.KEYCODE_COMMA to 54,
            KeyEvent.KEYCODE_PERIOD to 55,
            KeyEvent.KEYCODE_SLASH to 56,
            KeyEvent.KEYCODE_CAPS_LOCK to 57,

            KeyEvent.KEYCODE_F1 to 58,
            KeyEvent.KEYCODE_F2 to 59,
            KeyEvent.KEYCODE_F3 to 60,
            KeyEvent.KEYCODE_F4 to 61,
            KeyEvent.KEYCODE_F5 to 62,
            KeyEvent.KEYCODE_F6 to 63,
            KeyEvent.KEYCODE_F7 to 64,
            KeyEvent.KEYCODE_F8 to 65,
            KeyEvent.KEYCODE_F9 to 66,
            KeyEvent.KEYCODE_F10 to 67,
            KeyEvent.KEYCODE_F11 to 68,
            KeyEvent.KEYCODE_F12 to 69,

            KeyEvent.KEYCODE_SYSRQ to 70, // SysRq, PrintScreen
            KeyEvent.KEYCODE_SCROLL_LOCK to 71,
            KeyEvent.KEYCODE_BREAK to 72, // Break, Pause
            KeyEvent.KEYCODE_INSERT to 73,
            KeyEvent.KEYCODE_MOVE_HOME to 74,
            KeyEvent.KEYCODE_PAGE_UP to 75,
            KeyEvent.KEYCODE_FORWARD_DEL to 76,
            KeyEvent.KEYCODE_MOVE_END to 77,
            KeyEvent.KEYCODE_PAGE_DOWN to 78,

            KeyEvent.KEYCODE_DPAD_RIGHT to 79,
            KeyEvent.KEYCODE_DPAD_LEFT to 80,
            KeyEvent.KEYCODE_DPAD_DOWN to 81,
            KeyEvent.KEYCODE_DPAD_UP to 82,

            KeyEvent.KEYCODE_NUM_LOCK to 83,

            KeyEvent.KEYCODE_MENU to 118,
            KeyEvent.KEYCODE_VOLUME_MUTE to 127,
            KeyEvent.KEYCODE_VOLUME_UP to 128,
            KeyEvent.KEYCODE_VOLUME_DOWN to 129,

            KeyEvent.KEYCODE_AT to 31,
            KeyEvent.KEYCODE_POUND to 32,
            KeyEvent.KEYCODE_STAR to 37
        )
    }
}