"use client";

import { useState } from "react";
import { CaseCard } from "@/components/shared/CaseCard";
import { 
  CaretLeft, 
  CaretRight,
  MagnifyingGlass 
} from "@phosphor-icons/react";
import { cn } from "@/lib/utils";
import { motion, AnimatePresence } from "framer-motion";
import { Expediente, DashboardStatus } from "@/lib/types/divorce";

interface CaseInboxProps {
  cases: Expediente[];
  isLoading: boolean;
  isRefreshing?: boolean;
  activeCaseId: string | null;
  onCaseSelect: (expediente: Expediente) => void;
  isCollapsed: boolean;
  onToggleCollapse: () => void;
}

export function CaseInbox({ 
  cases, 
  isLoading, 
  isRefreshing = false,
  activeCaseId, 
  onCaseSelect,
  isCollapsed,
  onToggleCollapse
}: CaseInboxProps) {
  const [searchQuery, setSearchQuery] = useState("");

  const getStatusLabel = (status: string): DashboardStatus => {
    if (!status) return "Action Required";
    if (status.includes("DATA_COLLECTION")) return "Intake";
    if (status.includes("DATA_COMPLETE")) return "Review";
    if (status.includes("CLOSED")) return "Approved";
    return "Action Required";
  };

  const formatLastActivity = (isoString: string) => {
    if (!isoString) return "n/a";
    const date = new Date(isoString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.round(diffMs / 60000);
    if (diffMins < 60) return `hace ${diffMins}m`;
    const diffHours = Math.round(diffMins / 60);
    if (diffHours < 24) return `hace ${diffHours}h`;
    return date.toLocaleDateString();
  };

  const filteredCases = cases.filter(c => 
    c.petitioner?.fullName?.fullName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    c.id.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <motion.aside 
      initial={false}
      animate={{ width: isCollapsed ? 76 : 384 }}
      transition={{ type: "spring", stiffness: 300, damping: 30 }}
      className="border-r border-border flex flex-col h-full bg-surface/40 backdrop-blur-3xl z-10 relative overflow-hidden"
    >
      <button 
        onClick={onToggleCollapse}
        className="absolute -right-3 top-24 w-6 h-6 rounded-full bg-background border border-border flex items-center justify-center text-zinc-500 hover:text-white hover:border-border-accent transition-all z-20 shadow-xl"
        title={isCollapsed ? "Expandir lista" : "Colapsar lista"}
      >
        {isCollapsed ? <CaretRight size={12} weight="bold" /> : <CaretLeft size={12} weight="bold" />}
      </button>

      <div className="flex flex-col h-full w-full overflow-hidden">
        <AnimatePresence mode="wait">
          {!isCollapsed ? (
            <motion.div 
              key="inbox-content"
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              transition={{ duration: 0.2 }}
              className="flex flex-col h-full w-96"
            >
              <div className="p-8 border-b border-border bg-surface/[0.02]">
                <div className="flex items-center gap-2 mb-4">
                  <div className="w-2 h-2 rounded-full bg-accent shadow-[0_0_12px_rgba(59,130,246,0.5)] animate-pulse" />
                  <span className="text-[10px] uppercase tracking-[0.2em] font-bold text-zinc-500 whitespace-nowrap">Mesa de Entradas</span>
                </div>
                <div className="flex items-center justify-between">
                  <h3 className="text-3xl font-bold tracking-tighter text-white">Expedientes</h3>
                  {isRefreshing && !isLoading && (
                    <div className="flex items-center gap-2 px-2 py-1 rounded-md bg-amber-500/10 border border-amber-500/20">
                      <div className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse" />
                      <span className="text-[8px] font-bold text-amber-500 uppercase tracking-tighter">Sync</span>
                    </div>
                  )}
                </div>
                <p className="text-xs text-secondary font-medium mt-1">
                  {isLoading ? "Cargando..." : `${cases.length} casos activos`}
                </p>

                {/* Búsqueda rápida */}
                <div className="mt-6 relative group">
                  <MagnifyingGlass 
                    size={16} 
                    className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500 group-focus-within:text-accent transition-colors" 
                  />
                  <input 
                    type="text"
                    placeholder="Filtrar por nombre o ID..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="w-full bg-background border border-border rounded-xl py-2 pl-10 pr-4 text-xs focus:outline-none focus:border-accent/50 transition-all font-medium placeholder:text-zinc-600"
                  />
                </div>
              </div>
              
              <div className="flex-1 overflow-y-auto p-4 space-y-1 scrollbar-hide">
                {isLoading ? (
                  <div className="flex justify-center p-12 opacity-20">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-white" />
                  </div>
                ) : filteredCases.length === 0 ? (
                  <div className="p-12 text-center text-zinc-600">
                    <p className="text-[10px] font-bold uppercase tracking-widest">No se encontraron casos</p>
                  </div>
                ) : (
                  filteredCases.map((doc: Expediente) => (
                    <CaseCard 
                      key={doc.id}
                      id={doc.id.substring(0, 8).toUpperCase()}
                      name={doc.petitioner?.fullName?.fullName || "Expediente Nuevo"}
                      status={getStatusLabel(doc.status)}
                      lastActivity={formatLastActivity(doc.createdAt)}
                      location={doc.lastConjugalResidence?.locality || "N/A"}
                      isActive={activeCaseId === doc.id}
                      onClick={() => onCaseSelect(doc)}
                    />
                  ))
                )}
              </div>
            </motion.div>
          ) : (
            <motion.div 
               key="inbox-collapsed"
               initial={{ opacity: 0 }}
               animate={{ opacity: 1 }}
               className="flex flex-col items-center pt-8 gap-6"
            >
               <div className="w-10 h-10 rounded-2xl bg-accent/10 border border-accent/20 flex items-center justify-center text-accent">
                 <MagnifyingGlass size={20} />
               </div>
               <div className="flex-1 flex flex-col items-center gap-4">
                  {cases.slice(0, 8).map(c => (
                    <div 
                      key={c.id}
                      onClick={() => onCaseSelect(c)}
                      className={cn(
                        "w-10 h-10 rounded-full border flex items-center justify-center cursor-pointer transition-all",
                        activeCaseId === c.id ? "bg-accent border-accent text-white" : "border-border text-zinc-600 hover:border-zinc-400"
                      )}
                    >
                      <span className="text-[10px] font-bold">{c.petitioner?.fullName?.fullName?.[0] || "?"}</span>
                    </div>
                  ))}
               </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </motion.aside>
  );
}
