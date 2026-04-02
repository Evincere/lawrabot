const { chromium } = require('playwright');
const fs = require('fs');

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
    viewport: { width: 1280, height: 800 }
  });
  const page = await context.newPage();

  console.log("Navegando a https://blsg.pjm.gob.ar/");
  try {
    await page.goto('https://blsg.pjm.gob.ar/');
    console.log("Página cargada. URL actual: " + page.url());
    console.log("Título: " + await page.title());

    await page.waitForTimeout(3000);
    
    console.log("Tomando screenshot...");
    await page.screenshot({ path: 'blsg_debug.png' });

    console.log("Guardando HTML...");
    const html = await page.content();
    fs.writeFileSync('blsg_debug.html', html);

    console.log("Verificando existencia de selector 'input#i0116' (email) o 'input#dni'...");
    const emailVisible = await page.locator('input#i0116').isVisible();
    const dniVisible = await page.locator('input#dni').isVisible();
    
    console.log(`Input Email visible: ${emailVisible}`);
    console.log(`Input DNI visible: ${dniVisible}`);

  } catch (e) {
    console.error("Error durante Playwright:", e);
  } finally {
    await browser.close();
  }
})();
