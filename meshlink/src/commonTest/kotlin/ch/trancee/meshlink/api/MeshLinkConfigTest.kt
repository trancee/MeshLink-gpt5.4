package ch.trancee.meshlink.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class MeshLinkConfigTest {
    @Test
    public fun default_buildsTheBalancedPreset(): Unit {
        // Arrange
        val expected = MeshLinkConfig.Preset.BALANCED

        // Act
        val actual = MeshLinkConfig.default()

        // Assert
        assertEquals(expected = MeshLinkConfig.Builder.DEFAULT_APPLICATION_ID, actual = actual.applicationId)
        assertEquals(expected = RegulatoryRegion.WORLDWIDE, actual = actual.regulatoryRegion)
        assertEquals(expected = expected.advertisingIntervalMs, actual = actual.advertising.intervalMs)
        assertEquals(expected = expected.scanDutyCyclePercent, actual = actual.scanning.scanDutyCyclePercent)
        assertEquals(expected = expected.maxPeerCount, actual = actual.connections.maxPeerCount)
        assertEquals(expected = expected.maxPayloadBytes, actual = actual.messaging.maxPayloadBytes)
        assertEquals(expected = expected.chunkSizeBytes, actual = actual.transfers.chunkSizeBytes)
        assertEquals(expected = expected.maxRouteCount, actual = actual.routing.maxRouteCount)
        assertEquals(expected = expected.trustMode, actual = actual.security.trustMode)
        assertEquals(expected = expected.diagnosticBufferSize, actual = actual.diagnostics.bufferSize)
        assertEquals(expected = expected.redactPeerIds, actual = actual.diagnostics.redactPeerIds)
    }

    @Test
    public fun invoke_dslSupportsAllNestedSubConfigurations(): Unit {
        // Arrange
        // Act
        val actual = MeshLinkConfig {
            applicationId = "chat-app"
            regulatoryRegion = RegulatoryRegion.US
            preset(preset = MeshLinkConfig.Preset.DIAGNOSTIC)
            advertising {
                intervalMs = 180
            }
            scanning {
                scanDutyCyclePercent = 65
            }
            connections {
                maxPeerCount = 9
            }
            messaging {
                maxPayloadBytes = 4_096
            }
            transfers {
                chunkSizeBytes = 2_048
            }
            routing {
                maxRouteCount = 300
            }
            security {
                trustMode = TrustMode.STRICT
            }
            diagnostics {
                bufferSize = 512
                redactPeerIds = true
            }
        }

        // Assert
        assertEquals(expected = "chat-app", actual = actual.applicationId)
        assertEquals(expected = RegulatoryRegion.US, actual = actual.regulatoryRegion)
        assertEquals(expected = 180, actual = actual.advertising.intervalMs)
        assertEquals(expected = 65, actual = actual.scanning.scanDutyCyclePercent)
        assertEquals(expected = 9, actual = actual.connections.maxPeerCount)
        assertEquals(expected = 4_096, actual = actual.messaging.maxPayloadBytes)
        assertEquals(expected = 2_048, actual = actual.transfers.chunkSizeBytes)
        assertEquals(expected = 300, actual = actual.routing.maxRouteCount)
        assertEquals(expected = TrustMode.STRICT, actual = actual.security.trustMode)
        assertEquals(expected = 512, actual = actual.diagnostics.bufferSize)
        assertEquals(expected = true, actual = actual.diagnostics.redactPeerIds)
    }

    @Test
    public fun build_rejectsBlankApplicationIds(): Unit {
        // Arrange
        val expectedMessage = "MeshLinkConfig applicationId must not be blank."

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            MeshLinkConfig {
                applicationId = ""
            }
        }

        // Assert
        assertEquals(expected = expectedMessage, actual = error.message)
    }

    @Test
    public fun build_rejectsNonPositiveDiagnosticBufferSizes(): Unit {
        // Arrange
        val expectedMessage = "MeshLinkConfig diagnostics.bufferSize must be greater than 0."

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            MeshLinkConfig {
                diagnostics {
                    bufferSize = 0
                }
            }
        }

        // Assert
        assertEquals(expected = expectedMessage, actual = error.message)
    }

    @Test
    public fun build_clampsRegionSensitiveDiscoveryParameters(): Unit {
        // Arrange
        // Act
        val actual = MeshLinkConfig {
            regulatoryRegion = RegulatoryRegion.EU
            advertising {
                intervalMs = 100
            }
            scanning {
                scanDutyCyclePercent = 95
            }
        }

        // Assert
        assertEquals(expected = 300, actual = actual.advertising.intervalMs)
        assertEquals(expected = 70, actual = actual.scanning.scanDutyCyclePercent)
    }

    @Test
    public fun build_clampsRangesForBestEffortParameters(): Unit {
        // Arrange
        // Act
        val actual = MeshLinkConfig {
            connections {
                maxPeerCount = 99
            }
            messaging {
                maxPayloadBytes = 200_000
            }
            transfers {
                chunkSizeBytes = 100
            }
            routing {
                maxRouteCount = 9_999
            }
            diagnostics {
                bufferSize = 9_999
            }
        }

        // Assert
        assertEquals(expected = 16, actual = actual.connections.maxPeerCount)
        assertEquals(expected = 100_000, actual = actual.messaging.maxPayloadBytes)
        assertEquals(expected = 256, actual = actual.transfers.chunkSizeBytes)
        assertEquals(expected = 512, actual = actual.routing.maxRouteCount)
        assertEquals(expected = 4_096, actual = actual.diagnostics.bufferSize)
    }

    @Test
    public fun presets_exposeTheCanonicalFourProfiles(): Unit {
        // Arrange
        val expectedCount = 4

        // Act
        val actualCount = MeshLinkConfig.Preset.entries.size

        // Assert
        assertEquals(expected = expectedCount, actual = actualCount)
    }
}
