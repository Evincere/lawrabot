# AGENTS.md — TemplateClaw Guidelines

**Working Directory**: `agent/`

## 🚀 High-Signal Commands

```bash
# Start agent (Requires --spec <path>)
npm run dev -- --spec ./specs/travel-agent
npm run validate -- --spec ./specs/travel-agent  # Validates config/tools without starting

# Verification Flow: lint -> typecheck -> test
npm run lint && npm run check && npm run test

# Focused Testing
npm run test -- src/utils/logger.test.ts       # Single file
npm run test -- -t "test name"                 # Single case
```

## 🛠️ Architecture & Quirks

- **ESM Mandatory**: Always use `.js` extensions in relative imports: `import { x } from "./file.js"`.
- **Specialization System**: The agent is a shell. Domain logic lives in `specs/<name>/` (config, prompts, tools).
- **Tool Execution Loop**: LLM → Tool Call → Handler → Result → LLM. Max rounds: 10.
- **Session Storage**: Per-conversation history is persisted as JSONL in `.data/sessions`.
- **Configuration**: Validated at runtime via Zod schemas in `src/config/schema.ts`.

## 📏 Critical Conventions

- **Language**:
  - **Code**: All code, variable names, and types must be in **English**.
  - **Comments/Docs**: Use **Spanish** for code comments, documentation, and developer notes.
  - **Chat**: All communication with the user must be in **Spanish**.
- **Logging**: NEVER use `console.log`. Use `createLogger("name")` from `src/utils/logger.js`.
- **Error Handling**: Use the pattern: `const msg = error instanceof Error ? error.message : String(error);`.
- **Env Vars**: Use `envRequired()` for mandatory keys; it throws if missing.
- **Naming**:
  - Directories: `kebab-case`
  - TS Files: `camelCase`
  - Classes: `PascalCase`
  - Tools: Must export `AgentTool` interface.

## 📁 Directory Map

- `src/channels/`: Platform adapters (WhatsApp/Baileys, Telegram/grammY).
- `src/gateway/`: HTTP server and `message-router.ts` (the main brain).
- `src/llm/`: Ollama provider implementation.
- `src/hooks/`: Lifecycle hooks (`message_received` → `tool_called` → `before_reply` → `after_reply`).
- `src/tools/`: Tool registry and built-in tools (PDF generation, Datetime).
