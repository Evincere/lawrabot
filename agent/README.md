# 🧩 TemplateClaw

A reusable, domain-agnostic AI agent template framework built with Node.js and TypeScript.

TemplateClaw provides the heavy infrastructure (messaging channels, LLM integrations, session management, tool engine) so you can focus only on the domain logic (prompts and tools).

## Features

- **Multi-channel:** WhatsApp (via Baileys) and Telegram (via grammY)
- **Ollama LLM:** Built-in adapter with tool-calling support
- **Session Manager:** Per-conversation history with JSONL persistence
- **Specialization System:** Drop in a directory with your domain logic and the agent adapts instantly
- **Document Engine:** Built-in PDF generation tools
- **Hook System:** Priority-based lifecycle hooks

## Quick Start

1. Install dependencies:
   ```bash
   npm install
   ```
2. Set your environment variables (e.g. `TELEGRAM_BOT_TOKEN`)
3. Run an example specialization:
   ```bash
   npm run dev -- --spec ./specs/travel-agent
   # OR
   npm run dev -- --spec ./specs/divorce-assistant
   ```

## Creating a Specialization

Create a directory with the following structure:

```text
my-spec/
├── agent.config.json5    # Configuration (name, channels, LLM)
├── prompts/
│   └── system.md         # The system prompt / personality
└── tools/
    └── my-tool.ts        # Domain-specific tools (exports AgentTool)
```

## Available Scripts

- `npm run dev` - Start the agent locally with tsx
- `npm run build` - Build the project to `./dist` using tsdown
- `npm run start` - Run the built output
- `npm run check` - Type-check the codebase
- `npm run test` - Run Vitest test suites
