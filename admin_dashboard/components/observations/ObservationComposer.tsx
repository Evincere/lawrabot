"use client";

import { useState, useEffect } from "react";
import type {
  CreateObservationInput,
  ObservationSeverity,
  TaskType,
} from "@/lib/types/observation";
import { cn } from "@/lib/utils";
import {
  WarningCircle,
  Warning,
  Info,
  PaperPlaneRight,
  Robot,
  Plus,
} from "@phosphor-icons/react";

interface ObservationComposerProps {
  expedienteId: string;
  operatorId?: string;
  onSubmit: (input: CreateObservationInput) => Promise<unknown>;
  onSuccess?: () => void;
  onCancel?: () => void;
}

const severityOptions: {
  value: ObservationSeverity;
  label: string;
  icon: typeof WarningCircle;
  color: string;
  bg: string;
  border: string;
}[] = [
  {
    value: "ERROR",
    label: "Error",
    icon: WarningCircle,
    color: "text-rose-400",
    bg: "bg-rose-500/10",
    border: "border-rose-500/20",
  },
  {
    value: "WARNING",
    label: "Advertencia",
    icon: Warning,
    color: "text-amber-400",
    bg: "bg-amber-500/10",
    border: "border-amber-500/20",
  },
  {
    value: "INFO",
    label: "Info",
    icon: Info,
    color: "text-blue-400",
    bg: "bg-blue-500/10",
    border: "border-blue-500/20",
  },
];

const taskTypeOptions: { value: TaskType; label: string; description: string }[] =
  [
    {
      value: "CLARIFY_DATA",
      label: "Aclarar dato",
      description: "Solicitar aclaración sobre información confusa",
    },
    {
      value: "CORRECT_ERROR",
      label: "Corregir error",
      description: "Notificar error con sugerencia de corrección",
    },
    {
      value: "REQUEST_DOCUMENT",
      label: "Solicitar documento",
      description: "Pedir envío de documentación",
    },
    {
      value: "NOTIFY_APPOINTMENT",
      label: "Notificar cita",
      description: "Convocar a firma de documentos",
    },
  ];

const availableFields = [
  { name: "petitioner.name", label: "Nombre del Peticionante" },
  { name: "petitioner.dni", label: "DNI del Peticionante" },
  { name: "petitioner.address", label: "Domicilio del Peticionante" },
  { name: "respondent.name", label: "Nombre del Demandado" },
  { name: "respondent.dni", label: "DNI del Demandado" },
  { name: "respondent.address", label: "Domicilio del Demandado" },
  { name: "marriage.date", label: "Fecha de Matrimonio" },
  { name: "marriage.separationDate", label: "Fecha de Separación" },
  { name: "agreement.alimonyAmount", label: "Cuota Alimentaria" },
  { name: "agreement.personalCare", label: "Cuidado Personal" },
  { name: "agreement.communication", label: "Régimen Comunicacional" },
  { name: "children.info", label: "Datos de Hijos" },
  { name: "documents.missing", label: "Documentación Faltante" },
];

