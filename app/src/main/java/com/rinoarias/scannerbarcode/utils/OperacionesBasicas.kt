package com.rinoarias.scannerbarcode.utils

import kotlin.math.roundToInt


class OperacionesBasicas {

    companion object {
        fun StringDosDecimales(num: Double) : String {
            return ((num * 100.0).roundToInt() / 100.0).toString()
        }
    }
}