package com.kr0us.michirun.ui

import android.content.Context
import android.view.MotionEvent
import android.widget.Button

/** Los gestos mantenidos y los clics de accesibilidad comparten un botón nativo. */
class ActionButton(context: Context) : Button(context) {
    var action: (() -> Unit)? = null
    var onHold: ((Boolean) -> Unit)? = null
    private var completingTouch = false

    override fun performClick(): Boolean {
        super.performClick()
        if (!completingTouch) action?.invoke()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val hold = onHold ?: return super.onTouchEvent(event)
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { isPressed = true; hold(true) }
            MotionEvent.ACTION_UP -> {
                isPressed = false; hold(false)
                completingTouch = true
                performClick()
                completingTouch = false
            }
            MotionEvent.ACTION_CANCEL -> { isPressed = false; hold(false) }
        }
        return true
    }
}
