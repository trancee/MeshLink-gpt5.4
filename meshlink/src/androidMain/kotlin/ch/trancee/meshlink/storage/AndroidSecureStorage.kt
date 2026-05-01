package ch.trancee.meshlink.storage

public class AndroidSecureStorage {
    private val valuesByKey: MutableMap<String, String> = mutableMapOf()

    public fun putString(
        key: String,
        value: String,
    ): Unit {
        require(key.isNotBlank()) {
            "AndroidSecureStorage key must not be blank."
        }
        valuesByKey[key] = value
    }

    public fun getString(key: String): String? {
        require(key.isNotBlank()) {
            "AndroidSecureStorage key must not be blank."
        }
        return valuesByKey[key]
    }

    public fun remove(key: String): Unit {
        require(key.isNotBlank()) {
            "AndroidSecureStorage key must not be blank."
        }
        valuesByKey.remove(key)
    }
}
