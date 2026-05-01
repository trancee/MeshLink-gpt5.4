package ch.trancee.meshlink.crypto

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse

public class WycheproofCryptoTest {
    private val provider: JvmCryptoProvider = JvmCryptoProvider()
    private val json: Json = Json { ignoreUnknownKeys = true }

    @Test
    public fun chacha20Poly1305_vectorsMatchExpectedResults(): Unit {
        // Arrange
        val root: JsonObject = loadVectorFile(name = "chacha20_poly1305_test.json")

        // Act
        root.getValue("testGroups").jsonArray.forEach { groupElement ->
            val group: JsonObject = groupElement.jsonObject
            group.getValue("tests").jsonArray.forEach { testElement ->
                val test: JsonObject = testElement.jsonObject
                val tcId: Int = test.getValue("tcId").jsonPrimitive.int
                val result: String = test.getValue("result").jsonPrimitive.content
                val key: ByteArray = hex(test.getValue("key").jsonPrimitive.content)
                val nonce: ByteArray = hex(test.getValue("iv").jsonPrimitive.content)
                val aad: ByteArray = hex(test.getValue("aad").jsonPrimitive.content)
                val plaintext: ByteArray = hex(test.getValue("msg").jsonPrimitive.content)
                val ciphertext: ByteArray = hex(test.getValue("ct").jsonPrimitive.content) + hex(test.getValue("tag").jsonPrimitive.content)

                // Assert
                when (result) {
                    "valid" -> {
                        assertContentEquals(
                            expected = ciphertext,
                            actual = provider.chaCha20Poly1305Encrypt(
                                key = key,
                                nonce = nonce,
                                aad = aad,
                                plaintext = plaintext,
                            ),
                            message = "Wycheproof AEAD vector tcId=$tcId should encrypt to the expected ciphertext+tag",
                        )
                        assertContentEquals(
                            expected = plaintext,
                            actual = provider.chaCha20Poly1305Decrypt(
                                key = key,
                                nonce = nonce,
                                aad = aad,
                                ciphertext = ciphertext,
                            ),
                            message = "Wycheproof AEAD vector tcId=$tcId should decrypt to the expected plaintext",
                        )
                    }
                    "invalid" -> {
                        assertFails(
                            message = "Wycheproof AEAD vector tcId=$tcId should fail decryption for invalid ciphertext/tag combinations",
                        ) {
                            provider.chaCha20Poly1305Decrypt(
                                key = key,
                                nonce = nonce,
                                aad = aad,
                                ciphertext = ciphertext,
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    @Test
    public fun ed25519_vectorsMatchExpectedResults(): Unit {
        // Arrange
        val root: JsonObject = loadVectorFile(name = "ed25519_test.json")

        // Act
        root.getValue("testGroups").jsonArray.forEach { groupElement ->
            val group: JsonObject = groupElement.jsonObject
            val publicKey: ByteArray = hex(
                group.getValue("publicKey").jsonObject.getValue("pk").jsonPrimitive.content,
            )
            group.getValue("tests").jsonArray.forEach { testElement ->
                val test: JsonObject = testElement.jsonObject
                val tcId: Int = test.getValue("tcId").jsonPrimitive.int
                val result: String = test.getValue("result").jsonPrimitive.content
                val message: ByteArray = hex(test.getValue("msg").jsonPrimitive.content)
                val signature: ByteArray = hex(test.getValue("sig").jsonPrimitive.content)

                // Assert
                if (result == "valid" || result == "invalid") {
                    val actualResult: Result<Boolean> = runCatching {
                        provider.ed25519Verify(
                            publicKey = publicKey,
                            message = message,
                            signature = signature,
                        )
                    }
                    if (result == "valid") {
                        assertEquals(
                            expected = true,
                            actual = actualResult.getOrThrow(),
                            message = "Wycheproof Ed25519 vector tcId=$tcId should verify successfully",
                        )
                    } else {
                        if (actualResult.isSuccess) {
                            assertEquals(
                                expected = false,
                                actual = actualResult.getOrThrow(),
                                message = "Wycheproof Ed25519 vector tcId=$tcId should be rejected",
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    public fun x25519_vectorsMatchExpectedResults(): Unit {
        // Arrange
        val root: JsonObject = loadVectorFile(name = "x25519_test.json")

        // Act
        root.getValue("testGroups").jsonArray.forEach { groupElement ->
            val group: JsonObject = groupElement.jsonObject
            group.getValue("tests").jsonArray.forEach { testElement ->
                val test: JsonObject = testElement.jsonObject
                val tcId: Int = test.getValue("tcId").jsonPrimitive.int
                val result: String = test.getValue("result").jsonPrimitive.content
                val privateKey: ByteArray = hex(test.getValue("private").jsonPrimitive.content)
                val publicKey: ByteArray = hex(test.getValue("public").jsonPrimitive.content)
                val expectedSharedSecret: ByteArray = hex(test.getValue("shared").jsonPrimitive.content)
                val actualResult: Result<ByteArray> = runCatching {
                    provider.x25519(privateKey = privateKey, publicKey = publicKey)
                }

                // Assert
                if (result == "valid") {
                    assertContentEquals(
                        expected = expectedSharedSecret,
                        actual = actualResult.getOrThrow(),
                        message = "Wycheproof X25519 vector tcId=$tcId should derive the expected shared secret",
                    )
                } else if (result == "invalid") {
                    if (actualResult.isSuccess) {
                        assertFalse(
                            actual = actualResult.getOrThrow().contentEquals(expectedSharedSecret),
                            message = "Wycheproof X25519 vector tcId=$tcId should not derive the valid shared secret for invalid inputs",
                        )
                    }
                }
            }
        }
    }

    @Test
    public fun hkdfSha256_vectorsMatchExpectedResults(): Unit {
        // Arrange
        val root: JsonObject = loadVectorFile(name = "hkdf_sha256_test.json")

        // Act
        root.getValue("testGroups").jsonArray.forEach { groupElement ->
            val group: JsonObject = groupElement.jsonObject
            group.getValue("tests").jsonArray.forEach { testElement ->
                val test: JsonObject = testElement.jsonObject
                val tcId: Int = test.getValue("tcId").jsonPrimitive.int
                val result: String = test.getValue("result").jsonPrimitive.content
                val ikm: ByteArray = hex(test.getValue("ikm").jsonPrimitive.content)
                val salt: ByteArray = hex(test.getValue("salt").jsonPrimitive.content)
                val info: ByteArray = hex(test.getValue("info").jsonPrimitive.content)
                val outputLength: Int = test.getValue("size").jsonPrimitive.int
                val expectedOkm: ByteArray = hex(test.getValue("okm").jsonPrimitive.content)

                // Assert
                if (result == "valid") {
                    assertContentEquals(
                        expected = expectedOkm,
                        actual = provider.hkdfSha256(
                            ikm = ikm,
                            salt = salt,
                            info = info,
                            outputLength = outputLength,
                        ),
                        message = "Wycheproof HKDF vector tcId=$tcId should derive the expected output keying material",
                    )
                }
            }
        }
    }

    private fun loadVectorFile(name: String): JsonObject {
        val resourcePath: String = "/wycheproof/$name"
        val content: String = requireNotNull(javaClass.getResource(resourcePath)) {
            "Missing Wycheproof resource: $resourcePath"
        }.readText()
        return json.parseToJsonElement(content).jsonObject
    }

    private fun hex(value: String): ByteArray {
        if (value.isEmpty()) {
            return byteArrayOf()
        }
        return value.chunked(size = 2)
            .map { chunk -> chunk.toInt(radix = 16).toByte() }
            .toByteArray()
    }
}
