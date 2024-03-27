/*
 * Copyright (c) 2024  Gaurav Ujjwal.
 *
 * SPDX-License-Identifier:  GPL-3.0-or-later
 *
 * See COPYING.txt for more details.
 */

package com.gaurav.avnc.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData

/**
 * ViewModel for profile editor
 */
class ExtraKeysViewModel(app: Application) : BaseViewModel(app) {

    val isAltChecked = MutableLiveData(false)
    val isCtrlChecked = MutableLiveData(false)
    val isShiftChecked = MutableLiveData(false)
    val isMetaChecked = MutableLiveData(false)

    val isAltLocked = MutableLiveData(false)
    val isCtrlChecked = MutableLiveData(false)
    val isShiftChecked = MutableLiveData(false)
    val isMetaChecked = MutableLiveData(false)

}