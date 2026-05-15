"use client";

import { cn } from "@/lib/utils";
import {
  ChatCircleDots,
  ClockCounterClockwise,
  Files,
  NotePencil,
  Plus,
} from "@phosphor-icons/react";
import { motion } from "framer-motion";

export interface UtilityAction {
  id: string;
  icon: React.ElementType;
  label: string;
  onClick: () => void;
  variant?: "success" | "danger" | "accent" | "default";
}

interface UtilityBarProps {
  activeTool: string | null;
  onToolToggle: (tool: string) => void;
  actions?: UtilityAction[];
}

const TOOLS = [
  { id: "chat", icon: ChatCircleDots, label: "Chat con Ciudadano" },
  { id: "history", icon: ClockCounterClockwise, label: "Historial de Eventos" },
  { id: "docs", icon: Files, label: "Documentación del Caso" },
  { id: "notes", icon: NotePencil, label: "Notas Rápidas" },
];

export function UtilityBar({ activeTool, onToolToggle, actions = [] }: UtilityBarProps) {
  return (
    <aside className="w-16 h-full border-l border-border bg-surface flex flex-col items-center py-6 gap-4 z-40 shrink-0 relative overflow-hidden shadow-2xl">
      {/* Subtle noise and texture */}
      <div className="absolute inset-0 noise pointer-events-none opacity-20" />

      {/* 1. Actions Section (Operational Commands) */}
      <div className="flex flex-col gap-3 relative z-10">
        {actions.map((action) => {
          const Icon = action.icon;
          return (
            <button
              key={action.id}
              onClick={action.onClick}
              title={action.label}
              className={cn(
                "w-10 h-10 rounded-xl flex items-center justify-center transition-all duration-300 relative overflow-hidden group shrink-0 border",
                action.variant === "success" && "bg-success/10 text-success border-success/30 hover:bg-success/20 hover:border-success/50 shadow-lg shadow-success/10",
                action.variant === "danger" && "bg-danger/10 text-danger border-danger/30 hover:bg-danger/20 hover:border-danger/50 shadow-lg shadow-danger/10",
                action.variant === "accent" && "bg-accent/10 text-accent border-accent/30 hover:bg-accent/20 hover:border-accent/50 shadow-lg shadow-accent/10",
                (!action.variant || action.variant === "default") && "bg-background border-border text-zinc-500 hover:text-white hover:border-border-strong"
              )}
            >
              <Icon size={18} weight="bold" className="group-hover:scale-110 transition-transform" />
            </button>
          );
        })}
      </div>

      {actions.length > 0 && <div className="w-8 h-[2px] bg-border rounded-full my-2 relative z-10" />}

      {/* 2. Tools Section (State Toggles) */}
      <div className="flex flex-col gap-3 relative z-10">
        {TOOLS.map((tool) => {
          const Icon = tool.icon;
          const isActive = activeTool === tool.id;

          return (
            <button
              key={tool.id}
              onClick={() => onToolToggle(tool.id)}
              title={tool.label}
              className={cn(
                "w-10 h-10 rounded-xl flex items-center justify-center transition-all duration-300 relative overflow-hidden shrink-0",
                isActive
                  ? "bg-accent text-white shadow-lg shadow-accent/20 border border-accent/20"
                  : "text-zinc-600 hover:text-zinc-300 hover:bg-white/5 border border-transparent hover:border-border",
              )}
            >
              <Icon size={18} weight={isActive ? "bold" : "duotone"} />

              {isActive && (
                <motion.div
                  layoutId="active-tool-glow"
                  className="absolute inset-0 bg-white/10 blur-xl px-1 outline-none"
                />
              )}
            </button>
          );
        })}
      </div>

      <div className="mt-auto relative z-10">
        <button className="w-10 h-10 rounded-full border border-dashed border-border text-zinc-700 hover:text-zinc-400 hover:border-border-strong flex items-center justify-center transition-all">
          <Plus size={16} weight="bold" />
        </button>
      </div>
    </aside>
  );
}
