"use client";

import { useState } from "react";
import { EvidenceItem, Expediente } from "@/lib/types/divorce";
import { 
  CloudArrowUp, 
  FilePdf, 
  Image as ImageIcon,
  CheckCircle,
  WarningCircle,
  Plus,
  Trash,
  ArrowsLeftRight,
  FolderSimple
} from "@phosphor-icons/react";
import { motion, AnimatePresence } from "framer-motion";
import { cn } from "@/lib/utils";

interface EvidenceTabProps {
  expediente: Expediente;
  evidences: EvidenceItem[];
  isUploading: boolean;
  selectedDocType: string;
  setSelectedDocType: (type: string) => void;
  onFileUpload: (file: File, type: string) => void;
  onEvidenceStatusUpdate: (id: string, approved: boolean, reason?: string) => void;
  onDeleteEvidence: (id: string) => void;
  onReclassifyEvidence: (id: string, newType: string) => void;
}

const DOC_TYPE_OPTIONS = [
  { value: "DNI_FRONT", label: "DNI Frente" },
  { value: "DNI_BACK", label: "DNI Dorso" },
  { value: "MARRIAGE_CERT", label: "Acta Matrimonio" },
  { value: "BIRTH_CERT", label: "Partida Nacimiento" },
  { value: "PAYSLIP", label: "Recibo Sueldo" },
  { value: "OTHER", label: "Otro" }
];

