package gestureDetect.drivers.input

import gestureDetect.GestureDetect
import gestureDetect.drivers.SensorInput

/*
MT touchscreen with gestures
 */
open class InputTouchscreen(gesture: GestureDetect) : InputHandler(gesture)
{
    //  HCT version gesture for Android 5x and Android 6x
    override var GESTURE_PATH = arrayOf(
            //  Unknown 3.10 FTS touchscreen gestures for driver FT6206_X2605
            GS("/sys/class/syna/gesenable"),

            //  Doogee x5 Max Pro
            GS("/sys/devices/platform/mt-i2c.0/i2c-0/0-0038/gesture"),
            GS("/sys/devices/bus/11008000.i2c/i2c-1/1-0038/gesture"),

            GS("/sys/devices/platform/mt-i2c.0/i2c-0/0-005d/gesture"),
            GS("/sys/devices/bus/11008000.i2c/i2c-1/1-005d/gesture"),

            GS("/sys/devices/platform/mt-i2c.0/i2c-0/0-0020/gesture"),
            GS("/sys/devices/bus/11008000.i2c/i2c-1/1-0020/gesture"),

            GS("/sys/devices/platform/mt-i2c.0/i2c-0/0-004b/gesture"),
            GS("/sys/devices/bus/11008000.i2c/i2c-1/1-004b/gesture"),

            GS("/sys/class/gesture/gesture_ft5x06/enable"),
            //  Xiaomi?
            GS("/data/tp/wakeup_mode")
    )
    override fun onDetect(name:String):Boolean
    {
        //  "ft5x06_ts", "ft5435_ts", "fts_ts"
        if (!onDetect(name,
                arrayOf("mtk-tpd", "atmel-maxtouch", "touch_dev", Regex("ft.*_ts"))))
            return false

        super.onDetect(name)
        super.onDetectTouch(name)
        return true
    }
    override fun onEvent(ev: SensorInput.EvData): String?
    {
        val keys = arrayOf(
            Pair("KEY_WAKEUP",  "KEY_U")
        )
        return super.onEvent(ev) ?: filter(ev, ev.evButton, keys)
    }
}