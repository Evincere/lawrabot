"use client";

import type { Observation } from "@/lib/types/observation";
import { cn } from "@/lib/utils";
import {
  WarningCircle,
  Warning,
  Info,
  CheckCircle,
  Clock,
  Robot,
  User,
  Trash,
} from "@phosphor-icons/react";

interface ObservationCardProps {
  observation: Observation;
  onResolve?: () => void;
  onDismiss?: () => void;
  onDelete?: () => void;
}

const severityConfig = {
  ERROR: {
    icon: WarningCircle,
    color: "text-rose-400",
    bg: "bg-rose-500/10",
    border: "border-rose-500/20",
    label: "Error",
  },
  WARNING: {
    icon: Warning,
    color: "text-amber-400",
    bg: "bg-amber-500/10",
    border: "border-amber-500/20",
    label: "Advertencia",
  },
  INFO: {
    icon: Info,
    color: "text-blue-400",
    bg: "bg-blue-500/10",
    border: "border-blue-500/20",
    label: "Info",
  },
};

const statusConfig = {
  PENDING: { icon: Clock, label: "Pendiente", color: "text-zinc-400" },
  ASSIGNED_TO_BOT: { icon: Robot, label: "En LawraBot", color: "text-accent" },
  RESOLVED: { icon: CheckCircle, label: "Resuelto", color: "text-emerald-400" },
  DISMISSED: { icon: User, label: "Descartado", color: "text-zinc-500" },
};

function formatFieldName(fieldName: string): string {
  const labels: Record<string, string> = {
    "petitioner.name": "Nombre del Peticionante",
    "petitioner.dni": "DNI del Peticionante",
    "petitioner.address": "Domicilio del Peticionante",
    "respondent.name": "Nombre del Demandado",
    "respondent.dni": "DNI del Demandado",
    "respondent.address": "Domicilio del Demandado",
    "marriage.date": "Fecha de Matrimonio",
    "marriage.separationDate": "Fecha de Separación",
    "agreement.alimonyAmount": "Cuota Alimentaria",
    "agreement.personalCare": "Cuidado Personal",
    "agreement.communication": "Régimen Comunicacional",
    "children.info": "Datos de Hijos",
    "documents.missing": "Documentación Faltante",
  };
  return labels[fieldName] || fieldName;
}

function timeAgo(dateStr: string): string {
  const now = new Date();
  const date = new Date(dateStr);
  const diffMs = now.getTime() - date.getTime();
  const diffMin = Math.floor(diffMs / 60000);
  if (diffMin < 1) return "ahora";
  if (diffMin < 60) return `hace ${diffMin}m`;
  const diffHrs = Math.floor(diffMin / 60);
  if (diffHrs < 24) return `hace ${diffHrs}h`;
  const diffDays = Math.floor(diffHrs / 24);
  return `hace ${diffDays}d`;
}

export function ObservationCard({
  observation,
  onResolve,
  onDismiss,
  onDelete,
}: ObservationCardProps) {
  const severity = severityConfig[observation.severity];
  const status = statusConfig[observation.status];
  const SeverityIcon = severity.icon;
  const StatusIcon = status.icon;

  return (
    <div
      className={cn(
        "p-4 rounded-2xl border transition-all group",
        severity.bg,
        severity.border,
        observation.status === "RESOLVED" && "opacity-60",
        observation.status === "DISMISSED" && "opacity-40"
      )}
    >
      {/* Header */}
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center gap-2">
          <div className={cn("p-1.5 rounded-lg", severity.bg)}>
            <SeverityIcon
              size={16}
              className={severity.color}
              weight="fill"
            />
          </div>
          <span
            className={cn(
              "text-[10px] font-bold uppercase tracking-wider",
              severity.color
            )}
          >
            {severity.label}
          </span>
        </div>
        <div className="flex items-center gap-1.5">
          <StatusIcon size={14} className={status.color} />
          <span className={cn("text-xs font-medium", status.color)}>
            {status.label}
          </span>
        </div>
      </div>

      {/* Field Name */}
      <div className="mb-2">
        <span className="text-[10px] uppercase tracking-widest text-zinc-500 font-bold">
          Campo: {formatFieldName(observation.fieldName)}
        </span>
      </div>

      {/* Message */}
      <p className="text-sm text-zinc-200 mb-3 leading-relaxed">
        {observation.message}
      </p>

      {/* Suggested Value */}
      {observation.suggestedValue && (
        <div className="p-2.5 rounded-xl bg-white/5 border border-white/10 mb-3">
          <span className="text-[10px] uppercase tracking-widest text-zinc-500 font-bold block mb-1">
            Valor Sugerido
          </span>
          <span className="text-sm font-mono text-emerald-300">
            {observation.suggestedValue}
          </span>
        </div>
      )}

      {/* Task Info */}
      {observation.task && observation.status === "ASSIGNED_TO_BOT" && (
        <div className="flex items-center gap-2 p-2.5 rounded-xl bg-accent/5 border border-accent/10 mb-3">
          <Robot size={16} className="text-accent" />
          <span className="text-xs text-accent">
            Tarea enviada a LawraBot &bull; Esperando respuesta del ciudadano
          </span>
        </div>
      )}

      {/* Footer */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4 text-zinc-500">
          <div className="flex items-center gap-2">
            <User size={12} />
            <span className="text-xs">{timeAgo(observation.createdAt)}</span>
          </div>
          {onDelete && (
            <button
              onClick={onDelete}
              className="p-1.5 rounded bg-rose-500/10 text-rose-400 hover:bg-rose-500/20 transition-all opacity-0 group-hover:opacity-100"
              title="Eliminar observación"
            >
              <Trash size={14} weight="bold" />
            </button>
          )}
        </div>

        {/* Actions */}
        {observation.status === "PENDING" && (
          <div className="flex items-center gap-2">
            {onDismiss && (
              <button
                onClick={onDismiss}
                className="px-3 py-1.5 rounded-lg text-xs font-medium text-zinc-400 hover:text-white hover:bg-white/5 transition-all"
              >
                Descartar
              </button>
            )}
            {onResolve && (
              <button
                onClick={onResolve}
                className="px-3 py-1.5 rounded-lg text-xs font-medium bg-emerald-500/10 text-emerald-400 hover:bg-emerald-500/20 transition-all"
              >
                Resolver
              </button>
            )}
          </div>
        )}

        {observation.status === "ASSIGNED_TO_BOT" && onResolve && (
          <button
            onClick={onResolve}
            className="px-3 py-1.5 rounded-lg text-xs font-medium bg-accent/10 text-accent hover:bg-accent/20 transition-all"
          >
            Marcar Resuelto
          </button>
        )}
      </div>

      {/* Resolution Notes */}
      {observation.resolutionNotes && (
        <div className="mt-3 pt-3 border-t border-white/5">
          <span className="text-[10px] uppercase tracking-widest text-zinc-500 font-bold block mb-1">
            Resolución
          </span>
          <p className="text-xs text-zinc-400">
            {observation.resolutionNotes}
          </p>
        </div>
      )}
    </div>
  );
}