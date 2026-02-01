package com.ailuoku6.kisstate.core

import com.ailuoku6.kuiklyKisstate.core.Watcher
import com.ailuoku6.kuiklyKisstate.core.kSettimeout

interface ScheduleTask {
    fun run()
}

class WatcherTask(
    private val watcher: Watcher
) : ScheduleTask {

    override fun run() {
        watcher.update()
    }
}

class Scheduler {

    private val queue = mutableListOf<ScheduleTask>()
    private val taskMap = mutableMapOf<Watcher, WatcherTask>()
    private var hasNextConsumer = false

    fun addWatcher(watcher: Watcher, immediate: Boolean = false) {
        val task = taskMap.getOrPut(watcher) {
            WatcherTask(watcher)
        }
        addTask(task, immediate)
    }

    private fun addTask(task: ScheduleTask, immediate: Boolean) {
        queue.add(task)
        if (immediate) {
            run()
            return
        }
        startTask()
    }

    private fun startTask() {
        if (hasNextConsumer) return
        hasNextConsumer = true
        kSettimeout{ run() }
    }

    // 消费任务
    private fun run() {
        val runSet = mutableSetOf<ScheduleTask>()
        val tasks = queue.toList()

        queue.clear()
        hasNextConsumer = false

        for (task in tasks) {
            if (runSet.add(task)) {
                task.run()
            }
        }
    }
}


val scheduler = Scheduler()
