package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.PeerIdHex
import ch.trancee.meshlink.transport.VirtualMeshTransport
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

public class MeshEngineApiIdentityTest {
  @Test
  public fun pseudonymAt_delegatesToTheConfiguredPseudonymRotator(): Unit {
    // Arrange
    val engine =
      MeshEngine.create(
        config = MeshEngineConfig.default(),
        transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233")),
        cryptoProvider = FakeCryptoProvider(),
      )
    val identityKey = byteArrayOf(0x11, 0x22)
    val expected =
      engine.pseudonymRotator.pseudonymAt(identityKey = identityKey, timestampMillis = 1_000L)

    // Act
    val actual = engine.pseudonymAt(identityKey = identityKey, timestampMillis = 1_000L)

    // Assert
    assertContentEquals(expected = expected, actual = actual)
  }

  @Test
  public fun verifyPseudonym_acceptsCurrentWindowCandidatesAndRejectsOlderOnes(): Unit {
    // Arrange
    val engine =
      MeshEngine.create(
        config = MeshEngineConfig.default(),
        transport = VirtualMeshTransport(localPeerId = PeerIdHex(value = "00112233")),
        cryptoProvider = FakeCryptoProvider(),
      )
    val identityKey = byteArrayOf(0x11, 0x22)
    val validCandidate = engine.pseudonymAt(identityKey = identityKey, timestampMillis = 1_000L)
    val invalidCandidate =
      engine.pseudonymRotator.pseudonymForEpoch(identityKey = identityKey, epoch = 5L)

    // Act
    val validActual =
      engine.verifyPseudonym(
        candidate = validCandidate,
        identityKey = identityKey,
        timestampMillis = 1_000L,
      )
    val invalidActual =
      engine.verifyPseudonym(
        candidate = invalidCandidate,
        identityKey = identityKey,
        timestampMillis = 10_000L,
      )

    // Assert
    assertEquals(expected = true, actual = validActual)
    assertEquals(expected = false, actual = invalidActual)
  }
}
