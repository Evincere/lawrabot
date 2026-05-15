"use client";

import { useState } from "react";
import { Expediente, UpdateCaseDataRequest, EvidenceItem } from "@/lib/types/divorce";
import { 
  Calendar,
  MapPin,
  Buildings,
  PencilSimple,
  Copy,
  CheckCircle,
  WarningCircle,
  Scales,
  Certificate,
  DownloadSimple,
  Files,
  Stamp,
  Plus
} from "@phosphor-icons/react";
import { cn } from "@/lib/utils";
import { motion, AnimatePresence } from "framer-motion";
import { useToast } from "@/lib/contexts/ToastContext";
import Image from "next/image";

interface MarriageTabProps {
  expediente: Expediente;
  evidences: EvidenceItem[];
  onUpdate: (data: UpdateCaseDataRequest) => Promise<void>;
  onEvidenceStatusUpdate: (evidenceId: string, approved: boolean, reason?: string) => void;
}

// Formatea una fecha ISO "YYYY-MM-DD" a "DD / MM / YYYY"
function formatDate(dateStr?: string): string {
  if (!dateStr) return "No declarada";
  const [year, month, day] = dateStr.split("-");
  if (!year || !month || !day) return dateStr;
  return `${day} / ${month} / ${year}`;
}

// Calcular duración entre dos fechas
function calcDuration(start?: string, end?: string): string {
  if (!start || !end) return "—";
  const s = new Date(start);
  const e = new Date(end);
  const diffMs = e.getTime() - s.getTime();
  if (diffMs < 0) return "Fechas inválidas";
  const years = Math.floor(diffMs / (1000 * 60 * 60 * 24 * 365.25));
  const months = Math.floor((diffMs % (1000 * 60 * 60 * 24 * 365.25)) / (1000 * 60 * 60 * 24 * 30.44));
  const parts: string[] = [];
  if (years > 0) parts.push(`${years} año${years !== 1 ? "s" : ""}`);
  if (months > 0) parts.push(`${months} mes${months !== 1 ? "es" : ""}`);
  return parts.length > 0 ? parts.join(" y ") : "Menos de un mes";
}

const DIVORCE_TYPE_LABELS: Record<string, { label: string; desc: string }> = {
  UNILATERAL: { label: "Unilateral", desc: "Presentado por uno de los cónyuges sin requerir acuerdo del otro" },
  BILATERAL: { label: "Bilateral / Consensual", desc: "Ambas partes acuerdan el divorcio y sus condiciones" },
};

interface EditableFieldProps {
  label: string;
  value?: string;
  icon: React.ElementType;
  isEditing: boolean;
  type?: string;
  onChange: (value: string) => void;
}

