"use client";

import { Expediente } from "@/lib/types/divorce";
import { 
  FileText, 
  DownloadSimple,
  ShieldCheck,
  WarningCircle,
  Clock,
  DotsThreeVertical
} from "@phosphor-icons/react";
import { motion, AnimatePresence } from "framer-motion";
import { cn } from "@/lib/utils";

interface CaseCaratulaProps {
  expediente: Expediente | null;
  viewMode?: "idle" | "overview" | "focus";
}

export function CaseCaratula({ expediente, viewMode = "overview" }: CaseCaratulaProps) {
  const isFocus = viewMode === "focus";
  if (!expediente) return (
    <header className="h-32 px-10 border-b border-border flex items-center justify-between bg-surface/20 backdrop-blur-md shrink-0 opacity-50">
       <div className="flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl border border-dashed border-border flex items-center justify-center">
             <FileText size={24} className="text-zinc-600" />
          </div>
          <div>
             <h4 className="text-sm font-bold tracking-widest text-zinc-500 uppercase">Sin Expediente Seleccionado</h4>
             <p className="text-[10px] text-zinc-600 font-medium">Seleccione un caso de la mesa de entradas para comenzar</p>
          </div>
       </div>
    </header>
  );

  const getStatusConfig = (status: string) => {
    if (status.includes("DATA_COLLECTION")) return { label: "RECOLECCIÓN", color: "text-blue-400", border: "border-blue-500/30", bg: "bg-blue-500/10", icon: Clock };
    if (status.includes("DATA_COMPLETE")) return { label: "EN REVISIÓN", color: "text-amber-400", border: "border-amber-500/30", bg: "bg-amber-500/10", icon: WarningCircle };
    if (status.includes("READY")) return { label: "LISTO P/FIRMA", color: "text-purple-400", border: "border-purple-500/30", bg: "bg-purple-500/10", icon: FileText };
    if (status.includes("CLOSED")) return { label: "FINALIZADO", color: "text-emerald-400", border: "border-emerald-500/30", bg: "bg-emerald-500/10", icon: ShieldCheck };
    return { label: status, color: "text-zinc-400", border: "border-zinc-500/30", bg: "bg-zinc-500/10", icon: FileText };
  };

  const status = getStatusConfig(expediente.status);

  return (
    <motion.header 
       layout
       className={cn(
         "px-10 border-b border-border bg-surface/[0.03] shrink-0 relative overflow-hidden group flex transition-colors",
         isFocus ? "py-4 flex-row items-center justify-between bg-surface/40" : "py-8 flex-col"
       )}
    >
       {/* Background structural glow */}
       <div className="absolute top-0 right-0 w-96 h-96 bg-accent/5 blur-[100px] pointer-events-none -mt-48 -mr-48 transition-opacity group-hover:opacity-100 opacity-50" />
       
       <motion.div layout className={cn("flex flex-1 relative z-10", isFocus ? "items-center justify-between" : "items-start justify-between")}>
          <motion.div layout className={cn("flex", isFocus ? "flex-row items-center gap-6" : "flex-col gap-4")}>
             {/* Expediente ID & Status */}
             <motion.div layout className="flex items-center gap-3">
                <span className="font-mono text-xs font-bold tracking-[0.2em] text-accent bg-accent/5 px-3 py-1 rounded-md border border-accent/20">
                   EXP-{expediente.id.substring(0, 8).toUpperCase()}
                </span>
                <div className={cn(
                   "flex items-center gap-1.5 px-3 py-1 rounded-md border text-[9px] font-black uppercase tracking-[0.15em] shrink-0",
                   status.color,
                   status.border,
                   status.bg
                )}>
                   <status.icon size={12} weight="bold" />
                   {status.label}
                </div>
             </motion.div>

             {/* Nomenclatura Judicial */}
             <motion.div layout className={cn("space-y-1", isFocus && "flex items-center gap-3 space-y-0")}>
                <motion.h1 layout className={cn("font-black tracking-tighter text-white leading-none", isFocus ? "text-base" : "text-2xl")}>
                   {expediente.petitioner?.fullName?.fullName?.toUpperCase() || "CARÁTULA PENDIENTE"}
                </motion.h1>
                <motion.div layout className="flex items-center gap-2">
                   {!isFocus && <div className="h-[1px] w-6 bg-border" />}
                   <span className="italic text-zinc-500 text-xs font-serif lowercase">v.</span>
                   {!isFocus && <div className="h-[1px] w-6 bg-border" />}
                </motion.div>
                <motion.h1 layout className={cn("font-black tracking-tighter text-white/70 leading-none", isFocus ? "text-base" : "text-2xl")}>
                   {expediente.respondent?.fullName?.fullName?.toUpperCase() || "DEMANDADO S/D"}
                </motion.h1>
                
                <AnimatePresence>
                  {!isFocus && (
                    <motion.p 
                      initial={{ opacity: 0, height: 0 }}
                      animate={{ opacity: 1, height: "auto", marginTop: "8px" }}
                      exit={{ opacity: 0, height: 0, marginTop: 0 }}
                      className="text-xs font-bold text-accent uppercase tracking-[0.2em]"
                    >
                       s/ DIVORCIO VINCULAR {expediente.divorceType === "BILATERAL" ? "PRESENTACIÓN CONJUNTA" : "ART. 437 CCCN"}
                    </motion.p>
                  )}
                </AnimatePresence>
             </motion.div>
          </motion.div>

          <motion.div layout className={cn("flex", isFocus ? "flex-row items-center gap-6" : "flex-col items-end gap-6")}>
             <motion.div layout className="flex items-center gap-2">
                <button className="p-2 rounded-xl bg-surface border border-border text-zinc-400 hover:text-white hover:border-border-strong transition-all">
                   <DownloadSimple size={isFocus ? 16 : 20} weight="bold" />
                </button>
                <button className="p-2 rounded-xl bg-surface border border-border text-zinc-400 hover:text-white hover:border-border-strong transition-all">
                   <DotsThreeVertical size={isFocus ? 16 : 20} weight="bold" />
                </button>
             </motion.div>

             <AnimatePresence>
               {!isFocus && (
                  <motion.div 
                    initial={{ opacity: 0, scale: 0.95 }}
                    animate={{ opacity: 1, scale: 1 }}
                    exit={{ opacity: 0, scale: 0.95, display: "none" }}
                    className="text-right"
                  >
                    <p className="text-[10px] uppercase tracking-[0.2em] text-zinc-600 font-bold">Fuero Civil y Comercial</p>
                    <p className="text-[11px] font-bold text-zinc-400 mt-0.5">San Rafael, Circunscripción II, MZA</p>
                    <div className="mt-2 flex items-center justify-end gap-2 text-[10px] text-zinc-500 font-medium bg-white/5 border border-white/5 px-2 py-0.5 rounded-md">
                       <Clock size={12} />
                       Ingreso: {new Date(expediente.createdAt).toLocaleDateString()}
                    </div>
                  </motion.div>
               )}
             </AnimatePresence>
          </motion.div>
       </motion.div>
    </motion.header>
  );
}
