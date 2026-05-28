"use client";

import { useState } from "react";
import { 
  X, 
  WarningCircle, 
  PaperPlaneTilt,
  ChatCenteredText
} from "@phosphor-icons/react";
import { motion, AnimatePresence } from "framer-motion";
import { cn } from "@/lib/utils";

interface ObservationModalProps {
  isOpen: boolean;
  onClose: () => void;
  evidenceName: string;
  evidenceType: string;
  onConfirm: (reason: string) => void;
}

const QUICK_TAGS_MAP: Record<string, string[]> = {
  BIRTH_CERT: [
    "El documento no es legible / está borroso",
    "El documento está cortado o incompleto",
    "No corresponde al hijo seleccionado",
    "No es el acta oficial (ej. captura de pantalla)",
    "Falta sello oficial o firma digital"
  ],
  DISABILITY_CERT: [
    "El certificado de discapacidad está vencido",
    "No corresponde al hijo indicado",
    "El documento no es legible / está borroso",
    "No es el CUD oficial emitido por ANDIS",
    "El código QR o de barras es ilegible"
  ],
  MARRIAGE_CERT: [
    "El documento no es legible / está borroso",
    "El documento está cortado o incompleto",
    "No corresponde a los cónyuges del caso",
    "Falta el folio, tomo o acta registral clara",
    "El documento cargado no es el acta de matrimonio oficial"
  ],
  DEFAULT: [
    "El documento no es legible o está borroso",
    "El documento está incompleto o cortado",
    "No corresponde al caso o persona solicitada",
    "No es un documento oficial válido"
  ]
};

