package ru.vpro.kernelgesture

import android.content.Context
import android.preference.Preference
import android.preference.SwitchPreference
import android.util.AttributeSet
import android.widget.Switch
import android.view.View
import android.widget.Checkable
import android.widget.CompoundButton


/**
 * Created by Костя on 30.05.2017.
 */
class GestureSwitchPreference(context: Context, attrs: AttributeSet) :
        SwitchPreference(context, attrs)
{
    init
    {
        widgetLayoutResource = R.layout.preference_switch_item
    }

    override fun onBindView(view: View)
    {
        super.onBindView(view)

        view.setOnClickListener{
            onPreferenceClickListener?.onPreferenceClick(this)
        }

        val checkableView:Switch? = view.findViewById(R.id.switch_widget) as Switch?
        checkableView?.setOnCheckedChangeListener({ compoundButton: CompoundButton, b: Boolean ->

            if (!callChangeListener(b)) {
                // Listener didn't like it, change it back.
                // CompoundButton will make sure we don't recurse.
                checkableView.setChecked(!b)
            }else {
                setChecked(b)
            }

        })
        checkableView?.isChecked = isChecked
    }
}