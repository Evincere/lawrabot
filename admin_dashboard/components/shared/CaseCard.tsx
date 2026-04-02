"use client";

import { cn } from "@/lib/utils";
import { 
  Circle, 
  MapPin, 
  Calendar,
  FileText,
  ChatCircle
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

const STATUS_COLORS = {
  "Intake": "text-blue-400 bg-blue-400/10",
  "Review": "text-yellow-400 bg-yellow-400/10",
  "Approved": "text-emerald-400 bg-emerald-400/10",
  "Action Required": "text-rose-400 bg-rose-400/10",
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
  return (
    <motion.div
      whileHover={{ scale: 1.02 }}
      whileTap={{ scale: 0.98 }}
      onClick={onClick}
      className={cn(
        "group cursor-pointer p-4 rounded-3xl transition-all duration-300 border mb-2",
        isActive 
          ? "glass bg-white/5 border-white/20 shadow-xl" 
          : "bg-transparent border-transparent hover:bg-white/5"
      )}
    >
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-zinc-800 flex items-center justify-center border border-white/10">
            <ChatCircle size={20} weight="duotone" className="text-zinc-400" />
          </div>
          <div>
            <h4 className="font-bold text-sm tracking-tight group-hover:text-accent transition-colors">
              {name}
            </h4>
            <p className="text-[10px] text-zinc-500 uppercase tracking-widest font-medium">
              ID: {id}
            </p>
          </div>
        </div>
        <div className={cn(
          "px-2 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider flex items-center gap-1.5",
          STATUS_COLORS[status]
        )}>
          <Circle size={6} weight="fill" />
          {status}
        </div>
      </div>

      <div className="flex flex-wrap gap-3 mt-4">
        <div className="flex items-center gap-1.5 text-zinc-500 text-[11px]">
          <MapPin size={14} />
          {location}
        </div>
        <div className="flex items-center gap-1.5 text-zinc-500 text-[11px]">
          <Calendar size={14} />
          {lastActivity}
        </div>
      </div>
      
      {/* Visual Indicator for Active Case */}
      {isActive && (
        <div className="absolute right-4 top-1/2 -translate-y-1/2 w-1.5 h-6 bg-accent rounded-full shadow-[0_0_15px_rgba(59,130,246,0.5)]" />
      )}
    </motion.div>
  );
}
