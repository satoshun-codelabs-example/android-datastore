package com.codelab.android.datastore.ui

import android.content.Context
import androidx.datastore.DataStore
import androidx.datastore.preferences.Preferences
import androidx.datastore.preferences.createDataStore
import androidx.datastore.preferences.edit
import androidx.datastore.preferences.emptyPreferences
import androidx.datastore.preferences.preferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

inline fun <reified T : Any> Context.singleDataStore(
  name: String
): SingleDataStore<T> {
  val key = preferencesKey<T>(name)
  return SingleDataStoreImpl(this, key)
}

class SingleDataStoreImpl<T : Any>(
  context: Context,
  private val key: Preferences.Key<T>
) : SingleDataStore<T> {
  private val dataStore: DataStore<Preferences> =
    context.createDataStore(name = key.name)

  override val data: Flow<T>
    get() = dataStore.data
      .catch { exception ->
        // dataStore.data throws an IOException when an error is encountered when reading data
        if (exception is IOException) {
          emit(emptyPreferences())
        } else {
          throw exception
        }
      }
      .map { preferences ->
        preferences[key] ?: throw IllegalStateException("use default value")
      }

  override suspend fun getValue(): T {
    return data.first()
  }

  override suspend fun setValue(t: T) {
    dataStore.edit { it[key] = t }
  }
}

interface SingleDataStore<T> {
  val data: Flow<T>

  suspend fun getValue(): T
  suspend fun setValue(t: T)
}
