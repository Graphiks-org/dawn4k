import XCTest
import UIKit
import QuartzCore

final class Dawn4kDemoTests: XCTestCase {
    @MainActor
    func testLiveViewportUsesItsWindowRetinaScale() throws {
        func viewport(_ view: UIView) -> UIView? {
            if String(describing: type(of: view)).contains("IosMetalView") { return view }
            return view.subviews.lazy.compactMap { viewport($0) }.first
        }
        var metalView: UIView?
        let end = Date().addingTimeInterval(15)
        while metalView == nil && Date() < end {
            for scene in UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene }) {
                for window in scene.windows {
                    if let found = viewport(window), found.bounds.width > 0 { metalView = found }
                }
            }
            if metalView == nil { RunLoop.current.run(until: Date().addingTimeInterval(0.01)) }
        }
        let view = try XCTUnwrap(metalView, "live native particle viewport missing")
        let layer = try XCTUnwrap(view.layer as? CAMetalLayer)
        let scale = try XCTUnwrap(view.window).screen.scale
        XCTAssertGreaterThan(scale, 1, "Retina simulator required")
        XCTAssertEqual(view.contentScaleFactor, scale)
        XCTAssertEqual(layer.drawableSize.width, view.bounds.width * scale, accuracy: 1)
        XCTAssertEqual(layer.drawableSize.height, view.bounds.height * scale, accuracy: 1)
    }
}
