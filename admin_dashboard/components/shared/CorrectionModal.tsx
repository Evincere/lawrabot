"use client";

import { useState } from "react";
import { 
  X, 
  CheckCircle, 
  WarningCircle, 
  MagicWand, 
  FloppyDisk,
  Warning
} from "@phosphor-icons/react";
import { motion, AnimatePresence } from "framer-motion";
import { cn } from "@/lib/utils";

interface CorrectionModalProps {
  isOpen: boolean;
  onClose: () => void;
  field: string;
  originalText?: string;
  aiValue: string;
  citizenId?: string;
  caseId?: string;
}

export function CorrectionModal({ 
  isOpen, 
  onClose, 
  field, 
  originalText, 
  aiValue, 
  citizenId, 
  caseId 
}: CorrectionModalProps) {
  const [humanValue, setHumanValue] = useState(aiValue);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    
    try {
      const response = await fetch("http://localhost:8081/api/mci/correction", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          fieldName: field,
          originalText,
          aiValue,
          humanValue,
          citizenId,
          caseId
        })
      });

      if (!response.ok) throw new Error("Error al registrar corrección");
      
      setIsSuccess(true);
      setTimeout(() => {
        setIsSuccess(false);
        onClose();
      }, 2000);
    } catch (error) {
      console.error("Error submitting correction:", error);
      alert("Error al conectar con el servidor de operaciones.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          {/* Backdrop */}
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="fixed inset-0 bg-black/60 backdrop-blur-md z-[100] cursor-pointer"
          />

          {/* Modal Container - Double Bezel Architecture */}
          <motion.div 
            initial={{ scale: 0.95, opacity: 0, y: 20 }}
            animate={{ scale: 1, opacity: 1, y: 0 }}
            exit={{ scale: 0.95, opacity: 0, y: 20 }}
            className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-full max-w-xl z-[101] p-1.5 rounded-[2.5rem] bg-white/5 border border-white/10 shadow-2xl"
          >
            <div className="bg-zinc-950 rounded-[calc(2.5rem-0.375rem)] p-8 overflow-hidden relative">
              
              {/* Header */}
              <div className="flex items-center justify-between mb-8">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-2xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center">
                    <MagicWand size={24} weight="bold" className="text-amber-500" />
                  </div>
                  <div>
                    <h3 className="text-xl font-bold tracking-tight text-white italic">Learning Loop</h3>
                    <p className="text-xs text-zinc-500 font-medium tracking-wide uppercase">Corrección de Extracción</p>
                  </div>
                </div>
                <button 
                  onClick={onClose}
                  className="p-2 rounded-xl hover:bg-white/5 text-zinc-500 transition-colors"
                >
                  <X size={20} weight="bold" />
                </button>
              </div>

              {/* Success State Overlay */}
              <AnimatePresence>
                {isSuccess && (
                  <motion.div 
                    initial={{ opacity: 0, scale: 0.9 }}
                    animate={{ opacity: 1, scale: 1 }}
                    className="absolute inset-0 z-10 bg-zinc-950 flex flex-col items-center justify-center text-center p-8"
                  >
                    <div className="w-16 h-16 rounded-full bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center mb-4">
                      <CheckCircle size={40} weight="fill" className="text-emerald-500" />
                    </div>
                    <h4 className="text-xl font-bold text-white mb-2">Corrección Registrada</h4>
                    <p className="text-sm text-zinc-500">Gracias. Esta información se usará para mejorar la precisión del modelo en futuras intervenciones.</p>
                  </motion.div>
                )}
              </AnimatePresence>

              <form onSubmit={handleSubmit} className="flex flex-col gap-6">
                
                {/* Field Header */}
                <div className="p-4 rounded-3xl bg-white/[0.02] border border-white/5">
                   <span className="text-[10px] uppercase tracking-widest text-zinc-500 font-bold mb-1 block">Campo en Revisión</span>
                   <p className="text-sm font-bold text-accent">{field}</p>
                </div>

                {/* Values Split */}
                <div className="grid grid-cols-2 gap-4">
                  <div className="flex flex-col gap-2">
                    <span className="text-[10px] uppercase tracking-widest text-zinc-500 font-bold flex items-center gap-1">
                      <WarningCircle size={12} /> AI / Agent
                    </span>
                    <div className="p-4 rounded-2xl bg-red-500/5 border border-red-500/10 text-xs font-mono text-red-200/50 line-through">
                      {aiValue}
                    </div>
                  </div>
                  <div className="flex flex-col gap-2">
                    <span className="text-[10px] uppercase tracking-widest text-zinc-500 font-bold flex items-center gap-1">
                      <CheckCircle size={12} className="text-emerald-500" /> Humano (Correcto)
                    </span>
                    <input 
                      type="text"
                      value={humanValue}
                      onChange={(e) => setHumanValue(e.target.value)}
                      className="p-4 rounded-2xl bg-emerald-500/5 border border-emerald-500/20 text-xs font-bold text-white focus:outline-none focus:ring-2 focus:ring-emerald-500/50 transition-all shadow-lg shadow-emerald-500/10"
                    />
                  </div>
                </div>

                {/* Original Context (Scrollable) */}
                {originalText && (
                  <div className="flex flex-col gap-2">
                     <span className="text-[10px] uppercase tracking-widest text-zinc-500 font-bold">Contexto Original (Transcripción)</span>
                     <div className="p-4 rounded-3xl bg-white/[0.05] border border-white/5 max-h-32 overflow-y-auto text-[11px] leading-relaxed text-zinc-400 italic font-medium scrollbar-hide">
                        "{originalText}"
                     </div>
                  </div>
                )}

                {/* Advisory Notice */}
                <div className="flex items-start gap-4 p-4 rounded-3xl bg-amber-500/5 border border-amber-500/10">
                   <Warning size={32} weight="duotone" className="text-amber-500 shrink-0" />
                   <p className="text-[10px] text-amber-200/60 leading-normal">
                      **Importante:** Al confirmar, el sistema registrará la diferencia para re-entrenar los prompts del agente. Evite subir información sensible (PII) si no es estrictamente necesario para la corrección táctica.
                   </p>
                </div>

                {/* Footer Actions */}
                <div className="flex items-center gap-4 mt-2">
                  <button 
                    type="button" 
                    onClick={onClose}
                    className="flex-1 py-4 rounded-2xl bg-white/5 border border-white/10 text-zinc-400 font-bold text-sm hover:bg-white/10 transition-all"
                  >
                    Descartar
                  </button>
                  <button 
                    type="submit" 
                    disabled={isSubmitting}
                    className="flex-[2] py-4 rounded-2xl bg-accent text-white font-bold text-sm shadow-lg shadow-accent/20 hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center justify-center gap-2 group disabled:opacity-50"
                  >
                    {isSubmitting ? (
                      <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    ) : (
                      <>
                        <FloppyDisk size={20} weight="bold" />
                        Validar y Entrenar
                      </>
                    )}
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
