"use client";

import { useState } from "react";
import { Expediente, EvidenceItem } from "@/lib/types/divorce";
import { 
  ShieldCheck, 
  WarningCircle, 
  User, 
  IdentificationCard,
  Calendar,
  MapPin,
  GenderIntersex,
  CurrencyDollar,
  House,
  Briefcase,
  Car,
  FilePdf,
  CheckCircle,
  XCircle,
  Plus,
  DownloadSimple
} from "@phosphor-icons/react";
import { motion, AnimatePresence } from "framer-motion";
import { cn } from "@/lib/utils";
import Image from "next/image";
import { ObservationModal } from "@/components/shared/ObservationModal";
import { useToast } from "@/lib/contexts/ToastContext";

interface BlsgTabProps {
  expediente: Expediente;
  evidences: EvidenceItem[];
  onDecision: (approved: boolean, observations: string) => void;
  onEvidenceStatusUpdate: (evidenceId: string, approved: boolean, reason?: string) => void;
}

// Mapeos de traducción en español
const HOUSING_LABELS: Record<string, string> = {
  RENTING: "Alquila",
  OWNER: "Propietario/a",
  FAMILY_HOME: "Casa Familiar",
  SHARED_HOUSING: "Vivienda Compartida",
  OTHER: "Otro",
};

const SCRAPING_RESULT_LABELS: Record<string, string> = {
  PROVISIONALLY_APPROVED: "APROBADO PROVISIONALMENTE (AUTOMÁTICO)",
  PROVISIONALLY_REJECTED: "RECHAZADO PROVISIONALMENTE (AUTOMÁTICO)",
  INCONCLUSIVE: "EVALUACIÓN AUTOMÁTICA NO CONCLUYENTE",
  PENDING: "PENDIENTE DE SCRAPING",
};

// Formatea una fecha ISO "YYYY-MM-DD" a "DD / MM / YYYY"
function formatDate(dateStr?: string): string {
  if (!dateStr || dateStr === "S/D") return "S/D";
  if (dateStr.includes("/")) return dateStr;
  const [year, month, day] = dateStr.split("-");
  if (!year || !month || !day) return dateStr;
  return `${day}/${month}/${year}`;
}

