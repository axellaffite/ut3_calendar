package com.edt.ut3.misc

import android.content.Context

/**
 * This is the base class to
 * specify a State into a viewModel.
 *
 * All states must extends this class.
 */
sealed class BaseState {
    abstract class FeatureState: BaseState()

    abstract class Failure: BaseState() {
        abstract fun reason(context: Context): String
    }

    abstract class Information: BaseState() {
        abstract fun get(context: Context): String
    }
}