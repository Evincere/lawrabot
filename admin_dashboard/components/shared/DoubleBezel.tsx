"use client";

import { cn } from "@/lib/utils";
import { motion } from "framer-motion";

interface DoubleBezelProps {
  children: React.ReactNode;
  className?: string;
  innerClassName?: string;
}

/**
 * DoubleBezel implements the "Nested Architecture" skill.
 * It creates a physical, machined hardware look using a wrapper shell and inner core.
 */
export function DoubleBezel({ children, className, innerClassName }: DoubleBezelProps) {
  return (
    <div
      className={cn(
        "p-2 rounded-[2rem] bg-white/5 border border-white/5 shadow-2xl relative overflow-hidden group",
        className
      )}
    >
      {/* Background Mesh Gradient Glow (Subtle) */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_-20%,rgba(59,130,246,0.05),transparent_70%)] pointer-events-none" />
      
      <div
        className={cn(
          "h-full w-full rounded-[calc(2.5rem-0.5rem)] bg-zinc-900/40 backdrop-blur-md border border-white/10 shadow-[inset_0_1px_1px_rgba(255,255,255,0.05)] relative overflow-hidden",
          innerClassName
        )}
      >
        {children}
      </div>
    </div>
  );
}
