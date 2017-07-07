package gestureDetect

import SuperSU.ShellSU
import android.content.Context
import android.util.Log
import gestureDetect.drivers.sensor.SensorHandler
import gestureDetect.drivers.sensor.SensorInput
import gestureDetect.drivers.sensor.SensorProximity
import ru.vpro.kernelgesture.BuildConfig
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class GestureDetect (val context:Context)
{
    /**
     * Sensor devices
     */
    private val sensorHandlers = arrayOf(
            SensorProximity(this),
            SensorInput(this)
    )

    val hw = GestureHW(context)
    val su = ShellSU()
    val settings = GestureSettings(context)
    /**
     * Devices control
     */
    //  Supported devices and keys
    private var supported = emptyList<String>()
    //  Detected sensor devices
    private var sensorDevices = emptyList<SensorHandler>()
    //  SuperSU
    /**
     * Disable event detection
     */
    private var _disabled = false
    var disabled: Boolean
        get() = _disabled
        set(value) {
            _disabled = value
            if (value) eventMutex.unlock()
        }

    private var _screenOnMode:Boolean = false
    var screenOnMode:Boolean
        get () = _screenOnMode
        set(value){
            if (value != _screenOnMode)
            {
                _screenOnMode = value
                sensorDevices.forEach { it.onScreenState(value) }
            }
        }

    private var screenEvents = emptyList<Pair<String, String>>()
    fun registerScreenEvents(event:String, screenEvent:String)
    {
        screenEvents
                .filter { (first) -> first.contains(event) }
                .forEach { return }

        screenEvents += Pair(event, screenEvent)
        addSupport(event)
        addSupport(screenEvent)
    }
    /**
     * Events with wait 500ms for twice
     */
    private var delayEvents = emptyList<Pair<String, String>>()
    fun registerDelayEvents(event:String, delayEvent:String)
    {
        delayEvents
                .filter { (first) -> first.contains(event) }
                .forEach { return }
        delayEvents += Pair(event, delayEvent)
        addSupport(event)
        addSupport(delayEvent)
    }
    /**
     *  Settings for filter events with proximity sensor
     */
    private var timeNearChange = System.currentTimeMillis()
    private var _bIsNearProximity:Boolean = false
    var isNearProximity: Boolean
        get() {
            val timeDiff = System.currentTimeMillis() - timeNearChange
            return _bIsNearProximity || timeDiff < 1*1000
        }
        set(value) {
            if (_bIsNearProximity != value) {
                _bIsNearProximity = value
                timeNearChange = System.currentTimeMillis()
            }
        }

    private val eventMutex = Mutex()
    /**
     * CODE
     */
    init{
        onDetect()
    }

    fun close()
    {
        disabled = true

        onStop()
        sensorDevices.forEach{ it.close() }

        eventMutex.unlock()
    }

    fun enable(bEnable: Boolean)
    {
        if (bEnable) onDetect()
        sensorDevices.forEach { it.enable(bEnable) }
    }

    fun onDetect()
    {
        val prevSensor = sensorDevices

        delayEvents = emptyList()
        screenEvents = emptyList()
        supported = emptyList()

        sensorDevices = sensorHandlers.filter { it.onDetect() }

        if (bStart){
            sensorDevices.subtract(prevSensor).forEach {
                it.onStart()
            }
        }

        prevSensor.subtract(sensorDevices).forEach {
            if (bStart) it.onStop()
            it.close()
        }
    }

    private var bStart = false
    private fun onStart(){
        if (bStart) return
        bStart = true

        if (BuildConfig.DEBUG){
            Log.d("GestureDetect", "start")
        }
        sensorDevices.forEach { it.onStart() }
    }
    private fun onStop(){
        if (!bStart) return
        bStart = false

        if (BuildConfig.DEBUG){
            Log.d("GestureDetect", "stop")
        }
        sensorDevices.forEach { it.onStop() }
    }

    private  var sensorEventGesture = LinkedList<String>()
    fun waitGesture(): String?
            = getEventThread()

    private fun getEventThread():String?
    {
        sensorEventGesture.clear()
        onStart()

        var thisEvent:String?
        do{
            thisEvent = getCurrentEvent()
            if (disabled) break

            thisEvent?.apply {

                if (BuildConfig.DEBUG) {
                    Log.d("Lock gesture", thisEvent)
                }

                if (screenOnMode)
                {
                    delayEvents.find { it.first == thisEvent  }?.apply {
                        screenEvents.find { it.first == second && settings.getEnable(second) }?.apply {
                            val evDelay = getCurrentEvent(350)
                            if (evDelay == thisEvent) thisEvent = first
                        }
                    }
                }else{
                    delayEvents.find { it.first == thisEvent && settings.getEnable(it.second) }?.apply {
                        val evDelay = getCurrentEvent(350)
                        if (evDelay == first) thisEvent = second
                    }
                }

                if (screenOnMode){
                    thisEvent = screenEvents.find {
                        it.first == thisEvent && settings.getEnable(it.second)
                    }?.second
                }
            }
        }while (!disabled && !settings.getEnable(thisEvent))

        onStop()

        return thisEvent
    }
    private fun getCurrentEvent():String?
    {
        with(sensorEventGesture) {
            synchronized(sensorEventGesture) {
                if (isNotEmpty()) return pop()
            }
            eventMutex.lock()
            synchronized(sensorEventGesture) {
                return if (isNotEmpty()) pop() else null
            }
        }
    }
    private fun getCurrentEvent(timeout:Long):String?
    {
        with(sensorEventGesture) {

            synchronized(sensorEventGesture) {
                if (isNotEmpty()) return pop()
            }
            eventMutex.lock(timeout)
//            Thread.sleep(timeout)
            synchronized(sensorEventGesture) {
                return if (isNotEmpty()) pop() else null
            }
        }
    }

    fun sensorEvent(value:String):Boolean
    {
        if (disabled) return false
        if (!bStart) return false
        if (!isEventEnable(value)) return false

        if (BuildConfig.DEBUG){
            Log.d("SensorEvent", value)
        }
        hw.powerON()
        synchronized(sensorEventGesture){
            sensorEventGesture.push(value)
            eventMutex.unlock()
        }

        return true
    }

    /**
     * Once event or double tap events enable
     */
    fun isEventEnable(event:String):Boolean
    {
        if (screenOnMode){
            var ev = event
            delayEvents.find { it.first == ev}?.second?.apply { ev = this }
            return screenEvents.find { it.first == ev && settings.getEnable(it.second) } != null
        }

        if (settings.getEnable(event)) return true
        return delayEvents.find { it.first == event && settings.getEnable(it.second) } != null
    }

    fun addSupport(value:String)
    {
        if (supported.contains(value)) return
        supported += value
    }
    fun addSupport(value:List<String>)
            = value.forEach { addSupport(it) }

    fun getSupport():List<String>
    {
/*
        if (SU.exec("find /sys -name *gesture*") && SU.exec("echo --END--"))
        {
            while (true) {
                val line = SU.readExecLine() ?: break
                Log.d("Read line", line)
                if (line == "--END--") break

                val path =  line.substring(line.lastIndexOf("/")+1)
                when(path){
//                        "tpgesture_status" -> { addSupport("GESTURE") }
                }
            }
        }
 */
        return supported
    }

    class Mutex
    {
        private var bLocked = false
        private var semaphore = Semaphore(0)

        fun lock() {
            synchronized(bLocked) {
                if (bLocked) return
                bLocked = true
             }

            if (BuildConfig.DEBUG) {
                Log.d("Lock", "Wait semaphore lock")
            }
            semaphore.acquire()
            synchronized(bLocked) {
                bLocked = false
            }
        }
        fun lock(timeout:Long):Boolean
        {
            synchronized(bLocked)
            {
                if (bLocked) return false
                bLocked = true
                if (BuildConfig.DEBUG) {
                    Log.d("Lock", "Wait ${timeout}ms semaphore lock")
                }
            }

            val bRet = semaphore.tryAcquire(1, timeout, TimeUnit.MILLISECONDS)

            synchronized(bLocked) {
                bLocked = false
            }
            return bRet
        }
        fun unlock()
        {
            synchronized(bLocked)
            {
                if (!bLocked) return
                bLocked = false

                if (BuildConfig.DEBUG) {
                    Log.d("Lock", "Unlock locked semaphore")
                }

                semaphore.release()
            }
        }
    }
}