export function EvidenceTab({ 
  evidences, 
  isUploading, 
  selectedDocType, 
  setSelectedDocType,
  onFileUpload,
  onEvidenceStatusUpdate,
  onDeleteEvidence,
  onReclassifyEvidence
}: EvidenceTabProps) {
  const [showLightbox, setShowLightbox] = useState<string | null>(null);
  const [rejectionReason, setRejectionReason] = useState("");
  const [activeReasonInput, setActiveReasonInput] = useState<string | null>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      onFileUpload(e.target.files[0], selectedDocType);
    }
  };

  // Agrupación de evidencias por tipo
  const groupedEvidences = evidences.reduce((acc, ev) => {
    const type = ev.documentType || "OTHER";
    if (!acc[type]) acc[type] = [];
    acc[type].push(ev);
    return acc;
  }, {} as Record<string, EvidenceItem[]>);

  const getTypeLabel = (type: string) => {
    return DOC_TYPE_OPTIONS.find(opt => opt.value === type)?.label || "Otros";
  };

  return (
    <div className="space-y-10 pb-20">
      {/* Evidence Management */}
      <section>
        <div className="flex items-center justify-between mb-6">
           <div className="flex items-center gap-3">
              <div className="p-2 rounded-xl bg-accent/10 border border-accent/20">
                <FolderSimple size={20} weight="duotone" className="text-accent" />
              </div>
              <h5 className="text-[10px] uppercase tracking-[0.2em] text-zinc-500 font-black">Galería Documental</h5>
           </div>

           <div className="flex items-center gap-4">
              <select 
                value={selectedDocType}
                onChange={(e) => setSelectedDocType(e.target.value)}
                className="bg-surface border border-border rounded-xl px-4 py-2 text-[10px] font-black uppercase tracking-widest text-zinc-400 outline-none focus:border-accent/50 transition-all cursor-pointer"
              >
                {DOC_TYPE_OPTIONS.map(opt => (
                  <option key={opt.value} value={opt.value} className="bg-zinc-900 text-zinc-200">{opt.label}</option>
                ))}
              </select>

              <label className={cn(
                "flex items-center gap-3 px-6 py-3 bg-accent text-white rounded-xl transition-all shadow-xl shadow-accent/20 cursor-pointer hover:scale-105 active:scale-95 group",
                isUploading && "opacity-50 pointer-events-none"
              )}>
                {isUploading ? (
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                ) : (
                  <CloudArrowUp size={20} weight="duotone" className="group-hover:animate-bounce" />
                )}
                <span className="text-[10px] font-black uppercase tracking-[0.15em]">Subir Documento</span>
                <input type="file" className="hidden" onChange={handleFileChange} accept="application/pdf,image/*" />
              </label>
           </div>
        </div>

        {evidences.length === 0 ? (
          <div className="p-20 rounded-3xl bg-surface/30 border-2 border-dashed border-border flex flex-col items-center justify-center text-zinc-600 gap-4">
            <CloudArrowUp size={48} weight="thin" className="opacity-20" />
            <div className="text-center">
              <p className="text-sm font-bold text-zinc-500">Sin documentación cargada</p>
              <p className="text-[10px] uppercase tracking-widest mt-1">Sube archivos o espera el envío del usuario</p>
            </div>
          </div>
        ) : (
          <div className="space-y-12">
            {Object.entries(groupedEvidences).map(([type, evs]) => (
              <div key={type} className="space-y-4">
                <div className="flex items-center gap-4">
                   <h6 className="text-[9px] font-black text-zinc-600 uppercase tracking-[0.2em]">{getTypeLabel(type)}</h6>
                   <div className="flex-1 h-px bg-border/50"></div>
                   <span className="text-[9px] font-black text-zinc-600 px-2 py-0.5 rounded bg-border/30">{evs.length}</span>
                </div>
                
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                  {evs.map((ev) => (
                    <EvidenceCard 
                      key={ev.id} 
                      evidence={ev} 
                      onShow={() => setShowLightbox(ev.id)}
                      onStatusUpdate={onEvidenceStatusUpdate}
                      activeReasonInput={activeReasonInput}
                      setActiveReasonInput={setActiveReasonInput}
                      rejectionReason={rejectionReason}
                      setRejectionReason={setRejectionReason}
                      onDelete={() => onDeleteEvidence(ev.id)}
                      onReclassify={(newType) => onReclassifyEvidence(ev.id, newType)}
                    />
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Lightbox for PDF/Images */}
      <AnimatePresence>
        {showLightbox && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-[100] bg-black/95 backdrop-blur-xl flex items-center justify-center p-4 md:p-8"
            onClick={() => setShowLightbox(null)}
          >
            <button className="absolute top-6 right-6 z-[110] text-white/40 hover:text-white transition-colors p-4 hover:bg-white/5 rounded-full">
              <Plus size={32} className="rotate-45" />
            </button>
            
            <div className="relative w-full h-full max-w-5xl flex flex-col items-center justify-between gap-4" onClick={e => e.stopPropagation()}>
              <div className="w-full flex-1 flex flex-col gap-4 min-h-0">
                 <div className="flex-1 rounded-2xl overflow-hidden bg-white border border-white/10 shadow-2xl">
                   {evidences.find(e => e.id === showLightbox)?.mimeType?.includes('pdf') ? (
                     <iframe 
                       src={`http://localhost:8081/api/divorce/evidence/download/${showLightbox}#toolbar=0`}
                       className="w-full h-full border-none"
                       title="Previsualización de Documento"
                     />
                   ) : (
                     <div className="w-full h-full flex items-center justify-center bg-zinc-900">
                        {/* eslint-disable-next-line @next/next/no-img-element */}
                        <img 
                          src={`http://localhost:8081/api/divorce/evidence/download/${showLightbox}`}
                          className="max-w-full max-h-full object-contain"
                          alt="Evidencia"
                        />
                     </div>
                   )}
                 </div>
                 <div className="text-center">
                   <a 
                     href={`http://localhost:8081/api/divorce/evidence/download/${showLightbox}`}
                     target="_blank"
                     className="inline-flex px-8 py-3 bg-accent text-white rounded-xl font-black text-[11px] uppercase tracking-[0.2em] shadow-xl shadow-accent/20 hover:scale-105 transition-all"
                   >
                     Abrir Original / Descargar
                   </a>
                 </div>
              </div>
              
              <div className="text-center pb-4">
                <p className="text-lg font-black text-white uppercase tracking-tight">{evidences.find(e => e.id === showLightbox)?.fileName}</p>
                <div className="flex items-center justify-center gap-3 mt-2">
                  <span className="px-3 py-1 bg-white/5 rounded-lg border border-white/10 text-[10px] font-black text-zinc-400 uppercase tracking-widest">
                    {evidences.find(e => e.id === showLightbox)?.documentType}
                  </span>
                  <span className="px-3 py-1 bg-white/5 rounded-lg border border-white/10 text-[10px] font-black text-zinc-400 uppercase tracking-widest">
                    {new Date(evidences.find(e => e.id === showLightbox)?.createdAt || "").toLocaleDateString()}
                  </span>
                </div>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

function EvidenceCard({ 
  evidence, 
  onShow, 
  onStatusUpdate,
  activeReasonInput,
  setActiveReasonInput,
  rejectionReason,
  setRejectionReason,
  onDelete,
  onReclassify
}: { 
  evidence: EvidenceItem, 
  onShow: () => void,
  onStatusUpdate: (id: string, approved: boolean, reason?: string) => void,
  activeReasonInput: string | null,
  setActiveReasonInput: (id: string | null) => void,
  rejectionReason: string,
  setRejectionReason: (r: string) => void,
  onDelete: () => void,
  onReclassify: (type: string) => void
}) {
  const isPdf = evidence.mimeType?.includes('pdf');

  return (
    <div className="group relative bg-surface border border-border rounded-2xl overflow-hidden hover:border-accent/40 transition-all">
      {/* Badge Estado */}
      <div className={cn(
        "absolute top-3 left-3 z-10 px-2 py-1 rounded-lg text-[8px] font-black uppercase tracking-widest border",
        evidence.approved ? "bg-success/10 text-success border-success/20" :
        evidence.rejectionReason ? "bg-danger/10 text-danger border-danger/20" : "bg-warning/10 text-warning border-warning/20 shadow-lg"
      )}>
        {evidence.approved ? "Validado" : evidence.rejectionReason ? "Impugnado" : "Pendiente"}
      </div>

      {/* Preview Area */}
      <div 
        onClick={onShow}
        className="aspect-[4/3] bg-background flex items-center justify-center cursor-pointer group-hover:scale-[1.02] transition-transform relative"
      >
        {isPdf ? (
          <div className="flex flex-col items-center gap-3 text-zinc-600 group-hover:text-danger/60 transition-colors">
            <div className="p-4 rounded-2xl bg-white/5 ring-1 ring-white/10">
              <FilePdf size={40} weight="duotone" />
            </div>
            <span className="text-[9px] font-black tracking-widest uppercase">Documento PDF</span>
          </div>
        ) : (
          <div className="w-full h-full relative">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img 
              src={`http://localhost:8081/api/divorce/evidence/download/${evidence.id}`}
              className="w-full h-full object-cover opacity-60 group-hover:opacity-100 transition-opacity"
              alt={evidence.fileName}
            />
            <div className="absolute inset-0 flex items-center justify-center">
               <ImageIcon size={32} weight="duotone" className="text-white/20 group-hover:text-white transition-all scale-150" />
            </div>
          </div>
        )}
        
        {/* Hover Overlay */}
        <div className="absolute inset-0 bg-accent/20 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
           <div className="bg-white text-accent px-4 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest shadow-2xl">Visualizar</div>
        </div>
      </div>

      {/* Content */}
      <div className="p-5 space-y-4">
        <div className="flex items-start justify-between">
           <div className="min-w-0">
             <p className="text-[10px] font-bold text-white truncate w-full" title={evidence.fileName}>
               {evidence.fileName}
             </p>
             <p className="text-[9px] text-zinc-500 uppercase font-black tracking-widest mt-1">
               {new Date(evidence.createdAt).toLocaleDateString()}
             </p>
           </div>
           
           <div className="flex gap-2">
              <button 
                onClick={onDelete}
                className="p-2 rounded-lg bg-danger/5 border border-danger/10 text-danger hover:bg-danger hover:text-white transition-all"
                title="Eliminar"
              >
                <Trash size={14} weight="bold" />
              </button>
           </div>
        </div>

        {/* Acciones de Validación */}
        <div className="flex flex-col gap-2">
          {activeReasonInput === evidence.id ? (
            <div className="space-y-2 animate-in slide-in-from-top-1">
               <input 
                 autoFocus
                 className="w-full bg-background border border-danger/30 rounded-xl px-3 py-2 text-[10px] text-white focus:outline-none focus:border-danger transition-all"
                 placeholder="Motivo de impugnación..."
                 value={rejectionReason}
                 onChange={(e) => setRejectionReason(e.target.value)}
                 onKeyDown={(e) => {
                   if (e.key === 'Enter') {
                     onStatusUpdate(evidence.id, false, rejectionReason);
                     setActiveReasonInput(null);
                     setRejectionReason("");
                   }
                   if (e.key === 'Escape') setActiveReasonInput(null);
                 }}
               />
               <div className="flex gap-2">
                 <button 
                   onClick={() => {
                     onStatusUpdate(evidence.id, false, rejectionReason);
                     setActiveReasonInput(null);
                     setRejectionReason("");
                   }}
                   className="flex-1 py-1 px-2 rounded-lg bg-danger text-white text-[9px] font-black uppercase tracking-widest"
                 >Confirmar</button>
                 <button 
                   onClick={() => setActiveReasonInput(null)}
                   className="py-1 px-2 rounded-lg bg-zinc-800 text-zinc-400 text-[9px] font-black uppercase tracking-widest border border-border"
                 >Cancelar</button>
               </div>
            </div>
          ) : !evidence.approved && !evidence.rejectionReason ? (
            <div className="flex gap-2">
              <button 
                onClick={() => onStatusUpdate(evidence.id, true)}
                className="flex-1 h-9 flex items-center justify-center gap-2 rounded-xl transition-all border font-black text-[9px] uppercase tracking-widest bg-success/5 text-success border-success/20 hover:bg-success hover:text-white"
              >
                <CheckCircle size={16} weight="fill" />
                Validar
              </button>
              <button 
                onClick={() => {
                  setRejectionReason("");
                  setActiveReasonInput(evidence.id);
                }}
                className="flex-1 h-9 flex items-center justify-center gap-2 rounded-xl transition-all border font-black text-[9px] uppercase tracking-widest bg-danger/5 text-danger border-danger/20 hover:bg-danger hover:text-white"
              >
                <WarningCircle size={16} weight="fill" />
                Impugnar
              </button>
            </div>
          ) : (
            <div className="flex items-center justify-between text-[10px] font-black uppercase tracking-widest py-1">
              <span className={cn(
                "px-3 py-1.5 rounded-lg border",
                evidence.approved 
                  ? "bg-success/10 text-success border-success/20" 
                  : "bg-danger/10 text-danger border-danger/20"
              )}>
                {evidence.approved ? "✓ Validado por operador" : "✗ Impugnado por operador"}
              </span>
              <button
                onClick={() => {
                  if (evidence.approved) {
                    setRejectionReason("");
                    setActiveReasonInput(evidence.id);
                  } else {
                    onStatusUpdate(evidence.id, true);
                  }
                }}
                className="text-[9px] text-zinc-500 hover:text-white underline cursor-pointer transition-colors"
              >
                Cambiar
              </button>
            </div>
          )}

          {/* Selector de Reclasificación */}
          <div className="pt-2 border-t border-border/50 flex items-center justify-between gap-2 overflow-hidden">
             <div className="flex items-center gap-2 text-[9px] font-black text-zinc-600 uppercase tracking-widest">
               <ArrowsLeftRight size={14} />
               Reclasificar
             </div>
             <select 
               className="bg-background/50 border border-border/50 rounded-lg px-2 py-1 text-[8px] font-black uppercase tracking-widest text-zinc-300 outline-none focus:border-accent/40 w-[120px] cursor-pointer hover:text-white transition-colors"
               value={evidence.documentType}
               onChange={(e) => onReclassify(e.target.value)}
             >
                {DOC_TYPE_OPTIONS.map(opt => (
                  <option key={opt.value} value={opt.value} className="bg-zinc-900 text-zinc-200">{opt.label}</option>
                ))}
             </select>
          </div>
        </div>

        {evidence.rejectionReason && (
          <div className="p-3 rounded-xl bg-danger/5 border border-danger/10">
             <p className="text-[9px] text-danger/80 italic leading-snug">Motivo: {evidence.rejectionReason}</p>
          </div>
        )}
      </div>
    </div>
  );
}
