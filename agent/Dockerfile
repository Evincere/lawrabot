FROM mirror.gcr.io/library/node:22-alpine AS builder

# Install pnpm
RUN corepack enable pnpm

WORKDIR /app

# Copy package files
COPY package.json pnpm-lock.yaml ./

# Install dependencies
RUN pnpm install --frozen-lockfile --ignore-scripts

# Copy source code
COPY . .

# Build
RUN pnpm build

FROM mirror.gcr.io/library/node:22-alpine

WORKDIR /app

# Install pnpm
RUN corepack enable pnpm

# Copy built files and dependencies
COPY --from=builder /app/package.json ./
COPY --from=builder /app/pnpm-lock.yaml ./
COPY --from=builder /app/dist ./dist

# Install production dependencies only
RUN pnpm install --prod --frozen-lockfile --ignore-scripts

# Expose any ports if needed (MCP uses stdio, but maybe web server too?)
# EXPOSE 3000

CMD ["node", "dist/index.js"]
