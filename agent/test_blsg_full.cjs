const { chromium } = require('playwright');
const fs = require('fs');

(async () => {
  console.log("Iniciando browser...");
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();

  console.log("Navegando a https://blsg.pjm.gob.ar/ ...");
  try {
    await page.goto('https://blsg.pjm.gob.ar/', { waitUntil: 'load' });
    console.log("URL inicial cargada: " + page.url());
    
    // Esperar que redireccione o se estabilice
    await page.waitForTimeout(5000);
    console.log("URL después de 5s: " + page.url());

    if (page.url().includes('login.microsoftonline.com')) {
      console.log("Intentando encontrar el email input `#i0116` con timeout de 20s...");
      await page.waitForSelector('input#i0116', { timeout: 20000 });
      console.log("✅ input#i0116 ENCONTRADO!");
    } else {
      console.log("No estamos en Microsoft Login. HTML final:");
    }
  } catch (e) {
    console.error("❌ Error Playwright: ", e.message);
  } finally {
    await browser.close();
  }
})();
