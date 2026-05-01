# iOS Crypto Bridge

`IosCryptoProvider` delegates to a Swift-installed `IosCryptoDelegate` so MeshLink can use Apple-native crypto without shipping third-party crypto binaries.

## Install order

1. Create a Swift class that conforms to the generated `MeshLinkIosCryptoDelegate` protocol.
2. Install it once during app startup via `MeshLinkIosFactory.shared.installCryptoDelegate(...)`.
3. Create the MeshLink API with `MeshLinkIosFactory.shared.create(...)`.

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

## Notes

- Keep the implementation in Swift/CryptoKit.
- Do not route iOS crypto through third-party native libraries.
- Install the delegate before calling `MeshLinkIosFactory.create(...)`.
- Return `nil` from delegate methods when CryptoKit throws; `IosCryptoProvider` converts that into a Kotlin failure.
