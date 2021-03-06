package gestureDetect.drivers

import gestureDetect.GestureDetect

/**
 * Базовый класс для всех типов детекторов событий
 */

abstract class SensorHandler(val gesture:GestureDetect)
{
    val context = gesture.context
    /**
     * Определить доступность сенсора для работы
     */
    abstract fun onDetect():Boolean

    /**
     * Screen state changed
     */
    open fun onScreenState(bScreenON:Boolean) {}
    /**
     * Начало процесса детектирования событий
     */
    open fun onResume() {}

    /**
     * Окончание процесса детектирование событий
     */
    open fun onPause() {}

    /**
     * Завершение работы сервиса, выход из программы
     */
    open fun close(){}

    /**
     * Включение и выключение событий
     */
    open fun enable(bEnable:Boolean) {}

    /**
     * Вызвать срабатывание события
     */
    fun sensorEvent(event:String):Boolean{
        return gesture.sensorEvent(event)
    }
}