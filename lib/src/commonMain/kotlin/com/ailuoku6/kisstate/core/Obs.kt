package com.ailuoku6.kuiklyKisstate.core

import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.timer.setTimeout

typealias KObservableList<T> = ObservableList<T>

fun <T> kObservable(initialValue: T) =
    observable(initialValue)

fun <T> kObservableList() =
    observableList<T>()

inline fun kSettimeout(timeout: Int = 0, noinline callback: () -> Unit) =
    setTimeout(timeout, callback)

