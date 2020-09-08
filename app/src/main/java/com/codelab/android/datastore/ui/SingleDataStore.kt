package com.codelab.android.datastore.ui

import android.content.Context
import androidx.datastore.DataMigration
import androidx.datastore.DataStore
import androidx.datastore.preferences.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

inline fun <reified T : Any> Context.singleDataStore(
  name: String,
  migrationKey: String? = null
): SingleDataStore<T> {
  val key = preferencesKey<T>(name)
  val migrations =
    if (migrationKey != null) listOf(SharedPreferencesMigration(this, migrationKey))
    else emptyList()
  return SingleDataStoreImpl(this, key, migrations)
}

class SingleDataStoreImpl<T : Any>(
  context: Context,
  private val key: Preferences.Key<T>,
  migrations: List<DataMigration<Preferences>>
) : SingleDataStore<T> {
  private val dataStore: DataStore<Preferences> =
    context.createDataStore(name = key.name, migrations = migrations)

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
