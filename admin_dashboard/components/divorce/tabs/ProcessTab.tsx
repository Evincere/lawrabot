"use client";

import { Expediente } from "@/lib/types/divorce";
import { 
  NotePencil, 
  CheckCircle,
  ShieldCheck,
  WarningCircle,
  FileDoc,
  FilePdf,
  ArrowsClockwise,
  ListChecks,
  CalendarCheck,
  Bank,
  Check,
  X,
  Hourglass
} from "@phosphor-icons/react";
import { cn } from "@/lib/utils";
import { useState } from "react";
import { API_CONFIG } from "@/lib/config/api";

interface ProcessTabProps {
  expediente: Expediente;
  onGenerateDraft: (asPdf: boolean) => void;
  onFetchCases: () => void;
}

export function ProcessTab({
  expediente,
  onGenerateDraft,
  onFetchCases
}: ProcessTabProps) {

  // 1. Pre-Validación (Checklist)
  const isProfileComplete = expediente.petitioner?.fullName && expediente.petitioner?.dni && expediente.respondent?.fullName;
  const isMarriageDataComplete = expediente.marriageCertificateNumber && expediente.marriageRegistryBook;
  const isBlsgApproved = expediente.socioEconomicProfile?.blsgApprovedByDefensoria === true;
  // TODO: Add logic to check regulatory agreement and evidence
  const isEvidenceComplete = true; // Placeholder
  const isAgreementComplete = expediente.rawAgreementText ? true : false;

  const allValidationsPassed = isProfileComplete && isMarriageDataComplete && isBlsgApproved && isEvidenceComplete && isAgreementComplete;

  const [isApproving, setIsApproving] = useState(false);

  const handleApproveCase = async () => {
    // Aquí idealmente abriríamos un modal para cargar los datos del acta si no están, 
    // pero por ahora asumiremos que se hace en la pestaña Evidence o aquí directamente.
    // Lógica para enviar la aprobación al backend
    setIsApproving(true);
    try {
      // POST API_CONFIG.ENDPOINTS.APPROVE_CASE
      await fetch(API_CONFIG.BASE_URL + API_CONFIG.ENDPOINTS.APPROVE_CASE(expediente.id), {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          certificateNumber: expediente.marriageCertificateNumber || "S/D",
          registryBook: expediente.marriageRegistryBook || "S/D",
          registryPage: expediente.marriageRegistryPage || "S/D",
          registryOffice: expediente.marriageRegistryOffice || "S/D",
          place: expediente.marriagePlace || "S/D"
        })
      });
      onFetchCases();
    } catch (error) {
      console.error(error);
    } finally {
      setIsApproving(false);
    }
  };

  const steps = [
    {
      title: "Pre-Validación",
      icon: <ListChecks size={24} weight="duotone" />,
      status: expediente.status === "DRAFT" || expediente.status === "DATA_COLLECTION_IN_PROGRESS" ? "current" : "completed",
      content: (
        <div className="space-y-4 text-sm mt-4">
          <div className="flex items-center gap-3">
            {isProfileComplete ? <CheckCircle size={20} className="text-success" /> : <X size={20} className="text-error" />}
            <span className={isProfileComplete ? "text-zinc-300" : "text-zinc-500"}>Datos personales completos</span>
          </div>
          <div className="flex items-center gap-3">
            {isMarriageDataComplete ? <CheckCircle size={20} className="text-success" /> : <WarningCircle size={20} className="text-warning" />}
            <span className={isMarriageDataComplete ? "text-zinc-300" : "text-zinc-500"}>Datos del matrimonio y acta (requiere carga manual)</span>
          </div>
          <div className="flex items-center gap-3">
            {isBlsgApproved ? <CheckCircle size={20} className="text-success" /> : <WarningCircle size={20} className="text-warning" />}
            <span className={isBlsgApproved ? "text-zinc-300" : "text-zinc-500"}>BLSG visado por Defensoría</span>
          </div>
          <div className="flex items-center gap-3">
            {isAgreementComplete ? <CheckCircle size={20} className="text-success" /> : <X size={20} className="text-error" />}
            <span className={isAgreementComplete ? "text-zinc-300" : "text-zinc-500"}>Propuesta Reguladora validada</span>
          </div>
          <div className="flex items-center gap-3">
            {isEvidenceComplete ? <CheckCircle size={20} className="text-success" /> : <X size={20} className="text-error" />}
            <span className={isEvidenceComplete ? "text-zinc-300" : "text-zinc-500"}>Evidencia documental mínima cargada</span>
          </div>
          
          {(expediente.status === "DRAFT" || expediente.status === "DATA_COLLECTION_IN_PROGRESS") && (
            <button 
              onClick={handleApproveCase}
              disabled={!allValidationsPassed || isApproving}
              className={cn(
                "mt-6 py-3 px-6 rounded-xl font-bold text-xs uppercase tracking-wider transition-all",
                allValidationsPassed 
                  ? "bg-success text-white hover:bg-success/90 shadow-lg shadow-success/20"
                  : "bg-zinc-800 text-zinc-500 cursor-not-allowed"
              )}
            >
              {isApproving ? "Aprobando..." : "Aprobar Expediente"}
            </button>
          )}
        </div>
      )
    },
    {
      title: "Generación Documental",
      icon: <NotePencil size={24} weight="duotone" />,
      status: expediente.status === "DATA_COMPLETE" ? "current" : (expediente.status !== "DRAFT" && expediente.status !== "DATA_COLLECTION_IN_PROGRESS" ? "completed" : "pending"),
      content: (
        <div className="mt-4 flex flex-col gap-4">
          <p className="text-sm text-zinc-400">Construye la demanda pro-forma con la información validada en este expediente.</p>
          <button 
            onClick={() => onGenerateDraft(false)}
            disabled={expediente.status === "DRAFT" || expediente.status === "DATA_COLLECTION_IN_PROGRESS"}
            className="w-max py-3 px-6 rounded-xl bg-accent text-white font-bold text-xs uppercase tracking-wider hover:bg-accent/90 transition-all shadow-lg shadow-accent/20 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Generar Demanda (.DOCX)
          </button>
        </div>
      )
    },
    {
      title: "Firma del Interesado",
      icon: <FileDoc size={24} weight="duotone" />,
      status: expediente.status === "WAITING_SIGNATURE" ? "current" : (expediente.status === "READY_FOR_PORTAL" ? "completed" : "pending"),
      content: (
        <div className="mt-4 flex flex-col gap-4">
          {expediente.status === "WAITING_SIGNATURE" ? (
             <div className="p-4 rounded-xl bg-warning/10 border border-warning/20">
               <div className="flex items-start gap-3">
                 <Hourglass size={20} className="text-warning animate-pulse mt-0.5" />
                 <div>
                   <p className="text-sm text-warning font-semibold">Esperando Firma</p>
                   <p className="text-xs text-zinc-400 mt-1">El documento ha sido generado. Deposite el PDF firmado en la carpeta compartida o súbalo manualmente.</p>
                   <button 
                      onClick={onFetchCases}
                      className="mt-3 flex items-center gap-2 text-xs text-zinc-300 hover:text-white transition-colors"
                    >
                      <ArrowsClockwise size={14} /> Forzar escaneo de archivos
                    </button>
                 </div>
               </div>
             </div>
          ) : expediente.status === "READY_FOR_PORTAL" ? (
            <p className="text-sm text-success flex items-center gap-2"><CheckCircle size={16} /> Demanda firmada y lista.</p>
          ) : (
            <p className="text-sm text-zinc-500">Pendiente de generación de documento.</p>
          )}
        </div>
      )
    },
    {
      title: "Gestión de Agenda y Citación",
      icon: <CalendarCheck size={24} weight="duotone" />,
      status: expediente.status === "READY_FOR_PORTAL" ? "current" : "pending", // Adjust as per actual workflow
      content: (
        <div className="mt-4 flex flex-col gap-4">
          <p className="text-sm text-zinc-400">Asigne un turno para la firma presencial o permita que LawraBot contacte al usuario ofreciendo opciones.</p>
          <div className="flex gap-4">
            <button className="py-3 px-6 rounded-xl bg-surface border border-border text-zinc-300 font-bold text-xs uppercase tracking-wider hover:bg-zinc-800 transition-all disabled:opacity-50">
              Ver Agenda
            </button>
            <button className="py-3 px-6 rounded-xl bg-success/20 text-success font-bold text-xs uppercase tracking-wider hover:bg-success/30 transition-all disabled:opacity-50">
              Notificar por WhatsApp
            </button>
          </div>
        </div>
      )
    },
    {
      title: "Presentación Judicial",
      icon: <Bank size={24} weight="duotone" />,
      status: "pending",
      content: (
        <div className="mt-4 flex flex-col gap-4">
          <p className="text-sm text-zinc-500">Subir el expediente consolidado al portal del Poder Judicial.</p>
          <button disabled className="w-max py-3 px-6 rounded-xl bg-zinc-800 text-zinc-500 font-bold text-xs uppercase tracking-wider cursor-not-allowed">
            Presentar ante Juzgado (Próximamente)
          </button>
        </div>
      )
    }
  ];

  return (
    <div className="max-w-4xl mx-auto space-y-8 pb-10">
      <div className="flex items-center gap-3 mb-8">
         <div className="p-2 rounded-xl bg-accent/10 border border-accent/20">
           <ShieldCheck size={24} weight="duotone" className="text-accent" />
         </div>
         <h2 className="text-xl font-black tracking-wide">Centro de Operaciones</h2>
      </div>
      
      <div className="relative border-l-2 border-border ml-6 space-y-12">
        {steps.map((step, index) => (
          <div key={index} className="relative pl-10">
            <div className={cn(
              "absolute -left-[25px] top-0 w-12 h-12 rounded-full flex items-center justify-center border-4 border-background transition-colors duration-500",
              step.status === "completed" ? "bg-success text-black" :
              step.status === "current" ? "bg-accent text-white shadow-[0_0_20px_rgba(var(--accent),0.3)]" :
              "bg-surface text-zinc-500 border-border"
            )}>
              {step.status === "completed" ? <Check size={20} weight="bold" /> : step.icon}
            </div>
            
            <div className={cn(
              "p-6 rounded-2xl border transition-all duration-500",
              step.status === "current" ? "bg-surface border-accent/30 shadow-lg" : 
              step.status === "completed" ? "bg-background border-border opacity-70" :
              "bg-background border-border/50 opacity-50"
            )}>
              <h3 className={cn(
                "text-lg font-bold tracking-wide",
                step.status === "current" ? "text-white" : "text-zinc-400"
              )}>
                {index + 1}. {step.title}
              </h3>
              {step.content}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
