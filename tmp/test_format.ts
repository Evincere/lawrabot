function formatForWhatsApp(text: string): string {
    return text
        .replace(/\*\*/g, "*") // Convert Markdown bold to WhatsApp bold
        .replace(/^#+\s+/gm, "") // Remove Markdown headers
        .trim();
}

const testCases = [
    {
        input: "Hola, soy **LawraBot**",
        expected: "Hola, soy *LawraBot*"
    },
    {
        input: "### Título de Sección\nContenido con **negrita**.",
        expected: "Título de Sección\nContenido con *negrita*."
    },
    {
        input: "#### Subtítulo\n- Item 1\n- Item **2**",
        expected: "Subtítulo\n- Item 1\n- Item *2*"
    }
];

testCases.forEach((tc, i) => {
    const result = formatForWhatsApp(tc.input);
    if (result === tc.expected) {
        console.log(`Test ${i + 1} PASSED`);
    } else {
        console.log(`Test ${i + 1} FAILED`);
        console.log(`  Input: ${tc.input}`);
        console.log(`  Expected: ${tc.expected}`);
        console.log(`  Got: ${result}`);
    }
});
