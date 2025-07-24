// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "ClienApp",
    platforms: [
        .iOS(.v16)
    ],
    products: [
        .library(
            name: "ClienApp",
            targets: ["ClienApp"]),
    ],
    dependencies: [
        .package(url: "https://github.com/scinfu/SwiftSoup.git", from: "2.6.0"),
        .package(url: "https://github.com/Alamofire/Alamofire.git", from: "5.8.0")
    ],
    targets: [
        .target(
            name: "ClienApp",
            dependencies: ["SwiftSoup", "Alamofire"]),
    ]
)