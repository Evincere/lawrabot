# AGENTS.md — Development Guidelines for TemplateClaw

**Working Directory**: `agent/`

## Build / Lint / Test Commands

```bash
# Development
npm run dev              # Start agent (requires --spec <path>)
npm run dev -- --spec ./specs/travel-agent

# Build
npm run build            # Build to ./dist (tsdown, ESM)
npm run start            # Run built output

# Type Checking
npm run check            # TypeScript --noEmit

# Linting
npm run lint             # Run oxlint on src/

# Testing
npm run test             # Run Vitest suites
npm run test:watch       # Watch mode
npm run test -- -t name  # Run tests matching pattern
npm run test -- file.test.ts  # Run single test file
```

### Running a Single Test
```bash
npm run test -- src/utils/logger.test.ts
npx vitest run -t "test name"
```

## Code Style Guidelines

### TypeScript Configuration

- **Target**: ES2023
- **Module System**: NodeNext (ESM)
- **Strict Mode**: Enabled (`strict: true`)
- **Output**: No emit during development (`noEmit: true`)
- **Module Resolution**: NodeNext

### Imports

- Use **ESM syntax** exclusively (`import`/`export`)
- Always include `.js` extension in relative imports:
  ```typescript
  import { foo } from "./utils.js";
  import type { Bar } from "../types.js";
  ```
- Group imports: standard library → third-party → local modules
- Use type imports for types only: `import type { X } from ...`

### File Naming

- **kebab-case** for directories: `channels/`, `specialization/`
- **camelCase** for TypeScript files: `message-router.ts`, `loader.ts`
- **Index files**: `index.ts` for module entry points
- Test files: `*.test.ts` suffix

### Code Organization

```
src/
├── channels/       # Messaging platform adapters (WhatsApp, Telegram)
├── config/         # Configuration schemas and loaders
├── gateway/        # HTTP server and message routing
├── hooks/          # Lifecycle hook system
├── llm/            # LLM provider implementations
├── sessions/       # Session management and persistence
├── specialization/ # Dynamic specialization loading
├── tools/          # Tool registry and definitions
└── utils/          # Shared utilities (logger, env helpers)
```

### Naming Conventions

- **Classes**: PascalCase (`OllamaProvider`, `ToolRegistry`)
- **Functions**: camelCase (`createLogger`, `loadConfig`)
- **Interfaces**: PascalCase with descriptive suffix (`InboundMessage`, `ToolContext`)
- **Types**: PascalCase (`Logger`, `AgentTool`)
- **Constants**: camelCase for objects, UPPER_CASE for primitives
- **Private members**: Use `private` keyword (no underscore prefix)

### Error Handling

- Use **try-catch** blocks for async operations
- Type errors properly: `error instanceof Error ? error.message : String(error)`
- Log errors with context using pino logger
- Return graceful error messages to users
- Exit with code 1 on fatal startup errors

```typescript
try {
  await riskyOperation();
} catch (error) {
  const msg = error instanceof Error ? error.message : String(error);
  log.error(`Operation failed: ${msg}`);
  throw error; // or return error result
}
```

### Logging

- Use pino logger with named loggers: `createLogger("templateclaw")`
- Log levels: `debug` < `info` < `warn` < `error`
- Include context in log messages (IDs, names, paths)
- Production: JSON logs; Development: pretty-printed with colors

### Async/Await Patterns

- Always use `async/await` (no `.then()` chains)
- Await all Promises before returning
- Use `Promise.all()` for parallel operations
- Handle timeouts and cancellations gracefully

### Type Safety

- Use **Zod** for runtime validation of configs
- Define explicit types for function parameters and returns
- Use type aliases for complex shapes: `type AppConfig = z.infer<typeof Schema>`
- Prefer interfaces for object shapes that methods extend
- Use discriminated unions for variant types

### Documentation & Testing

- JSDoc for public APIs
- Test files: `*.test.ts` (Vitest)
- Mock external dependencies
- No `console.log` — use logger

### Specialization Structure

```
my-spec/
├── agent.config.json5  # Zod-validated config
├── prompts/
│   └── system.md       # System prompt
└── tools/
    └── my-tool.ts      # AgentTool export
```

### Environment Variables

- `envOrDefault()` — optional
- `envRequired()` — mandatory (throws)
- `isTruthy()` — boolean parsing

### Git

- Imperative commits ("Add", "Fix")
- Ignore: node_modules/, dist/, .data/
