const { chromium } = require('playwright');
const fs = require('fs');

(async () => {
  console.log("Iniciando browser headless...");
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
      userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
      viewport: { width: 1280, height: 800 }
  });
  const page = await context.newPage();

  try {
    console.log("[1] Navegando a https://blsg.pjm.gob.ar/");
    await page.goto('https://blsg.pjm.gob.ar/');

    console.log("[2] Esperando networkidle...");
    await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => console.log("Timeout networkidle ignorado"));

    if (page.url().includes('microsoftonline.com') || await page.title().then(t => t.includes('Sign in'))) {
        console.log("[3] Detectado Microsoft Login. Llenando i0116...");
        await page.waitForSelector('input#i0116', { timeout: 10000 });
        await page.fill('input#i0116', 'spereyra@jus.mendoza.gov.ar');
        await page.click('input#idSIButton9');
        
        console.log("[4] Esperando password i0118...");
        await page.waitForSelector('input#i0118', { timeout: 10000 });
        await page.fill('input#i0118', 'Pasar$1235');
        await page.click('input#idSIButton9');

        console.log("[5] Esperando idBtn_Back...");
        await page.waitForSelector('input#idBtn_Back', { timeout: 10000 }).catch(e => console.log("idBtn_Back no encontrado."));
        if (await page.locator('input#idBtn_Back').isVisible()) {
            await page.click('input#idBtn_Back');
        }

        console.log("[6] Esperando input#dni...");
        await page.waitForSelector('input#dni', { timeout: 10000 });
        console.log("Sesión iniciada!");
    }

    console.log("[7] Tomando screenshot pre-DNI...");
    await page.screenshot({ path: 'debug_1.png' });

    console.log("[8] Ingresando DNI 26598410...");
    await page.fill("input[type='text']#dni", "26598410");
    await page.keyboard.press("Enter");

    console.log("[9] Esperando texto DNI 26598410...");
    await page.waitForSelector("text=DNI 26598410", { timeout: 15000 });
    console.log("✅ DNI ENCONTRADO!");

  } catch (e) {
    console.error("❌ Excepción capturada:", e.message);
    const html = await page.content();
    fs.writeFileSync('debug_error.html', html);
    await page.screenshot({ path: 'debug_error.png' });
    console.log("HTML/Screenshot de error guardados.");
  } finally {
    await browser.close();
  }
})();