export function ObservationComposer({
  expedienteId,
  operatorId,
  onSubmit,
  onSuccess,
  onCancel,
}: ObservationComposerProps) {
  const [fieldName, setFieldName] = useState("");
  const [severity, setSeverity] = useState<ObservationSeverity>("WARNING");
  const [message, setMessage] = useState("");
  const [suggestedValue, setSuggestedValue] = useState("");
  const [createTask, setCreateTask] = useState(true);
  const [taskType, setTaskType] = useState<TaskType>("CORRECT_ERROR");
  const [isImmediate, setIsImmediate] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  // Auto-select task type based on field
  useEffect(() => {
    if (fieldName === "documents.missing") {
      setTaskType("REQUEST_DOCUMENT");
    } else if (fieldName.includes("amount") || fieldName.includes("dni") || fieldName.includes("date")) {
      setTaskType("CORRECT_ERROR");
    }
  }, [fieldName]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!fieldName || !message) return;

    setIsSubmitting(true);
    try {
      await onSubmit({
        expedienteId,
        fieldName,
        severity,
        message,
        suggestedValue: suggestedValue || undefined,
        createTask,
        taskType: createTask ? taskType : undefined,
        isImmediate: createTask ? isImmediate : false,
        operatorId,
      });

      // Reset form
      setFieldName("");
      setSeverity("WARNING");
      setMessage("");
      setSuggestedValue("");
      setCreateTask(true);
      setTaskType("CORRECT_ERROR");
      setIsImmediate(false);
      onSuccess?.();
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="p-6 rounded-3xl bg-zinc-950/50 border border-white/10"
    >
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-amber-500/10 flex items-center justify-center">
            <Warning size={24} className="text-amber-400" />
          </div>
          <div>
            <h3 className="text-lg font-bold text-white">Nueva Observación</h3>
            <p className="text-xs text-zinc-500">
              Crear observación y asignar tarea a LawraBot
            </p>
          </div>
        </div>
        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            className="p-2 rounded-xl border border-white/10 text-zinc-500 hover:text-white hover:bg-white/5 transition-all"
          >
            <Plus size={20} className="rotate-45" />
          </button>
        )}
      </div>

      <div className="space-y-4">
        {/* Field Selection */}
        <div>
          <label className="text-[10px] uppercase tracking-widest text-zinc-500 font-bold block mb-2">
            Campo Afectado
          </label>
          <select
            value={fieldName}
            onChange={(e) => setFieldName(e.target.value)}
            className="w-full bg-zinc-900 border border-white/10 rounded-xl px-4 py-3 text-sm text-white focus:outline-none focus:border-accent/50"
            required
          >
            <option value="">Seleccionar campo...</option>
            {availableFields.map((field) => (
              <option key={field.name} value={field.name}>
                {field.label}
              </option>
            ))}
          </select>
        </div>

        {/* Severity */}
        <div>
          <label className="text-[10px] uppercase tracking-widest text-zinc-500 font-bold block mb-2">
            Severidad
          </label>
          <div className="flex gap-2">
            {severityOptions.map((opt) => {
              const Icon = opt.icon;
              return (
                <button
                  key={opt.value}
                  type="button"
                  onClick={() => setSeverity(opt.value)}
                  className={cn(
                    "flex-1 flex items-center justify-center gap-2 px-4 py-3 rounded-xl border transition-all",
                    severity === opt.value
                      ? cn(opt.bg, opt.border, opt.color)
                      : "border-white/10 text-zinc-500 hover:border-white/20"
                  )}
                >
                  <Icon
                    size={18}
                    weight={severity === opt.value ? "fill" : "regular"}
                  />
                  <span className="text-sm font-medium">{opt.label}</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Message */}
        <div>
          <label className="text-[10px] uppercase tracking-widest text-zinc-500 font-bold block mb-2">
            Descripción del Problema
          </label>
          <textarea
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            placeholder="Ej: El valor ingresado parece incorrecto..."
            className="w-full bg-zinc-900 border border-white/10 rounded-xl px-4 py-3 text-sm text-white focus:outline-none focus:border-accent/50 resize-none h-24"
            required
          />
        </div>

        {/* Suggested Value (Conditional) */}
        {taskType === "CORRECT_ERROR" && (
          <div>
            <label className="text-[10px] uppercase tracking-widest text-zinc-500 font-bold block mb-2">
              Valor Sugerido (opcional)
            </label>
            <input
              type="text"
              value={suggestedValue}
              onChange={(e) => setSuggestedValue(e.target.value)}
              placeholder="Ej: 50000"
              className="w-full bg-zinc-900 border border-white/10 rounded-xl px-4 py-3 text-sm text-white focus:outline-none focus:border-accent/50"
            />
          </div>
        )}

        {/* Create Task Toggle */}
        <div className="space-y-3">
          <div className="flex items-center gap-3 p-4 rounded-xl bg-zinc-900/50 border border-white/5">
            <input
              type="checkbox"
              id="createTask"
              checked={createTask}
              onChange={(e) => setCreateTask(e.target.checked)}
              className="w-5 h-5 rounded border-white/20 accent-accent"
            />
            <label
              htmlFor="createTask"
              className="flex items-center gap-2 cursor-pointer"
            >
              <Robot size={18} className="text-accent" />
              <span className="text-sm text-zinc-300">
                Crear tarea y asignar a LawraBot
              </span>
            </label>
          </div>

          {createTask && (
            <div className="flex items-center gap-3 p-4 rounded-xl bg-violet-500/5 border border-violet-500/10 ml-6">
              <input
                type="checkbox"
                id="isImmediate"
                checked={isImmediate}
                onChange={(e) => setIsImmediate(e.target.checked)}
                className="w-5 h-5 rounded border-violet-500/20 accent-violet-500"
              />
              <label
                htmlFor="isImmediate"
                className="flex flex-col cursor-pointer"
              >
                <span className="text-sm font-bold text-violet-400">
                  Ejecución Inmediata
                </span>
                <span className="text-[10px] text-zinc-500">
                  El bot contactará al ciudadano proactivamente ahora mismo.
                </span>
              </label>
            </div>
          )}
        </div>

        {/* Task Type (conditional) */}
        {createTask && (
          <div>
            <label className="text-[10px] uppercase tracking-widest text-zinc-500 font-bold block mb-2">
              Tipo de Tarea
            </label>
            <div className="grid grid-cols-2 gap-2">
              {taskTypeOptions.map((opt) => (
                <button
                  key={opt.value}
                  type="button"
                  onClick={() => setTaskType(opt.value)}
                  className={cn(
                    "p-3 rounded-xl border text-left transition-all",
                    taskType === opt.value
                      ? "border-accent bg-accent/10"
                      : "border-white/10 hover:border-white/20"
                  )}
                >
                  <span
                    className={cn(
                      "text-sm font-medium block",
                      taskType === opt.value ? "text-accent" : "text-zinc-300"
                    )}
                  >
                    {opt.label}
                  </span>
                  <span className="text-xs text-zinc-500 block mt-1">
                    {opt.description}
                  </span>
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Submit */}
        <button
          type="submit"
          disabled={isSubmitting || !fieldName || !message}
          className="w-full flex items-center justify-center gap-2 py-4 rounded-xl bg-accent text-white font-bold hover:bg-accent/90 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
        >
          {isSubmitting ? (
            <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
          ) : (
            <>
              <PaperPlaneRight size={20} weight="bold" />
              Crear Observación
            </>
          )}
        </button>
      </div>
    </form>
  );
}