package com.nct.xmusicstation.data.local.prefs

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.toan_itc.core.kotlinify.exceptions.InitializeException
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Created by Toan.IT on 11/1/17.
 * Email:Huynhvantoan.itc@gmail.com
 */

@SuppressLint("CommitPrefEdits")
@Suppress("unused","UNCHECKED_CAST")
object PreferenceManager{
    private lateinit var sharedPreferences: SharedPreferences
    private var initialized = false
    private var mGson: Gson = Gson()
    /**
     * @param application Global context in order use everywhere
     *      without the need for context every time
     * @param sharedPreferencesName custom name for SharedPreferences
     * @return instance of the GlobalSharedPreferences
     */
    fun initialize(application: Application, sharedPreferencesName: String): PreferenceManager {
        sharedPreferences = application.getSharedPreferences(sharedPreferencesName+"SharedPreferences", Context.MODE_PRIVATE)
        initialized = true
        return this
    }

    //region editor
    private val edit: SharedPreferences.Editor by lazy { requiredOrThrow(sharedPreferences.edit()) }
    //endregion editor

    //region get
    fun getAll(): Map<String, *> = requiredOrThrow(sharedPreferences.all)

    fun getInt(key: String, defaultValue: Int) =
            requiredOrThrow(sharedPreferences.getInt(key, defaultValue))

    fun getInt(key: String) =
            requiredOrThrow(sharedPreferences.getInt(key, 0))

    fun getLong(key: String, defaultValue: Long) =
            requiredOrThrow(sharedPreferences.getLong(key, defaultValue))

    fun getFloat(key: String, defaultValue: Float) =
            requiredOrThrow(sharedPreferences.getFloat(key, defaultValue))

    fun getBoolean(key: String, defaultValue: Boolean) =
            requiredOrThrow(sharedPreferences.getBoolean(key, defaultValue))

    fun getString(key: String, defaultValue: String) : String =
            requiredOrThrow(sharedPreferences.getString(key, defaultValue))?:""

    fun getString(key: String) : String=
            requiredOrThrow(sharedPreferences.getString(key, ""))?:""

    fun getStringSet(key: String, defaultValue: Set<String>): MutableSet<String>? =
            requiredOrThrow(sharedPreferences.getStringSet(key, defaultValue))

    infix fun String.forStringSet(defaultValue: Set<String>): MutableSet<String>? =
            requiredOrThrow(sharedPreferences.getStringSet(this, defaultValue))

    infix fun String.forInt(defaultValue: Int) =
            requiredOrThrow(sharedPreferences.getInt(this, defaultValue))

    infix fun String.forLong(defaultValue: Long) =
            requiredOrThrow(sharedPreferences.getLong(this, defaultValue))

    infix fun String.forFloat(defaultValue: Float) =
            requiredOrThrow(sharedPreferences.getFloat(this, defaultValue))

    infix fun String.forBoolean(defaultValue: Boolean) =
            requiredOrThrow(sharedPreferences.getBoolean(this, defaultValue))

    infix fun String.forString(defaultValue: String) : String =
            requiredOrThrow(sharedPreferences.getString(this, defaultValue))?:""

    //endregion get

    //region contains
    operator fun contains(key: String) = requiredOrThrow(sharedPreferences.contains(key))
    //endregion contains

    //region put
    fun put(key: String, value: String) = also { edit.putString(key, value).commit() }

    fun put(key: String, value: Int) = also { edit.putInt(key, value).commit() }

    fun put(key: String, value: Long) = also { edit.putLong(key, value).commit() }

    fun put(key: String, value: Boolean) = also { edit.putBoolean(key, value).commit() }

    fun put(key: String, value: Float) = also { edit.putFloat(key, value).commit() }

    fun put(key: String, value: Set<String>) = also { edit.putStringSet(key, value).commit() }

    @Synchronized
    fun put(key: String, value: Any) = also { edit.putString(key, mGson.toJson(value)).commit() }

    @Synchronized
    fun put(key: String, value: List<*>) = also { edit.putString(key, mGson.toJson(value)).commit() }

