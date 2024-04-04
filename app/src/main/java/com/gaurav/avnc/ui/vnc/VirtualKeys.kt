/*
 * Copyright (c) 2021  Gaurav Ujjwal.
 *
 * SPDX-License-Identifier:  GPL-3.0-or-later
 *
 * See COPYING.txt for more details.
 */

package com.gaurav.avnc.ui.vnc

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ToggleButton
import androidx.core.view.children
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.gaurav.avnc.databinding.VirtualKeysBinding


class CustomHScrollView(context: Context, attributeSet: AttributeSet? = null) : HorizontalScrollView(context, attributeSet) {
    private var detectedScrollX = 0
    private val gestureDetector by lazy {
        GestureDetector(context, object : SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                detectedScrollX = 0
                return true
            }

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                detectedScrollX = distanceX.toInt()
                return true
            }
        })
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        if (detectedScrollX != 0 && !canScrollHorizontally(detectedScrollX))
            return false

        return super.onInterceptTouchEvent(ev)
    }
}


/**
 * Virtual keys allow the user to input keys which are not normally found on
 * keyboards but can be useful for controlling remote server.
 *
 * This class manages the inflation & visibility of virtual keys.
 */
class VirtualKeys(activity: VncActivity) {

    private val pref = activity.viewModel.pref.input
    private val keyHandler = activity.keyHandler
    private val frameView = activity.binding.frameView
    private val stub = activity.binding.virtualKeysStub
    private val toggleKeys = mutableSetOf<ToggleButton>()
    private val lockedToggleKeys = mutableSetOf<ToggleButton>()
    private var openedWithKb = false

    val container: View? get() = stub.root

    fun show() {
        init()
        container?.visibility = View.VISIBLE
    }

    fun hide() {
        container?.visibility = View.GONE
        openedWithKb = false //Reset flag
    }

    fun onKeyboardOpen() {
        if (pref.vkOpenWithKeyboard && container?.visibility != View.VISIBLE) {
            show()
            openedWithKb = true
        }
    }

    fun onKeyboardClose() {
        if (openedWithKb) {
            hide()
            openedWithKb = false
        }
    }

    fun releaseMetaKeys() {
        toggleKeys.forEach {
            if (it.isChecked)
                it.isChecked = false
        }
    }

    private fun releaseUnlockedMetaKeys() {
        toggleKeys.forEach {
            if (it.isChecked && !lockedToggleKeys.contains(it))
                it.isChecked = false
        }
    }

    private fun onAfterKeyEvent(event: KeyEvent) {
        if (event.action == KeyEvent.ACTION_UP && !KeyEvent.isModifierKey(event.keyCode))
            releaseUnlockedMetaKeys()
    }

    private fun init() {
        if (stub.isInflated)
            return

        stub.viewStub?.inflate()
        val binding = stub.binding as VirtualKeysBinding
        initControls(binding)
        initKeys(binding)
        binding.tmpPageHost.doOnLayout { binding.root.post { switcharoo(binding) } }
        keyHandler.processedEventObserver = ::onAfterKeyEvent
    }

