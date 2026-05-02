package ch.trancee.meshlink.storage

/**
 * Temporary in-memory stand-in for Android secure storage.
 *
 * The current implementation behaves like a simple key/value map so higher layers can be exercised
 * before a real Keystore-backed implementation lands.
 */
public class AndroidSecureStorage {
  private val valuesByKey: MutableMap<String, String> = mutableMapOf()

  public fun putString(key: String, value: String): Unit {
    require(key.isNotBlank()) { "AndroidSecureStorage key must not be blank." }
    valuesByKey[key] = value
  }

  public fun getString(key: String): String? {
    require(key.isNotBlank()) { "AndroidSecureStorage key must not be blank." }
    return valuesByKey[key]
  }

  public fun remove(key: String): Unit {
    require(key.isNotBlank()) { "AndroidSecureStorage key must not be blank." }
    valuesByKey.remove(key)
  }

  /** Erases every stored value. */
  public fun clear(): Unit {
    valuesByKey.clear()
  }
}
