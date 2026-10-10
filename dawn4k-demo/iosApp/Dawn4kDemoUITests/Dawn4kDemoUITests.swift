import XCTest

final class Dawn4kDemoUITests: XCTestCase {
    func testParticlesAndSharedTouchControls() throws {
        let app = XCUIApplication()
        app.launch()
        let pause = app.buttons["Pause"]
        XCTAssertTrue(pause.waitForExistence(timeout: 30))
        let ready = XCTNSPredicateExpectation(predicate: NSPredicate(format: "enabled == true"), object: pause)
        XCTAssertEqual(XCTWaiter.wait(for: [ready], timeout: 30), .completed)
        pause.tap()
        XCTAssertTrue(app.buttons["Resume"].waitForExistence(timeout: 5))
        app.buttons["256"].tap()
        XCTAssertTrue(app.staticTexts["Particles: 256"].waitForExistence(timeout: 5))
        app.buttons["Reset"].tap()
        let image = XCTAttachment(screenshot: app.screenshot())
        image.name = "Dawn Metal particles with common touch controls"
        image.lifetime = .keepAlways
        add(image)
        app.buttons["Stop"].tap()
        XCTAssertTrue(app.buttons["Restart"].waitForExistence(timeout: 10))
        app.buttons["Restart"].tap()
        XCTAssertTrue(app.buttons["Pause"].waitForExistence(timeout: 15))
    }
}
