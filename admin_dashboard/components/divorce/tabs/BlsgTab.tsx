"use client";

import { useState } from "react";
import { Expediente } from "@/lib/types/divorce";
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
  Plus
} from "@phosphor-icons/react";
import { motion, AnimatePresence } from "framer-motion";
import { cn } from "@/lib/utils";

interface BlsgTabProps {
  expediente: Expediente;
  onDecision: (approved: boolean, observations: string) => void;
}

export function BlsgTab({ expediente, onDecision }: BlsgTabProps) {
  const profile = expediente.socioEconomicProfile;
  const [showLightbox, setShowLightbox] = useState(false);
  const [observations, setObservations] = useState(profile?.blsgObservations || "");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isEditing, setIsEditing] = useState(false);

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

  const handleDecision = async (approved: boolean) => {
    setIsSubmitting(true);
    await onDecision(approved, observations);
    setIsSubmitting(false);
  };

  return (
    <div className="space-y-8 pb-20">
      {/* SECCIÓN 1: Identidad Verificada (Scraping) */}
      <section className="space-y-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-accent/10 border border-accent/20 text-accent">
              <IdentificationCard size={20} weight="duotone" />
            </div>
            <h5 className="text-[10px] uppercase tracking-[0.2em] text-zinc-500 font-black">Identidad Verificada (Portal Judicial)</h5>
          </div>
          
          {profile.certificatePath && (
            <button 
              onClick={() => setShowLightbox(true)}
              className="flex items-center gap-2 px-4 py-2 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-zinc-400 hover:text-white transition-all text-[10px] font-black uppercase tracking-widest"
            >
              <FilePdf size={16} weight="duotone" className="text-danger" />
              Ver Constancia Judicial
            </button>
          )}
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <InfoCard label="Nombre Completo" value={profile.scrapingFullName} icon={<User />} />
          <InfoCard label="DNI" value={profile.scrapingDni} icon={<IdentificationCard />} />
          <InfoCard label="CUIL" value={profile.scrapingCuil} icon={<IdentificationCard />} />
          <InfoCard label="Fecha Nac." value={profile.scrapingBirthDate} icon={<Calendar />} />
          <InfoCard label="Provincia" value={profile.scrapingProvince} icon={<MapPin />} />
          <InfoCard label="Sexo" value={profile.scrapingSex} icon={<GenderIntersex />} />
        </div>
      </section>

      {/* SECCIÓN 2: Evaluación Patrimonial (WhatsApp + Scraping) */}
      <section className="space-y-6">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-success/10 border border-success/20 text-success">
            <CurrencyDollar size={20} weight="duotone" />
          </div>
          <h5 className="text-[10px] uppercase tracking-[0.2em] text-zinc-500 font-black">Evaluación Patrimonial y Socioeconómica</h5>
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
              <StatusMiniCard icon={<House />} label="Vivienda" value={profile.housingType || "S/D"} />
              <StatusMiniCard icon={<Briefcase />} label="Empleo" value={profile.hasFormalEmployment ? "Formal" : "Informal"} />
            </div>
            <StatusMiniCard icon={<Car />} label="Vehículos" value={`${profile.vehiclesRegistered || 0} Registrados`} />
          </div>

          <div className="col-span-12 lg:col-span-8 p-8 rounded-2xl bg-background border border-border flex flex-col h-full">
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
                  {profile.blsgScrapingResult || "PENDIENTE"}
                </h6>
                <p className="text-sm text-zinc-400 leading-relaxed italic">
                  &quot;{profile.scrapingJustification || profile.blsgObservations || "Sin observaciones detalladas."}&quot;
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* SECCIÓN 3: Decisión de la Defensoría */}
      <section className="space-y-6 pt-6 border-t border-border">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-warning/10 border border-warning/20 text-warning">
            <ShieldCheck size={20} weight="duotone" />
          </div>
          <h5 className="text-[10px] uppercase tracking-[0.2em] text-zinc-500 font-black">Fase 3: Decisión de la Defensoría (HITL)</h5>
        </div>

        {profile.blsgApprovedByDefensoria === null || isEditing ? (
          <div className="p-8 rounded-3xl bg-surface border border-border space-y-6">
            <div className="space-y-3">
              <label className="text-[10px] uppercase tracking-widest font-black text-zinc-500">Observaciones del Defensor</label>
              <textarea 
                value={observations}
                onChange={(e) => setObservations(e.target.value)}
                placeholder="Escriba aquí los motivos de la decisión final para informar al ciudadano..."
                className="w-full bg-background border border-border rounded-2xl p-4 text-sm text-white focus:outline-none focus:border-accent/50 min-h-[120px] transition-all"
              />
            </div>
            
            <div className="flex gap-4">
              <button
                disabled={isSubmitting}
                onClick={() => handleDecision(true)}
                className="flex-1 h-16 rounded-2xl bg-success text-white font-black text-[12px] uppercase tracking-[0.2em] hover:bg-success/90 shadow-xl shadow-success/20 transition-all flex items-center justify-center gap-3 active:scale-[0.98]"
              >
                {isSubmitting ? (
                  <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                ) : (
                  <>
                    <CheckCircle size={20} weight="fill" />
                    Aprobar BLSG y Tramitar
                  </>
                )}
              </button>
              <button
                disabled={isSubmitting}
                onClick={() => handleDecision(false)}
                className="flex-1 h-16 rounded-2xl bg-danger/10 border border-danger/20 text-danger font-black text-[12px] uppercase tracking-[0.2em] hover:bg-danger/20 transition-all flex items-center justify-center gap-3 active:scale-[0.98]"
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
                  Patrocinio {profile.blsgApprovedByDefensoria ? "APROBADO" : "RECHAZADO"}
                </h4>
                <button 
                  onClick={() => setIsEditing(true)}
                  className="text-[10px] text-zinc-500 underline hover:text-white uppercase tracking-widest font-black"
                >
                  Cambiar decisión
                </button>
              </div>
              <p className="text-sm text-zinc-400 leading-relaxed italic">
                &quot;{profile.blsgObservations || "Sin observaciones adicionales."}&quot;
              </p>
            </div>
          </div>
        )}
      </section>

      {/* LIGHTBOX PARA CONSTANCIA */}
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
                     className="inline-flex px-8 py-3 bg-accent text-white rounded-xl font-black text-[11px] uppercase tracking-[0.2em] shadow-xl shadow-accent/20 hover:scale-105 transition-all"
                   >
                     Abrir en nueva pestaña / Descargar
                   </a>
                 </div>
              </div>
              
              <div className="text-center pb-4">
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
    </div>
  );
}

function InfoCard({ label, value, icon }: { label: string, value?: string, icon: React.ReactNode }) {
  return (
    <div className="p-4 rounded-2xl bg-surface border border-border flex items-center gap-4 group hover:border-border-strong transition-all">
      <div className="w-10 h-10 rounded-xl bg-background border border-border flex items-center justify-center text-zinc-500 group-hover:text-amber-400 transition-colors">
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