    private fun switcharoo(binding: VirtualKeysBinding) {
        val pages = binding.tmpPageHost.children.toList()
        val maxPageWidth = pages.maxOf { it.width }
        val maxPageHeight = pages.maxOf { it.height }

        pages.forEach {
            binding.tmpPageHost.removeView(it)
            it.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        binding.pager.let {
            it.offscreenPageLimit = pages.size
            it.adapter = PagerAdapter(pages)
            it.layoutParams = it.layoutParams.apply {
                width = maxPageWidth + it.paddingLeft + it.paddingRight
                height = maxPageHeight + it.paddingTop + it.paddingBottom
            }
            it.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    if (position == 2) {
                        binding.editBox.requestFocus()
                    }
                }
            })
        }

        //Remove tmp host
    }

    private inner class PagerAdapter(private val views: List<View>) : RecyclerView.Adapter<PagerAdapter.ViewHolder>() {
        private inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v)

        override fun getItemCount() = views.size
        override fun getItemViewType(position: Int) = position
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(views[viewType])
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {}
    }

    private fun initControls(binding: VirtualKeysBinding) {
        binding.editorBtn.setOnClickListener {
            binding.pager.setCurrentItem(2, true)
            binding.editBox.requestFocus()

        }
        binding.closeEditorBtn.setOnClickListener {
            binding.pager.setCurrentItem(0, true)
        }
        binding.editBox.setOnEditorActionListener { _, _, _ ->
            handleEditTextAction(binding.editBox)
            true
        }
        binding.editBox.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) frameView.requestFocus()
        }
        binding.hideBtn.setOnClickListener {
            hide()
        }
    }

    private fun initKeys(binding: VirtualKeysBinding) {
        initToggleKey(binding.vkSuper, KeyEvent.KEYCODE_META_LEFT)
        initToggleKey(binding.vkShift, KeyEvent.KEYCODE_SHIFT_RIGHT) // See if we can switch to 'left' versions of these
        initToggleKey(binding.vkAlt, KeyEvent.KEYCODE_ALT_RIGHT)
        initToggleKey(binding.vkCtrl, KeyEvent.KEYCODE_CTRL_RIGHT)

        initNormalKey(binding.vkEsc, KeyEvent.KEYCODE_ESCAPE)
        initNormalKey(binding.vkTab, KeyEvent.KEYCODE_TAB)
        initNormalKey(binding.vkHome, KeyEvent.KEYCODE_MOVE_HOME)
        initNormalKey(binding.vkEnd, KeyEvent.KEYCODE_MOVE_END)
        initNormalKey(binding.vkPageUp, KeyEvent.KEYCODE_PAGE_UP)
        initNormalKey(binding.vkPageDown, KeyEvent.KEYCODE_PAGE_DOWN)
        initNormalKey(binding.vkInsert, KeyEvent.KEYCODE_INSERT)
        initNormalKey(binding.vkDelete, KeyEvent.KEYCODE_FORWARD_DEL)

        initNormalKey(binding.vkLeft, KeyEvent.KEYCODE_DPAD_LEFT)
        initNormalKey(binding.vkRight, KeyEvent.KEYCODE_DPAD_RIGHT)
        initNormalKey(binding.vkUp, KeyEvent.KEYCODE_DPAD_UP)
        initNormalKey(binding.vkDown, KeyEvent.KEYCODE_DPAD_DOWN)

        initNormalKey(binding.vkF1, KeyEvent.KEYCODE_F1)
        initNormalKey(binding.vkF2, KeyEvent.KEYCODE_F2)
        initNormalKey(binding.vkF3, KeyEvent.KEYCODE_F3)
        initNormalKey(binding.vkF4, KeyEvent.KEYCODE_F4)
        initNormalKey(binding.vkF5, KeyEvent.KEYCODE_F5)
        initNormalKey(binding.vkF6, KeyEvent.KEYCODE_F6)
        initNormalKey(binding.vkF7, KeyEvent.KEYCODE_F7)
        initNormalKey(binding.vkF8, KeyEvent.KEYCODE_F8)
        initNormalKey(binding.vkF9, KeyEvent.KEYCODE_F9)
        initNormalKey(binding.vkF10, KeyEvent.KEYCODE_F10)
        initNormalKey(binding.vkF11, KeyEvent.KEYCODE_F11)
        initNormalKey(binding.vkF12, KeyEvent.KEYCODE_F12)
    }


    private fun initToggleKey(key: ToggleButton, keyCode: Int) {
        key.setOnCheckedChangeListener { _, isChecked ->
            keyHandler.onKeyEvent(keyCode, isChecked)
            if (!isChecked) lockedToggleKeys.remove(key)
        }
        key.setOnLongClickListener {
            lockedToggleKeys.add(key)
            false /* allow the key to be toggled */
        }
        toggleKeys.add(key)
    }

    private fun initNormalKey(key: View, keyCode: Int) {
        check(key !is ToggleButton) { "use initToggleKey()" }
        key.setOnClickListener { keyHandler.onKey(keyCode) }
        makeKeyRepeatable(key, true)
    }

    /**
     * When a View is touched, we schedule a callback to to simulate a click.
     * As long as finger stays on the view, we keep repeating this callback.
     *
     * Another option here is to send VNC KeyEvent(down) on [MotionEvent.ACTION_DOWN]
     * and then send VNC KeyEvent(up) on [MotionEvent.ACTION_UP].
     */
    private fun makeKeyRepeatable(keyView: View, repeatable: Boolean) {
        if (!repeatable)
            return

        keyView.setOnTouchListener(object : View.OnTouchListener {
            private var doRepeat = false

            private fun repeat(v: View) {
                if (doRepeat) {
                    v.performClick()
                    v.postDelayed({ repeat(v) }, ViewConfiguration.getKeyRepeatDelay().toLong())
                }
            }

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        doRepeat = true
                        v.postDelayed({ repeat(v) }, ViewConfiguration.getKeyRepeatTimeout().toLong())
                    }

                    MotionEvent.ACTION_POINTER_DOWN,
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        doRepeat = false
                    }
                }
                return false
            }
        })
    }

    private fun handleEditTextAction(editText: EditText) {
        editText.text?.let {
            if (it.isEmpty())
                keyHandler.onKey(KeyEvent.KEYCODE_ENTER)
            else {
                injectKeyEventsForText(it.toString())
                editText.setText("")
            }
        }
    }


    private val keyCharMap by lazy { KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD) }

    private fun injectKeyEventsForText(text: String) {
        // todo handle c-cedilla
        val events = keyCharMap.getEvents(text.toCharArray())
        events.forEach { keyHandler.onKeyEvent(it) }
    }
}