export function ObservationModal({ 
  isOpen, 
  onClose, 
  evidenceName, 
  evidenceType, 
  onConfirm 
}: ObservationModalProps) {
  const [reason, setReason] = useState("");
  const [error, setError] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!reason.trim()) {
      setError(true);
      return;
    }
    onConfirm(reason);
    onClose();
  };

  const getQuickTags = () => {
    return QUICK_TAGS_MAP[evidenceType] || QUICK_TAGS_MAP.DEFAULT;
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          {/* Backdrop blur */}
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="fixed inset-0 bg-black/80 backdrop-blur-md z-[200] cursor-pointer"
          />

          {/* Modal Container */}
          <motion.div 
            initial={{ scale: 0.95, opacity: 0, y: 20 }}
            animate={{ scale: 1, opacity: 1, y: 0 }}
            exit={{ scale: 0.95, opacity: 0, y: 20 }}
            className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-full max-w-xl max-h-[90vh] z-[201] p-1.5 rounded-[2.5rem] bg-white/5 border border-white/10 shadow-2xl flex flex-col"
          >
            <div className="bg-zinc-950 rounded-[calc(2.5rem-0.375rem)] p-6 md:p-8 flex flex-col max-h-[calc(90vh-12px)] overflow-hidden relative">
              
              {/* Header (Static) */}
              <div className="flex items-center justify-between mb-5 shrink-0">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-2xl bg-danger/10 border border-danger/20 flex items-center justify-center">
                    <ChatCenteredText size={24} weight="duotone" className="text-danger" />
                  </div>
                  <div>
                    <h3 className="text-xl font-bold tracking-tight text-white italic">Observar Documento</h3>
                    <p className="text-xs text-zinc-500 font-medium tracking-wide uppercase">Solicitar Corrección al Ciudadano</p>
                  </div>
                </div>
                <button 
                  onClick={onClose}
                  className="p-2 rounded-xl hover:bg-white/5 text-zinc-500 hover:text-white transition-colors"
                >
                  <X size={20} weight="bold" />
                </button>
              </div>

              <form onSubmit={handleSubmit} className="flex flex-col flex-1 min-h-0 overflow-hidden">
                
                {/* Scrollable Content Body */}
                <div className="flex-1 overflow-y-auto pr-1.5 space-y-5 scrollbar-thin scrollbar-thumb-zinc-800 scrollbar-track-transparent">
                  {/* Field Details */}
                  <div className="p-4 rounded-3xl bg-white/[0.02] border border-white/5 flex flex-col gap-1">
                     <span className="text-[10px] uppercase tracking-widest text-zinc-500 font-bold">Documento a Observar</span>
                     <p className="text-sm font-bold text-white truncate">{evidenceName}</p>
                  </div>

                  {/* Quick Tags Section */}
                  <div className="flex flex-col gap-2.5">
                    <span className="text-[10px] uppercase tracking-widest text-zinc-500 font-bold flex items-center gap-1">
                      Selección Rápida
                    </span>
                    <div className="flex flex-wrap gap-2">
                      {getQuickTags().map((tag, idx) => (
                        <button
                          key={idx}
                          type="button"
                          onClick={() => {
                            setReason(tag);
                            setError(false);
                          }}
                          className={cn(
                            "px-3.5 py-2 rounded-xl border text-[11px] font-bold text-left transition-all",
                            reason === tag
                              ? "bg-danger/10 border-danger text-danger shadow-lg shadow-danger/10"
                              : "bg-white/[0.02] border-white/5 text-zinc-400 hover:border-zinc-700 hover:text-zinc-200"
                          )}
                        >
                          {tag}
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Textarea Reason */}
                  <div className="flex flex-col gap-2">
                    <span className="text-[10px] uppercase tracking-widest text-zinc-500 font-bold">
                      Detalle de la Observación (Le llegará al ciudadano)
                    </span>
                    <textarea
                      rows={4}
                      value={reason}
                      onChange={(e) => {
                        setReason(e.target.value);
                        if (e.target.value.trim()) setError(false);
                      }}
                      placeholder="Escriba aquí los detalles de por qué el documento no es válido y qué debe hacer el ciudadano..."
                      className={cn(
                        "w-full bg-white/[0.02] border rounded-2xl p-4 text-xs font-bold text-white focus:outline-none focus:ring-2 transition-all resize-none leading-relaxed",
                        error 
                          ? "border-danger focus:ring-danger/50 shadow-lg shadow-danger/10" 
                          : "border-white/5 focus:border-danger/30 focus:ring-danger/20"
                      )}
                    />
                    {error && (
                      <span className="text-[10px] font-bold text-danger uppercase mt-1 flex items-center gap-1">
                        <WarningCircle size={12} weight="fill" /> Por favor, ingrese un motivo para continuar
                      </span>
                    )}
                  </div>

                  {/* Informative advice */}
                  <div className="flex items-start gap-4 p-4 rounded-3xl bg-danger/5 border border-danger/10">
                     <WarningCircle size={28} weight="duotone" className="text-danger shrink-0 mt-0.5" />
                     <p className="text-[10px] text-zinc-400 leading-normal">
                        <strong>Acción del Sistema:</strong> Lawrabot notificará al ciudadano por WhatsApp informándole que este documento fue rechazado con la explicación provista arriba y le solicitará que envíe un reemplazo.
                     </p>
                  </div>
                </div>

                {/* Footer Buttons (Static, always visible at bottom) */}
                <div className="flex items-center gap-4 mt-5 pt-4 border-t border-white/5 shrink-0">
                  <button 
                    type="button" 
                    onClick={onClose}
                    className="flex-1 py-4 rounded-2xl bg-white/5 border border-white/10 text-zinc-400 font-bold text-sm hover:bg-white/10 transition-all hover:text-white"
                  >
                    Descartar
                  </button>
                  <button 
                    type="submit" 
                    className="flex-[2] py-4 rounded-2xl bg-danger text-white font-bold text-sm shadow-lg shadow-danger/20 hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center justify-center gap-2 group"
                  >
                    <PaperPlaneTilt size={20} weight="bold" />
                    Enviar Observación
                  </button>
                </div>

              </form>

            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
}
