const { chromium } = require('playwright');
(async () => {
  const [,, svg, png] = process.argv;
  const b = await chromium.launch(); const p = await b.newPage({ viewport: { width: 400, height: 640 } });
  await p.goto('file://' + svg); await p.screenshot({ path: png }); await b.close();
})();
