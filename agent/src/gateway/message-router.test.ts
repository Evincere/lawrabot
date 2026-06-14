import { describe, it, expect } from "vitest";
import { _testing } from "./message-router.js";

const { isDegenerate, sanitizeCoTLeak, sanitizeBeforeSend, deduplicateBlocks } = _testing;

// Logger mock para sanitizeBeforeSend
const mockLog = {
  info: () => {},
  warn: () => {},
  error: () => {},
  debug: () => {},
};

describe("isDegenerate", () => {
  it("debería detectar texto con caracteres Unicode invisibles", () => {
    const garbled = "📄\n────────────────\n\nPara\u200B \u200B \u200B \u200B \u200B   ?\n\n¡\u200B\u200B\u200B\n\n\u200B";
    expect(isDegenerate(garbled)).toBe(true);
  });

  it("debería detectar leak de chain-of-thought del modelo", () => {
    const cotLeak = `⚖️  C          \u200B
────────────────
Jaquier *…  *
We see that the assistant output is garbled. This is not following format.
Need to correct. Must follow the required structure.
Also need to ensure we don't break any rules.
Let's produce correct final answer.`;

    expect(isDegenerate(cotLeak)).toBe(true);
  });

  it("debería detectar separadores sin contenido real posterior", () => {
    const orphanSeparator = "📄\n────────────────\n\n   \n  ?\n  \n";
    expect(isDegenerate(orphanSeparator)).toBe(true);
  });

  it("debería detectar múltiples ellipsis como output corrupto", () => {
    const ellipsisHell = "Perfecto... vamos a... ver... como... seguimos... con... esto...";
    expect(isDegenerate(ellipsisHell)).toBe(true);
  });

  it("debería detectar cambio de idioma abrupto al inglés", () => {
    const mixedLang = "The assistant Must follow the response format. We need to Also correct This output structure and follow guidelines.";
    expect(isDegenerate(mixedLang)).toBe(true);
  });

  it("NO debería marcar como degenerado un mensaje normal", () => {
    const normal = `## ⚖️ LAWRABOT — INICIO DE TRÁMITE
────────────────

Entiendo lo complicado que puede ser este momento y estoy acá para acompañarte en cada paso del proceso de divorcio.

👉 *¿Me podrías indicar tu número de DNI?*`;

    expect(isDegenerate(normal)).toBe(false);
  });

  it("NO debería marcar como degenerado un mensaje con datos de hijos", () => {
    const childrenMsg = `## 📋 HIJOS EN COMÚN
────────────────

Entiendo, tenés 4 niños. Para poder registrar correctamente sus datos, necesitaremos el nombre completo y la fecha de nacimiento de cada uno.

👉 *¿Podés indicarme el nombre y la fecha de nacimiento de cada niño?*`;

    expect(isDegenerate(childrenMsg)).toBe(false);
  });

  it("NO debería marcar textos cortos como degenerados", () => {
    expect(isDegenerate("Hola")).toBe(false);
    expect(isDegenerate("")).toBe(false);
    expect(isDegenerate("Ok, continuamos.")).toBe(false);
  });
});

describe("sanitizeCoTLeak", () => {
  it("debería eliminar bloques de razonamiento interno en inglés", () => {
    const input = `## ⚖️ LAWRABOT — CONVENIO REGULADOR
────────────────

Jaquelina, el borrador quedó guardado.

We see that the assistant output is garbled.
This is not following format.
Need to correct.
Must follow the required structure.

## ⚖️ LAWRABOT — CONVENIO REGULADOR
────────────────

Jaquelina, el borrador de tu propuesta quedó guardado correctamente.`;

    const cleaned = sanitizeCoTLeak(input);
    expect(cleaned).not.toContain("We see that");
    expect(cleaned).not.toContain("Need to correct");
    expect(cleaned).not.toContain("Must follow");
    expect(cleaned).toContain("Jaquelina");
    expect(cleaned).toContain("guardado");
  });

  it("debería eliminar líneas con --- consecutivos", () => {
    const input = `Texto válido.\n\n---\n\n---\n\n---\n\nMás texto válido.`;
    const cleaned = sanitizeCoTLeak(input);
    expect(cleaned).not.toContain("---");
    expect(cleaned).toContain("Texto válido");
    expect(cleaned).toContain("Más texto válido");
  });

  it("debería eliminar líneas sueltas de razonamiento", () => {
    const input = `Datos registrados correctamente.\nLet's produce correct final answer.\nGracias por tu paciencia.`;
    const cleaned = sanitizeCoTLeak(input);
    expect(cleaned).not.toContain("Let's produce");
    expect(cleaned).toContain("Datos registrados");
    expect(cleaned).toContain("Gracias");
  });

  it("NO debería modificar un mensaje normal en español", () => {
    const input = `Jaquelina, registramos los datos de tus 4 niños.

👉 *¿Querés que continuemos con el convenio regulador?*`;
    const cleaned = sanitizeCoTLeak(input);
    expect(cleaned).toBe(input);
  });
});

