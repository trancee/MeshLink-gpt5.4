# iOS Crypto Bridge

`IosCryptoProvider` delegates to a Swift-installed `IosCryptoDelegate` so MeshLink can use Apple-native crypto without shipping third-party crypto binaries.

## Scope and platform support

- MeshLink's iOS BLE packaging is **physical-device only**.
- Simulator execution is intentionally out of scope for BLE acceptance and release verification.
- The release XCFramework is distributed through a Swift Package Manager `binaryTarget` whose URL and checksum are written into `Package.swift`.

## Install order

1. Create a Swift class that conforms to the generated `MeshLinkIosCryptoDelegate` protocol.
2. Install it once during app startup via `MeshLinkIosFactory.shared.installCryptoDelegate(...)`.
3. Create the MeshLink API with `MeshLinkIosFactory.shared.create(...)`.
4. For releases, update `Package.swift` before tagging:

   ```bash
   ./scripts/update-package-swift.sh --version v0.1.0 --checksum <swiftpm-checksum>
   ```

## Swift reference sketch

```swift
import CryptoKit
import MeshLink
import Foundation

final class MeshCryptoDelegate: NSObject, MeshLinkIosCryptoDelegate {
    func generateX25519KeyPair() -> MeshLinkIosCryptoKeyPair? {
        let privateKey = Curve25519.KeyAgreement.PrivateKey()
        return MeshLinkIosCryptoKeyPair(
            publicKey: privateKey.publicKey.rawRepresentation as NSData,
            secretKey: privateKey.rawRepresentation as NSData
        )
    }

    func generateEd25519KeyPair() -> MeshLinkIosCryptoKeyPair? {
        let privateKey = Curve25519.Signing.PrivateKey()
        let publicKey = privateKey.publicKey.rawRepresentation
        let secretKey = privateKey.rawRepresentation + publicKey
        return MeshLinkIosCryptoKeyPair(
            publicKey: publicKey as NSData,
            secretKey: secretKey as NSData
        )
    }

    func x25519(privateKey: NSData, publicKey: NSData) -> NSData? {
        do {
            let local = try Curve25519.KeyAgreement.PrivateKey(rawRepresentation: privateKey as Data)
            let remote = try Curve25519.KeyAgreement.PublicKey(rawRepresentation: publicKey as Data)
            let secret = try local.sharedSecretFromKeyAgreement(with: remote)
            return secret.withUnsafeBytes { Data($0) } as NSData
        } catch {
            return nil
        }
    }

    func ed25519Sign(privateKey: NSData, message: NSData) -> NSData? {
        do {
            let seed = (privateKey as Data).prefix(32)
            let signingKey = try Curve25519.Signing.PrivateKey(rawRepresentation: Data(seed))
            return try signingKey.signature(for: message as Data) as NSData
        } catch {
            return nil
        }
    }

    func ed25519Verify(publicKey: NSData, message: NSData, signature: NSData) -> Bool {
        do {
            let verifyingKey = try Curve25519.Signing.PublicKey(rawRepresentation: publicKey as Data)
            return verifyingKey.isValidSignature(signature as Data, for: message as Data)
        } catch {
            return false
        }
    }
}

MeshLinkIosFactory.shared.installCryptoDelegate(delegate: MeshCryptoDelegate())
```

## Swift interop verification

Use the generated framework in a physical-device iOS build and verify both of the SKIE-backed behaviors below before shipping a release.

### 1. Exhaustive `MeshLinkState` switching

`MeshLinkState` should bridge into Swift as an exhaustive enum, so a `switch` over the state should not need a `default` branch.

```swift
func render(state: MeshLinkState) {
    switch state {
    case .uninitialized:
        print("boot")
    case .running:
        print("running")
    case .paused:
        print("paused")
    case .stopped:
        print("stopped")
    case .recoverable:
        print("recoverable")
    case .terminal:
        print("terminal")
    }
}
```

### 2. `diagnosticEvents` as `AsyncSequence`

`MeshLinkApi.diagnosticEvents` should bridge into Swift as an `AsyncSequence`, so consumers can iterate with `for await`.

```swift
@MainActor
func observeDiagnostics(api: MeshLinkApi) async {
    for await event in api.diagnosticEvents {
        print("diagnostic code: \(event.code)")
    }
}
```

If either of these checks regresses, re-run the release framework build and inspect the generated Swift interface before publishing.

## Release packaging notes

1. Build the device XCFramework on macOS:

   ```bash
   ./gradlew :meshlink:compileKotlinIosArm64 :meshlink:assembleMeshLinkReleaseXCFramework
   ```

2. Package and checksum it for SwiftPM:

   ```bash
   ./scripts/package-xcframework.sh
   ```

3. Update `Package.swift` with the exact release tag and checksum:

   ```bash
   ./scripts/update-package-swift.sh --version v0.1.0 --checksum <swiftpm-checksum>
   ```

4. Validate the packaged artifact contains no forbidden crypto payloads:

   ```bash
   ./scripts/verify-publish.sh meshlink/build/XCFrameworks/release
   ```

## Notes

- Keep the implementation in Swift/CryptoKit.
- Do not route iOS crypto through third-party native libraries.
- Install the delegate before calling `MeshLinkIosFactory.create(...)`.
- Return `nil` from delegate methods when CryptoKit throws; `IosCryptoProvider` converts that into a Kotlin failure.
- Keep `Package.swift` aligned with the release asset URL and checksum before tagging a release.