    fun <T> getEntity(key: String, classOfT: Class<T>): T? {
        var entity: T? = null
        try {
            entity = mGson.fromJson(getString(key, ""), classOfT)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return entity
    }

    fun <T : Any> getList(key: String, classOfT: Class<T>?): MutableList<T> {
        val list = ArrayList<T>()
        try {
            mGson.fromJson<ArrayList<JsonElement>>(getString(key, ""), object : TypeToken<ArrayList<JsonElement>>() {}.type).mapNotNullTo(list) { mGson.fromJson(it, classOfT) }
        } catch (jsonParseException: JsonParseException) {
            jsonParseException.printStackTrace()
        } catch (jsonSyntaxException: JsonSyntaxException) {
            jsonSyntaxException.printStackTrace()
        }
        return list
    }

    infix fun String.put(value: Any) {
        also {
            when (value) {
                is String ->
                    put(this, value)
                is Int ->
                    put(this, value)
                is Long ->
                    put(this, value)
                is Boolean ->
                    put(this, value)
                is Float ->
                    put(this, value)
                is Set<*> -> {
                    put(this, value.map { it.toString() }.toSet())
                }
            }
        }
    }

    operator fun <T> get(key: String, defaultValue: T): T? where T : Any {
        return when (defaultValue) {
            String::class -> getString(key, defaultValue as String) as T?
            Int::class -> getInt(key, defaultValue as? Int ?: -1) as T?
            Boolean::class -> getBoolean(key, defaultValue as? Boolean == true) as T?
            Float::class -> getFloat(key, defaultValue as? Float ?: -1f) as T?
            Long::class -> getLong(key, defaultValue as? Long ?: -1) as T?
            else -> null
        }
    }

/*  //Test get smart class
    operator inline fun <reified T : Any> String.get(key: String, defaultValue: T? = null): T? {
        return when (T::class) {
            String::class -> getString(key, defaultValue as String) as T?
            Int::class -> getInt(key, defaultValue as? Int ?: -1) as T?
            Boolean::class -> getBoolean(key, defaultValue as? Boolean == true) as T?
            Float::class -> getFloat(key, defaultValue as? Float ?: -1f) as T?
            Long::class -> getLong(key, defaultValue as? Long ?: -1) as T?
            else -> throw UnsupportedOperationException("Not yet implemented")
        }
    }*/

    operator fun String.plusAssign(value: Any) = this.put(value)

    operator fun plusAssign(keyValuePair: Pair<String, Any>) =
            keyValuePair.first.put(keyValuePair.second)

    operator fun plus(keyValuePair: Pair<String, Any>) =
            keyValuePair.first.put(keyValuePair.second)

    //endregion put

    //region remove
    fun remove(key: String) = also { edit.remove(key).commit() }

    operator fun minus(key: String) = remove(key)
    //endregion remove

    //region commit/apply
    fun commit() = edit.commit()

    fun apply() = edit.apply()
    //endregion commit/apply

    /**
     * @param returnIfInitialized object to be returned if class is initialized
     * @throws InitializeException
     */
    @Throws(InitializeException::class)
    private fun <T> requiredOrThrow(returnIfInitialized: T) = if (initialized) {
        returnIfInitialized
    } else {
        throw InitializeException("GlobalSharedPreferences", "initialize")
    }

    //Test Beta
    //Save object Date in SharedPreferences
    fun SharedPreferences.date(def: Date = Date(), key: String? = null): ReadWriteProperty<Any, Date> {
        return object : ReadWriteProperty<Any, Date> {
            override fun getValue(thisRef: Any, property: KProperty<*>): Date {
                val dateLong = getLong(key ?: property.name, def.time)
                return Date(dateLong)
            }

            override fun setValue(thisRef: Any, property: KProperty<*>, value: Date) =
                    edit().putLong(key ?: property.name, value.time).apply()
        }
    }
    //How to use...
    /*val prefs: SharedPreferences by lazy { App.instance.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE) }
    var currentDate by prefs.date()

    currentDate = Date()*/
}


inline fun pref(sharedPreferences: PreferenceManager.() -> Unit) = with(PreferenceManager) {
    also {
        sharedPreferences()
        apply()
    }
}

