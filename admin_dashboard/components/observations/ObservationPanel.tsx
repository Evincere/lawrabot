"use client";

import { useState } from "react";
import { useObservations } from "@/lib/hooks/useObservations";
import { ObservationCard } from "./ObservationCard";
import { ObservationComposer } from "./ObservationComposer";
import type { CreateObservationInput, ResolveObservationInput } from "@/lib/types/observation";
import { cn } from "@/lib/utils";
import {
  WarningCircle,
  CheckCircle,
  Clock,
  Robot,
  Plus,
} from "@phosphor-icons/react";

interface ObservationPanelProps {
  expedienteId: string;
  operatorId?: string;
  enabled?: boolean;
}

function StatCard({
  icon: Icon,
  label,
  value,
  color,
  bg,
}: {
  icon: typeof WarningCircle;
  label: string;
  value: number;
  color: string;
  bg: string;
}) {
  return (
    <div className="p-3 rounded-2xl bg-white/5 border border-white/10">
      <div className="flex items-center gap-2 mb-1">
        <div className={cn("p-1.5 rounded-lg", bg)}>
          <Icon size={14} className={color} />
        </div>
        <span className="text-[10px] uppercase tracking-wider text-zinc-500 font-bold">
          {label}
        </span>
      </div>
      <span className={cn("text-2xl font-bold", color)}>{value}</span>
    </div>
  );
}

function EmptyState({ onCreate }: { onCreate: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center py-12 text-center">
      <div className="w-16 h-16 rounded-full bg-white/5 flex items-center justify-center mb-4">
        <CheckCircle size={32} className="text-zinc-600" />
      </div>
      <h4 className="text-white font-bold mb-2">Sin observaciones</h4>
      <p className="text-sm text-zinc-500 mb-4">
        No hay observaciones registradas para este caso
      </p>
      <button
        onClick={onCreate}
        className="px-4 py-2 rounded-xl bg-accent/10 text-accent text-sm font-medium hover:bg-accent/20 transition-all"
      >
        Crear primera observación
      </button>
    </div>
  );
}

function LoadingState() {
  return (
    <div className="flex flex-col items-center justify-center py-12">
      <div className="w-8 h-8 border-2 border-accent/30 border-t-accent rounded-full animate-spin mb-4" />
      <p className="text-sm text-zinc-500">Cargando observaciones...</p>
    </div>
  );
}

type TabValue = "all" | "pending" | "errors" | "resolved";

export function ObservationPanel({
  expedienteId,
  operatorId,
  enabled = true,
}: ObservationPanelProps) {
  const { observations, isLoading, createObservation, resolveObservation, dismissObservation, deleteObservation } =
    useObservations(expedienteId, enabled);
  const [activeTab, setActiveTab] = useState<TabValue>("all");
  const [showComposer, setShowComposer] = useState(false);

  const filteredObservations = observations?.filter((obs) => {
    if (activeTab === "all") return true;
    if (activeTab === "pending")
      return obs.status === "PENDING" || obs.status === "ASSIGNED_TO_BOT";
    if (activeTab === "resolved")
      return obs.status === "RESOLVED" || obs.status === "DISMISSED";
    if (activeTab === "errors") return obs.severity === "ERROR";
    return true;
  });

  const stats = {
    total: observations?.length || 0,
    pending:
      observations?.filter(
        (o) => o.status === "PENDING" || o.status === "ASSIGNED_TO_BOT"
      ).length || 0,
    errors:
      observations?.filter(
        (o) => o.severity === "ERROR" && o.status !== "RESOLVED"
      ).length || 0,
    withBot: observations?.filter((o) => o.status === "ASSIGNED_TO_BOT").length || 0,
  };

  const handleResolve = async (observationId: string) => {
    const notes = prompt("Notas de resolución (opcional):");
    if (notes === null) return;
    const input: ResolveObservationInput = { notes: notes || "" };
    await resolveObservation(observationId, input);
  };

  const tabs: { value: TabValue; label: string }[] = [
    { value: "all", label: "Todas" },
    { value: "pending", label: "Pendientes" },
    { value: "errors", label: "Errores" },
    { value: "resolved", label: "Resueltas" },
  ];

  return (
    <div className="flex flex-col h-full">
      {/* Header Stats */}
      <div className="grid grid-cols-4 gap-3 mb-4">
        <StatCard
          icon={WarningCircle}
          label="Errores"
          value={stats.errors}
          color="text-rose-400"
          bg="bg-rose-500/10"
        />
        <StatCard
          icon={Clock}
          label="Pendientes"
          value={stats.pending}
          color="text-amber-400"
          bg="bg-amber-500/10"
        />
        <StatCard
          icon={Robot}
          label="Con LawraBot"
          value={stats.withBot}
          color="text-accent"
          bg="bg-accent/10"
        />
        <StatCard
          icon={CheckCircle}
          label="Total"
          value={stats.total}
          color="text-zinc-400"
          bg="bg-white/5"
        />
      </div>

      {/* Tabs */}
      <div className="flex gap-1 mb-4 p-1 rounded-2xl bg-white/5 border border-white/5">
        {tabs.map((tab) => (
          <button
            key={tab.value}
            onClick={() => setActiveTab(tab.value)}
            className={cn(
              "flex-1 py-2 px-3 rounded-xl text-xs font-medium transition-all",
              activeTab === tab.value
                ? "bg-white/10 text-white shadow-sm"
                : "text-zinc-500 hover:text-zinc-300"
            )}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Observations List & Composer Area */}
      <div className="flex-1 overflow-y-auto space-y-3 pr-2 custom-scrollbar">
        {isLoading ? (
          <LoadingState />
        ) : filteredObservations?.length === 0 && !showComposer ? (
          <EmptyState onCreate={() => setShowComposer(true)} />
        ) : (
          <>
            {filteredObservations?.map((obs) => (
              <ObservationCard
                key={obs.id}
                observation={obs}
                onResolve={() => handleResolve(obs.id)}
                onDismiss={() => dismissObservation(obs.id)}
                onDelete={() => deleteObservation(obs.id)}
              />
            ))}
          </>
        )}

        {/* Composer inside scrollable area */}
        {showComposer && (
          <div className="mt-4 pb-6">
            <ObservationComposer
              expedienteId={expedienteId}
              operatorId={operatorId}
              onSubmit={createObservation}
              onSuccess={() => setShowComposer(false)}
              onCancel={() => setShowComposer(false)}
            />
          </div>
        )}
      </div>

      {/* Create Button (now toggles only) */}
      {!showComposer && (
        <button
          onClick={() => setShowComposer(true)}
          className="mt-4 w-full py-3 rounded-xl border border-dashed border-white/20 text-zinc-400 hover:text-white hover:border-white/40 transition-all flex items-center justify-center gap-2"
        >
          <Plus size={18} />
          Nueva Observación
        </button>
      )}
    </div>
  );
}