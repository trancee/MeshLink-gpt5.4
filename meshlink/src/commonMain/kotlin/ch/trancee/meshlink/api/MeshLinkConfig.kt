package ch.trancee.meshlink.api

public data class MeshLinkConfig(
    public val applicationId: String,
    public val regulatoryRegion: RegulatoryRegion,
    public val advertising: AdvertisingConfig,
    public val scanning: ScanningConfig,
    public val connections: ConnectionsConfig,
    public val messaging: MessagingConfig,
    public val transfers: TransfersConfig,
    public val routing: RoutingConfig,
    public val security: SecurityConfig,
    public val diagnostics: DiagnosticsConfig,
) {
    public companion object {
        public fun default(): MeshLinkConfig {
            return Builder().build()
        }

        public operator fun invoke(block: Builder.() -> Unit): MeshLinkConfig {
            return Builder().apply(block).build()
        }
    }

    public class Builder {
        public var applicationId: String = DEFAULT_APPLICATION_ID
        public var regulatoryRegion: RegulatoryRegion = RegulatoryRegion.WORLDWIDE

        private val advertisingBuilder: AdvertisingConfig.Builder = AdvertisingConfig.Builder()
        private val scanningBuilder: ScanningConfig.Builder = ScanningConfig.Builder()
        private val connectionsBuilder: ConnectionsConfig.Builder = ConnectionsConfig.Builder()
        private val messagingBuilder: MessagingConfig.Builder = MessagingConfig.Builder()
        private val transfersBuilder: TransfersConfig.Builder = TransfersConfig.Builder()
        private val routingBuilder: RoutingConfig.Builder = RoutingConfig.Builder()
        private val securityBuilder: SecurityConfig.Builder = SecurityConfig.Builder()
        private val diagnosticsBuilder: DiagnosticsConfig.Builder = DiagnosticsConfig.Builder()

        init {
            preset(preset = Preset.BALANCED)
        }

        public fun preset(preset: Preset): Unit {
            advertisingBuilder.intervalMs = preset.advertisingIntervalMs
            scanningBuilder.scanDutyCyclePercent = preset.scanDutyCyclePercent
            connectionsBuilder.maxPeerCount = preset.maxPeerCount
            messagingBuilder.maxPayloadBytes = preset.maxPayloadBytes
            transfersBuilder.chunkSizeBytes = preset.chunkSizeBytes
            routingBuilder.maxRouteCount = preset.maxRouteCount
            securityBuilder.trustMode = preset.trustMode
            diagnosticsBuilder.bufferSize = preset.diagnosticBufferSize
            diagnosticsBuilder.redactPeerIds = preset.redactPeerIds
        }

        public fun advertising(block: AdvertisingConfig.Builder.() -> Unit): Unit {
            advertisingBuilder.apply(block)
        }

        public fun scanning(block: ScanningConfig.Builder.() -> Unit): Unit {
            scanningBuilder.apply(block)
        }

        public fun connections(block: ConnectionsConfig.Builder.() -> Unit): Unit {
            connectionsBuilder.apply(block)
        }

        public fun messaging(block: MessagingConfig.Builder.() -> Unit): Unit {
            messagingBuilder.apply(block)
        }

        public fun transfers(block: TransfersConfig.Builder.() -> Unit): Unit {
            transfersBuilder.apply(block)
        }

        public fun routing(block: RoutingConfig.Builder.() -> Unit): Unit {
            routingBuilder.apply(block)
        }

        public fun security(block: SecurityConfig.Builder.() -> Unit): Unit {
            securityBuilder.apply(block)
        }

        public fun diagnostics(block: DiagnosticsConfig.Builder.() -> Unit): Unit {
            diagnosticsBuilder.apply(block)
        }

        public fun build(): MeshLinkConfig {
            require(applicationId.isNotBlank()) { "MeshLinkConfig applicationId must not be blank." }

            return MeshLinkConfig(
                applicationId = applicationId,
                regulatoryRegion = regulatoryRegion,
                advertising = advertisingBuilder.build(regulatoryRegion = regulatoryRegion),
                scanning = scanningBuilder.build(regulatoryRegion = regulatoryRegion),
                connections = connectionsBuilder.build(),
                messaging = messagingBuilder.build(),
                transfers = transfersBuilder.build(),
                routing = routingBuilder.build(),
                security = securityBuilder.build(),
                diagnostics = diagnosticsBuilder.build(),
            )
        }

        public companion object {
            public const val DEFAULT_APPLICATION_ID: String = "meshlink"
        }
    }

    public data class AdvertisingConfig(
        public val intervalMs: Int,
    ) {
        public class Builder {
            public var intervalMs: Int = 200

            internal fun build(regulatoryRegion: RegulatoryRegion): AdvertisingConfig {
                return AdvertisingConfig(
                    intervalMs = regulatoryRegion.clampAdvertisementIntervalMs(intervalMs = intervalMs),
                )
            }
        }
    }

    public data class ScanningConfig(
        public val scanDutyCyclePercent: Int,
    ) {
        public class Builder {
            public var scanDutyCyclePercent: Int = 50

            internal fun build(regulatoryRegion: RegulatoryRegion): ScanningConfig {
                return ScanningConfig(
                    scanDutyCyclePercent = regulatoryRegion.clampScanDutyCyclePercent(percent = scanDutyCyclePercent),
                )
            }
        }
    }

    public data class ConnectionsConfig(
        public val maxPeerCount: Int,
    ) {
        public class Builder {
            public var maxPeerCount: Int = 8

            internal fun build(): ConnectionsConfig {
                return ConnectionsConfig(maxPeerCount = maxPeerCount.coerceIn(minimumValue = 1, maximumValue = 16))
            }
        }
    }

    public data class MessagingConfig(
        public val maxPayloadBytes: Int,
    ) {
        public class Builder {
            public var maxPayloadBytes: Int = 100_000

            internal fun build(): MessagingConfig {
                return MessagingConfig(maxPayloadBytes = maxPayloadBytes.coerceIn(minimumValue = 1, maximumValue = 100_000))
            }
        }
    }

    public data class TransfersConfig(
        public val chunkSizeBytes: Int,
    ) {
        public class Builder {
            public var chunkSizeBytes: Int = 1_024

            internal fun build(): TransfersConfig {
                return TransfersConfig(chunkSizeBytes = chunkSizeBytes.coerceIn(minimumValue = 256, maximumValue = 4_096))
            }
        }
    }

    public data class RoutingConfig(
        public val maxRouteCount: Int,
    ) {
        public class Builder {
            public var maxRouteCount: Int = 128

            internal fun build(): RoutingConfig {
                return RoutingConfig(maxRouteCount = maxRouteCount.coerceIn(minimumValue = 1, maximumValue = 512))
            }
        }
    }

    public data class SecurityConfig(
        public val trustMode: TrustMode,
    ) {
        public class Builder {
            public var trustMode: TrustMode = TrustMode.TOFU

            internal fun build(): SecurityConfig {
                return SecurityConfig(trustMode = trustMode)
            }
        }
    }

    public data class DiagnosticsConfig(
        public val bufferSize: Int,
        public val redactPeerIds: Boolean,
    ) {
        public class Builder {
            public var bufferSize: Int = DiagnosticSink.DEFAULT_BUFFER_SIZE
            public var redactPeerIds: Boolean = false

            internal fun build(): DiagnosticsConfig {
                require(bufferSize > 0) { "MeshLinkConfig diagnostics.bufferSize must be greater than 0." }

                return DiagnosticsConfig(
                    bufferSize = bufferSize.coerceAtMost(maximumValue = 4_096),
                    redactPeerIds = redactPeerIds,
                )
            }
        }
    }

    public enum class Preset(
        public val advertisingIntervalMs: Int,
        public val scanDutyCyclePercent: Int,
        public val maxPeerCount: Int,
        public val maxPayloadBytes: Int,
        public val chunkSizeBytes: Int,
        public val maxRouteCount: Int,
        public val trustMode: TrustMode,
        public val diagnosticBufferSize: Int,
        public val redactPeerIds: Boolean,
    ) {
        BALANCED(
            advertisingIntervalMs = 200,
            scanDutyCyclePercent = 50,
            maxPeerCount = 8,
            maxPayloadBytes = 100_000,
            chunkSizeBytes = 1_024,
            maxRouteCount = 128,
            trustMode = TrustMode.TOFU,
            diagnosticBufferSize = 64,
            redactPeerIds = false,
        ),
        PERFORMANCE(
            advertisingIntervalMs = 100,
            scanDutyCyclePercent = 100,
            maxPeerCount = 16,
            maxPayloadBytes = 100_000,
            chunkSizeBytes = 2_048,
            maxRouteCount = 256,
            trustMode = TrustMode.TOFU,
            diagnosticBufferSize = 128,
            redactPeerIds = false,
        ),
        POWER_SAVER(
            advertisingIntervalMs = 1_000,
            scanDutyCyclePercent = 5,
            maxPeerCount = 4,
            maxPayloadBytes = 50_000,
            chunkSizeBytes = 512,
            maxRouteCount = 64,
            trustMode = TrustMode.STRICT,
            diagnosticBufferSize = 32,
            redactPeerIds = true,
        ),
        DIAGNOSTIC(
            advertisingIntervalMs = 150,
            scanDutyCyclePercent = 75,
            maxPeerCount = 8,
            maxPayloadBytes = 100_000,
            chunkSizeBytes = 1_024,
            maxRouteCount = 256,
            trustMode = TrustMode.PROMPT,
            diagnosticBufferSize = 256,
            redactPeerIds = false,
        ),
    }
}
