package gesture

/*
MT touchscreen with gestures
 */
open class InputMTK_TPD : InputHandler
{
    override fun onDetect(name:String):Boolean{
        return name.toLowerCase() == "mtk-tpd"
    }
    override fun onEvent(line:String):String?
    {
        val arg = line.replace(Regex("\\s+"), " ").split(" ")
        if (arg[0] == "EV_KEY") return GestureDetect.GS.runGesture(arg[1])
        return null
    }
}