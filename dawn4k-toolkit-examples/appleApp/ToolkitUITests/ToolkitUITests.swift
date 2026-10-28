import XCTest

final class ToolkitUITests: XCTestCase {
    func testComputeAndTriangleUseTheSharedToolkit() {
        let app = XCUIApplication()
        app.launch()
        let status = app.staticTexts.firstMatch
        XCTAssertTrue(status.waitForExistence(timeout: 30))
        let presented = NSPredicate(format: "label CONTAINS %@", "triangle presented")
        let expectation = XCTNSPredicateExpectation(predicate: presented, object: status)
        XCTAssertEqual(XCTWaiter.wait(for: [expectation], timeout: 60), .completed)
        XCTAssertTrue(status.label.contains("[2, 4, 6, 8]"), status.label)
        let image = XCTAttachment(screenshot: app.screenshot())
        image.lifetime = .keepAlways
        add(image)
        app.terminate()
    }
}
