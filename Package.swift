// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "MeshLink",
    platforms: [
        .iOS(.v15),
    ],
    products: [
        .library(
            name: "MeshLink",
            targets: ["MeshLink"],
        ),
    ],
    targets: [
        .binaryTarget(
            name: "MeshLink",
            path: "MeshLink.xcframework",
        ),
    ],
)
