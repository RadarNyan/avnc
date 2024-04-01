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
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.ToggleButton
import androidx.recyclerview.widget.RecyclerView
import com.gaurav.avnc.databinding.VirtualKeys1Binding
import com.gaurav.avnc.databinding.VirtualKeys2Binding
import com.gaurav.avnc.databinding.VirtualKeys3Binding
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

    private var viewForPagerSize: View? = null

    private fun init() {
        if (stub.isInflated)
            return

        stub.viewStub?.inflate()
        val binding = stub.binding as VirtualKeysBinding

        val adapter = PagerAdapter()
        binding.root.alpha = 0f
        binding.pager.adapter = adapter
        binding.pager.offscreenPageLimit = 3




        binding.pager.addOnLayoutChangeListener { v, left, _, right, _, _, _, _, _ ->
            val newWidth = (right - left)
            viewForPagerSize?.let {
                if (newWidth != it.width) {
                    v.layoutParams = v.layoutParams.apply { width = it.width }
                    v.post {
                        v.requestLayout()
                    }
                } else {
                    binding.root.alpha = 1f
                }
            }
        }
    }

    private inner class PagerAdapter : RecyclerView.Adapter<PagerAdapter.ViewHolder>() {
        val viewList = mutableListOf<View>()

        private inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v)

        override fun getItemCount() = 3
        override fun getItemViewType(position: Int) = position

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = when (viewType) {
                0 -> createPage1(inflater, parent)
                1 -> createPage2(inflater, parent)
                2 -> createPage3(inflater, parent)
                else -> throw IllegalStateException("Unexpected view type: [$viewType]")
            }
            viewList.add(view)
            return ViewHolder(view)
        }


        override fun onBindViewHolder(holder: ViewHolder, position: Int) {}
    }

    private fun createPage1(inflater: LayoutInflater, parent: ViewGroup): View {
        VirtualKeys1Binding.inflate(inflater, parent, false).let {
            viewForPagerSize = it.primaryKeys
            it.hideBtn.setOnClickListener { hide() }

            initToggleKey(it.vkSuper, KeyEvent.KEYCODE_META_LEFT)
            initToggleKey(it.vkShift, KeyEvent.KEYCODE_SHIFT_RIGHT) // See if we can switch to 'left' versions of these
            initToggleKey(it.vkAlt, KeyEvent.KEYCODE_ALT_RIGHT)
            initToggleKey(it.vkCtrl, KeyEvent.KEYCODE_CTRL_RIGHT)

            initNormalKey(it.vkEsc, KeyEvent.KEYCODE_ESCAPE)
            initNormalKey(it.vkTab, KeyEvent.KEYCODE_TAB)
            initNormalKey(it.vkHome, KeyEvent.KEYCODE_MOVE_HOME)
            initNormalKey(it.vkEnd, KeyEvent.KEYCODE_MOVE_END)
            initNormalKey(it.vkPageUp, KeyEvent.KEYCODE_PAGE_UP)
            initNormalKey(it.vkPageDown, KeyEvent.KEYCODE_PAGE_DOWN)


            initNormalKey(it.vkLeft, KeyEvent.KEYCODE_DPAD_LEFT)
            initNormalKey(it.vkRight, KeyEvent.KEYCODE_DPAD_RIGHT)
            initNormalKey(it.vkUp, KeyEvent.KEYCODE_DPAD_UP)
            initNormalKey(it.vkDown, KeyEvent.KEYCODE_DPAD_DOWN)
            return it.root
        }
    }

    private fun createPage2(inflater: LayoutInflater, parent: ViewGroup): View {
        VirtualKeys2Binding.inflate(inflater, parent, false).let {
            initNormalKey(it.vkInsert, KeyEvent.KEYCODE_INSERT)
            initNormalKey(it.vkDelete, KeyEvent.KEYCODE_FORWARD_DEL)

            initNormalKey(it.vkF1, KeyEvent.KEYCODE_F1)
            initNormalKey(it.vkF2, KeyEvent.KEYCODE_F2)
            initNormalKey(it.vkF3, KeyEvent.KEYCODE_F3)
            initNormalKey(it.vkF4, KeyEvent.KEYCODE_F4)
            initNormalKey(it.vkF5, KeyEvent.KEYCODE_F5)
            initNormalKey(it.vkF6, KeyEvent.KEYCODE_F6)
            initNormalKey(it.vkF7, KeyEvent.KEYCODE_F7)
            initNormalKey(it.vkF8, KeyEvent.KEYCODE_F8)
            initNormalKey(it.vkF9, KeyEvent.KEYCODE_F9)
            initNormalKey(it.vkF10, KeyEvent.KEYCODE_F10)
            initNormalKey(it.vkF11, KeyEvent.KEYCODE_F11)
            initNormalKey(it.vkF12, KeyEvent.KEYCODE_F12)
            return it.root
        }
    }

    private fun createPage3(inflater: LayoutInflater, parent: ViewGroup): View {
        VirtualKeys3Binding.inflate(inflater, parent, false).let {
            it.editBox.setOnEditorActionListener { _, _, _ ->
                it.editBox.setText("")
                true
            }
            return it.root
        }
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
