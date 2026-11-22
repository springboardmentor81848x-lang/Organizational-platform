import json
import os
import subprocess
import time
import urllib.request
import urllib.parse
import websocket

# 1. Login to get tokens
roles_config = [
    ("EMPLOYEE", "employee.regression@example.com", "employee-dashboard.html"),
    ("MANAGER", "manager.test@example.com", "manager-dashboard.html"),
    ("TRAINER_MENTOR", "mentor.test@example.com", "trainer-dashboard.html"),
    ("HR_MANAGER", "hr.specialist@wki.com", "hr-dashboard.html"),
    ("DEPT_HEAD", "depthead.test@example.com", "dept-head-dashboard.html"),
]

sessions = {}
for role, email, page in roles_config:
    req = urllib.request.Request(
        "http://localhost:8080/api/auth/login",
        data=json.dumps({"email": email, "password": "Password123!"}).encode("utf-8"),
        headers={"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(req) as resp:
        sessions[role] = json.loads(resp.read().decode("utf-8"))

print("Obtained tokens for all roles successfully.")

# 2. Launch Chrome headless with debugging port
chrome_path = r"C:\Program Files\Google\Chrome\Application\chrome.exe"
temp_dir = os.path.join(os.environ["TEMP"], "wki_measure_chrome")
cmd = [
    chrome_path,
    "--headless=new",
    "--remote-debugging-port=9222",
    f"--user-data-dir={temp_dir}",
    "--window-size=1440,900",
    "--disable-gpu",
    "--no-sandbox"
]
proc = subprocess.Popen(cmd)
time.sleep(2)

try:
    # 3. Connect to Chrome CDP via websocket
    tabs_url = "http://localhost:9222/json"
    req = urllib.request.urlopen(tabs_url)
    tabs = json.loads(req.read().decode("utf-8"))
    ws_url = tabs[0]["webSocketDebuggerUrl"]
    ws = websocket.create_connection(ws_url)

    msg_id = 0
    def send_cmd(method, params=None):
        nonlocal msg_id
        msg_id += 1
        payload = {"id": msg_id, "method": method}
        if params:
            payload["params"] = params
        ws.send(json.dumps(payload))
        while True:
            res = json.loads(ws.recv())
            if res.get("id") == msg_id:
                return res

    send_cmd("Page.enable")
    send_cmd("Runtime.enable")

    print("\n=== MEASURING REAL BROWSER COMPUTED BOUNDS ACROSS ALL 5 DASHBOARDS ===")

    for role, email, page in roles_config:
        sess = sessions[role]
        url = f"http://localhost:5500/{page}"

        # Navigate
        send_cmd("Page.navigate", {"url": url})
        time.sleep(0.5)

        # Set localStorage session
        sess_json = json.dumps(sess)
        set_storage_js = f"""
        localStorage.setItem('wki_session', {json.dumps(sess_json)});
        location.reload();
        """
        send_cmd("Runtime.evaluate", {"expression": set_storage_js})
        time.sleep(1.0)

        # Measure bounding rects
        measure_js = """
        (() => {
            const top = document.querySelector('.top');
            const nav = document.querySelector('nav#nav');
            const main = document.querySelector('main, #main, #view');
            const topRect = top ? top.getBoundingClientRect() : null;
            const navRect = nav ? nav.getBoundingClientRect() : null;
            const mainRect = main ? main.getBoundingClientRect() : null;
            return {
                top: topRect ? { top: topRect.top, left: topRect.left, width: topRect.width, height: topRect.height } : null,
                nav: navRect ? { top: navRect.top, left: navRect.left, width: navRect.width, height: navRect.height } : null,
                main: mainRect ? { top: mainRect.top, left: mainRect.left, width: mainRect.width, height: mainRect.height } : null,
                navScrollHeight: nav ? nav.scrollHeight : 0,
                navClientHeight: nav ? nav.clientHeight : 0,
                bodyScrollWidth: document.body.scrollWidth,
                windowWidth: window.innerWidth
            };
        })()
        """
        res = send_cmd("Runtime.evaluate", {"expression": measure_js, "returnByValue": True})
        metrics = res["result"]["value"]

        print(f"\nDashboard: {page} (Role: {role})")
        top_m = metrics["top"]
        nav_m = metrics["nav"]
        main_m = metrics["main"]

        print(f"  Header (.top):")
        print(f"    position: top={top_m['top']}px, left={top_m['left']}px, height={top_m['height']}px, width={top_m['width']}px")
        print(f"  Sidebar (nav#nav):")
        print(f"    position: top={nav_m['top']}px, left={nav_m['left']}px, width={nav_m['width']}px, height={nav_m['height']}px")
        print(f"    scrollable height={nav_m['height']}px (clientHeight={metrics['navClientHeight']}px)")
        print(f"  Main Content:")
        print(f"    position: top={main_m['top']}px, left={main_m['left']}px, width={main_m['width']}px")
        print(f"  Horizontal scroll check: body.scrollWidth={metrics['bodyScrollWidth']}px vs window={metrics['windowWidth']}px")

        # Assertions
        assert nav_m["left"] == 0, f"Sidebar not at extreme left (left={nav_m['left']})"
        assert nav_m["top"] == 60, f"Sidebar top not at 60px (top={nav_m['top']})"
        assert abs(nav_m["width"] - 230) <= 1, f"Sidebar width not ~230px (width={nav_m['width']})"
        assert abs(main_m["left"] - 230) <= 1, f"Main content left not aligned with sidebar (left={main_m['left']})"
        assert metrics["bodyScrollWidth"] <= metrics["windowWidth"], "Horizontal overflow detected!"
        print(f"  ==> ALL CHECKS PASSED FOR {page}!")

        # Take desktop screenshot
        shot_res = send_cmd("Page.captureScreenshot", {"format": "png"})
        import base64
        shot_data = base64.b64decode(shot_res["result"]["data"])
        out_shot = f"scratch/{page.replace('.html', '_desktop.png')}"
        with open(out_shot, "wb") as f:
            f.write(shot_data)
        print(f"  Screenshot saved to {out_shot}")

    # 4. Test Mobile Responsive Viewport
    print("\n=== TESTING MOBILE RESPONSIVE DRAWER AT 800PX VIEWPORT ===")
    send_cmd("Emulation.setDeviceMetricsOverride", {
        "width": 800,
        "height": 900,
        "deviceScaleFactor": 1,
        "mobile": False
    })
    time.sleep(0.5)

    test_page = "dept-head-dashboard.html"
    send_cmd("Page.navigate", {"url": f"http://localhost:5500/{test_page}"})
    time.sleep(0.5)

    mobile_check_js = """
    (() => {
        const toggleBtn = document.querySelector('#sidebarToggleBtn');
        const nav = document.querySelector('nav#nav');
        const main = document.querySelector('main, #main, #view');
        const btnStyle = toggleBtn ? window.getComputedStyle(toggleBtn).display : 'none';
        const navRect = nav ? nav.getBoundingClientRect() : null;
        return {
            toggleVisible: btnStyle !== 'none',
            navLeft: navRect ? navRect.left : null,
            mainWidth: main ? main.getBoundingClientRect().width : null
        };
    })()
    """
    res = send_cmd("Runtime.evaluate", {"expression": mobile_check_js, "returnByValue": True})
    mob = res["result"]["value"]
    print(f"Mobile Drawer closed:")
    print(f"  Hamburger toggle button visible: {mob['toggleVisible']}")
    print(f"  Sidebar off-screen (navLeft < 0): {mob['navLeft']}px")
    print(f"  Main content full width: {mob['mainWidth']}px")

    # Click toggle button to open drawer
    send_cmd("Runtime.evaluate", {"expression": "document.querySelector('#sidebarToggleBtn').click()"})
    time.sleep(0.4)

    res_open = send_cmd("Runtime.evaluate", {"expression": "document.querySelector('nav#nav').getBoundingClientRect().left", "returnByValue": True})
    open_left = res_open["result"]["value"]
    print(f"Mobile Drawer opened:")
    print(f"  Sidebar slid into view (left == 0): {open_left}px")

    shot_res = send_cmd("Page.captureScreenshot", {"format": "png"})
    shot_data = base64.b64decode(shot_res["result"]["data"])
    with open("scratch/mobile_drawer_open.png", "wb") as f:
        f.write(shot_data)
    print("  Mobile screenshot saved to scratch/mobile_drawer_open.png")

    print("\n ALL DESKTOP AND MOBILE TESTS COMPLETED SUCCESSFULLY!")

finally:
    try:
        ws.close()
    except Exception:
        pass
    proc.kill()

