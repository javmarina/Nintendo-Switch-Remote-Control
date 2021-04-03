package com.javmarina.util


/**
 * Runnable that executes in a loop until stopped.
 */
abstract class StoppableLoop : Runnable {

    private var running: Boolean = false
    private var stoppedCallback: StoppedCallback? = null

    abstract fun loop()

    override fun run() {
        running = true
        while (running) {
            loop()
        }
        stoppedCallback?.onStopped()
    }

    @JvmOverloads
    fun stop(stoppedCallback: StoppedCallback? = null) {
        this.stoppedCallback = stoppedCallback
        this.running = false
    }

    interface StoppedCallback {
        fun onStopped()
    }
}