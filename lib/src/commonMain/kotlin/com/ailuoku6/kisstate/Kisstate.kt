package com.ailuoku6.kuiklyKisstate

import com.ailuoku6.kuiklyKisstate.core.Dep
import com.ailuoku6.kuiklyKisstate.core.DepContext
import com.ailuoku6.kuiklyKisstate.core.Watcher
import com.ailuoku6.kuiklyKisstate.core.KObservableList
import com.ailuoku6.kuiklyKisstate.core.kObservable
import com.ailuoku6.kuiklyKisstate.core.kObservableList
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class KisContext {
    private val watchers = mutableListOf<Watcher>()

    internal fun add(watcher: Watcher) {
        watchers.add(watcher)
    }

    fun dispose() {
        watchers.forEach { it.teardown() }
        watchers.clear()
    }
}

class KisstateObservable<T>(private val wrapped: ReadWriteProperty<Any, T>) {
    private val dep = Dep()

    operator fun getValue(thisRef: Any, property: KProperty<*>): T {
        dep.depend()
        return wrapped.getValue(thisRef, property)
    }

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        if (wrapped.getValue(thisRef, property) != value) {
            wrapped.setValue(thisRef, property, value)
            dep.notifyChange()
        }
    }
}

fun <T> kisstate(initialValue: T): KisstateObservable<T> {
    val obs = kObservable(initialValue)
    return KisstateObservable(obs)
}

class KissListProxy<T>(
    private val dep: Dep,
): MutableList<T> {

    private val inner: KObservableList<T> by kObservableList<T>()

    fun toObservableList(): KObservableList<T> {
        return inner
    }

    /** 所有读操作：建立依赖 */
    private fun depend(): Unit {
        dep.depend()
    }

    /** 所有写操作：通知更新 */
    private inline fun <R> mutate(block: () -> R): R {
        val r = block()
        dep.notifyChange()
        return r
    }

    // ---------- 读操作 ----------
    override val size: Int
        get() {
            depend()
            return inner.size
        }

    override fun get(index: Int): T {
        depend()
        return inner[index]
    }

    override fun indexOf(element: T): Int {
        depend()
        return inner.indexOf(element)
    }

    override fun contains(element: T): Boolean {
        depend()
        return inner.contains(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        depend()
        return inner.containsAll(elements)
    }

    override fun isEmpty(): Boolean {
        depend()
        return inner.isEmpty()
    }

    override fun iterator(): MutableIterator<T> {
        depend()
        return object : MutableIterator<T> {
            private val it = inner.iterator()
            override fun hasNext() = it.hasNext()
            override fun next() = it.next()
            override fun remove() = mutate { it.remove() }
        }
    }

    override fun lastIndexOf(element: T): Int {
        depend()
        return inner.lastIndexOf(element)
    }

    override fun listIterator(): MutableListIterator<T> {
        depend()
        return listIterator(0)
    }

    override fun listIterator(index: Int): MutableListIterator<T> {
        depend()
        val it = inner.listIterator(index)
        return object : MutableListIterator<T> {
            override fun hasNext() = it.hasNext()
            override fun next() = it.next()
            override fun hasPrevious() = it.hasPrevious()
            override fun previous() = it.previous()
            override fun nextIndex() = it.nextIndex()
            override fun previousIndex() = it.previousIndex()
            override fun remove() = mutate { it.remove() }
            override fun set(element: T) = mutate { it.set(element) }
            override fun add(element: T) = mutate { it.add(element) }
        }
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        depend()
        return inner.subList(fromIndex, toIndex)
    }

    // ---------- 写操作 ----------
    override fun add(element: T): Boolean =
        mutate { inner.add(element) }

    override fun add(index: Int, element: T) =
        mutate { inner.add(index, element) }

    override fun addAll(elements: Collection<T>): Boolean =
        mutate { inner.addAll(elements) }

    override fun addAll(index: Int, elements: Collection<T>): Boolean =
        mutate { inner.addAll(index, elements) }

    override fun remove(element: T): Boolean =
        mutate { inner.remove(element) }

    override fun removeAt(index: Int): T =
        mutate { inner.removeAt(index) }

    override fun removeAll(elements: Collection<T>): Boolean =
        mutate { inner.removeAll(elements) }

    override fun retainAll(elements: Collection<T>): Boolean =
        mutate { inner.retainAll(elements) }

    override fun clear() =
        mutate { inner.clear() }

    override fun set(index: Int, element: T): T =
        mutate { inner.set(index, element) }
}

class KisstateObservableList<T> {
    private val dep = Dep()
    private val value = KissListProxy<T>(dep)

    operator fun getValue(thisRef: Any?, property: KProperty<*>): KissListProxy<T> {
        dep.depend()
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: MutableList<T>) {
        value.clear()
        value.addAll(value)
        dep.notifyChange()
    }
}



fun <T : Any> kisstateList(): KisstateObservableList<T> {
    return KisstateObservableList<T>()
}



class Computed<T>(private val wrapped: ReadWriteProperty<Any, T?>, private val getter: () -> T) : Watcher {
//    private var value: T? = null
    private var dirty = true
    private val dep = Dep()
    private val deps = mutableListOf<Dep>()
    private var thisRef: Any? = null
    private var property: KProperty<*>? = null

    override fun update() {
        dirty = true
        if(this.thisRef != null&&this.property != null){
            get(this.thisRef!!, this.property!!)
        }
        dep.notifyChange()
    }

    override fun addDep(dep: Dep) {
        deps.add(dep)
    }

    override fun teardown() {
        deps.forEach { it.removeSub(this) }
        deps.clear()
    }

    fun get(thisRef: Any, property: KProperty<*>): T? {
        dep.depend()
        if (dirty) {
            teardown()
            DepContext.pushTarget(this)
            wrapped.setValue(thisRef, property, getter())
            DepContext.popTarget()
            dirty = false
        }
        val value = wrapped.getValue(thisRef, property)
        return value
    }
    
    operator fun getValue(thisRef: Any, property: KProperty<*>): T? {
        if(this.thisRef == null || this.property == null){
            this.thisRef = thisRef
            this.property = property
        }
        return get(thisRef, property)
    }
}

fun <T> computed(context: KisContext, getter: () -> T): Computed<T> {
    val v = kObservable<T?>(null)
    val c = Computed(v, getter)
    context.add(c)
    return c
}

class Watch<T>(
    private val source: () -> T,
    private val callback: (T, T) -> Unit
) : Watcher {
    private var oldValue: T? = null
    private val deps = mutableListOf<Dep>()

    init {
        DepContext.pushTarget(this)
        oldValue = source()
        DepContext.popTarget()
    }

    override fun update() {
        teardown()
        DepContext.pushTarget(this)
        val newValue = source()
        DepContext.popTarget()

        // KissListProxy实例不会变，所以加个逻辑绕过比较
        if (newValue != oldValue || newValue is KissListProxy<*>) {
            callback(newValue, oldValue!!)
            oldValue = newValue
        }
    }

    override fun addDep(dep: Dep) {
        deps.add(dep)
    }

    override fun teardown() {
        deps.forEach { it.removeSub(this) }
        deps.clear()
    }
}

fun <T> watch(context: KisContext, source: () -> T, callback: (newValue: T, preValue: T) -> Unit): Watch<T> {
    val w = Watch(source, callback)
    context.add(w)
    return w
}
