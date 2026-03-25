import PDFDocument from "pdfkit";
import type { AgentTool } from "../types.js";

/** Built-in tool: generate a PDF document from text content. */
export const generatePdfTool: AgentTool = {
  name: "generate_pdf",
  description:
    "Generate a PDF document from text content. Returns the PDF as a file attachment. Use this for creating reports, forms, letters, or any document.",
  parameters: {
    title: {
      type: "string",
      description: "Title of the document",
      required: true,
    },
    content: {
      type: "string",
      description:
        "Body text of the document. Supports simple formatting: lines starting with '# ' become headings, '## ' become subheadings, '- ' become bullet points.",
      required: true,
    },
    fileName: {
      type: "string",
      description: "Output file name (without extension). Defaults to 'document'.",
      required: false,
    },
  },
  async handler(params) {
    const title = params.title as string;
    const content = params.content as string;
    const fileName = (params.fileName as string) || "document";

    const chunks: Buffer[] = [];

    const doc = new PDFDocument({ margin: 50 });
    doc.on("data", (chunk: Buffer) => chunks.push(chunk));

    const done = new Promise<void>((resolve) => doc.on("end", resolve));

    // Title
    doc.fontSize(22).font("Helvetica-Bold").text(title, { align: "center" });
    doc.moveDown(1.5);

    // Body — parse simple markdown-like formatting
    const lines = content.split("\n");
    for (const line of lines) {
      if (line.startsWith("# ")) {
        doc.fontSize(16).font("Helvetica-Bold").text(line.slice(2));
        doc.moveDown(0.5);
      } else if (line.startsWith("## ")) {
        doc.fontSize(13).font("Helvetica-Bold").text(line.slice(3));
        doc.moveDown(0.3);
      } else if (line.startsWith("- ")) {
        doc.fontSize(11).font("Helvetica").text(`  •  ${line.slice(2)}`, { indent: 10 });
      } else if (line.trim() === "") {
        doc.moveDown(0.5);
      } else {
        doc.fontSize(11).font("Helvetica").text(line);
      }
    }

    doc.end();
    await done;

    const pdfBuffer = Buffer.concat(chunks);
    return {
      content: `Generated PDF document "${title}" (${pdfBuffer.length} bytes)`,
      file: {
        name: `${fileName}.pdf`,
        data: pdfBuffer,
        mimeType: "application/pdf",
      },
    };
  },
};
