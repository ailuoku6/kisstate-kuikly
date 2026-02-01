package com.ailuoku6.kuiklyKisstate.core

import com.ailuoku6.kisstate.core.scheduler

internal object DepContext {
    var target: Watcher? = null
    val targetStack = mutableListOf<Watcher>()

    fun pushTarget(target: Watcher) {
        if (this.target != null) targetStack.add(this.target!!)
        this.target = target
    }

    fun popTarget() {
        this.target = if (targetStack.isNotEmpty()) targetStack.removeAt(targetStack.lastIndex) else null
    }
}

class Dep {
    private val subscribers = mutableListOf<Watcher>()

    fun depend() {
        if (DepContext.target != null) {
            if (!subscribers.contains(DepContext.target!!)) {
                subscribers.add(DepContext.target!!)
                DepContext.target!!.addDep(this)
            }
        }
    }

    fun notifyChange() {
        // Copy to avoid ConcurrentModificationException if update triggers dependency changes
        val subs = subscribers.toList()
        subs.forEach { watcher ->
            scheduler.addWatcher(watcher)
        }

    }

    fun removeSub(watcher: Watcher) {
        subscribers.remove(watcher)
    }
}

interface Watcher {
    fun update()
    fun addDep(dep: Dep)
    fun teardown()
}
