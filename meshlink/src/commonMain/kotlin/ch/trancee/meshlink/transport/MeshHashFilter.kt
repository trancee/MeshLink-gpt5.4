package ch.trancee.meshlink.transport

public class MeshHashFilter(private val maxEntries: Int = DEFAULT_MAX_ENTRIES) {
  private val entries: MutableList<String> = mutableListOf()

  init {
    require(maxEntries > 0) { "MeshHashFilter maxEntries must be greater than 0." }
  }

  public fun isDuplicate(meshHash: ByteArray): Boolean {
    require(meshHash.isNotEmpty()) { "MeshHashFilter meshHash must not be empty." }

    val normalizedHash: String = meshHash.toHexString()
    val existingIndex: Int = entries.indexOf(normalizedHash)
    if (existingIndex >= 0) {
      entries.removeAt(index = existingIndex)
      entries += normalizedHash
      return true
    }

    if (entries.size == maxEntries) {
      entries.removeAt(index = 0)
    }
    entries += normalizedHash
    return false
  }

  public fun clear(): Unit {
    entries.clear()
  }

  public companion object {
    public const val DEFAULT_MAX_ENTRIES: Int = 128
  }
}

private fun ByteArray.toHexString(): String {
  return joinToString(separator = "") { byte ->
    (byte.toInt() and 0xFF).toString(radix = 16).padStart(length = 2, padChar = '0')
  }
}
