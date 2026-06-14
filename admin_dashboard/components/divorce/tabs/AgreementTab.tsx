"use client";

import { useState, useEffect } from "react";
import { Expediente, RegulatoryAgreement } from "@/lib/types/divorce";
import { 
  FileText, 
  Quotes,
  Calendar,
  MapPin,
  PencilSimple,
  Check,
  X,
  HeartStraight,
  CurrencyDollar,
  House,
  Scales,
  Warning,
  ShieldCheck,
  ArrowsClockwise
} from "@phosphor-icons/react";
import { cn } from "@/lib/utils";
import { API_CONFIG } from "@/lib/config/api";

interface AgreementTabProps {
  expediente: Expediente;
  isEditing: boolean;
  setIsEditing: (val: boolean) => void;
  isSaving: boolean;
  onSave: (text: string) => void;
}

export function AgreementTab({
  expediente,
  isEditing,
  setIsEditing,
  isSaving,
  onSave
}: AgreementTabProps) {
  // Estado local para alertas de validación legal
  const [validationAlerts, setValidationAlerts] = useState<string[]>([]);
  const [isValidating, setIsValidating] = useState(false);
  const [saveStatus, setSaveStatus] = useState<"idle" | "saving" | "success" | "error">("idle");
  const [isUpdatingStatus, setIsUpdatingStatus] = useState(false);
  
  // Estado de edición estructurada del convenio tipado de acuerdo a divorce.ts
  const [isStructuredEditing, setIsStructuredEditing] = useState(false);
  const [agreement, setAgreement] = useState<RegulatoryAgreement>(
    expediente.regulatoryAgreement || {
      status: "PROPOSED",
      personalCare: { careType: "SHARED_INDISTINCT", mainResidence: "PETITIONER" },
      communicationRegime: { regimeType: "BROAD_AND_FLEXIBLE", regularSchedule: "", holidaySchedule: "" },
      alimonyProvision: {
        provisionType: "MONETARY",
        amount: { value: 0, currencyOrParameter: "ARS" },
        paymentFrequency: "MONTHLY",
        paymentMethod: "BANK_TRANSFER",
        paymentDetails: "",
        updateMechanism: "IPC_INDEX"
      },
      assetDistribution: { homeAttributionTo: "PETITIONER", assetsSummary: "", liabilitiesSummary: "" },
      economicCompensation: {
        appliesEconomicCompensation: false,
        beneficiary: "PETITIONER",
        imbalanceJustification: "",
        paymentMethod: "SINGLE_PAYMENT",
        compensationAmount: { value: 0, currencyOrParameter: "ARS" },
        installmentsCount: 1
      }
    }
  );

  // Cargar alertas de validación legal del convenio al montar la pestaña
  const fetchValidationAlerts = async () => {
    setIsValidating(true);
    try {
      const response = await fetch(`${API_CONFIG.BASE_URL}/api/divorce/cases/${expediente.id}/validate-agreement`, {
        method: "POST"
      });
      if (response.ok) {
        const alerts = await response.json();
        setValidationAlerts(alerts);
      }
    } catch (error) {
      console.error("Error al validar convenio:", error);
    } finally {
      setIsValidating(false);
    }
  };

  useEffect(() => {
    fetchValidationAlerts();
  }, [expediente.id]);

  // Actualizar estado del convenio (PROPOSED -> ACCEPTED, etc.)
  const handleUpdateStatus = async (newStatus: "PROPOSED" | "ACCEPTED") => {
    setIsUpdatingStatus(true);
    try {
      const params = new URLSearchParams({ status: newStatus });
      const response = await fetch(`${API_CONFIG.BASE_URL}/api/divorce/cases/${expediente.id}/agreement-status?${params}`, {
        method: "PUT"
      });
      if (response.ok) {
        // Recargar la página o forzar actualización para reflejar el cambio
        window.location.reload();
      }
    } catch (error) {
      console.error("Error al actualizar estado del convenio:", error);
    } finally {
      setIsUpdatingStatus(false);
    }
  };

  // Guardar convenio estructurado completo realizando el mapeo correcto al DTO plano del backend
  const handleSaveStructuredAgreement = async () => {
    setSaveStatus("saving");
    
    // Transformación robusta: frontend Types (React) -> backend DTO (Java Spring Boot)
    const requestBody = {
      personalCare: {
        careType: agreement.personalCare?.careType === "SHARED_INDISTINCT" ? "SHARED_INDISTINGUISHED" : 
                  agreement.personalCare?.careType === "SHARED_ALTERNATED" ? "SHARED_ALTERNATING" : 
                  agreement.personalCare?.careType === "UNILATERAL_PETITIONER" ? "UNIPERSONAL" :
                  agreement.personalCare?.careType === "UNILATERAL_RESPONDENT" ? "UNIPERSONAL" : "SHARED_INDISTINGUISHED",
        mainResidence: agreement.personalCare?.mainResidence === "BOTH_EQUITABLE" ? "SHARED" : agreement.personalCare?.mainResidence
      },
      communicationRegime: {
        regimeType: agreement.communicationRegime?.regimeType === "BROAD_AND_FLEXIBLE" ? "FREE_OR_FLEXIBLE" :
                    agreement.communicationRegime?.regimeType === "SPECIFIC_SCHEDULE" ? "STRUCTURED" : 
                    agreement.communicationRegime?.regimeType === "RESTRICTED_SUPERVISED" ? "SUPERVISED" : "FREE_OR_FLEXIBLE",
        regularSchedule: agreement.communicationRegime?.regularSchedule,
        holidaySchedule: agreement.communicationRegime?.holidaySchedule
      },
      alimonyProvision: {
        provisionType: agreement.alimonyProvision?.provisionType,
        amountValue: agreement.alimonyProvision?.amount?.value,
        amountCurrency: agreement.alimonyProvision?.amount?.currencyOrParameter,
        customParameter: agreement.alimonyProvision?.amount?.customParameter,
        paymentFrequency: agreement.alimonyProvision?.paymentFrequency,
        paymentMethod: agreement.alimonyProvision?.paymentMethod,
        paymentDetails: agreement.alimonyProvision?.paymentDetails,
        updateMechanism: agreement.alimonyProvision?.updateMechanism === "IPC_INDEX" ? "IPC_INDEX" :
                         agreement.alimonyProvision?.updateMechanism === "SALARY_PARITY" ? "SALARY_PARITY" :
                         agreement.alimonyProvision?.updateMechanism === "SMVM_PERCENTAGE" ? "SMVM_PERCENTAGE" : "NONE"
      },
      assetDistribution: {
        homeAttributionTo: agreement.assetDistribution?.homeAttributionTo === "BOTH_SALE" ? "BOTH_SALE" :
                           agreement.assetDistribution?.homeAttributionTo === "PETITIONER" ? "PETITIONER" :
                           agreement.assetDistribution?.homeAttributionTo === "RESPONDENT" ? "RESPONDENT" : "OTHER",
        assetsSummary: agreement.assetDistribution?.assetsSummary,
        liabilitiesSummary: agreement.assetDistribution?.liabilitiesSummary
      },
      economicCompensation: {
        appliesEconomicCompensation: agreement.economicCompensation?.appliesEconomicCompensation,
        beneficiary: agreement.economicCompensation?.beneficiary,
        imbalanceJustification: agreement.economicCompensation?.imbalanceJustification,
        paymentMethod: agreement.economicCompensation?.paymentMethod,
        compensationAmountValue: agreement.economicCompensation?.compensationAmount?.value,
        compensationAmountCurrency: agreement.economicCompensation?.compensationAmount?.currencyOrParameter,
        compensationCustomParameter: agreement.economicCompensation?.compensationAmount?.customParameter,
        installmentsCount: agreement.economicCompensation?.installmentsCount
      }
    };

    try {
      const response = await fetch(`${API_CONFIG.BASE_URL}/api/divorce/cases/${expediente.id}/agreement`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(requestBody)
      });
      if (response.ok) {
        setSaveStatus("success");
        setIsStructuredEditing(false);
        fetchValidationAlerts();
        // Recargar datos locales del expediente
        setTimeout(() => {
          window.location.reload();
        }, 1000);
      } else {
        setSaveStatus("error");
      }
    } catch (error) {
      console.error("Error al guardar convenio estructurado:", error);
      setSaveStatus("error");
    }
  };

  return (
    <div className="space-y-10">
      {/* ① PANEL DE CABECERA: Estado Global y Alertas de Validación Legal */}
      <section className="p-8 rounded-3xl bg-surface border border-border flex flex-col md:flex-row gap-6 justify-between items-start md:items-center">
        <div>
          <span className="text-[9px] font-black text-zinc-500 uppercase tracking-widest">Encuadre Legal y Estado del Convenio</span>
          <div className="flex items-center gap-3 mt-2">
            <h4 className="text-xl font-bold tracking-tight text-white">Convenio Regulador (Art. 439)</h4>
            <span className={cn(
              "px-3 py-1 rounded-full text-[10px] font-black tracking-widest uppercase border",
              agreement.status === "ACCEPTED" 
                ? "bg-success/10 text-success border-success/20" 
                : "bg-warning/10 text-warning border-warning/20"
            )}>
              {agreement.status === "ACCEPTED" ? "Aceptado y Validado" : "Propuesto para Control"}
            </span>
          </div>
        </div>

        <div className="flex gap-3 shrink-0">
          <button 
            onClick={fetchValidationAlerts}
            disabled={isValidating}
            className="flex items-center gap-2 px-4 py-2 rounded-xl bg-surface hover:bg-background border border-border text-[10px] font-black uppercase tracking-widest text-zinc-300 transition-all"
          >
            <ArrowsClockwise size={14} className={cn("text-zinc-400", isValidating && "animate-spin")} />
            {isValidating ? "Validando..." : "Re-Validar"}
          </button>

          {!isStructuredEditing && (
            <button 
              onClick={() => setIsStructuredEditing(true)}
              className="flex items-center gap-2 px-4 py-2 rounded-xl bg-accent/10 hover:bg-accent/20 text-accent border border-accent/20 text-[10px] font-black uppercase tracking-widest transition-all"
            >
              <PencilSimple size={14} weight="bold" />
              Editar Estructurado
            </button>
          )}

          {agreement.status !== "ACCEPTED" && (
            <button 
              onClick={() => handleUpdateStatus("ACCEPTED")}
              disabled={isUpdatingStatus}
              className="flex items-center gap-2 px-6 py-2 rounded-xl bg-success hover:bg-success/90 text-white text-[10px] font-black uppercase tracking-widest transition-all shadow-lg shadow-success/20"
            >
              {isUpdatingStatus ? (
                <div className="w-3 h-3 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <ShieldCheck size={14} weight="bold" />
              )}
              Aprobar y Firmar
            </button>
          )}
        </div>
      </section>

      {/* PANEL DE ALERTAS LEGALES (Si existen advertencias o alertas de inconsistencia) */}
      {validationAlerts.length > 0 ? (
        <section className="p-6 rounded-3xl bg-danger/5 border border-danger/20">
          <div className="flex items-center gap-3 mb-4 text-danger">
            <Warning size={20} weight="duotone" />
            <h6 className="text-[11px] font-black uppercase tracking-widest">Advertencias del Auditor Legal Automatizado</h6>
          </div>
          <ul className="space-y-2">
            {validationAlerts.map((alert, i) => (
              <li key={i} className="text-xs text-zinc-300 flex items-start gap-2 leading-relaxed">
                <span className="w-1.5 h-1.5 rounded-full bg-danger shrink-0 mt-1.5" />
                <span>{alert}</span>
              </li>
            ))}
          </ul>
        </section>
      ) : (
        <section className="p-6 rounded-3xl bg-success/5 border border-success/20 flex items-center gap-4">
          <div className="p-2 rounded-xl bg-success/10 text-success border border-success/20 shrink-0">
            <ShieldCheck size={20} weight="duotone" />
          </div>
          <div>
            <h6 className="text-[10px] font-black uppercase tracking-widest text-success">Convenio sin Inconsistencias Críticas</h6>
            <p className="text-xs text-zinc-400 mt-0.5">El sistema no detectó omisiones legales respecto al cuidado de los hijos, cuota alimentaria o compensación económica en base a las manifestaciones recibidas.</p>
          </div>
        </section>
      )}

      {/* FORMULARIO ESTRUCTURADO EN MODO VISUALIZACIÓN / EDICIÓN */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        
        {/* ② Responsabilidad Parental (Cuidado Personal + Régimen Comunicacional) */}
        <div className="p-8 rounded-3xl bg-surface border border-border flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-3 mb-6">
              <div className="p-2 rounded-xl bg-accent/10 border border-accent/20">
                <HeartStraight size={20} weight="duotone" className="text-accent" />
              </div>
              <h5 className="text-[10px] uppercase tracking-[0.2em] text-zinc-400 font-black">① Cuidado y Responsabilidad Parental</h5>
            </div>

            {isStructuredEditing ? (
              <div className="space-y-4">
                <div>
                  <label className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Modalidad de Cuidado Personal (Art. 649)</label>
                  <select 
                    value={agreement.personalCare?.careType || "SHARED_INDISTINCT"}
                    onChange={(e) => setAgreement({
                      ...agreement,
                      personalCare: { ...agreement.personalCare, careType: e.target.value as any }
                    })}
                    className="w-full bg-background border border-border text-xs text-white rounded-xl p-3 outline-none"
                  >
                    <option value="SHARED_INDISTINCT">Compartido Indistinto (Recomendado CCCN)</option>
                    <option value="SHARED_ALTERNATED">Compartido Alternado</option>
                    <option value="UNILATERAL_PETITIONER">Unilateral Peticionante</option>
                    <option value="UNILATERAL_RESPONDENT">Unilateral Demandado</option>
                  </select>
                </div>

                <div>
                  <label className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Residencia Principal</label>
                  <select 
                    value={agreement.personalCare?.mainResidence || "BOTH_EQUITABLE"}
                    onChange={(e) => setAgreement({
                      ...agreement,
                      personalCare: { ...agreement.personalCare, mainResidence: e.target.value as any }
                    })}
                    className="w-full bg-background border border-border text-xs text-white rounded-xl p-3 outline-none"
                  >
                    <option value="PETITIONER">Domicilio del Peticionante</option>
                    <option value="RESPONDENT">Domicilio del Demandado</option>
                    <option value="BOTH_EQUITABLE">Compartida / Ambos Domicilios</option>
                  </select>
                </div>

                <div>
                  <label className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Régimen de Comunicación</label>
                  <select 
                    value={agreement.communicationRegime?.regimeType || "BROAD_AND_FLEXIBLE"}
                    onChange={(e) => setAgreement({
                      ...agreement,
                      communicationRegime: { ...agreement.communicationRegime, regimeType: e.target.value as any }
                    })}
                    className="w-full bg-background border border-border text-xs text-white rounded-xl p-3 outline-none"
                  >
                    <option value="BROAD_AND_FLEXIBLE">Libre y Flexible (Según necesidad de los hijos)</option>
                    <option value="SPECIFIC_SCHEDULE">Estructurado (Días y horarios pre-pactados)</option>
                    <option value="RESTRICTED_SUPERVISED">Supervisado / Con Acompañante</option>
                  </select>
                </div>

                <div>
                  <label className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Cronograma Regular (Días y Horarios)</label>
                  <textarea 
                    value={agreement.communicationRegime?.regularSchedule || ""}
                    onChange={(e) => setAgreement({
                      ...agreement,
                      communicationRegime: { ...agreement.communicationRegime, regularSchedule: e.target.value }
                    })}
                    rows={2}
                    placeholder="Ej: Fines de semana alternados y dos tardes entre semana de 17:00 a 20:00 hs."
                    className="w-full bg-background border border-border text-xs text-white rounded-xl p-3 outline-none resize-none"
                  />
                </div>
              </div>
            ) : (
              <div className="space-y-4">
                <div className="p-4 rounded-xl bg-background border border-border">
                  <span className="text-[8px] font-black text-zinc-500 uppercase tracking-widest">Modalidad de Cuidado</span>
                  <p className="text-xs font-bold text-white mt-1">
                    {agreement.personalCare?.careType === "SHARED_INDISTINCT" && "Compartido Indistinto"}
                    {agreement.personalCare?.careType === "SHARED_ALTERNATED" && "Compartido Alternado"}
                    {agreement.personalCare?.careType === "UNILATERAL_PETITIONER" && "Unilateral Peticionante"}
                    {agreement.personalCare?.careType === "UNILATERAL_RESPONDENT" && "Unilateral Demandado"}
                    {!agreement.personalCare?.careType && "No declarado / Sin hijos menores"}
                  </p>
                </div>

                <div className="p-4 rounded-xl bg-background border border-border">
                  <span className="text-[8px] font-black text-zinc-500 uppercase tracking-widest">Residencia Principal</span>
                  <p className="text-xs font-bold text-white mt-1">
                    {agreement.personalCare?.mainResidence === "PETITIONER" && "Domicilio del Peticionante"}
                    {agreement.personalCare?.mainResidence === "RESPONDENT" && "Domicilio del Demandado"}
                    {agreement.personalCare?.mainResidence === "BOTH_EQUITABLE" && "Compartida / Alternada"}
                    {!agreement.personalCare?.mainResidence && "No declarada"}
                  </p>
                </div>

                <div className="p-4 rounded-xl bg-background border border-border">
                  <span className="text-[8px] font-black text-zinc-500 uppercase tracking-widest">Régimen Comunicacional</span>
                  <p className="text-xs font-bold text-white mt-1">
                    {agreement.communicationRegime?.regimeType === "BROAD_AND_FLEXIBLE" && "Libre y Flexible"}
                    {agreement.communicationRegime?.regimeType === "SPECIFIC_SCHEDULE" && "Estructurado con días y horarios preestablecidos"}
                    {agreement.communicationRegime?.regimeType === "RESTRICTED_SUPERVISED" && "Supervisado"}
                    {!agreement.communicationRegime?.regimeType && "No declarado"}
                  </p>
                  {agreement.communicationRegime?.regularSchedule && (
                    <p className="text-xs text-zinc-400 mt-2 italic font-serif">
                      "{agreement.communicationRegime.regularSchedule}"
                    </p>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* ③ Cuota Alimentaria */}
        <div className="p-8 rounded-3xl bg-surface border border-border flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-3 mb-6">
              <div className="p-2 rounded-xl bg-accent/10 border border-accent/20">
                <CurrencyDollar size={20} weight="duotone" className="text-accent" />
              </div>
              <h5 className="text-[10px] uppercase tracking-[0.2em] text-zinc-400 font-black">② Cuota Alimentaria (Art. 658)</h5>
            </div>

            {isStructuredEditing ? (
              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Tipo de Prestación</label>
                    <select 
                      value={agreement.alimonyProvision?.provisionType || "MONETARY"}
                      onChange={(e) => setAgreement({
                        ...agreement,
                        alimonyProvision: { ...agreement.alimonyProvision, provisionType: e.target.value as any }
                      })}
                      className="w-full bg-background border border-border text-xs text-white rounded-xl p-3 outline-none"
                    >
                      <option value="MONETARY">Monetaria (Efectivo)</option>
                      <option value="IN_KIND">En Especie (Pago Directo)</option>
                      <option value="MIXED">Mixta (Efectivo + Especie)</option>
                      <option value="OTHER">Sin Prestación</option>
                    </select>
                  </div>

                  <div>
                    <label className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Frecuencia</label>
                    <select 
                      value={agreement.alimonyProvision?.paymentFrequency || "MONTHLY"}
                      onChange={(e) => setAgreement({
                        ...agreement,
                        alimonyProvision: { ...agreement.alimonyProvision, paymentFrequency: e.target.value as any }
                      })}
                      className="w-full bg-background border border-border text-xs text-white rounded-xl p-3 outline-none"
                    >
                      <option value="MONTHLY">Mensual</option>
                      <option value="WEEKLY">Semanal</option>
                      <option value="FORTNIGHTLY">Quincenal</option>
                      <option value="ONE_OFF">Pago Único</option>
                    </select>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Valor/Monto</label>
                    <input 
                      type="number"
                      value={agreement.alimonyProvision?.amount?.value || 0}
                      onChange={(e) => setAgreement({
                        ...agreement,
                        alimonyProvision: { 
                          ...agreement.alimonyProvision, 
                          amount: { ...agreement.alimonyProvision?.amount, value: parseFloat(e.target.value) } 
                        }
                      })}
                      className="w-full bg-background border border-border text-xs text-white rounded-xl p-3 outline-none"
                    />
                  </div>

                  <div>
                    <label className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Moneda / Parámetro</label>
                    <select 
                      value={agreement.alimonyProvision?.amount?.currencyOrParameter || "ARS"}
                      onChange={(e) => setAgreement({
                        ...agreement,
                        alimonyProvision: { 
                          ...agreement.alimonyProvision, 
                          amount: { ...agreement.alimonyProvision?.amount, currencyOrParameter: e.target.value as any } 
                        }
                      })}
                      className="w-full bg-background border border-border text-xs text-white rounded-xl p-3 outline-none"
                    >
                      <option value="ARS">Pesos Argentinos ($)</option>
                      <option value="USD">Dólares (US$)</option>
                      <option value="SALARY_PERCENTAGE">Porcentaje Sueldo (%)</option>
                      <option value="SMVM_PERCENTAGE">Porcentaje SMVM (%)</option>
                    </select>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Medio de Pago</label>
                    <select 
                      value={agreement.alimonyProvision?.paymentMethod || "BANK_TRANSFER"}
                      onChange={(e) => setAgreement({
                        ...agreement,
                        alimonyProvision: { ...agreement.alimonyProvision, paymentMethod: e.target.value as any }
                      })}
                      className="w-full bg-background border border-border text-xs text-white rounded-xl p-3 outline-none"
                    >
                      <option value="BANK_TRANSFER">Transferencia Bancaria</option>
                      <option value="CASH">Efectivo</option>
                      <option value="JUDICIAL_DEPOSIT">Depósito en Cuenta Judicial</option>
                      <option value="EMPLOYER_WITHHOLDING">Retención directa por Empleador</option>
                    </select>
                  </div>

                  <div>
                    <label className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Indexación / Ajuste</label>
                    <select 
                      value={agreement.alimonyProvision?.updateMechanism || "IPC_INDEX"}
                      onChange={(e) => setAgreement({
                        ...agreement,
                        alimonyProvision: { ...agreement.alimonyProvision, updateMechanism: e.target.value as any }
                      })}
                      className="w-full bg-background border border-border text-xs text-white rounded-xl p-3 outline-none"
                    >
                      <option value="IPC_INDEX">Ajuste por IPC (INDEC)</option>
                      <option value="SMVM_PERCENTAGE">Ajuste por SMVM</option>
                      <option value="SALARY_PARITY">Ajuste por RIPTE (Salarios)</option>
                      <option value="NONE">Sin ajuste (No recomendado)</option>
                    </select>
                  </div>
                </div>

                <div>
                  <label className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Detalles de Transferencia / CBU / Alias</label>
                  <input 
                    type="text"
                    value={agreement.alimonyProvision?.paymentDetails || ""}
                    onChange={(e) => setAgreement({
                      ...agreement,
                      alimonyProvision: { ...agreement.alimonyProvision, paymentDetails: e.target.value }
                    })}
                    placeholder="Ej: CBU 0170098740000034298177 o Alias: pablo.alimentos.mp"
                    className="w-full bg-background border border-border text-xs text-white rounded-xl p-3 outline-none"
                  />
                </div>
              </div>
            ) : (
              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="p-4 rounded-xl bg-background border border-border">
                    <span className="text-[8px] font-black text-zinc-500 uppercase tracking-widest">Monto Prestación</span>
                    <p className="text-xs font-bold text-white mt-1">
                      {agreement.alimonyProvision?.amount?.value !== undefined && agreement.alimonyProvision?.amount?.currencyOrParameter === "ARS" && `$${agreement.alimonyProvision.amount.value.toLocaleString()} ARS`}
                      {agreement.alimonyProvision?.amount?.value !== undefined && agreement.alimonyProvision?.amount?.currencyOrParameter === "USD" && `US$${agreement.alimonyProvision.amount.value} USD`}
                      {agreement.alimonyProvision?.amount?.value !== undefined && agreement.alimonyProvision?.amount?.currencyOrParameter === "SALARY_PERCENTAGE" && `${agreement.alimonyProvision.amount.value}% del Sueldo`}
                      {agreement.alimonyProvision?.amount?.value !== undefined && agreement.alimonyProvision?.amount?.currencyOrParameter === "SMVM_PERCENTAGE" && `${agreement.alimonyProvision.amount.value}% del SMVM`}
                      {agreement.alimonyProvision?.amount?.value === undefined && "No definido / En especie"}
                    </p>
                  </div>

                  <div className="p-4 rounded-xl bg-background border border-border">
                    <span className="text-[8px] font-black text-zinc-500 uppercase tracking-widest">Indexación / Ajuste</span>
                    <p className="text-xs font-bold text-white mt-1">
                      {agreement.alimonyProvision?.updateMechanism === "IPC_INDEX" && "Ajuste por IPC (INDEC)"}
                      {agreement.alimonyProvision?.updateMechanism === "SMVM_PERCENTAGE" && "Ajuste por Salario Mínimo (SMVM)"}
                      {agreement.alimonyProvision?.updateMechanism === "SALARY_PARITY" && "Ajuste por RIPTE / Paritarias"}
                      {agreement.alimonyProvision?.updateMechanism === "NONE" && "Sin actualización inflacionaria (Alerta)"}
                      {!agreement.alimonyProvision?.updateMechanism && "No especificado"}
                    </p>
                  </div>
                </div>

                <div className="p-4 rounded-xl bg-background border border-border">
                  <span className="text-[8px] font-black text-zinc-500 uppercase tracking-widest">Medio de Pago e Instrumento</span>
                  <p className="text-xs font-bold text-white mt-1">
                    {agreement.alimonyProvision?.paymentMethod === "BANK_TRANSFER" && "Transferencia Bancaria directa"}
                    {agreement.alimonyProvision?.paymentMethod === "CASH" && "Pago en efectivo contra recibo manual"}
                    {agreement.alimonyProvision?.paymentMethod === "JUDICIAL_DEPOSIT" && "Depósito en cuenta judicial de Defensoría"}
                    {agreement.alimonyProvision?.paymentMethod === "EMPLOYER_WITHHOLDING" && "Retención directa de haberes del empleador"}
                    {!agreement.alimonyProvision?.paymentMethod && "No definido"}
                  </p>
                  {agreement.alimonyProvision?.paymentDetails && (
                    <p className="text-xs text-zinc-400 mt-2 font-mono bg-surface p-2 rounded border border-border">
                      {agreement.alimonyProvision.paymentDetails}
                    </p>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* ④ Distribución Patrimonial (vivienda, bienes, deudas) */}
        <div className="p-8 rounded-3xl bg-surface border border-border flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-3 mb-6">
              <div className="p-2 rounded-xl bg-accent/10 border border-accent/20">
                <House size={20} weight="duotone" className="text-accent" />
              </div>
              <h5 className="text-[10px] uppercase tracking-[0.2em] text-zinc-400 font-black">③ Atribución del Hogar y Bienes</h5>
            </div>

            {isStructuredEditing ? (
              <div className="space-y-4">
                <div>
                  <label className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Atribución de la Vivienda Conyugal (Art. 443)</label>
                  <select 
                    value={agreement.assetDistribution?.homeAttributionTo || "PETITIONER"}
                    onChange={(e) => setAgreement({
                      ...agreement,
                      assetDistribution: { ...agreement.assetDistribution, homeAttributionTo: e.target.value as any }
                    })}
                    className="w-full bg-background border border-border text-xs text-white rounded-xl p-3 outline-none"
                  >
                    <option value="PETITIONER">Atribuido al Peticionante (Junto a los Hijos)</option>
                    <option value="RESPONDENT">Atribuido al Demandado</option>
                    <option value="BOTH_SALE">A vender y dividir el importe en partes iguales</option>
                    <option value="OTHER">Sin atribución / No poseían vivienda propia</option>
                  </select>
                </div>

                <div>
                  <label className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Resumen de Bienes Comunes (Acuerdo de liquidación)</label>
                  <textarea 
                    value={agreement.assetDistribution?.assetsSummary || ""}
                    onChange={(e) => setAgreement({
                      ...agreement,
                      assetDistribution: { ...agreement.assetDistribution, assetsSummary: e.target.value }
                    })}
                    rows={3}
                    placeholder="Ej: El peticionante conserva el automotor marca Fiat Cronos y el demandado conserva los electrodomésticos."
                    className="w-full bg-background border border-border text-xs text-white rounded-xl p-3 outline-none resize-none font-sans"
                  />
                </div>
              </div>
            ) : (
              <div className="space-y-4">
                <div className="p-4 rounded-xl bg-background border border-border">
                  <span className="text-[8px] font-black text-zinc-500 uppercase tracking-widest">Atribución del Hogar</span>
                  <p className="text-xs font-bold text-white mt-1">
                    {agreement.assetDistribution?.homeAttributionTo === "PETITIONER" && "Atribuido al Peticionante para vivienda familiar con los hijos"}
                    {agreement.assetDistribution?.homeAttributionTo === "RESPONDENT" && "Atribuido al Demandado"}
                    {agreement.assetDistribution?.homeAttributionTo === "BOTH_SALE" && "Venta comercial del inmueble y división de fondos al 50%"}
                    {agreement.assetDistribution?.homeAttributionTo === "OTHER" && "Sin inmuebles comunes que atribuir"}
                    {!agreement.assetDistribution?.homeAttributionTo && "No pactado"}
                  </p>
                </div>

                <div className="p-4 rounded-xl bg-background border border-border">
                  <span className="text-[8px] font-black text-zinc-500 uppercase tracking-widest">Liquidación y Resumen de Bienes</span>
                  {agreement.assetDistribution?.assetsSummary ? (
                    <p className="text-xs text-zinc-300 mt-2 font-serif leading-relaxed italic">
                      "{agreement.assetDistribution.assetsSummary}"
                    </p>
                  ) : (
                    <p className="text-xs text-zinc-500 mt-1">Sin bienes declarados o sujetos a reparto en este proceso.</p>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* ⑤ Compensación Económica (Art. 441) */}
        <div className="p-8 rounded-3xl bg-surface border border-border flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-3 mb-6">
              <div className="p-2 rounded-xl bg-accent/10 border border-accent/20">
                <Scales size={20} weight="duotone" className="text-accent" />
              </div>
              <h5 className="text-[10px] uppercase tracking-[0.2em] text-zinc-400 font-black">④ Compensación Económica (Art. 441)</h5>
            </div>

            {isStructuredEditing ? (
              <div className="space-y-4">
                <div className="flex items-center gap-3 p-3 bg-background border border-border rounded-xl">
                  <input 
                    type="checkbox"
                    checked={agreement.economicCompensation?.appliesEconomicCompensation || false}
                    onChange={(e) => setAgreement({
                      ...agreement,
                      economicCompensation: { ...agreement.economicCompensation, appliesEconomicCompensation: e.target.checked }
                    })}
                    className="w-4 h-4 rounded text-accent focus:ring-accent"
                  />
                  <span className="text-xs text-white font-bold">¿Aplica Compensación Económica?</span>
                </div>

                {(agreement.economicCompensation?.appliesEconomicCompensation) && (
                  <>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Cónyuge Beneficiario</label>
                        <select 
                          value={agreement.economicCompensation?.beneficiary || "PETITIONER"}
                          onChange={(e) => setAgreement({
                            ...agreement,
                            economicCompensation: { ...agreement.economicCompensation, beneficiary: e.target.value as any }
                          })}
                          className="w-full bg-background border border-border text-xs text-white rounded-xl p-3 outline-none"
                        >
                          <option value="PETITIONER">Peticionante (Actora)</option>
                          <option value="RESPONDENT">Demandado (Demandante)</option>
                        </select>
                      </div>

                      <div>
                        <label className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Forma de Pago</label>
                        <select 
                          value={agreement.economicCompensation?.paymentMethod || "SINGLE_PAYMENT"}
                          onChange={(e) => setAgreement({
                            ...agreement,
                            economicCompensation: { ...agreement.economicCompensation, paymentMethod: e.target.value as any }
                          })}
                          className="w-full bg-background border border-border text-xs text-white rounded-xl p-3 outline-none"
                        >
                          <option value="SINGLE_PAYMENT">Pago Único</option>
                          <option value="INSTALLMENTS">En Cuotas Mensuales</option>
                          <option value="USUFRUCT">Constitución de Usufructo</option>
                        </select>
                      </div>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Monto de Compensación</label>
                        <input 
                          type="number"
                          value={agreement.economicCompensation?.compensationAmount?.value || 0}
                          onChange={(e) => setAgreement({
                            ...agreement,
                            economicCompensation: { 
                              ...agreement.economicCompensation, 
                              compensationAmount: { ...agreement.economicCompensation?.compensationAmount, value: parseFloat(e.target.value) } 
                            }
                          })}
                          className="w-full bg-background border border-border text-xs text-white rounded-xl p-3 outline-none"
                        />
                      </div>

                      <div>
                        <label className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Cuotas (Si aplica)</label>
                        <input 
                          type="number"
                          value={agreement.economicCompensation?.installmentsCount || 1}
                          onChange={(e) => setAgreement({
                            ...agreement,
                            economicCompensation: { ...agreement.economicCompensation, installmentsCount: parseInt(e.target.value) }
                          })}
                          className="w-full bg-background border border-border text-xs text-white rounded-xl p-3 outline-none"
                        />
                      </div>
                    </div>

                    <div>
                      <label className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">Justificación del Desequilibrio Económico</label>
                      <textarea 
                        value={agreement.economicCompensation?.imbalanceJustification || ""}
                        onChange={(e) => setAgreement({
                          ...agreement,
                          economicCompensation: { ...agreement.economicCompensation, imbalanceJustification: e.target.value }
                        })}
                        rows={2}
                        placeholder="Ej: Debido a la dedicación exclusiva de la peticionante a las tareas de cuidado familiar durante el matrimonio..."
                        className="w-full bg-background border border-border text-xs text-white rounded-xl p-3 outline-none resize-none font-sans"
                      />
                    </div>
                  </>
                )}
              </div>
            ) : (
              <div className="space-y-4">
                <div className="p-4 rounded-xl bg-background border border-border">
                  <span className="text-[8px] font-black text-zinc-500 uppercase tracking-widest">Estado Compensación</span>
                  <p className="text-xs font-bold text-white mt-1">
                    {agreement.economicCompensation?.appliesEconomicCompensation 
                      ? "Aplica Compensación Económica a Favor de Cónyuge" 
                      : "Ambos cónyuges declaran no poseer desequilibrio económico de mutuo acuerdo."}
                  </p>
                </div>

                {agreement.economicCompensation?.appliesEconomicCompensation && (
                  <>
                    <div className="grid grid-cols-2 gap-4">
                      <div className="p-4 rounded-xl bg-background border border-border">
                        <span className="text-[8px] font-black text-zinc-500 uppercase tracking-widest">Monto y Beneficiario</span>
                        <p className="text-xs font-bold text-white mt-1">
                          {agreement.economicCompensation?.compensationAmount?.value?.toLocaleString() || "Sin monto"} {agreement.economicCompensation?.compensationAmount?.currencyOrParameter} a favor de {agreement.economicCompensation?.beneficiary === "PETITIONER" ? "Peticionante" : "Demandado"}
                        </p>
                      </div>

                      <div className="p-4 rounded-xl bg-background border border-border">
                        <span className="text-[8px] font-black text-zinc-500 uppercase tracking-widest">Modalidad</span>
                        <p className="text-xs font-bold text-white mt-1">
                          {agreement.economicCompensation?.paymentMethod === "SINGLE_PAYMENT" && "Pago único"}
                          {agreement.economicCompensation?.paymentMethod === "INSTALLMENTS" && `En ${agreement.economicCompensation.installmentsCount} cuotas mensuales`}
                          {agreement.economicCompensation?.paymentMethod === "USUFRUCT" && "Por constitución de Usufructo de bienes"}
                        </p>
                      </div>
                    </div>

                    <div className="p-4 rounded-xl bg-background border border-border">
                      <span className="text-[8px] font-black text-zinc-500 uppercase tracking-widest">Justificación del Desequilibrio</span>
                      <p className="text-xs text-zinc-300 mt-2 font-serif leading-relaxed italic">
                        "{agreement.economicCompensation.imbalanceJustification}"
                      </p>
                    </div>
                  </>
                )}
              </div>
            )}
          </div>
        </div>

      </div>

      {/* ACCIONES DE FORMULARIO ESTRUCTURADO (Si está en edición) */}
      {isStructuredEditing && (
        <section className="flex gap-4 justify-end p-6 bg-surface border border-border rounded-3xl">
          <button 
            onClick={() => setIsStructuredEditing(false)}
            disabled={saveStatus === "saving"}
            className="px-4 py-2 rounded-xl text-zinc-500 hover:text-white text-[10px] font-black uppercase tracking-widest transition-all"
          >
            Cancelar
          </button>
          <button 
            onClick={handleSaveStructuredAgreement}
            disabled={saveStatus === "saving"}
            className="flex items-center gap-2 px-6 py-2 rounded-xl bg-success text-white text-[10px] font-black uppercase tracking-widest hover:bg-success/90 transition-all shadow-lg shadow-success/20"
          >
            {saveStatus === "saving" ? (
              <div className="w-3 h-3 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            ) : (
              <Check size={14} weight="bold" />
            )}
            {saveStatus === "saving" ? "Guardando..." : "Guardar Convenio"}
          </button>
        </section>
      )}

      {/* ⑥ Texto Crudo del Convenio (Redactado de Manifestaciones de WhatsApp) */}
      <section>
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-info/10 border border-info/20">
              <Quotes size={20} weight="duotone" className="text-info" />
            </div>
            <h5 className="text-[10px] uppercase tracking-[0.2em] text-zinc-500 font-black">Convenio Regulador: Manifestaciones Recolectadas por Bot</h5>
          </div>
          
          {!isEditing ? (
            <button 
              onClick={() => setIsEditing(true)}
              className="flex items-center gap-2 px-4 py-2 rounded-xl bg-accent/10 text-accent border border-accent/20 text-[10px] font-black uppercase tracking-widest hover:bg-accent/20 transition-all"
            >
              <PencilSimple size={14} weight="bold" />
              Modificar Texto Borrador
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
                onClick={() => onSave(expediente.rawAgreementText || "")}
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
              defaultValue={expediente.rawAgreementText || ""}
              onChange={(e) => {
                expediente.rawAgreementText = e.target.value;
              }}
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
