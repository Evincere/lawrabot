"use client";

import { Expediente } from "@/lib/types/divorce";
import { 
  FileText, 
  Quotes,
  Calendar,
  MapPin,
  PencilSimple,
  Check,
  X 
} from "@phosphor-icons/react";
import { cn } from "@/lib/utils";

interface AgreementTabProps {
  expediente: Expediente;
  isEditing: boolean;
  setIsEditing: (val: boolean) => void;
  draftText: string;
  setDraftText: (val: string) => void;
  isSaving: boolean;
  onSave: () => void;
}

export function AgreementTab({
  expediente,
  isEditing,
  setIsEditing,
  draftText,
  setDraftText,
  isSaving,
  onSave
}: AgreementTabProps) {
  return (
    <div className="space-y-10">
      {/* 1. Hitos Temporales & Datos Core */}
      <section>
        <div className="flex items-center gap-3 mb-6">
           <div className="p-2 rounded-xl bg-danger/10 border border-danger/20">
             <Calendar size={20} weight="duotone" className="text-danger" />
           </div>
           <h5 className="text-[10px] uppercase tracking-[0.2em] text-zinc-500 font-black">Hitos Temporales y Encuadre</h5>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
           <div className="p-6 rounded-2xl bg-surface border border-border">
              <span className="text-[9px] font-black text-zinc-500 uppercase tracking-widest">Matrimonio</span>
              <p className="text-base font-bold tracking-tight text-white mt-1 legal-text">{expediente.marriageDate || "S/D"}</p>
           </div>
           <div className="p-6 rounded-2xl bg-surface border border-border">
              <span className="text-[9px] font-black text-zinc-500 uppercase tracking-widest">Separación de Hecho</span>
              <p className="text-base font-bold tracking-tight text-white mt-1 legal-text">{expediente.deFactoSeparationDate || "No declarada"}</p>
           </div>
           <div className="p-6 rounded-2xl bg-surface border border-border">
              <span className="text-[9px] font-black text-zinc-500 uppercase tracking-widest">Tipo de Divorcio</span>
              <p className="text-base font-bold tracking-tight text-white mt-1 uppercase tracking-tighter">
                {expediente.divorceType || "Libre (Art. 437)"}
              </p>
           </div>
           <div className="p-6 rounded-2xl bg-surface border border-border">
              <span className="text-[9px] font-black text-zinc-500 uppercase tracking-widest">Estado Convenio</span>
              <p className={cn(
                "text-base font-bold tracking-tight mt-1",
                expediente.rawAgreementText ? "text-success" : "text-warning"
              )}>
                {expediente.rawAgreementText ? "Borrador Listado" : "Pendiente de redactar"}
              </p>
           </div>
        </div>

        <div className="mt-4 p-6 rounded-2xl bg-background border border-border flex items-start gap-4">
           <div className="w-10 h-10 rounded-xl bg-surface flex items-center justify-center text-zinc-600 shrink-0 border border-border">
             <MapPin size={24} weight="duotone" />
           </div>
           <div>
              <span className="text-[9px] font-black text-zinc-500 uppercase tracking-widest">Último Domicilio Conyugal (Competencia)</span>
              <p className="text-sm font-bold text-white mt-1">
                {expediente.lastConjugalResidence 
                  ? `${expediente.lastConjugalResidence.street || ''} ${expediente.lastConjugalResidence.number || ''} ${expediente.lastConjugalResidence.locality || ''}` 
                  : "Dato no recolectado"}
              </p>
           </div>
        </div>
      </section>

      {/* 2. Editor del Convenio (Drafting de WhatsApp) */}
      <section>
        <div className="flex items-center justify-between mb-6">
           <div className="flex items-center gap-3">
              <div className="p-2 rounded-xl bg-info/10 border border-info/20">
                <Quotes size={20} weight="duotone" className="text-info" />
              </div>
              <h5 className="text-[10px] uppercase tracking-[0.2em] text-zinc-500 font-black">Convenio Regulador: Redacción Basada en Manifestaciones</h5>
           </div>
           
           {!isEditing ? (
              <button 
                onClick={() => setIsEditing(true)}
                className="flex items-center gap-2 px-4 py-2 rounded-xl bg-accent/10 text-accent border border-accent/20 text-[10px] font-black uppercase tracking-widest hover:bg-accent/20 transition-all"
              >
                <PencilSimple size={14} weight="bold" />
                Corregir Redacción
              </button>
           ) : (
              <div className="flex gap-2">
                <button 
                  onClick={() => setIsEditing(false)}
                  disabled={isSaving}
                  className="flex items-center gap-2 px-3 py-2 rounded-xl text-zinc-500 hover:text-white text-[10px] font-black uppercase tracking-widest transition-all"
                >
                  <X size={14} weight="bold" />
                  Descartar
                </button>
                <button 
                  onClick={onSave}
                  disabled={isSaving}
                  className="flex items-center gap-2 px-6 py-2 rounded-xl bg-success text-white text-[10px] font-black uppercase tracking-widest hover:bg-success/90 transition-all shadow-lg shadow-success/20"
                >
                  {isSaving ? (
                    <div className="w-3 h-3 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  ) : (
                    <Check size={14} weight="bold" />
                  )}
                  {isSaving ? "Guardando..." : "Confirmar Texto"}
                </button>
              </div>
           )}
        </div>

        <div className={cn(
          "rounded-3xl border transition-all duration-500 overflow-hidden bg-surface",
          isEditing ? "border-accent/30 ring-4 ring-accent/5" : "border-border"
        )}>
          {isEditing ? (
            <textarea
              value={draftText}
              onChange={(e) => setDraftText(e.target.value)}
              className="w-full h-80 bg-background/50 p-8 text-sm text-zinc-200 outline-none resize-none font-mono leading-relaxed legal-text"
              placeholder="Escriba o pegue aquí los acuerdos sobre alimentos, cuidado personal y régimen comunicacional redactados a partir de los mensajes de WhatsApp..."
              autoFocus
            />
          ) : (
            <div className="p-8 relative">
              <div className="absolute top-8 left-4 text-zinc-100/5 select-none pointer-events-none">
                 <Quotes size={120} weight="fill" />
              </div>
              <div className="relative z-10">
                 {expediente.rawAgreementText ? (
                   <div className="space-y-4">
                     {expediente.rawAgreementText.split('\n').map((line, i) => (
                       <p key={i} className="text-sm text-zinc-300 font-serif leading-relaxed italic">
                         {line}
                       </p>
                     ))}
                   </div>
                 ) : (
                   <div className="flex flex-col items-center justify-center py-20 text-zinc-600 gap-4">
                      <FileText size={40} weight="thin" className="opacity-20" />
                      <p className="text-xs font-bold uppercase tracking-widest text-center">
                        Contenido no declarado.<br/>El ciudadano no proveyó detalles del convenio durante la recolección automática.
                      </p>
                   </div>
                 )}
              </div>
            </div>
          )}
        </div>
        
        {isEditing && (
          <p className="text-[9px] font-black text-zinc-600 uppercase tracking-widest mt-4 text-center">
            Atención: Estos cambios afectarán directamente a la generación de la demanda final.
          </p>
        )}
      </section>
    </div>
  );
}
