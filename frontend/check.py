from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()
    
    # Capture console logs
    console_messages = []
    page.on('console', lambda msg: console_messages.append(f"[{msg.type}] {msg.text}"))
    
    # Capture page errors
    page_errors = []
    page.on('pageerror', lambda err: page_errors.append(str(err)))
    
    page.goto('http://localhost:5173')
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(2000)
    
    # Take screenshot
    page.screenshot(path='E:/cs/frontend/screenshot.png', full_page=True)
    
    # Print console messages
    print("=== Console Messages ===")
    for msg in console_messages:
        print(msg)
    
    # Print page errors
    print("\n=== Page Errors ===")
    for err in page_errors:
        print(err)
    
    # Get page title and content
    print(f"\n=== Page Title: {page.title()} ===")
    print(f"URL: {page.url}")
    
    # Check if login form is visible
    login_form = page.locator('.auth-card')
    print(f"Login form visible: {login_form.is_visible()}")
    
    browser.close()
