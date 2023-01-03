package com.nct.xmusicstation.data.local.prefs

import com.toan_itc.core.kotlinify.threads.RunnableThread
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Created by Toan.IT on 11/1/17.
 * Email:Huynhvantoan.itc@gmail.com
 */

sealed class PreferencesProperty<T : Any> : ReadWriteProperty<Any, T> {
    val pref = PreferenceManager

    companion object {
        fun string() = StringProperty(RunnableThread.CURRENT)
        fun backgroundString() = StringProperty(RunnableThread.BACKGROUND)

        fun int() = IntProperty(RunnableThread.CURRENT)
        fun backgroundInt() = StringProperty(RunnableThread.BACKGROUND)

        fun float() = FloatProperty(RunnableThread.CURRENT)
        fun backgroundFloat() = StringProperty(RunnableThread.BACKGROUND)

        fun long() = LongProperty(RunnableThread.CURRENT)
        fun backgroundLong() = StringProperty(RunnableThread.BACKGROUND)

        fun boolean() = BooleanProperty(RunnableThread.CURRENT)
        fun backgroundBoolean() = StringProperty(RunnableThread.BACKGROUND)

        fun stringSet() = StringSetProperty(RunnableThread.CURRENT)
        fun backgroundStringSet() = StringProperty(RunnableThread.BACKGROUND)
    }

    class StringProperty internal constructor(private val runnableThread: RunnableThread) : PreferencesProperty<String>() {
        override fun getValue(thisRef: Any, property: KProperty<*>) =
                pref.getString(property.name, "")


        override fun setValue(thisRef: Any, property: KProperty<*>, value: String) {
            com.toan_itc.core.kotlinify.threads.run(runnableThread) { pref.put(property.name, value) }
        }
    }

    class IntProperty internal constructor(private val runnableThread: RunnableThread) : PreferencesProperty<Int>() {
        override fun getValue(thisRef: Any, property: KProperty<*>) =
                pref.getInt(property.name, 0)


        override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) {
            com.toan_itc.core.kotlinify.threads.run(runnableThread) { pref.put(property.name, value) }
        }
    }

    class FloatProperty internal constructor(private val runnableThread: RunnableThread) : PreferencesProperty<Float>() {
        override fun getValue(thisRef: Any, property: KProperty<*>) =
                pref.getFloat(property.name, 0f)


        override fun setValue(thisRef: Any, property: KProperty<*>, value: Float) {
            com.toan_itc.core.kotlinify.threads.run(runnableThread) { pref.put(property.name, value) }
        }
    }

    class BooleanProperty internal constructor(private val runnableThread: RunnableThread) : PreferencesProperty<Boolean>() {
        override fun getValue(thisRef: Any, property: KProperty<*>) =
                pref.getBoolean(property.name, false)


        override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
            com.toan_itc.core.kotlinify.threads.run(runnableThread) { pref.put(property.name, value) }
        }
    }

    class LongProperty internal constructor(private val runnableThread: RunnableThread) : PreferencesProperty<Long>() {
        override fun getValue(thisRef: Any, property: KProperty<*>) =
                pref.getLong(property.name, 0)


        override fun setValue(thisRef: Any, property: KProperty<*>, value: Long) {
            com.toan_itc.core.kotlinify.threads.run(runnableThread) { pref.put(property.name, value) }
        }
    }

    class StringSetProperty internal constructor(private val runnableThread: RunnableThread) : PreferencesProperty<Set<String>>() {
        override fun getValue(thisRef: Any, property: KProperty<*>) =
                pref.getStringSet(property.name, emptySet()) ?: emptySet<String>()

        override fun setValue(thisRef: Any, property: KProperty<*>, value: Set<String>) {
            com.toan_itc.core.kotlinify.threads.run(runnableThread) { pref.put(property.name, value) }
        }
    }
}
