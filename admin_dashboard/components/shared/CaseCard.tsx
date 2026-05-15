"use client";

import { cn } from "@/lib/utils";
import { 
  Circle, 
  MapPin, 
  Calendar,
  FileText,
  User,
  Clock
} from "@phosphor-icons/react";
import { motion } from "framer-motion";

interface CaseCardProps {
  name: string;
  id: string;
  status: "Intake" | "Review" | "Approved" | "Action Required";
  lastActivity: string;
  location: string;
  isActive?: boolean;
  onClick?: () => void;
}

const STATUS_CONFIG = {
  "Intake": { color: "text-blue-400", bg: "bg-blue-400/10", border: "border-blue-400/20" },
  "Review": { color: "text-amber-400", bg: "bg-amber-400/10", border: "border-amber-400/20" },
  "Approved": { color: "text-emerald-400", bg: "bg-emerald-400/10", border: "border-emerald-400/20" },
  "Action Required": { color: "text-rose-400", bg: "bg-rose-400/10", border: "border-rose-400/20" },
};

export function CaseCard({ 
  name, 
  id, 
  status, 
  lastActivity, 
  location,
  isActive,
  onClick 
}: CaseCardProps) {
  const config = STATUS_CONFIG[status];

  return (
    <motion.div
      whileHover={{ x: 4 }}
      whileTap={{ scale: 0.98 }}
      onClick={onClick}
      className={cn(
        "group cursor-pointer p-5 rounded-2xl transition-all duration-300 border relative overflow-hidden",
        isActive 
          ? "bg-surface border-border-accent shadow-[0_8px_16px_rgba(0,0,0,0.4)]" 
          : "bg-transparent border-transparent hover:bg-white/5 hover:border-white/5"
      )}
    >
      {/* Active Indicator Strip */}
      {isActive && (
        <div className="absolute left-0 top-0 bottom-0 w-1 bg-accent" />
      )}

      <div className="flex flex-col gap-4">
        <div className="flex items-start justify-between gap-2">
          <div className="flex gap-3">
             <div className={cn(
               "w-10 h-10 rounded-xl flex items-center justify-center border shrink-0 transition-colors",
               isActive ? "bg-accent/10 border-accent/20 text-accent" : "bg-zinc-900 border-border text-zinc-600"
             )}>
                <User size={20} weight={isActive ? "bold" : "duotone"} />
             </div>
             <div className="min-w-0">
                <h4 className={cn(
                  "font-bold text-sm tracking-tighter truncate transition-colors",
                  isActive ? "text-white" : "text-zinc-400 group-hover:text-zinc-200"
                )}>
                  {name.toUpperCase()}
                </h4>
                <p className="text-[10px] font-black text-zinc-600 tracking-[0.15em] mt-0.5 font-mono">
                  ID: {id}
                </p>
             </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
          <div className={cn(
            "flex items-center gap-1.5 px-2 py-0.5 rounded border text-[8px] font-black uppercase tracking-widest",
            config.color,
            config.bg,
            config.border
          )}>
            <Circle size={6} weight="fill" />
            {status}
          </div>

          <div className="flex items-center gap-1.5 text-zinc-600 text-[10px] font-bold">
            <MapPin size={12} weight="duotone" />
            {location}
          </div>

          <div className="flex items-center gap-1.5 text-zinc-600 text-[10px] font-bold ml-auto">
            <Clock size={12} weight="duotone" />
            {lastActivity}
          </div>
        </div>
      </div>
      
      {/* Visual Decoration: Corner ID stamp (very subtle) */}
      <div className="absolute -bottom-2 -right-2 text-[48px] font-black text-white/[0.02] select-none pointer-events-none font-mono italic">
         {id.substring(0, 2)}
      </div>
    </motion.div>
  );
}
