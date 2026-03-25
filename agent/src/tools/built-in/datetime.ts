import type { AgentTool } from "../types.js";

/** Built-in tool: get current date and time. */
export const datetimeTool: AgentTool = {
  name: "get_datetime",
  description: "Get the current date and time in ISO format and human-readable format.",
  parameters: {
    timezone: {
      type: "string",
      description: "IANA timezone (e.g. 'America/New_York'). Defaults to system timezone.",
      required: false,
    },
  },
  async handler(params) {
    const tz = (params.timezone as string) || Intl.DateTimeFormat().resolvedOptions().timeZone;
    const now = new Date();
    const formatted = now.toLocaleString("en-US", { timeZone: tz, dateStyle: "full", timeStyle: "long" });
    return {
      content: JSON.stringify({ iso: now.toISOString(), formatted, timezone: tz }),
    };
  },
};