const EditableField = ({ label, value, icon: Icon, isEditing, type = "text", onChange }: EditableFieldProps) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    if (!value || isEditing) return;
    navigator.clipboard.writeText(value);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div 
      className={cn(
        "p-6 rounded-2xl bg-surface border transition-all relative overflow-hidden group",
        isEditing ? "border-accent/30 bg-accent/5 shadow-inner" : "border-border hover:border-accent/30 cursor-pointer"
      )}
      onClick={handleCopy}
    >
      <div className="flex items-center justify-between mb-3">
        <span className="text-[10px] uppercase tracking-[0.2em] font-black text-zinc-500 flex items-center gap-2">
          <Icon size={16} weight="duotone" className={isEditing ? "text-accent" : "text-zinc-500"} />
          {label}
        </span>
        {!isEditing && value && (
          <div className="opacity-0 group-hover:opacity-100 transition-opacity">
            {copied ? <CheckCircle size={14} weight="fill" className="text-success" /> : <Copy size={14} className="text-zinc-500" />}
          </div>
        )}
      </div>

      {isEditing ? (
        <input 
          type={type}
          value={value || ""}
          onChange={(e) => onChange(e.target.value)}
          className="w-full bg-transparent text-xl font-black tracking-tight text-white focus:outline-none placeholder:text-zinc-800"
          placeholder="Completar..."
        />
      ) : (
        <p className={cn(
          "text-xl font-black tracking-tight",
          value ? "text-white" : "text-zinc-700 italic"
        )}>
          {/* Mostrar fecha formateada solo si no estamos editando */}
          {type === "date" ? formatDate(value) : (value || "No declarado")}
        </p>
      )}

      <AnimatePresence>
        {copied && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="absolute inset-0 bg-accent/10 backdrop-blur-[2px] flex items-center justify-center pointer-events-none"
          >
            <span className="text-[12px] font-black text-accent uppercase tracking-[0.2em]">Copiado</span>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export function MarriageTab({ expediente, evidences, onUpdate, onEvidenceStatusUpdate }: MarriageTabProps) {
  const [isEditing, setIsEditing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [previewEvidence, setPreviewEvidence] = useState<EvidenceItem | null>(null);
  const { showToast } = useToast();

  const [marriageDate, setMarriageDate] = useState(expediente.marriageDate || "");
  const [separationDate, setSeparationDate] = useState(expediente.deFactoSeparationDate || "");
  const [residence, setResidence] = useState(expediente.lastConjugalResidence || { locality: "", street: "", number: "" });

  // Nuevos campos registrales
  const [certNumber, setCertNumber] = useState(expediente.marriageCertificateNumber || "");
  const [regBook, setRegBook] = useState(expediente.marriageRegistryBook || "");
  const [regPage, setRegPage] = useState(expediente.marriageRegistryPage || "");
  const [regOffice, setRegOffice] = useState(expediente.marriageRegistryOffice || "");
  const [mPlace, setMPlace] = useState(expediente.marriagePlace || "");
  const [certId, setCertId] = useState(expediente.marriageCertificateId || "");
  const [certIssuanceDate, setCertIssuanceDate] = useState(expediente.marriageCertificateIssuanceDate || "");

  // Filtrar solo las actas de matrimonio
  const marriageCerts = evidences.filter(ev => ev.documentType === "MARRIAGE_CERT");

  const handleSave = async () => {
    setIsSaving(true);
    try {
      await onUpdate({
        marriageDate,
        deFactoSeparationDate: separationDate,
        lastConjugalResidence: residence,
        marriageCertificateId: certId,
        marriageCertificateIssuanceDate: certIssuanceDate,
        marriageCertificateNumber: certNumber,
        marriageRegistryBook: regBook,
        marriageRegistryPage: regPage,
        marriageRegistryOffice: regOffice,
        marriagePlace: mPlace
      });
      setIsEditing(false);
      showToast("Datos de matrimonio actualizados", "success");
    } catch {
      showToast("Error al guardar cambios", "error");
    } finally {
      setIsSaving(false);
    }
  };

  const isCertExpired = certIssuanceDate ? (
    (new Date().getTime() - new Date(certIssuanceDate).getTime()) > (1000 * 60 * 60 * 24 * 30 * 6)
  ) : false;

  const handleCopyFromPetitioner = () => {
    if (expediente.petitioner?.address) {
      setResidence({
        locality: expediente.petitioner.address.locality,
        street: expediente.petitioner.address.street,
        number: expediente.petitioner.address.number
      });
      showToast("Domicilio copiado del peticionante", "info");
    }
  };

  const handleCopyFromRespondent = () => {
    if (expediente.respondent?.residentialAddress) {
      setResidence({
        locality: expediente.respondent.residentialAddress.locality,
        street: expediente.respondent.residentialAddress.street,
        number: expediente.respondent.residentialAddress.number
      });
      showToast("Domicilio copiado del demandado", "info");
    }
  };

  const divorceTypeInfo = DIVORCE_TYPE_LABELS[expediente.divorceType || ""] || null;

  return (
    <div className="space-y-12 pb-32">
      {/* Header */}
      <div className="flex items-center justify-between mb-2">
         <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-accent/10 border border-accent/20">
              <Buildings size={24} weight="duotone" className="text-accent" />
            </div>
            <div>
              <h5 className="text-[10px] uppercase tracking-[0.2em] text-zinc-500 font-black">Nexo Conyugal: Datos del Matrimonio</h5>
              <p className="text-xs text-zinc-400 font-medium mt-0.5">Validación de fechas y residencia conyugal</p>
            </div>
         </div>

         <div className="flex items-center gap-2">
           {isEditing ? (
             <>
               <button 
                 onClick={() => setIsEditing(false)}
                 className="px-5 py-2.5 rounded-xl bg-surface border border-border text-[10px] font-black uppercase tracking-widest text-zinc-500 hover:text-white transition-all"
               >
                 Cancelar
               </button>
               <button 
                 onClick={handleSave}
                 disabled={isSaving}
                 className="px-8 py-2.5 rounded-xl bg-success text-white text-[10px] font-black uppercase tracking-widest shadow-lg shadow-success/20 hover:scale-105 transition-all"
               >
                 {isSaving ? "Guardando..." : "Consolidar Datos"}
               </button>
             </>
           ) : (
             <button 
               onClick={() => setIsEditing(true)}
               className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-surface border border-border text-[10px] font-black uppercase tracking-widest text-zinc-400 hover:text-accent hover:border-accent/30 transition-all"
             >
               <PencilSimple size={16} weight="bold" />
               Rectificar Datos
             </button>
           )}
         </div>
      </div>

      {/* Tipo de Divorcio */}
      {divorceTypeInfo && (
        <div className="p-5 rounded-2xl bg-surface border border-border flex items-center gap-4">
          <div className="p-3 rounded-xl bg-accent/10 border border-accent/20 flex-shrink-0">
            <Scales size={22} weight="duotone" className="text-accent" />
          </div>
          <div>
            <p className="text-[9px] uppercase tracking-[0.2em] font-black text-zinc-500 mb-0.5">Modalidad Procesal</p>
            <p className="text-base font-black text-white">{divorceTypeInfo.label}</p>
            <p className="text-[11px] text-zinc-500 mt-0.5">{divorceTypeInfo.desc}</p>
          </div>
          <div className="ml-auto">
            <span className={cn(
              "px-3 py-1.5 rounded-lg text-[9px] font-black uppercase tracking-widest border",
              expediente.divorceType === "BILATERAL" 
                ? "bg-success/10 text-success border-success/20"
                : "bg-accent/10 text-accent border-accent/20"
            )}>
              {expediente.divorceType}
            </span>
          </div>
        </div>
      )}

      {/* Fechas clave */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <EditableField 
          label="Fecha de Celebración"
          value={marriageDate}
          icon={Calendar}
          isEditing={isEditing}
          type="date"
          onChange={setMarriageDate}
        />
        <EditableField 
          label="Fecha de Separación de Hecho"
          value={separationDate}
          icon={Calendar}
          isEditing={isEditing}
          type="date"
          onChange={setSeparationDate}
        />
      </div>

      {/* Duración del matrimonio */}
      {expediente.marriageDate && expediente.deFactoSeparationDate && !isEditing && (
        <div className="grid grid-cols-2 gap-4">
          <div className="p-5 rounded-2xl bg-surface/50 border border-border/50 text-center">
            <p className="text-[9px] uppercase tracking-[0.2em] font-black text-zinc-600 mb-2">Duración del Matrimonio</p>
            <p className="text-2xl font-black text-white tracking-tight">
              {calcDuration(expediente.marriageDate, expediente.deFactoSeparationDate)}
            </p>
          </div>
          <div className="p-5 rounded-2xl bg-surface/50 border border-border/50 text-center">
            <p className="text-[9px] uppercase tracking-[0.2em] font-black text-zinc-600 mb-2">Años desde la Separación</p>
            <p className="text-2xl font-black text-white tracking-tight">
              {calcDuration(expediente.deFactoSeparationDate, new Date().toISOString().split("T")[0])}
            </p>
          </div>
        </div>
      )}

      {/* Último Domicilio Conyugal */}
      <section>
        <div className="flex items-center gap-2 mb-6">
          <MapPin size={18} weight="fill" className="text-zinc-600" />
          <h6 className="text-[10px] font-black uppercase tracking-[0.2em] text-zinc-500">Último Domicilio Conyugal</h6>
          <span className="text-[9px] text-zinc-700 font-bold uppercase tracking-widest ml-2">— Competencia territorial Art. 717 CCyC</span>
          
          {isEditing && (
            <div className="ml-auto flex items-center gap-2">
              <button 
                onClick={handleCopyFromPetitioner}
                className="px-3 py-1.5 rounded-lg bg-surface border border-border text-[8px] font-black uppercase tracking-widest text-zinc-500 hover:text-accent transition-all"
              >
                Copiar de Peticionante
              </button>
              <button 
                onClick={handleCopyFromRespondent}
                className="px-3 py-1.5 rounded-lg bg-surface border border-border text-[8px] font-black uppercase tracking-widest text-zinc-500 hover:text-accent transition-all"
              >
                Copiar de Demandado
              </button>
            </div>
          )}
        </div>

        <div className="p-8 rounded-3xl bg-surface border border-border">
          {isEditing ? (
            <div className="grid grid-cols-12 gap-4">
              <div className="col-span-12 lg:col-span-6">
                <span className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Localidad / Jurisdicción</span>
                <input 
                  value={residence.locality}
                  onChange={(e) => setResidence({...residence, locality: e.target.value})}
                  className="w-full bg-background border border-border/50 rounded-xl p-4 text-white font-bold text-sm focus:border-accent/40 focus:outline-none"
                  placeholder="Ej: San Rafael"
                />
              </div>
              <div className="col-span-8 lg:col-span-4">
                <span className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Calle</span>
                <input 
                  value={residence.street || ""}
                  onChange={(e) => setResidence({...residence, street: e.target.value})}
                  className="w-full bg-background border border-border/50 rounded-xl p-4 text-white font-bold text-sm focus:border-accent/40 focus:outline-none"
                  placeholder="Calle de residencia"
                />
              </div>
              <div className="col-span-4 lg:col-span-2">
                <span className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Número</span>
                <input 
                  value={residence.number || ""}
                  onChange={(e) => setResidence({...residence, number: e.target.value})}
                  className="w-full bg-background border border-border/50 rounded-xl p-4 text-white font-bold text-sm focus:border-accent/40 focus:outline-none"
                  placeholder="Alt."
                />
              </div>
            </div>
          ) : (
            <div className="flex items-start gap-6 group cursor-pointer" onClick={() => {
              navigator.clipboard.writeText(`${residence.street} ${residence.number}, ${residence.locality}`);
              showToast("Domicilio copiado", "info");
            }}>
              <div className="w-16 h-16 rounded-2xl bg-background border border-border flex items-center justify-center text-zinc-600 group-hover:text-accent group-hover:border-accent/30 transition-colors">
                <MapPin size={32} weight="duotone" />
              </div>
              <div>
                <p className="text-2xl font-black tracking-tight text-white uppercase">
                  {residence.street && residence.number 
                    ? `${residence.street} ${residence.number}` 
                    : "Domicilio no especificado"}
                </p>
                <div className="flex items-center gap-2 mt-1">
                  <p className="text-sm font-bold text-accent">{residence.locality || "Sin Localidad"}</p>
                  <span className="w-1 h-1 rounded-full bg-zinc-700" />
                  <p className="text-sm text-zinc-500">Mendoza, Argentina</p>
                </div>
              </div>
            </div>
          )}
        </div>
      </section>

      {/* Datos Registrales Consolidados (Modo Lectura) */}
      {!isEditing && (expediente.marriageCertificateNumber || expediente.marriageCertificateId) && (
        <section className="animate-in fade-in slide-in-from-bottom-4 duration-700">
          <div className="flex items-center gap-2 mb-6">
            <Stamp size={18} weight="fill" className="text-zinc-600" />
            <h6 className="text-[10px] font-black uppercase tracking-[0.2em] text-zinc-500">Información del Acta Consolidada</h6>
          </div>
          
          <div className="p-8 rounded-3xl bg-surface border border-border grid grid-cols-1 md:grid-cols-3 gap-8">
            <div className="space-y-1">
              <p className="text-[9px] font-black text-zinc-600 uppercase tracking-widest">Referencia del Acta</p>
              <p className="text-lg font-black text-white tracking-tight">
                {expediente.marriageCertificateNumber ? `Acta Nº ${expediente.marriageCertificateNumber}` : "Nº no especificado"}
              </p>
              <p className="text-[11px] text-zinc-500 font-medium">
                Libro {expediente.marriageRegistryBook || "—"} / Folio {expediente.marriageRegistryPage || "—"}
              </p>
            </div>

            <div className="space-y-1">
              <p className="text-[9px] font-black text-zinc-600 uppercase tracking-widest">Lugar y Emisión</p>
              <p className="text-lg font-black text-white tracking-tight truncate" title={expediente.marriagePlace}>
                {expediente.marriagePlace || "Lugar no cargado"}
              </p>
              <p className={cn(
                "text-[11px] font-bold flex items-center gap-1",
                isCertExpired ? "text-danger" : "text-success"
              )}>
                {isCertExpired && <WarningCircle size={12} weight="fill" />}
                Expedida: {formatDate(expediente.marriageCertificateIssuanceDate)}
              </p>
            </div>

            <div className="space-y-1">
              <p className="text-[9px] font-black text-zinc-600 uppercase tracking-widest">Documento Vinculado</p>
              {expediente.marriageCertificateId ? (
                (() => {
                  const cert = evidences.find(e => e.id === expediente.marriageCertificateId);
                  return cert ? (
                    <div 
                      onClick={() => setPreviewEvidence(cert)}
                      className="flex items-center gap-3 p-3 rounded-xl bg-background border border-border group cursor-pointer hover:border-accent/30 transition-all"
                    >
                      <div className="p-1.5 rounded bg-accent/10 text-accent">
                        <Certificate size={16} weight="duotone" />
                      </div>
                      <span className="text-[11px] font-bold text-white truncate max-w-[120px] group-hover:text-accent transition-colors">
                        {cert.fileName}
                      </span>
                    </div>
                  ) : (
                    <p className="text-[11px] text-danger font-bold uppercase tracking-widest mt-2">ID de acta no encontrado</p>
                  );
                })()
              ) : (
                <p className="text-[11px] text-zinc-600 font-bold uppercase tracking-widest mt-2">Sin documento oficial</p>
              )}
            </div>
          </div>
        </section>
      )}

      {/* Acta de Matrimonio */}
      <section>
        <div className="flex items-center gap-2 mb-6">
          <Certificate size={18} weight="fill" className="text-zinc-600" />
          <h6 className="text-[10px] font-black uppercase tracking-[0.2em] text-zinc-500">Acta de Matrimonio</h6>
          <span className={cn(
            "ml-auto px-2.5 py-1 rounded-lg text-[9px] font-black uppercase tracking-widest border",
            marriageCerts.length > 0 
              ? "bg-success/10 text-success border-success/20" 
              : "bg-warning/10 text-warning border-warning/20"
          )}>
            {marriageCerts.length > 0 ? `${marriageCerts.length} documento${marriageCerts.length > 1 ? "s" : ""}` : "Pendiente"}
          </span>
        </div>

        {marriageCerts.length === 0 ? (
          <div className="p-12 rounded-3xl bg-surface/30 border-2 border-dashed border-border flex flex-col items-center justify-center gap-4">
            <Stamp size={40} weight="thin" className="text-zinc-700 opacity-50" />
            <div className="text-center">
              <p className="text-sm font-bold text-zinc-500">Sin acta registrada</p>
              <p className="text-[10px] uppercase tracking-widest text-zinc-700 mt-1">
                El ciudadano aún no ha enviado el acta de matrimonio por WhatsApp.<br />
                Podés subirla manualmente desde la pestaña &quot;BLSG y Evidencia&quot;.
              </p>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
            {marriageCerts.map((ev) => (
              <div key={ev.id} className="rounded-2xl bg-surface border border-border overflow-hidden flex flex-col group hover:border-border-strong transition-all">
                <div
                  className="h-44 bg-background flex items-center justify-center cursor-pointer relative overflow-hidden"
                  onClick={() => setPreviewEvidence(ev)}
                >
                  {ev.mimeType?.startsWith("image/") ? (
                    <Image
                      src={`http://localhost:8081/api/divorce/evidence/download/${ev.id}`}
                      alt={ev.fileName}
                      fill
                      unoptimized
                      className="object-cover group-hover:scale-105 transition-transform duration-700"
                    />
                  ) : (
                    <div className="flex flex-col items-center gap-2 text-zinc-600">
                      <Files size={40} weight="thin" />
                      <span className="text-[10px] uppercase tracking-widest font-black">PDF</span>
                    </div>
                  )}

                  {/* Estado badge */}
                  <div className={cn(
                    "absolute top-3 right-3 px-2 py-1 rounded text-[8px] font-black uppercase tracking-widest border",
                    ev.approved ? "bg-success/20 text-success border-success/30" :
                    ev.rejectionReason ? "bg-danger/20 text-danger border-danger/30" : "bg-warning/20 text-warning border-warning/30"
                  )}>
                    {ev.approved ? "Validada" : ev.rejectionReason ? "Impugnada" : "Pendiente"}
                  </div>
                </div>

                <div className="p-4 flex flex-col gap-3">
                  <p className="text-xs font-bold text-white truncate" title={ev.fileName}>{ev.fileName}</p>

                  <div className="flex gap-2">
                    {!ev.approved && !ev.rejectionReason && (
                      <>
                        <button
                          onClick={() => onEvidenceStatusUpdate(ev.id, true)}
                          className="flex-1 px-3 py-2 rounded-lg bg-success/10 border border-success/20 text-success text-[9px] font-black uppercase tracking-[0.1em] hover:bg-success/20 transition-all"
                        >
                          Validar
                        </button>
                        <button
                          onClick={() => {
                            const reason = prompt("Motivo de la impugnación:");
                            if (reason) onEvidenceStatusUpdate(ev.id, false, reason);
                          }}
                          className="flex-1 px-3 py-2 rounded-lg bg-danger/10 border border-danger/20 text-danger text-[9px] font-black uppercase tracking-[0.1em] hover:bg-danger/20 transition-all"
                        >
                          Impugnar
                        </button>
                      </>
                    )}
                    <a
                      href={`http://localhost:8081/api/divorce/evidence/download/${ev.id}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="px-3 py-2 rounded-lg bg-background border border-border text-zinc-500 hover:text-white hover:border-border-strong transition-all"
                      title="Descargar original"
                    >
                      <DownloadSimple size={14} weight="bold" />
                    </a>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Datos del Acta (Solo si está en edición) */}
      <AnimatePresence>
        {isEditing && (
          <motion.section
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="p-8 rounded-3xl bg-surface border-2 border-accent/20 shadow-2xl shadow-accent/5"
          >
            <div className="flex items-center gap-3 mb-8">
              <div className="p-2 rounded-lg bg-accent/10 text-accent">
                <Stamp size={20} weight="duotone" />
              </div>
              <div>
                <h6 className="text-[10px] font-black uppercase tracking-[0.2em] text-accent">Datos Registrales del Acta</h6>
                <p className="text-[10px] text-zinc-500 font-medium">Información necesaria para el encabezado de la demanda</p>
              </div>
            </div>

            <div className="grid grid-cols-12 gap-6">
              <div className="col-span-12 md:col-span-4">
                <span className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Fecha de Emisión</span>
                <input 
                  type="date"
                  value={certIssuanceDate}
                  onChange={(e) => setCertIssuanceDate(e.target.value)}
                  className={cn(
                    "w-full bg-background border rounded-xl p-4 text-white font-bold text-sm focus:outline-none transition-colors",
                    isCertExpired ? "border-danger/50 focus:border-danger" : "border-border/50 focus:border-accent/40"
                  )}
                />
                {isCertExpired && (
                  <p className="text-[9px] font-bold text-danger uppercase mt-2 flex items-center gap-1">
                    <WarningCircle size={12} weight="fill" /> El acta tiene más de 6 meses
                  </p>
                )}
              </div>

              <div className="col-span-12 md:col-span-8">
                <span className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Oficina / Registro Civil</span>
                <input 
                  value={regOffice}
                  onChange={(e) => setRegOffice(e.target.value)}
                  className="w-full bg-background border border-border/50 rounded-xl p-4 text-white font-bold text-sm focus:border-accent/40 focus:outline-none"
                  placeholder="Ej: Registro Civil San Rafael"
                />
              </div>

              <div className="col-span-12 md:col-span-6">
                <span className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Lugar de Celebración</span>
                <input 
                  value={mPlace}
                  onChange={(e) => setMPlace(e.target.value)}
                  className="w-full bg-background border border-border/50 rounded-xl p-4 text-white font-bold text-sm focus:border-accent/40 focus:outline-none"
                  placeholder="Ej: San Rafael, Mendoza"
                />
              </div>

              <div className="col-span-4 md:col-span-2">
                <span className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Libro</span>
                <input 
                  value={regBook}
                  onChange={(e) => setRegBook(e.target.value)}
                  className="w-full bg-background border border-border/50 rounded-xl p-4 text-white font-bold text-sm focus:border-accent/40 focus:outline-none"
                  placeholder="Tomo/Libro"
                />
              </div>

              <div className="col-span-4 md:col-span-2">
                <span className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Folio</span>
                <input 
                  value={regPage}
                  onChange={(e) => setRegPage(e.target.value)}
                  className="w-full bg-background border border-border/50 rounded-xl p-4 text-white font-bold text-sm focus:border-accent/40 focus:outline-none"
                  placeholder="Pág."
                />
              </div>

              <div className="col-span-4 md:col-span-2">
                <span className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Acta Nº</span>
                <input 
                  value={certNumber}
                  onChange={(e) => setCertNumber(e.target.value)}
                  className="w-full bg-background border border-border/50 rounded-xl p-4 text-white font-bold text-sm focus:border-accent/40 focus:outline-none"
                  placeholder="Número"
                />
              </div>

              <div className="col-span-12">
                <span className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Seleccionar Acta de Matrimonio Oficial</span>
                <div className="flex flex-wrap gap-3">
                  {marriageCerts.map(ev => (
                    <button
                      key={ev.id}
                      onClick={() => setCertId(ev.id)}
                      className={cn(
                        "flex items-center gap-3 p-3 rounded-xl border transition-all",
                        certId === ev.id 
                          ? "bg-accent/10 border-accent text-accent" 
                          : "bg-surface border-border text-zinc-500 hover:border-zinc-700"
                      )}
                    >
                      {certId === ev.id ? <CheckCircle size={18} weight="fill" /> : <div className="w-[18px] h-[18px] rounded-full border-2 border-zinc-800" />}
                      <span className="text-xs font-bold truncate max-w-[150px]">{ev.fileName}</span>
                    </button>
                  ))}
                  {marriageCerts.length === 0 && (
                    <p className="text-[10px] text-warning font-bold uppercase tracking-widest py-4">⚠️ No hay actas cargadas para vincular</p>
                  )}
                </div>
              </div>
            </div>
          </motion.section>
        )}
      </AnimatePresence>

      {/* Alerta de datos faltantes */}
      {!isEditing && (!marriageDate || !separationDate || !residence.locality) && (
        <div className="p-6 rounded-2xl bg-danger/5 border border-danger/20 flex items-center gap-4">
          <div className="p-2 bg-danger/10 rounded-lg text-danger">
            <WarningCircle size={20} weight="fill" />
          </div>
          <p className="text-xs font-bold text-zinc-400">
            Faltan datos críticos para la demanda. Por favor, rectifique la información del matrimonio.
          </p>
        </div>
      )}

      {/* Lightbox */}
      <AnimatePresence>
        {previewEvidence && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-[100] bg-black/95 backdrop-blur-xl flex items-center justify-center p-4 md:p-8"
            onClick={() => setPreviewEvidence(null)}
          >
            <button className="absolute top-6 right-6 z-[110] text-white/40 hover:text-white transition-colors p-4 hover:bg-white/5 rounded-full">
               <Plus size={32} className="rotate-45" />
            </button>
            <div className="relative w-full h-full max-w-5xl flex flex-col items-center justify-between gap-4" onClick={e => e.stopPropagation()}>
              {previewEvidence.mimeType?.startsWith("image/") ? (
                <div className="relative w-full flex-1 shadow-2xl shadow-black">
                  <Image
                    src={`http://localhost:8081/api/divorce/evidence/download/${previewEvidence.id}`}
                    alt={previewEvidence.fileName}
                    fill
                    unoptimized
                    className="object-contain"
                  />
                </div>
              ) : (
                <div className="w-full flex-1 flex flex-col gap-4 min-h-0">
                   <div className="flex-1 rounded-2xl overflow-hidden bg-white border border-white/10 shadow-2xl">
                     <iframe 
                       src={`http://localhost:8081/api/divorce/evidence/download/${previewEvidence.id}#toolbar=0`}
                       className="w-full h-full border-none"
                       title={previewEvidence.fileName}
                     />
                   </div>
                   <div className="text-center">
                     <a 
                       href={`http://localhost:8081/api/divorce/evidence/download/${previewEvidence.id}`}
                       target="_blank"
                       className="inline-flex px-8 py-3 bg-accent text-white rounded-xl font-black text-[11px] uppercase tracking-[0.2em] shadow-xl shadow-accent/20 hover:scale-105 transition-all"
                     >
                       Abrir en nueva pestaña / Descargar
                     </a>
                   </div>
                </div>
              )}
              <div className="text-center pb-4">
                <p className="text-lg font-black text-white uppercase tracking-tight">{previewEvidence.fileName}</p>
                <div className="flex items-center justify-center gap-3 mt-2">
                  <span className="px-3 py-1 bg-white/5 rounded-lg border border-white/10 text-[10px] font-black text-zinc-400 uppercase tracking-widest">
                    Acta de Matrimonio
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
