/*
 * Copyright (c) 2021  Gaurav Ujjwal.
 *
 * SPDX-License-Identifier:  GPL-3.0-or-later
 *
 * See COPYING.txt for more details.
 */

package com.gaurav.avnc.ui.vnc

import android.annotation.SuppressLint
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.ToggleButton
import androidx.core.view.isVisible
import com.gaurav.avnc.databinding.VirtualKeysBinding

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
        toggleKeys.forEach { it.isChecked = false }
    }

    fun releaseUnlockedMetaKeys() {
        toggleKeys.forEach { if (!lockedToggleKeys.contains(it)) it.isChecked = false }
    }

    private fun init() {
        if (stub.isInflated)
            return

        stub.viewStub?.inflate()
        val binding = stub.binding as VirtualKeysBinding
        binding.secondaryKeys.isVisible = pref.vkShowAll
        binding.hideBtn.setOnClickListener { hide() }
        initControls(binding)
        initKeys(binding)
    }

    private fun initControls(binding: VirtualKeysBinding) {
        binding.showEditorBtn.setOnClickListener {
            binding.keys.isVisible = false
            binding.editor.isVisible = true
            binding.editBox.width = binding.primaryKeys.width - binding.hideBtn.width
            binding.editBox.requestFocus()

        }
        binding.closeEditorBtn.setOnClickListener {
            binding.keys.isVisible = true
            binding.editor.isVisible = false
            frameView.requestFocus()
        }

        binding.editBox.setOnEditorActionListener { _, _, _ ->
            binding.editBox.setText("")
            true
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
}