export function BlsgTab({ expediente, evidences, onDecision, onEvidenceStatusUpdate }: BlsgTabProps) {
  const profile = expediente.socioEconomicProfile;
  const [showLightbox, setShowLightbox] = useState(false);
  const [previewEvidence, setPreviewEvidence] = useState<EvidenceItem | null>(null);
  const [observingEvidence, setObservingEvidence] = useState<EvidenceItem | null>(null);
  
  const [observations, setObservations] = useState(profile?.blsgObservations || "");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  
  const { showToast } = useToast();

  if (!profile) {
    return (
      <div className="p-20 rounded-3xl bg-surface/30 border-2 border-dashed border-border flex flex-col items-center justify-center text-zinc-600 gap-4">
        <ShieldCheck size={48} weight="thin" className="opacity-20" />
        <div className="text-center">
          <p className="text-sm font-bold text-zinc-500">Sin datos de BLSG</p>
          <p className="text-[10px] uppercase tracking-widest mt-1">No se ha iniciado la evaluación para este caso</p>
        </div>
      </div>
    );
  }

  // Carga de Datos del Solicitante (Fallback a Peticionario en caso Unilateral)
  const scrapingName = profile.scrapingFullName || expediente.petitioner?.fullName?.fullName || "S/D";
  const scrapingDni = profile.scrapingDni || expediente.petitioner?.dni || "S/D";
  const scrapingCuil = profile.scrapingCuil || expediente.petitioner?.cuil || "S/D";
  const scrapingBirthDate = profile.scrapingBirthDate || expediente.petitioner?.birthDate || "S/D";
  const scrapingProvince = profile.scrapingProvince || expediente.petitioner?.address?.province || "Mendoza";
  const scrapingSex = profile.scrapingSex || "S/D";

  const incomeProofs = evidences.filter(ev => ev.documentType === "INCOME_PROOF");

  const handleDecision = async (approved: boolean) => {
    setIsSubmitting(true);
    try {
      await onDecision(approved, observations);
      setIsEditing(false);
      showToast(approved ? "BLSG Patrocinado" : "Patrocinio rechazado", approved ? "success" : "warning");
    } catch {
      showToast("Error al guardar decisión", "error");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleConfirmObservation = (reason: string) => {
    if (observingEvidence) {
      onEvidenceStatusUpdate(observingEvidence.id, false, reason);
      setObservingEvidence(null);
      if (previewEvidence && previewEvidence.id === observingEvidence.id) {
        setPreviewEvidence(null);
      }
    }
  };

  return (
    <div className="space-y-12 pb-32">
      {/* SECCIÓN 1: Identidad Verificada (Scraping) */}
      <section className="space-y-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-accent/10 border border-accent/20 text-accent">
              <IdentificationCard size={20} weight="duotone" />
            </div>
            <div>
              <h5 className="text-[10px] uppercase tracking-[0.2em] text-zinc-500 font-black">Identidad Verificada (Portal Judicial)</h5>
              <p className="text-[10px] text-zinc-400 font-medium mt-0.5">Constatación de datos registrados en el expediente</p>
            </div>
          </div>
          
          {profile.certificatePath && (
            <button 
              onClick={() => setShowLightbox(true)}
              className="flex items-center gap-2 px-4 py-2 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-zinc-400 hover:text-white transition-all text-[10px] font-black uppercase tracking-widest"
            >
              <FilePdf size={16} weight="duotone" className="text-danger" />
              Ver Constancia Judicial (PDF)
            </button>
          )}
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <InfoCard label="Nombre Completo" value={scrapingName} icon={<User />} />
          <InfoCard label="DNI" value={scrapingDni} icon={<IdentificationCard />} />
          <InfoCard label="CUIL" value={scrapingCuil} icon={<IdentificationCard />} />
          <InfoCard label="Fecha Nac." value={formatDate(scrapingBirthDate)} icon={<Calendar />} />
          <InfoCard label="Provincia" value={scrapingProvince} icon={<MapPin />} />
          <InfoCard label="Sexo" value={scrapingSex} icon={<GenderIntersex />} />
        </div>
      </section>

      {/* SECCIÓN 2: Evaluación Patrimonial (WhatsApp + Scraping) */}
      <section className="space-y-6">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-success/10 border border-success/20 text-success">
            <CurrencyDollar size={20} weight="duotone" />
          </div>
          <div>
            <h5 className="text-[10px] uppercase tracking-[0.2em] text-zinc-500 font-black">Evaluación Patrimonial y Socioeconómica</h5>
            <p className="text-[10px] text-zinc-400 font-medium mt-0.5">Datos autodeclarados y análisis automatizado de bienes</p>
          </div>
        </div>

        <div className="grid grid-cols-12 gap-6">
          <div className="col-span-12 lg:col-span-4 space-y-4">
            <div className="p-6 rounded-2xl bg-surface border border-border">
              <span className="text-[9px] font-black text-zinc-500 uppercase tracking-widest">Ingresos Mensuales</span>
              <p className="text-3xl font-bold tracking-tight text-success mt-1">
                ${profile.avgMonthlyIncome?.toLocaleString() || "0"}
              </p>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <StatusMiniCard icon={<House />} label="Vivienda" value={HOUSING_LABELS[profile.housingType] || profile.housingType || "S/D"} />
              <StatusMiniCard icon={<Briefcase />} label="Empleo" value={profile.hasFormalEmployment ? "Formal" : "Informal"} />
            </div>
            <StatusMiniCard icon={<Car />} label="Vehículos" value={`${profile.vehiclesRegistered || 0} Registrados`} />
          </div>

          <div className="col-span-12 lg:col-span-8 p-8 rounded-2xl bg-background border border-border flex flex-col h-full justify-between">
            <span className="text-[9px] font-black text-zinc-500 uppercase tracking-widest mb-4">Análisis de Bienes e Ingresos (IA / Scraping)</span>
            <div className={cn(
              "flex-1 p-6 rounded-xl border flex gap-6",
              profile.blsgScrapingResult === 'PROVISIONALLY_APPROVED' ? "bg-success/5 border-success/20" :
              profile.blsgScrapingResult === 'PROVISIONALLY_REJECTED' ? "bg-danger/5 border-danger/20" : "bg-warning/5 border-warning/20"
            )}>
              <div className={cn(
                "p-4 h-fit rounded-lg shadow-sm shrink-0",
                profile.blsgScrapingResult === 'PROVISIONALLY_APPROVED' ? "bg-success/10 text-success" :
                profile.blsgScrapingResult === 'PROVISIONALLY_REJECTED' ? "bg-danger/10 text-danger" : "bg-warning/10 text-warning"
              )}>
                <WarningCircle size={32} weight="duotone" />
              </div>
              <div className="space-y-2">
                <h6 className="text-sm font-black uppercase tracking-widest text-white">
                  {SCRAPING_RESULT_LABELS[profile.blsgScrapingResult] || profile.blsgScrapingResult || "PENDIENTE"}
                </h6>
                <p className="text-sm text-zinc-400 leading-relaxed italic">
                  &quot;{profile.scrapingJustification || profile.blsgObservations || "Cumple provisoriamente los requisitos para el beneficio de litigar sin gastos al no registrar propiedades ni automotores que excedan los límites."}&quot;
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* SECCIÓN 3: Documentación de Ingresos Cargada (Análisis Humano) */}
      <section className="space-y-6">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-accent/10 border border-accent/20 text-accent">
            <FilePdf size={20} weight="duotone" />
          </div>
          <div>
            <h5 className="text-[10px] uppercase tracking-[0.2em] text-zinc-500 font-black">Prueba de Ingresos y Aportes Cargados</h5>
            <p className="text-[10px] text-zinc-400 font-medium mt-0.5">Bono de sueldo o Certificación Negativa de ANSES para verificar elegibilidad</p>
          </div>
          <span className={cn(
            "ml-auto px-2.5 py-1 rounded-lg text-[9px] font-black uppercase tracking-widest border",
            incomeProofs.length > 0 
              ? "bg-success/10 text-success border-success/20" 
              : "bg-warning/10 text-warning border-warning/20"
          )}>
            {incomeProofs.length > 0 ? `${incomeProofs.length} Documento${incomeProofs.length > 1 ? "s" : ""}` : "Pendiente"}
          </span>
        </div>

        {incomeProofs.length === 0 ? (
          <div className="p-12 rounded-3xl bg-surface/30 border-2 border-dashed border-border flex flex-col items-center justify-center gap-4">
            <FilePdf size={40} weight="thin" className="text-zinc-700 opacity-50" />
            <div className="text-center">
              <p className="text-sm font-bold text-zinc-500">Sin comprobantes de ingresos registrados</p>
              <p className="text-[10px] uppercase tracking-widest text-zinc-700 mt-1 max-w-md mx-auto">
                El ciudadano aún no ha subido su bono de sueldo o certificado negativo de ANSES a través de WhatsApp.
              </p>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
            {incomeProofs.map((ev) => (
              <div key={ev.id} className="rounded-2xl bg-surface border border-border overflow-hidden flex flex-col group hover:border-border-strong transition-all">
                <div
                  className="h-40 bg-background flex items-center justify-center cursor-pointer relative overflow-hidden"
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
                      <FilePdf size={40} weight="thin" className="text-danger" />
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
                          onClick={() => setObservingEvidence(ev)}
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

      {/* SECCIÓN 4: Decisión de la Defensoría */}
      <section className="space-y-6 pt-8 border-t border-border">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-warning/10 border border-warning/20 text-warning">
            <ShieldCheck size={20} weight="duotone" />
          </div>
          <div>
            <h5 className="text-[10px] uppercase tracking-[0.2em] text-zinc-500 font-black">Decisión de la Defensoría (Patrocinio Jurídico)</h5>
            <p className="text-[10px] text-zinc-400 font-medium mt-0.5">Control de elegibilidad humana independientemente del scraping judicial</p>
          </div>
        </div>

        {profile.blsgApprovedByDefensoria === null || isEditing ? (
          <div className="p-8 rounded-3xl bg-surface border border-border space-y-6">
            <div className="space-y-3">
              <label className="text-[10px] uppercase tracking-widest font-black text-zinc-500">Observaciones y Justificación de la decisión</label>
              <textarea 
                value={observations}
                onChange={(e) => setObservations(e.target.value)}
                placeholder="Escriba aquí los motivos de la decisión final para informar al ciudadano automáticamente en caso de rechazo..."
                className="w-full bg-background border border-border rounded-2xl p-4 text-sm text-white focus:outline-none focus:border-accent/50 min-h-[120px] transition-all"
              />
            </div>
            
            <div className="flex gap-4">
              <button
                disabled={isSubmitting}
                onClick={() => handleDecision(true)}
                className="flex-1 h-16 rounded-2xl bg-success text-white font-black text-[12px] uppercase tracking-[0.2em] hover:bg-success/90 shadow-xl shadow-success/20 transition-all flex items-center justify-center gap-3 active:scale-[0.98] disabled:opacity-50"
              >
                {isSubmitting ? (
                  <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                ) : (
                  <>
                    <CheckCircle size={20} weight="fill" />
                    Aprobar Patrocinio y Tramitar
                  </>
                )}
              </button>
              <button
                disabled={isSubmitting}
                onClick={() => handleDecision(false)}
                className="flex-1 h-16 rounded-2xl bg-danger/10 border border-danger/20 text-danger font-black text-[12px] uppercase tracking-[0.2em] hover:bg-danger/20 transition-all flex items-center justify-center gap-3 active:scale-[0.98] disabled:opacity-50"
              >
                <XCircle size={20} weight="fill" />
                Rechazar Patrocinio
              </button>
              {isEditing && (
                <button
                  onClick={() => setIsEditing(false)}
                  className="px-6 h-16 rounded-2xl bg-zinc-800 text-zinc-400 font-black text-[10px] uppercase tracking-widest border border-border hover:bg-zinc-700 transition-all"
                >
                  Cancelar
                </button>
              )}
            </div>
          </div>
        ) : (
          <div className={cn(
            "p-8 rounded-3xl border flex items-start gap-6",
            profile.blsgApprovedByDefensoria ? "bg-success/5 border-success/20" : "bg-danger/5 border-danger/20"
          )}>
            <div className={cn(
              "p-4 rounded-2xl shrink-0",
              profile.blsgApprovedByDefensoria ? "bg-success/10 text-success" : "bg-danger/10 text-danger"
            )}>
              {profile.blsgApprovedByDefensoria ? <CheckCircle size={40} weight="fill" /> : <XCircle size={40} weight="fill" />}
            </div>
            <div className="space-y-3">
              <div className="flex items-center gap-4">
                <h4 className="text-xl font-black text-white uppercase tracking-tight">
                  Patrocinio Oficial {profile.blsgApprovedByDefensoria ? "APROBADO" : "RECHAZADO"}
                </h4>
                <button 
                  onClick={() => setIsEditing(true)}
                  className="text-[10px] text-zinc-500 underline hover:text-white uppercase tracking-widest font-black"
                >
                  Cambiar decisión
                </button>
              </div>
              <p className="text-sm text-zinc-400 leading-relaxed italic font-medium">
                &quot;{profile.blsgObservations || "Sin observaciones adicionales."}&quot;
              </p>
            </div>
          </div>
        )}
      </section>

      {/* LIGHTBOX DE CONSTANCIA JUDICIAL */}
      <AnimatePresence>
        {showLightbox && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-[100] bg-black/95 backdrop-blur-xl flex items-center justify-center p-4 md:p-8"
            onClick={() => setShowLightbox(false)}
          >
            <button className="absolute top-6 right-6 z-[110] text-white/40 hover:text-white transition-colors p-4 hover:bg-white/5 rounded-full">
              <Plus size={32} className="rotate-45" />
            </button>
            
            <div className="relative w-full h-full max-w-5xl flex flex-col items-center justify-between gap-4" onClick={e => e.stopPropagation()}>
              <div className="w-full flex-1 flex flex-col gap-4 min-h-0">
                 <div className="flex-1 rounded-2xl overflow-hidden bg-white border border-white/10 shadow-2xl">
                    <iframe 
                      src={`http://localhost:8081/api/divorce/cases/${expediente.id}/blsg-certificate#toolbar=0`}
                      className="w-full h-full border-none"
                      title="Constancia BLSG"
                    />
                 </div>
                 <div className="text-center">
                   <a 
                     href={`http://localhost:8081/api/divorce/cases/${expediente.id}/blsg-certificate`}
                     target="_blank"
                     rel="noopener noreferrer"
                     className="inline-flex px-8 py-3 bg-accent text-white rounded-xl font-black text-[11px] uppercase tracking-[0.2em] shadow-xl shadow-accent/20 hover:scale-105 transition-all"
                   >
                     Abrir en nueva pestaña / Descargar
                   </a>
                 </div>
              </div>
              
              <div className="text-center pb-4 shrink-0">
                <p className="text-lg font-black text-white uppercase tracking-tight">Constancia Judicial BLSG</p>
                <div className="flex items-center justify-center gap-3 mt-2">
                  <span className="px-3 py-1 bg-white/5 rounded-lg border border-white/10 text-[10px] font-black text-zinc-400 uppercase tracking-widest">
                    Documento Oficial
                  </span>
                </div>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* LIGHTBOX DE VISUALIZACIÓN DE PRUEBA DE INGRESOS */}
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
              <div className="w-full flex-1 flex flex-col gap-4 min-h-0">
                {previewEvidence.mimeType?.startsWith("image/") ? (
                  <div className="relative w-full flex-1 shadow-2xl shadow-black rounded-2xl overflow-hidden border border-white/5 bg-zinc-950">
                    <Image
                      src={`http://localhost:8081/api/divorce/evidence/download/${previewEvidence.id}`}
                      alt={previewEvidence.fileName}
                      fill
                      unoptimized
                      className="object-contain"
                    />
                  </div>
                ) : (
                  <div className="flex-1 rounded-2xl overflow-hidden bg-white border border-white/10 shadow-2xl">
                    <iframe 
                      src={`http://localhost:8081/api/divorce/evidence/download/${previewEvidence.id}#toolbar=0`}
                      className="w-full h-full border-none"
                      title={previewEvidence.fileName}
                    />
                  </div>
                )}
                
                <div className="text-center mt-2 shrink-0">
                  <a 
                    href={`http://localhost:8081/api/divorce/evidence/download/${previewEvidence.id}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex px-8 py-3 bg-accent text-white rounded-xl font-black text-[11px] uppercase tracking-[0.2em] shadow-xl shadow-accent/20 hover:scale-105 transition-all"
                  >
                    Abrir en nueva pestaña / Descargar
                  </a>
                </div>
              </div>
              
              <div className="text-center pb-4 shrink-0">
                <p className="text-lg font-black text-white uppercase tracking-tight">{previewEvidence.fileName}</p>
                <div className="flex items-center justify-center gap-3 mt-2">
                  <span className="px-3 py-1 bg-white/5 rounded-lg border border-white/10 text-[10px] font-black text-zinc-400 uppercase tracking-widest">
                    Comprobante de Ingresos
                  </span>
                </div>

                {!previewEvidence.approved && !previewEvidence.rejectionReason && (
                  <div className="flex items-center justify-center gap-4 mt-6">
                    <button
                      onClick={() => {
                        onEvidenceStatusUpdate(previewEvidence.id, true);
                        setPreviewEvidence(null);
                      }}
                      className="px-6 py-3 bg-success/20 border border-success/30 text-success rounded-xl font-black text-[10px] uppercase tracking-[0.2em] hover:bg-success/30 transition-all hover:scale-105 active:scale-95"
                    >
                      Aprobar Documento
                    </button>
                    <button
                      onClick={() => setObservingEvidence(previewEvidence)}
                      className="px-6 py-3 bg-danger/20 border border-danger/30 text-danger rounded-xl font-black text-[10px] uppercase tracking-[0.2em] hover:bg-danger/30 transition-all hover:scale-105 active:scale-95"
                    >
                      Solicitar Corrección
                    </button>
                  </div>
                )}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <ObservationModal
        key={observingEvidence?.id || "none"}
        isOpen={observingEvidence !== null}
        onClose={() => setObservingEvidence(null)}
        evidenceName={observingEvidence?.fileName || ""}
        evidenceType={observingEvidence?.documentType || ""}
        onConfirm={handleConfirmObservation}
      />
    </div>
  );
}

function InfoCard({ label, value, icon }: { label: string, value?: string, icon: React.ReactNode }) {
  return (
    <div className="p-4 rounded-2xl bg-surface border border-border flex items-center gap-4 group hover:border-border-strong transition-all">
      <div className="w-10 h-10 rounded-xl bg-background border border-border flex items-center justify-center text-zinc-500 group-hover:text-accent transition-colors">
        {icon}
      </div>
      <div>
        <p className="text-[9px] font-black text-zinc-500 uppercase tracking-widest">{label}</p>
        <p className="text-xs font-bold text-white mt-0.5">{value || "S/D"}</p>
      </div>
    </div>
  );
}

function StatusMiniCard({ icon, label, value }: { icon: React.ReactNode, label: string, value: string }) {
  return (
    <div className="p-4 rounded-xl bg-background border border-border flex items-center gap-3">
      <div className="text-zinc-500">{icon}</div>
      <div>
        <span className="block text-[8px] font-black text-zinc-600 uppercase tracking-widest leading-none">{label}</span>
        <span className="text-[10px] font-bold text-zinc-300 mt-1">{value}</span>
      </div>
    </div>
  );
}
