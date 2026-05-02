package ch.trancee.meshlink.crypto.noise

import ch.trancee.meshlink.crypto.CryptoProvider
import ch.trancee.meshlink.crypto.CryptoProviderFactory
import ch.trancee.meshlink.crypto.KeyPair
import ch.trancee.meshlink.wire.messages.HandshakeMessage
import ch.trancee.meshlink.wire.messages.HandshakeRound

/** Role-aware controller for the three-message Noise XX exchange. */
public class NoiseXXHandshake
private constructor(
  private val role: HandshakeRole,
  private val dependencies: NoiseHandshakeDependencies,
  private val dhCache: DhCache,
) {
  private val provider: CryptoProvider = dependencies.provider
  private val localStaticKeyPair: KeyPair = dependencies.localStaticKeyPair
  private val state: HandshakeState = HandshakeState(role = role)
  private val symmetricState: SymmetricState = createInitialSymmetricState(provider = provider)
  private var localEphemeralKeyPair: KeyPair? = null
  private var remoteEphemeralPublicKey: ByteArray? = null
  private var remoteStaticPublicKey: ByteArray? = null
  private var transportSession: NoiseSession? = null

  public constructor(
    role: HandshakeRole
  ) : this(role = role, dependencies = defaultDependencies(), dhCache = DhCache())

  internal constructor(
    role: HandshakeRole,
    provider: CryptoProvider,
    localStaticKeyPair: KeyPair = provider.generateX25519KeyPair(),
    dhCache: DhCache = DhCache(),
  ) : this(
    role = role,
    dependencies =
      NoiseHandshakeDependencies(provider = provider, localStaticKeyPair = localStaticKeyPair),
    dhCache = dhCache,
  )

  /** Produces the next outbound handshake message for the local role. */
  public fun createOutboundMessage(payload: ByteArray): HandshakeMessage {
    val round: HandshakeRound = expectedOutboundRound()
    val encodedPayload: ByteArray =
      when (round) {
        HandshakeRound.ONE -> writeMessageOne(payload = payload)
        HandshakeRound.TWO -> writeMessageTwo(payload = payload)
        HandshakeRound.THREE -> writeMessageThree(payload = payload)
      }
    state.recordSend()
    maybeEstablishTransportSession()
    return HandshakeMessage(round = round, payload = encodedPayload)
  }

  /** Validates and consumes the next inbound handshake message. */
  public fun receiveInboundMessage(message: HandshakeMessage): Unit {
    val expectedRound: HandshakeRound = expectedInboundRound()
    if (message.round != expectedRound) {
      throw IllegalArgumentException(
        "NoiseXXHandshake expected ${expectedRound.name} but received ${message.round.name} for ${role.name}."
      )
    }

    when (message.round) {
      HandshakeRound.ONE -> readMessageOne(message.payload)
      HandshakeRound.TWO -> readMessageTwo(message.payload)
      HandshakeRound.THREE -> readMessageThree(message.payload)
    }
    state.recordReceive()
    maybeEstablishTransportSession()
  }

  public fun isComplete(): Boolean = state.isComplete()

  internal fun transportSession(): NoiseSession? = transportSession

  internal fun remoteStaticPublicKey(): ByteArray? = remoteStaticPublicKey?.copyOf()

  private fun writeMessageOne(payload: ByteArray): ByteArray {
    val localEphemeral: KeyPair = localEphemeral()
    symmetricState.mixHash(data = localEphemeral.publicKey)
    return localEphemeral.publicKey + symmetricState.encryptAndHash(plaintext = payload)
  }

  private fun readMessageOne(payload: ByteArray): Unit {
    val reader = HandshakePayloadReader(payload)
    val remoteEphemeral: ByteArray = reader.read(length = X25519_KEY_SIZE)
    remoteEphemeralPublicKey = remoteEphemeral
    symmetricState.mixHash(data = remoteEphemeral)
    symmetricState.decryptAndHash(ciphertext = reader.readRemaining())
  }

  private fun writeMessageTwo(payload: ByteArray): ByteArray {
    val localEphemeral: KeyPair = localEphemeral()
    val remoteEphemeral: ByteArray = requireRemoteEphemeralPublicKey()
    symmetricState.mixHash(data = localEphemeral.publicKey)
    symmetricState.mixKey(
      inputKeyMaterial = dh(privateKey = localEphemeral.secretKey, publicKey = remoteEphemeral)
    )
    val encryptedStatic: ByteArray =
      symmetricState.encryptAndHash(plaintext = localStaticKeyPair.publicKey)
    symmetricState.mixKey(
      inputKeyMaterial = dh(privateKey = localStaticKeyPair.secretKey, publicKey = remoteEphemeral)
    )
    val encryptedPayload: ByteArray = symmetricState.encryptAndHash(plaintext = payload)
    return localEphemeral.publicKey + encryptedStatic + encryptedPayload
  }

  private fun readMessageTwo(payload: ByteArray): Unit {
    val reader = HandshakePayloadReader(payload)
    val remoteEphemeral: ByteArray = reader.read(length = X25519_KEY_SIZE)
    remoteEphemeralPublicKey = remoteEphemeral
    symmetricState.mixHash(data = remoteEphemeral)
    symmetricState.mixKey(
      inputKeyMaterial =
        dh(privateKey = requireLocalEphemeral().secretKey, publicKey = remoteEphemeral)
    )
    val decryptedStatic: ByteArray =
      symmetricState.decryptAndHash(ciphertext = reader.read(length = encryptedStaticLength()))
    remoteStaticPublicKey = decryptedStatic
    symmetricState.mixKey(
      inputKeyMaterial =
        dh(privateKey = requireLocalEphemeral().secretKey, publicKey = decryptedStatic)
    )
    symmetricState.decryptAndHash(ciphertext = reader.readRemaining())
  }

  private fun writeMessageThree(payload: ByteArray): ByteArray {
    val remoteEphemeral: ByteArray = requireRemoteEphemeralPublicKey()
    val encryptedStatic: ByteArray =
      symmetricState.encryptAndHash(plaintext = localStaticKeyPair.publicKey)
    symmetricState.mixKey(
      inputKeyMaterial = dh(privateKey = localStaticKeyPair.secretKey, publicKey = remoteEphemeral)
    )
    val encryptedPayload: ByteArray = symmetricState.encryptAndHash(plaintext = payload)
    return encryptedStatic + encryptedPayload
  }

  private fun readMessageThree(payload: ByteArray): Unit {
    val reader = HandshakePayloadReader(payload)
    val decryptedStatic: ByteArray =
      symmetricState.decryptAndHash(ciphertext = reader.read(length = encryptedStaticLength()))
    remoteStaticPublicKey = decryptedStatic
    symmetricState.mixKey(
      inputKeyMaterial =
        dh(privateKey = requireLocalEphemeral().secretKey, publicKey = decryptedStatic)
    )
    symmetricState.decryptAndHash(ciphertext = reader.readRemaining())
  }

  private fun localEphemeral(): KeyPair {
    val existing: KeyPair? = localEphemeralKeyPair
    if (existing != null) {
      return existing
    }
    return provider.generateX25519KeyPair().also { generated -> localEphemeralKeyPair = generated }
  }

  private fun requireLocalEphemeral(): KeyPair {
    return requireNotNull(localEphemeralKeyPair) {
      "NoiseXXHandshake has no local ephemeral key for ${role.name}."
    }
  }

  private fun requireRemoteEphemeralPublicKey(): ByteArray {
    return requireNotNull(remoteEphemeralPublicKey) {
      "NoiseXXHandshake has no remote ephemeral key for ${role.name}."
    }
  }

  private fun maybeEstablishTransportSession(): Unit {
    if (!state.isComplete() || transportSession != null) {
      return
    }

    val (initiatorToResponder, responderToInitiator) = symmetricState.split()
    transportSession =
      when (role) {
        HandshakeRole.INITIATOR ->
          NoiseSession(
            sendCipherState = initiatorToResponder,
            receiveCipherState = responderToInitiator,
          )
        HandshakeRole.RESPONDER ->
          NoiseSession(
            sendCipherState = responderToInitiator,
            receiveCipherState = initiatorToResponder,
          )
      }
  }

  private fun dh(privateKey: ByteArray, publicKey: ByteArray): ByteArray {
    return dhCache.getOrCompute(privateKey = privateKey, publicKey = publicKey) {
      provider.x25519(privateKey = privateKey, publicKey = publicKey)
    }
  }

  private fun encryptedStaticLength(): Int {
    return if (symmetricState.hasCipherKey()) {
      X25519_KEY_SIZE + CHACHA20_POLY1305_TAG_SIZE
    } else {
      X25519_KEY_SIZE
    }
  }

  private fun expectedOutboundRound(): HandshakeRound {
    return when (role) {
      HandshakeRole.INITIATOR ->
        when (state.currentRound()) {
          1 -> HandshakeRound.ONE
          3 -> HandshakeRound.THREE
          else ->
            throw IllegalStateException(
              "NoiseXXHandshake initiator cannot send in round ${state.currentRound()}."
            )
        }
      HandshakeRole.RESPONDER ->
        when (state.currentRound()) {
          2 -> HandshakeRound.TWO
          else ->
            throw IllegalStateException(
              "NoiseXXHandshake responder cannot send in round ${state.currentRound()}."
            )
        }
    }
  }

  private fun expectedInboundRound(): HandshakeRound {
    return when (role) {
      HandshakeRole.INITIATOR ->
        when (state.currentRound()) {
          2 -> HandshakeRound.TWO
          else ->
            throw IllegalStateException(
              "NoiseXXHandshake initiator cannot receive in round ${state.currentRound()}."
            )
        }
      HandshakeRole.RESPONDER ->
        when (state.currentRound()) {
          1 -> HandshakeRound.ONE
          3 -> HandshakeRound.THREE
          else ->
            throw IllegalStateException(
              "NoiseXXHandshake responder cannot receive in round ${state.currentRound()}."
            )
        }
    }
  }

  private class HandshakePayloadReader(private val payload: ByteArray) {
    private var offset: Int = 0

    fun read(length: Int): ByteArray {
      require(length >= 0) { "NoiseXXHandshake payload length must be non-negative." }
      require(offset + length <= payload.size) {
        "NoiseXXHandshake payload was truncated while decoding."
      }
      val next = payload.copyOfRange(fromIndex = offset, toIndex = offset + length)
      offset += length
      return next
    }

    fun readRemaining(): ByteArray {
      return read(length = payload.size - offset)
    }
  }

  private data class NoiseHandshakeDependencies(
    val provider: CryptoProvider,
    val localStaticKeyPair: KeyPair,
  )

  private companion object {
    private const val CHACHA20_POLY1305_TAG_SIZE: Int = 16
    private const val HASH_OUTPUT_SIZE: Int = 32
    private const val X25519_KEY_SIZE: Int = 32
    private val PROTOCOL_HASH_KEY: ByteArray = ByteArray(size = HASH_OUTPUT_SIZE)
    private val PROTOCOL_NAME: ByteArray = "Noise_XX_25519_ChaChaPoly_SHA256".encodeToByteArray()

    private fun createInitialSymmetricState(provider: CryptoProvider): SymmetricState {
      val initialValue: ByteArray =
        if (PROTOCOL_NAME.size <= HASH_OUTPUT_SIZE) {
          PROTOCOL_NAME.copyOf(HASH_OUTPUT_SIZE)
        } else {
          provider.hmacSha256(key = PROTOCOL_HASH_KEY, message = PROTOCOL_NAME)
        }
      return SymmetricState(
        provider = provider,
        initialChainingKey = initialValue,
        initialHandshakeHash = initialValue,
      )
    }

    private fun defaultDependencies(): NoiseHandshakeDependencies {
      val provider: CryptoProvider = CryptoProviderFactory.create()
      return NoiseHandshakeDependencies(
        provider = provider,
        localStaticKeyPair = provider.generateX25519KeyPair(),
      )
    }
  }
}