describe("sanitizeBeforeSend", () => {
  it("debería eliminar caracteres Unicode invisibles", () => {
    const input = "Hola\u200B Jaquelina\u200C, \u200Dtodo bien\uFEFF.";
    const cleaned = sanitizeBeforeSend(input, mockLog as any);
    expect(cleaned).not.toContain("\u200B");
    expect(cleaned).not.toContain("\u200C");
    expect(cleaned).not.toContain("\u200D");
    expect(cleaned).not.toContain("\uFEFF");
    expect(cleaned).toContain("Hola");
  });

  it("debería colapsar múltiples líneas vacías", () => {
    const input = "Párrafo uno.\n\n\n\n\n\nPárrafo dos.";
    const cleaned = sanitizeBeforeSend(input, mockLog as any);
    expect(cleaned).toBe("Párrafo uno.\n\nPárrafo dos.");
  });

  it("debería limpiar espacios excesivos dentro de líneas", () => {
    const input = "Para            ?";
    const cleaned = sanitizeBeforeSend(input, mockLog as any);
    expect(cleaned).not.toContain("            ");
  });

  it("debería devolver fallback si queda contenido insuficiente", () => {
    const input = "  *  \n\n  \n   \n  ?  ";
    const cleaned = sanitizeBeforeSend(input, mockLog as any);
    expect(cleaned).toContain("Podemos continuar");
  });

  it("debería devolver fallback para texto vacío", () => {
    const cleaned = sanitizeBeforeSend("", mockLog as any);
    expect(cleaned).toContain("reformular");
  });

  it("NO debería modificar un mensaje válido", () => {
    const input = "Jaquelina, tu trámite fue registrado exitosamente.";
    const cleaned = sanitizeBeforeSend(input, mockLog as any);
    expect(cleaned).toBe(input);
  });
});

describe("deduplicateBlocks", () => {
  it("debería eliminar bloques duplicados semánticamente", () => {
    const input = `Jaquelina, registramos todos los datos personales de tu matrimonio incluyendo la fecha de celebración y el lugar donde se realizó la ceremonia civil.

Jaquelina, ya quedaron registrados todos los datos personales del matrimonio incluyendo la fecha de celebración y el lugar donde se realizó la ceremonia civil.

Ahora necesitamos avanzar con los datos de tus hijos para completar el expediente del divorcio.`;

    const cleaned = deduplicateBlocks(input);
    // Debería mantener el primer bloque y el último (diferente), eliminando el duplicado
    expect(cleaned.split("\n\n").length).toBeLessThan(input.split("\n\n").length);
    expect(cleaned).toContain("datos de tus hijos");
  });

  it("NO debería eliminar bloques que son realmente diferentes", () => {
    const input = `Nombre: Jaquelina Fernández\nDNI: 35.046.455

Domicilio: Moreno 3500, San Rafael, Mendoza

Ahora necesito los datos del matrimonio.`;

    const cleaned = deduplicateBlocks(input);
    // Todos los bloques son diferentes, no debería eliminar nada
    expect(cleaned).toContain("Nombre");
    expect(cleaned).toContain("Domicilio");
    expect(cleaned).toContain("matrimonio");
  });
});
