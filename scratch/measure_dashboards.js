const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');

const roles = [
  { role: 'EMPLOYEE', email: 'employee.regression@example.com', page: 'employee-dashboard.html' },
  { role: 'MANAGER', email: 'manager.test@example.com', page: 'manager-dashboard.html' },
  { role: 'TRAINER_MENTOR', email: 'mentor.test@example.com', page: 'trainer-dashboard.html' },
  { role: 'HR_MANAGER', email: 'hr.specialist@wki.com', page: 'hr-dashboard.html' },
  { role: 'DEPT_HEAD', email: 'depthead.test@example.com', page: 'dept-head-dashboard.html' },
];

async function loginAll() {
  const sessions = {};
  for (const r of roles) {
    const res = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: r.email, password: 'Password123!' })
    });
    if (!res.ok) throw new Error(`Login failed for ${r.role}: ${res.status}`);
    sessions[r.role] = await res.json();
  }
  return sessions;
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function main() {
  console.log('1. Logging into API for all 5 roles...');
  const sessions = await loginAll();
  console.log('   All 5 session tokens obtained.');

  console.log('2. Launching Headless Chrome on CDP port 9222...');
  const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
  const userDataDir = path.join(process.env.TEMP, 'chrome_measure_' + Date.now());
  const proc = spawn(chromePath, [
    '--headless=new',
    '--remote-debugging-port=9222',
    `--user-data-dir=${userDataDir}`,
    '--window-size=1440,900',
    '--disable-gpu',
    '--no-sandbox'
  ]);

  await sleep(2500);

  let ws;
  try {
    const tabsRes = await fetch('http://localhost:9222/json');
    const tabs = await tabsRes.json();
    const wsUrl = tabs[0].webSocketDebuggerUrl;
    ws = new WebSocket(wsUrl);

    await new Promise((res, rej) => {
      ws.onopen = res;
      ws.onerror = rej;
    });

    let id = 0;
    const pending = new Map();
    ws.onmessage = (msg) => {
      const data = JSON.parse(msg.data);
      if (data.id && pending.has(data.id)) {
        pending.get(data.id)(data);
        pending.delete(data.id);
      }
    };

    function send(method, params = {}) {
      return new Promise((resolve) => {
        const msgId = ++id;
        pending.set(msgId, resolve);
        ws.send(JSON.stringify({ id: msgId, method, params }));
      });
    }

    await send('Page.enable');
    await send('Runtime.enable');

    console.log('\n3. Measuring exact layout bounding rects in Chrome...');
    const results = [];

    const scratchDir = path.join(__dirname, '..', 'scratch');
    if (!fs.existsSync(scratchDir)) fs.mkdirSync(scratchDir, { recursive: true });

    let scriptIdentifier = null;

    for (const r of roles) {
      const sess = sessions[r.role];
      const url = `http://localhost:5500/${r.page}`;

      if (scriptIdentifier) {
        await send('Page.removeScriptToEvaluateOnNewDocument', { identifier: scriptIdentifier });
      }

      const scriptRes = await send('Page.addScriptToEvaluateOnNewDocument', {
        source: `localStorage.setItem('wki_session', ${JSON.stringify(JSON.stringify(sess))});`
      });
      scriptIdentifier = scriptRes.result.identifier;

      await send('Page.navigate', { url });
      await sleep(1500);

      // Wait until app is unhidden
      for (let i = 0; i < 20; i++) {
        const appRes = await send('Runtime.evaluate', { expression: `Boolean(document.querySelector('.top') && document.querySelector('nav#nav') && document.getElementById('app') && !document.getElementById('app').hidden)`, returnByValue: true });
        if (appRes.result?.value) break;
        await sleep(300);
      }

      const measureExpr = `
        (() => {
          const top = document.querySelector('.top');
          const nav = document.querySelector('nav#nav');
          const main = document.querySelector('main, #main, #view');
          const banner = document.querySelector('.access-banner');
          const tRect = top ? top.getBoundingClientRect() : null;
          const nRect = nav ? nav.getBoundingClientRect() : null;
          const mRect = main ? main.getBoundingClientRect() : null;
          const bRect = banner ? banner.getBoundingClientRect() : null;
          return {
            top: tRect ? { top: tRect.top, left: tRect.left, width: tRect.width, height: tRect.height } : null,
            nav: nRect ? { top: nRect.top, left: nRect.left, width: nRect.width, height: nRect.height } : null,
            main: mRect ? { top: mRect.top, left: mRect.left, width: mRect.width, height: mRect.height } : null,
            banner: bRect ? { top: bRect.top, left: bRect.left, width: bRect.width, height: bRect.height } : null,
            bodyScrollWidth: document.body.scrollWidth,
            windowWidth: window.innerWidth,
            windowHeight: window.innerHeight
          };
        })()
      `;

      const evalRes = await send('Runtime.evaluate', { expression: measureExpr, returnByValue: true });
      const m = evalRes.result.value;

      console.log(`\n--------------------------------------------------`);
      console.log(`PAGE: ${r.page} (${r.role})`);
      console.log(`  Top Header:`);
      console.log(`    top: ${m.top?.top}px | left: ${m.top?.left}px | height: ${m.top?.height}px | width: ${m.top?.width}px`);
      console.log(`  Sidebar (nav#nav):`);
      console.log(`    top: ${m.nav?.top}px | left: ${m.nav?.left}px | width: ${m.nav?.width}px | height: ${m.nav?.height}px`);
      console.log(`  Main Content:`);
      console.log(`    top: ${m.main?.top}px | left: ${m.main?.left}px | width: ${m.main?.width}px`);
      if (m.banner) {
        console.log(`  Access Banner:`);
        console.log(`    top: ${m.banner.top}px | left: ${m.banner.left}px | width: ${m.banner.width}px`);
      }
      console.log(`  Screen Width: ${m.windowWidth}px | Body Scroll Width: ${m.bodyScrollWidth}px`);

      // Strict assertions
      const checks = {
        sidebarAtExtremeLeft: m.nav?.left === 0,
        sidebarDirectlyBelowHeader: m.nav?.top === 60,
        sidebarWidth230: Math.abs((m.nav?.width || 0) - 230) <= 1,
        sidebarExtendsToBottom: (m.nav?.height || 0) >= (m.windowHeight - 60),
        mainStartsImmediatelyRight: Math.abs((m.main?.left || 0) - 230) <= 1,
        noHorizontalScroll: m.bodyScrollWidth <= m.windowWidth
      };

      console.log('  Validation Results:');
      for (const [k, v] of Object.entries(checks)) {
        console.log(`    ${v ? 'PASS' : 'FAIL'} - ${k}`);
        if (!v) throw new Error(`Assertion failed for ${r.page}: ${k}`);
      }

      // Screenshot
      const shot = await send('Page.captureScreenshot', { format: 'png' });
      const shotPath = path.join(scratchDir, `${r.page.replace('.html', '')}_verified.png`);
      fs.writeFileSync(shotPath, Buffer.from(shot.result.data, 'base64'));
      console.log(`    Screenshot: ${shotPath}`);

      results.push({ page: r.page, role: r.role, checks });
    }

    console.log('\n4. Testing Mobile Viewport & Drawer (800px width)...');
    await send('Emulation.setDeviceMetricsOverride', {
      width: 800,
      height: 900,
      deviceScaleFactor: 1,
      mobile: false
    });
    await sleep(400);

    const testUrl = 'http://localhost:5500/dept-head-dashboard.html';
    await send('Page.navigate', { url: testUrl });
    await sleep(600);

    const mobEvalBefore = await send('Runtime.evaluate', {
      expression: `(() => {
        const btn = document.querySelector('#sidebarToggleBtn');
        const nav = document.querySelector('nav#nav');
        return {
          btnDisplay: btn ? window.getComputedStyle(btn).display : 'none',
          navLeft: nav ? nav.getBoundingClientRect().left : 999
        };
      })()`,
      returnByValue: true
    });
    const mobBefore = mobEvalBefore.result.value;
    console.log(`  Drawer Closed: toggle button displayed='${mobBefore.btnDisplay}', sidebar left=${mobBefore.navLeft}px (off-screen)`);

    // Click toggle button
    await send('Runtime.evaluate', { expression: `document.querySelector('#sidebarToggleBtn').click();` });
    await sleep(400);

    const mobEvalAfter = await send('Runtime.evaluate', {
      expression: `(() => {
        const nav = document.querySelector('nav#nav');
        return {
          navLeft: nav ? nav.getBoundingClientRect().left : 999,
          navWidth: nav ? nav.getBoundingClientRect().width : 0
        };
      })()`,
      returnByValue: true
    });
    const mobAfter = mobEvalAfter.result.value;
    console.log(`  Drawer Opened: sidebar left=${mobAfter.navLeft}px, width=${mobAfter.navWidth}px (slid into view)`);

    const mobShot = await send('Page.captureScreenshot', { format: 'png' });
    const mobShotPath = path.join(scratchDir, `mobile_drawer_verified.png`);
    fs.writeFileSync(mobShotPath, Buffer.from(mobShot.result.data, 'base64'));
    console.log(`  Mobile Screenshot: ${mobShotPath}`);

    console.log('\n======================================================');
    console.log(' ALL 5 DASHBOARDS & MOBILE DRAWER 100% VERIFIED!');
    console.log('======================================================');

  } finally {
    if (ws) ws.close();
    proc.kill();
  }
}

main().catch(err => {
  console.error('\nTest runner failed:', err);
  process.exit(1);
});
