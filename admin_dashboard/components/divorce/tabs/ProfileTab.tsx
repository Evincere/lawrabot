"use client";

import { useState } from "react";
import { Expediente, UpdateCaseDataRequest, SpouseUpdateData, ChildUpdateData, EvidenceItem } from "@/lib/types/divorce";
import { 
  User,
  UsersThree,
  MapPin,
  IdentificationCard,
  IdentificationBadge,
  Phone,
  EnvelopeSimple,
  Briefcase,
  GlobeHemisphereWest,
  Cake,
  Layout,
  Table,
  WarningCircle,
  MapPinLine,
  PencilSimple,
  Plus,
  Trash,
  Copy,
  CheckCircle,
  Certificate,
  DownloadSimple,
  Files,
  Stamp,
  FirstAidKit
} from "@phosphor-icons/react";
import { cn } from "@/lib/utils";
import { motion, AnimatePresence } from "framer-motion";
import { useToast } from "@/lib/contexts/ToastContext";
import Image from "next/image";
import { ObservationModal } from "@/components/shared/ObservationModal";

interface ProfileTabProps {
  expediente: Expediente;
  evidences: EvidenceItem[];
  onUpdate: (data: UpdateCaseDataRequest) => Promise<void>;
  onEvidenceStatusUpdate: (evidenceId: string, approved: boolean, reason?: string) => void;
}

interface InfoFieldProps {
  label: string;
  value?: string | number;
  icon?: React.ElementType;
  className?: string;
  editable?: boolean;
  name?: string;
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
  isEditing?: boolean;
  type?: string;
}

const InfoField = ({ 
  label, 
  value, 
  icon: Icon, 
  className,
  editable = true,
  name,
  onChange,
  isEditing = false,
  type = "text"
}: InfoFieldProps) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (!value || isEditing) return;
    navigator.clipboard.writeText(String(value));
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div 
      className={cn(
        "p-4 rounded-xl bg-surface/50 border border-border/50 group transition-all relative overflow-hidden",
        !isEditing && value && "cursor-pointer hover:border-accent/30 hover:bg-surface/80",
        isEditing && "border-accent/20 bg-accent/5",
        className
      )}
      onClick={handleCopy}
    >
      <div className="flex items-center justify-between mb-1.5">
        <span className="text-[9px] font-black text-zinc-500 uppercase tracking-widest flex items-center gap-1.5">
          {Icon && <Icon size={12} weight="bold" className={cn("text-zinc-400 group-hover:text-accent transition-colors", isEditing && "text-accent")} />}
          {label}
        </span>
        {!isEditing && value && (
          <div className="opacity-0 group-hover:opacity-100 transition-opacity">
            {copied ? (
              <CheckCircle size={12} weight="fill" className="text-success" />
            ) : (
              <Copy size={12} weight="bold" className="text-zinc-500" />
            )}
          </div>
        )}
      </div>

      {isEditing && editable ? (
        <input
          name={name}
          type={type}
          value={value || ""}
          onChange={onChange}
          className={cn(
            "w-full bg-transparent text-sm font-bold text-white focus:outline-none placeholder:text-zinc-700",
            type === "date" && "color-scheme-dark"
          )}
          placeholder="Completar..."
          autoFocus
          onClick={(e) => e.stopPropagation()}
        />
      ) : (
        <p className={cn(
          "text-sm font-bold tracking-tight mt-0.5 truncate",
          value ? "text-white" : "text-zinc-600 italic"
        )}>
          {value || "No declarado"}
        </p>
      )}

      {/* Visual Feedback for Copy */}
      <AnimatePresence>
        {copied && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className="absolute inset-0 bg-accent/10 flex items-center justify-center backdrop-blur-sm pointer-events-none"
          >
            <span className="text-[10px] font-black text-accent uppercase tracking-tighter">Copiado</span>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

interface AddressDisplayProps {
  label: string;
  address?: { street?: string; number?: string; locality?: string };
  isEditing: boolean;
  data: SpouseUpdateData;
  setData: (data: SpouseUpdateData) => void;
  icon: React.ElementType;
  onCopyFromOther?: () => void;
}

const AddressDisplay = ({ label, address, isEditing, data, setData, icon: Icon, onCopyFromOther }: AddressDisplayProps) => {
  const [copied, setCopied] = useState(false);
  const fullAddress = address ? `${address.street || ""} ${address.number || ""}, ${address.locality || ""}`.trim() : "";

  const handleCopy = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (!fullAddress || isEditing) return;
    navigator.clipboard.writeText(fullAddress);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div 
      className={cn(
        "p-5 rounded-xl bg-surface/50 border border-border/50 group transition-all relative overflow-hidden",
        !isEditing && fullAddress && "cursor-pointer hover:border-accent/30 hover:bg-surface/80",
        isEditing && "border-accent/20 bg-accent/5"
      )}
      onClick={handleCopy}
    >
      <div className="flex items-center justify-between mb-2">
        <span className="text-[9px] font-black text-zinc-500 uppercase tracking-[0.15em] flex items-center gap-1.5">
          <Icon size={12} weight="bold" className="text-zinc-400 group-hover:text-accent transition-colors" />
          {label}
        </span>
        <div className="flex items-center gap-2">
          {isEditing && onCopyFromOther && (
            <button 
              onClick={(e) => { e.stopPropagation(); onCopyFromOther(); }}
              className="px-2 py-1 rounded-md bg-accent/10 border border-accent/20 text-[8px] font-black uppercase text-accent hover:bg-accent hover:text-white transition-all"
            >
              Copiar de Peticionante
            </button>
          )}
          {!isEditing && fullAddress && (
            <div className="opacity-0 group-hover:opacity-100 transition-opacity">
              {copied ? (
                <CheckCircle size={12} weight="fill" className="text-success" />
              ) : (
                <Copy size={12} weight="bold" className="text-zinc-500" />
              )}
            </div>
          )}
        </div>
      </div>

      {isEditing ? (
        <div className="grid grid-cols-12 gap-3" onClick={(e) => e.stopPropagation()}>
           <div className="col-span-6">
              <input 
                placeholder="Calle"
                value={data.address?.street || ""}
                onChange={(e) => setData({...data, address: {...data.address!, street: e.target.value}})}
                className="w-full bg-background/50 border border-border/50 rounded-lg p-2 text-sm text-white focus:outline-none focus:border-accent/50 transition-colors"
              />
           </div>
           <div className="col-span-2">
              <input 
                placeholder="N°"
                value={data.address?.number || ""}
                onChange={(e) => setData({...data, address: {...data.address!, number: e.target.value}})}
                className="w-full bg-background/50 border border-border/50 rounded-lg p-2 text-sm text-white focus:outline-none focus:border-accent/50 transition-colors"
              />
           </div>
           <div className="col-span-4">
              <input 
                placeholder="Localidad"
                value={data.address?.locality || ""}
                onChange={(e) => setData({...data, address: {...data.address!, locality: e.target.value}})}
                className="w-full bg-background/50 border border-border/50 rounded-lg p-2 text-sm text-white focus:outline-none focus:border-accent/50 transition-colors"
              />
           </div>
        </div>
      ) : (
        <p className={cn(
          "text-sm font-bold transition-colors",
          fullAddress ? "text-white" : "text-zinc-600 italic"
        )}>
          {fullAddress || "Diferido o no declarado"}
        </p>
      )}

      {/* Visual Feedback for Copy */}
      <AnimatePresence>
        {copied && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className="absolute inset-0 bg-accent/10 flex items-center justify-center backdrop-blur-sm pointer-events-none"
          >
            <span className="text-[10px] font-black text-accent uppercase tracking-tighter">Copiado</span>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export function ProfileTab({ expediente, evidences, onUpdate, onEvidenceStatusUpdate }: ProfileTabProps) {
  const [childrenView, setChildrenView] = useState<"cards" | "table">("cards");
  const [isEditingPetitioner, setIsEditingPetitioner] = useState(false);
  const [isEditingRespondent, setIsEditingRespondent] = useState(false);
  const [isEditingChildren, setIsEditingChildren] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [previewEvidence, setPreviewEvidence] = useState<EvidenceItem | null>(null);
  const [observingEvidence, setObservingEvidence] = useState<EvidenceItem | null>(null);

  const handleConfirmObservation = (reason: string) => {
    if (observingEvidence) {
      onEvidenceStatusUpdate(observingEvidence.id, false, reason);
      setObservingEvidence(null);
      if (previewEvidence && previewEvidence.id === observingEvidence.id) {
        setPreviewEvidence(null);
      }
    }
  };
  
  // Filtrar evidencias por tipo
  const birthCerts = evidences.filter(ev => ev.documentType === "BIRTH_CERT");
  const disabilityCerts = evidences.filter(ev => ev.documentType === "DISABILITY_CERT");
  const childrenWithDisability = expediente.children?.filter(c => c.hasDisability) || [];
  
  const [petitionerData, setPetitionerData] = useState<SpouseUpdateData>({
    fullName: expediente.petitioner?.fullName?.fullName || "",
    dni: expediente.petitioner?.dni || "",
    cuil: expediente.petitioner?.cuil || "",
    phoneNumber: expediente.petitioner?.phoneNumber || "",
    email: expediente.petitioner?.email || "",
    profession: expediente.petitioner?.profession || "",
    nationality: expediente.petitioner?.nationality || "",
    birthDate: expediente.petitioner?.birthDate || "",
    address: expediente.petitioner?.address || { street: "", number: "", locality: "" }
  });

  const [respondentData, setRespondentData] = useState<SpouseUpdateData>({
    fullName: expediente.respondent?.fullName?.fullName || "",
    dni: expediente.respondent?.dni || "",
    cuil: expediente.respondent?.cuil || "",
    phoneNumber: expediente.respondent?.phoneNumber || "",
    email: expediente.respondent?.email || "",
    profession: expediente.respondent?.profession || "",
    nationality: expediente.respondent?.nationality || "",
    birthDate: expediente.respondent?.birthDate || "",
    address: expediente.respondent?.residentialAddress || { street: "", number: "", locality: "" }
  });

  const [childrenData, setChildrenData] = useState<ChildUpdateData[]>(
    expediente.children?.map(c => ({
      id: c.id,
      name: c.name,
      dni: c.dni,
      birthDate: c.birthDate,
      hasDisability: c.hasDisability,
      birthCertificateId: c.birthCertificateId
    })) || []
  );

  const { showToast } = useToast();

  const handleSavePetitioner = async () => {
    setIsSaving(true);
    try {
      await onUpdate({ petitioner: petitionerData });
      setIsEditingPetitioner(false);
      showToast("Datos del peticionante actualizados", "success");
    } catch {
      showToast("Error al guardar cambios", "error");
    } finally {
      setIsSaving(false);
    }
  };

  const handleSaveRespondent = async () => {
    setIsSaving(true);
    try {
      await onUpdate({ respondent: respondentData });
      setIsEditingRespondent(false);
      showToast("Datos del demandado actualizados", "success");
    } catch {
      showToast("Error al guardar cambios", "error");
    } finally {
      setIsSaving(false);
    }
  };

  const handleSaveChildren = async () => {
    setIsSaving(true);
    try {
      await onUpdate({ children: childrenData });
      setIsEditingChildren(false);
      showToast("Lista de hijos actualizada", "success");
    } catch {
      showToast("Error al guardar cambios", "error");
    } finally {
      setIsSaving(false);
    }
  };

  const addChild = () => {
    setChildrenData([...childrenData, { name: "", dni: "", birthDate: "", hasDisability: false }]);
  };

  const removeChild = (index: number) => {
    setChildrenData(childrenData.filter((_, i) => i !== index));
  };

  const updateChild = (index: number, field: keyof ChildUpdateData, value: string | boolean) => {
    const newChildren = [...childrenData];
    newChildren[index] = { ...newChildren[index], [field]: value };
    setChildrenData(newChildren);
  };

  return (
    <div className="space-y-16 pb-32">
      {/* 1. Peticionante */}
      <section className="relative">
        <div className="flex items-center justify-between mb-8">
           <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-xl bg-accent/10 border border-accent/20">
                <User size={22} weight="duotone" className="text-accent" />
              </div>
              <div>
                <h5 className="text-[10px] uppercase tracking-[0.2em] text-zinc-500 font-black">Sujeto I: Titular Peticionante</h5>
                <p className="text-xs text-zinc-400 font-medium mt-0.5">Datos del solicitante principal</p>
              </div>
           </div>

           <div className="flex items-center gap-2">
             {isEditingPetitioner ? (
               <>
                 <button 
                   onClick={() => setIsEditingPetitioner(false)}
                   className="px-4 py-2 rounded-xl bg-surface border border-border text-[10px] font-black uppercase tracking-widest text-zinc-500 hover:text-white transition-all"
                 >
                   Cancelar
                 </button>
                 <button 
                   onClick={handleSavePetitioner}
                   disabled={isSaving}
                   className="px-6 py-2 rounded-xl bg-success text-white text-[10px] font-black uppercase tracking-widest shadow-lg shadow-success/20 hover:scale-105 transition-all"
                 >
                   {isSaving ? "Guardando..." : "Confirmar Cambios"}
                 </button>
               </>
             ) : (
               <button 
                 onClick={() => setIsEditingPetitioner(true)}
                 className="flex items-center gap-2 px-4 py-2 rounded-xl bg-surface/50 border border-border text-[10px] font-black uppercase tracking-widest text-zinc-400 hover:text-accent hover:border-accent/40 transition-all"
               >
                 <PencilSimple size={14} weight="bold" />
                 Editar Datos
               </button>
             )}
           </div>
        </div>

        <div className="grid grid-cols-12 gap-4">
          <div className={cn(
            "col-span-12 lg:col-span-6 p-6 rounded-2xl bg-surface border-2 transition-all shadow-lg shadow-accent/5",
            isEditingPetitioner ? "border-accent/40" : "border-accent/10 hover:border-accent/30"
          )}>
             <span className="text-[9px] font-black text-accent uppercase tracking-widest flex items-center gap-2 mb-2">
               <IdentificationBadge size={14} weight="fill" />
               Identidad Civil
             </span>
             {isEditingPetitioner ? (
               <input 
                 value={petitionerData.fullName}
                 onChange={(e) => setPetitionerData({...petitionerData, fullName: e.target.value})}
                 className="text-2xl font-black tracking-tighter text-white bg-transparent w-full focus:outline-none placeholder:text-zinc-800 uppercase"
                 placeholder="NOMBRE COMPLETO"
               />
             ) : (
               <h3 className="text-2xl font-black tracking-tighter text-white uppercase leading-none">
                 {expediente.petitioner?.fullName?.fullName || "No declarado"}
               </h3>
             )}
             
             <div className="grid grid-cols-2 gap-3 mt-4">
                <div className="bg-background/50 p-3 rounded-lg border border-border/50">
                  <span className="text-[8px] font-black text-zinc-500 uppercase block mb-1">DNI</span>
                  {isEditingPetitioner ? (
                    <input 
                      value={petitionerData.dni}
                      onChange={(e) => setPetitionerData({...petitionerData, dni: e.target.value})}
                      className="text-base font-bold text-white tracking-widest bg-transparent w-full focus:outline-none"
                    />
                  ) : (
                    <p className="text-base font-bold text-white tracking-widest">{expediente.petitioner?.dni || "S/D"}</p>
                  )}
                </div>
                <div className="bg-background/50 p-3 rounded-lg border border-border/50">
                  <span className="text-[8px] font-black text-zinc-500 uppercase block mb-1">CUIL</span>
                  {isEditingPetitioner ? (
                    <input 
                      value={petitionerData.cuil}
                      onChange={(e) => setPetitionerData({...petitionerData, cuil: e.target.value})}
                      className="text-base font-bold text-white tracking-widest bg-transparent w-full focus:outline-none"
                    />
                  ) : (
                    <p className="text-base font-bold text-white tracking-widest">{expediente.petitioner?.cuil || "S/D"}</p>
                  )}
                </div>
             </div>
          </div>

          <div className="col-span-12 lg:col-span-6 grid grid-cols-2 gap-4">
             <InfoField 
                label="Teléfono (WhatsApp)" 
                value={petitionerData.phoneNumber} 
                icon={Phone} 
                editable={false} 
                className="opacity-80"
             />
             <InfoField 
                label="Correo" 
                value={petitionerData.email} 
                icon={EnvelopeSimple} 
                isEditing={isEditingPetitioner}
                onChange={(e) => setPetitionerData({...petitionerData, email: e.target.value})}
             />
             <InfoField 
                label="Profesión" 
                value={petitionerData.profession} 
                icon={Briefcase} 
                isEditing={isEditingPetitioner}
                onChange={(e) => setPetitionerData({...petitionerData, profession: e.target.value})}
             />
             <InfoField 
                label="Nacionalidad" 
                value={petitionerData.nationality} 
                icon={GlobeHemisphereWest} 
                isEditing={isEditingPetitioner}
                onChange={(e) => setPetitionerData({...petitionerData, nationality: e.target.value})}
             />
             <InfoField 
                label="Nacimiento" 
                value={petitionerData.birthDate} 
                icon={Cake} 
                type="date"
                isEditing={isEditingPetitioner}
                onChange={(e) => setPetitionerData({...petitionerData, birthDate: e.target.value})}
             />
             <InfoField 
                label="Sede Judicial (Domicilio Conyugal)" 
                value={expediente.lastConjugalResidence?.locality} 
                icon={GlobeHemisphereWest} 
                editable={false}
             />
          </div>

          {/* Petitioner Address Editor */}
          <div className="col-span-12">
            <AddressDisplay 
              label="Residencia Declarada"
              address={expediente.petitioner?.address}
              isEditing={isEditingPetitioner}
              data={petitionerData}
              setData={setPetitionerData}
              icon={MapPinLine}
            />
          </div>
        </div>
      </section>

      {/* 2. Demandado */}
      <section>
        <div className="flex items-center justify-between mb-8 opacity-90">
           <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-xl bg-zinc-500/10 border border-zinc-500/20">
                <User size={22} weight="duotone" className="text-zinc-400" />
              </div>
              <div>
                <h5 className="text-[10px] uppercase tracking-[0.2em] text-zinc-500 font-black">Sujeto II: Demandado</h5>
                <p className="text-xs text-zinc-400 font-medium mt-0.5">Datos de la contraparte</p>
              </div>
           </div>

           <div className="flex items-center gap-2">
             {isEditingRespondent ? (
               <>
                 <button 
                   onClick={() => setIsEditingRespondent(false)}
                   className="px-4 py-2 rounded-xl bg-surface border border-border text-[10px] font-black uppercase tracking-widest text-zinc-500 hover:text-white transition-all"
                 >
                   Cancelar
                 </button>
                 <button 
                   onClick={handleSaveRespondent}
                   className="px-6 py-2 rounded-xl bg-accent text-white text-[10px] font-black uppercase tracking-widest shadow-lg shadow-accent/20 hover:scale-105 transition-all"
                 >
                   Guardar Cambios
                 </button>
               </>
             ) : (
               <button 
                 onClick={() => setIsEditingRespondent(true)}
                 className="flex items-center gap-2 px-4 py-2 rounded-xl bg-surface/50 border border-border text-[10px] font-black uppercase tracking-widest text-zinc-400 hover:text-accent hover:border-accent/40 transition-all"
               >
                 <PencilSimple size={14} weight="bold" />
                 Editar Datos
               </button>
             )}
           </div>
        </div>

        <div className="grid grid-cols-12 gap-4">
          <div className="col-span-12 lg:col-span-6 p-6 rounded-2xl bg-surface border border-border/60">
             <span className="text-[9px] font-black text-zinc-400 uppercase tracking-widest flex items-center gap-2 mb-2">
               <IdentificationBadge size={14} weight="fill" />
               Identidad de Contraparte
             </span>
             {isEditingRespondent ? (
               <input 
                 value={respondentData.fullName}
                 onChange={(e) => setRespondentData({...respondentData, fullName: e.target.value})}
                 className="text-2xl font-black tracking-tighter text-white bg-transparent w-full focus:outline-none placeholder:text-zinc-800 uppercase"
                 placeholder="NOMBRE COMPLETO"
               />
             ) : (
               <h3 className="text-2xl font-black tracking-tighter text-white uppercase leading-none">
                 {expediente.respondent?.fullName?.fullName || "No declarado"}
               </h3>
             )}
             
             <div className="grid grid-cols-2 gap-3 mt-4">
                <div className="bg-background/50 p-3 rounded-lg border border-border/50">
                  <span className="text-[8px] font-black text-zinc-500 uppercase block mb-1">DNI</span>
                  {isEditingRespondent ? (
                    <input 
                      value={respondentData.dni}
                      onChange={(e) => setRespondentData({...respondentData, dni: e.target.value})}
                      className="text-base font-bold text-white tracking-widest bg-transparent w-full focus:outline-none"
                    />
                  ) : (
                    <p className="text-base font-bold text-white tracking-widest">{expediente.respondent?.dni || "S/D"}</p>
                  )}
                </div>
                <div className="bg-background/50 p-3 rounded-lg border border-border/50">
                  <span className="text-[8px] font-black text-zinc-500 uppercase block mb-1">CUIL</span>
                  {isEditingRespondent ? (
                    <input 
                      value={respondentData.cuil}
                      onChange={(e) => setRespondentData({...respondentData, cuil: e.target.value})}
                      className="text-base font-bold text-white tracking-widest bg-transparent w-full focus:outline-none"
                    />
                  ) : (
                    <p className="text-base font-bold text-white tracking-widest">{expediente.respondent?.cuil || "S/D"}</p>
                  )}
                </div>
             </div>
          </div>

          <div className="col-span-12 lg:col-span-6 grid grid-cols-2 gap-4">
             <InfoField 
                label="Teléfono" 
                value={respondentData.phoneNumber} 
                icon={Phone} 
                isEditing={isEditingRespondent}
                onChange={(e) => setRespondentData({...respondentData, phoneNumber: e.target.value})}
             />
             <InfoField 
                label="Correo" 
                value={respondentData.email} 
                icon={EnvelopeSimple} 
                isEditing={isEditingRespondent}
                onChange={(e) => setRespondentData({...respondentData, email: e.target.value})}
             />
             <InfoField 
                label="Profesión" 
                value={respondentData.profession} 
                icon={Briefcase} 
                isEditing={isEditingRespondent}
                onChange={(e) => setRespondentData({...respondentData, profession: e.target.value})}
             />
             <InfoField 
                label="Nacionalidad" 
                value={respondentData.nationality} 
                icon={GlobeHemisphereWest} 
                isEditing={isEditingRespondent}
                onChange={(e) => setRespondentData({...respondentData, nationality: e.target.value})}
             />
             <InfoField 
                label="Nacimiento" 
                value={respondentData.birthDate} 
                icon={Cake} 
                type="date"
                isEditing={isEditingRespondent}
                onChange={(e) => setRespondentData({...respondentData, birthDate: e.target.value})}
             />
          </div>

          <div className="col-span-12">
            <AddressDisplay 
              label="Domicilio Real Demandado"
              address={expediente.respondent?.residentialAddress}
              isEditing={isEditingRespondent}
              data={respondentData}
              setData={setRespondentData}
              icon={MapPin}
              onCopyFromOther={() => {
                if (expediente.petitioner?.address) {
                  setRespondentData({
                    ...respondentData,
                    address: { ...expediente.petitioner.address }
                  });
                }
              }}
            />
          </div>
        </div>
      </section>

      {/* 3. Hijos */}
      <section>
        <div className="flex items-center justify-between mb-8">
           <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-xl bg-warning/10 border border-warning/20">
                <UsersThree size={22} weight="duotone" className="text-warning" />
              </div>
              <div>
                <h5 className="text-[10px] uppercase tracking-[0.2em] text-zinc-500 font-black">Filiación: Descendencia Menor de Edad</h5>
                <p className="text-xs text-zinc-400 font-medium mt-0.5">Menores de edad declarados</p>
              </div>
           </div>

           <div className="flex items-center gap-4">
              {isEditingChildren ? (
                <div className="flex gap-2">
                  <button onClick={() => setIsEditingChildren(false)} className="px-4 py-2 rounded-xl bg-surface border border-border text-[10px] font-black uppercase text-zinc-500">Cancelar</button>
                  <button onClick={handleSaveChildren} className="px-4 py-2 rounded-xl bg-success text-white text-[10px] font-black uppercase tracking-widest shadow-lg shadow-success/20">Guardar</button>
                  <button onClick={addChild} className="flex items-center gap-2 px-4 py-2 rounded-xl bg-accent text-white text-[10px] font-black uppercase tracking-widest">
                    <Plus size={14} weight="bold" />
                    Añadir
                  </button>
                </div>
              ) : (
                <>
                  <button 
                    onClick={() => setIsEditingChildren(true)}
                    className="flex items-center gap-2 px-4 py-2 rounded-xl bg-surface/50 border border-border text-[10px] font-black uppercase tracking-widest text-zinc-400 hover:text-warning hover:border-warning/40 transition-all"
                  >
                    <PencilSimple size={14} weight="bold" />
                    Gestionar Hijos
                  </button>
                  <div className="flex p-1 bg-surface rounded-xl border border-border">
                    <button onClick={() => setChildrenView("cards")} className={cn("p-1.5 rounded-lg", childrenView === "cards" ? "bg-accent/10 text-accent" : "text-zinc-500")}><Layout size={18} weight="duotone" /></button>
                    <button onClick={() => setChildrenView("table")} className={cn("p-1.5 rounded-lg", childrenView === "table" ? "bg-accent/10 text-accent" : "text-zinc-500")}><Table size={18} weight="duotone" /></button>
                  </div>
                </>
              )}
           </div>
        </div>

        {isEditingChildren ? (
          <div className="space-y-4">
             {childrenData.map((child, idx) => (
               <div key={idx} className="p-6 rounded-2xl bg-surface border border-border flex items-center gap-4">
                  <input 
                    placeholder="Nombre Completo"
                    value={child.name}
                    onChange={(e) => updateChild(idx, "name", e.target.value)}
                    className="flex-1 bg-background/50 border border-border/50 rounded-lg p-3 text-sm text-white font-bold uppercase"
                  />
                  <input 
                    placeholder="DNI"
                    value={child.dni}
                    onChange={(e) => updateChild(idx, "dni", e.target.value)}
                    className="w-40 bg-background/50 border border-border/50 rounded-lg p-3 text-sm text-white font-bold tracking-widest"
                  />
                  <input 
                    type="date"
                    value={child.birthDate}
                    onChange={(e) => updateChild(idx, "birthDate", e.target.value)}
                    className="w-48 bg-background/50 border border-border/50 rounded-lg p-3 text-sm text-white"
                  />
                  <label className="flex items-center gap-2 cursor-pointer ml-4">
                    <input 
                      type="checkbox"
                      checked={child.hasDisability}
                      onChange={(e) => updateChild(idx, "hasDisability", e.target.checked)}
                      className="w-4 h-4 accent-danger"
                    />
                    <span className="text-[10px] font-black text-zinc-500 uppercase">Discapacidad</span>
                  </label>
                  <div className="flex flex-col gap-1 w-64">
                    <span className="text-[8px] font-black text-zinc-500 uppercase ml-1">Vincular Acta</span>
                    <select
                      value={child.birthCertificateId || ""}
                      onChange={(e) => updateChild(idx, "birthCertificateId", e.target.value)}
                      className="bg-background/50 border border-border/50 rounded-lg p-3 text-xs text-zinc-300 focus:outline-none focus:border-accent/50 cursor-pointer hover:text-white transition-colors"
                    >
                      <option value="" className="bg-zinc-900 text-zinc-200">Sin vincular</option>
                      {birthCerts.map(cert => (
                        <option key={cert.id} value={cert.id} className="bg-zinc-900 text-zinc-200">{cert.fileName}</option>
                      ))}
                    </select>
                  </div>
                  <button 
                    onClick={() => removeChild(idx)}
                    className="p-3 rounded-lg bg-danger/10 text-danger hover:bg-danger/20 transition-all mt-4"
                  >
                    <Trash size={18} weight="bold" />
                  </button>
               </div>
             ))}
             {childrenData.length === 0 && (
               <div className="text-center p-12 text-zinc-600 border border-dashed border-border rounded-2xl">
                 No hay hijos registrados. Haga clic en añadir para agregar uno.
               </div>
             )}
          </div>
        ) : !expediente.children || expediente.children.length === 0 ? (
          <div className="p-16 rounded-2xl bg-surface/30 border border-border border-dashed text-center">
            <WarningCircle size={32} weight="thin" className="text-zinc-600 mx-auto mb-4" />
            <p className="text-xs font-bold uppercase tracking-widest text-zinc-600">No existen menores de edad declarados</p>
          </div>
        ) : childrenView === "cards" ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {expediente.children.map((child, idx) => {
              // Buscar acta vinculada por childFullName
              const linkedBirthCert = birthCerts.find(ev => 
                ev.childFullName && ev.childFullName.toLowerCase() === child.name.toLowerCase()
              );
              const linkedDisabilityCert = child.hasDisability 
                ? disabilityCerts.find(ev =>
                    ev.childFullName && ev.childFullName.toLowerCase() === child.name.toLowerCase()
                  )
                : null;
              const hasBirthCert = !!linkedBirthCert || !!child.birthCertificateId;
              const hasCUD = !!linkedDisabilityCert;

              return (
                <div key={idx} className="p-6 rounded-2xl bg-surface border border-border flex flex-col gap-4 relative overflow-hidden group hover:border-accent/40 transition-all cursor-pointer" onClick={() => {
                  navigator.clipboard.writeText(`${child.name} - DNI ${child.dni}`);
                  showToast(`Copiado: ${child.name}`, "info");
                }}>
                   <div className="flex items-center gap-4">
                      <div className="w-12 h-12 rounded-xl bg-background border border-border flex items-center justify-center text-zinc-500 group-hover:text-accent">
                         <IdentificationCard size={24} weight="duotone" />
                      </div>
                      <div className="flex-1 min-w-0">
                         <p className="text-base font-black tracking-tight text-white uppercase truncate">{child.name}</p>
                         <div className="flex flex-wrap gap-1.5 mt-1.5">
                            <span className="text-[10px] bg-background/80 px-2 py-0.5 rounded-md border border-border text-zinc-500 font-black uppercase">{child.age} AÑOS</span>
                            {child.hasDisability && (
                              <span className="text-[10px] bg-danger/10 text-danger border border-danger/30 px-2 py-0.5 rounded-md font-black uppercase">DISCAPACIDAD</span>
                            )}
                          </div>
                      </div>
                   </div>
                   <div className="grid grid-cols-2 gap-3 pointer-events-none">
                      <div className="bg-background/30 p-2.5 rounded-lg border border-border/50">
                         <span className="text-[8px] font-black text-zinc-600 uppercase block tracking-widest">DNI</span>
                         <p className="text-xs font-bold text-white tracking-widest mt-1">{child.dni || "S/D"}</p>
                      </div>
                      <div className="bg-background/30 p-2.5 rounded-lg border border-border/50">
                         <span className="text-[8px] font-black text-zinc-600 uppercase block tracking-widest">F. Nacimiento</span>
                         <p className="text-xs font-bold text-white mt-1">{child.birthDate || "S/D"}</p>
                      </div>
                   </div>

                   {/* Documentación vinculada */}
                   <div className="border-t border-border/50 pt-3 space-y-2 pointer-events-none">
                     <span className="text-[8px] font-black text-zinc-600 uppercase tracking-widest">Documentación</span>
                     <div className="flex flex-col gap-1.5">
                       <div className={cn(
                         "flex items-center gap-2 px-2.5 py-1.5 rounded-lg text-[10px] font-black uppercase",
                         hasBirthCert 
                           ? "bg-success/10 text-success border border-success/20" 
                           : "bg-warning/10 text-warning border border-warning/20"
                       )}>
                         {hasBirthCert ? <CheckCircle size={12} weight="fill" /> : <WarningCircle size={12} weight="fill" />}
                         {hasBirthCert ? "Acta de Nacimiento ✓" : "Acta Pendiente"}
                       </div>
                       {child.hasDisability && (
                         <div className={cn(
                           "flex items-center gap-2 px-2.5 py-1.5 rounded-lg text-[10px] font-black uppercase",
                           hasCUD 
                             ? "bg-success/10 text-success border border-success/20" 
                             : "bg-danger/10 text-danger border border-danger/20 animate-pulse"
                         )}>
                           {hasCUD ? <CheckCircle size={12} weight="fill" /> : <FirstAidKit size={12} weight="fill" />}
                           {hasCUD ? "CUD Presentado ✓" : "CUD Requerido"}
                         </div>
                       )}
                     </div>
                   </div>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="rounded-2xl bg-surface border border-border overflow-hidden">
             <table className="w-full text-left border-collapse">
               <thead>
                 <tr className="bg-background/50 border-b border-border">
                   <th className="px-6 py-4 text-[9px] font-black text-zinc-500 uppercase tracking-widest">Nombre Completo</th>
                   <th className="px-6 py-4 text-[9px] font-black text-zinc-500 uppercase tracking-widest">DNI / Documento</th>
                   <th className="px-6 py-4 text-[9px] font-black text-zinc-500 uppercase tracking-widest text-center">Edad</th>
                   <th className="px-6 py-4 text-[9px] font-black text-zinc-500 uppercase tracking-widest">Fecha Nac.</th>
                   <th className="px-6 py-4 text-[9px] font-black text-zinc-500 uppercase tracking-widest">Condición</th>
                 </tr>
               </thead>
               <tbody className="divide-y divide-border/50">
                 {expediente.children.map((child, idx) => {
                    const linkedBirthCert = birthCerts.find(ev => 
                      ev.childFullName && ev.childFullName.toLowerCase() === child.name.toLowerCase()
                    );
                    const hasBirthCert = !!linkedBirthCert || !!child.birthCertificateId;
                    const linkedCUD = child.hasDisability 
                      ? disabilityCerts.find(ev => ev.childFullName && ev.childFullName.toLowerCase() === child.name.toLowerCase())
                      : null;

                    return (
                    <tr key={idx} className="group hover:bg-white/[0.02] transition-colors cursor-pointer" onClick={() => {
                      navigator.clipboard.writeText(child.dni);
                      showToast(`DNI Copiado: ${child.dni}`, "info");
                    }}>
                      <td className="px-6 py-4 text-sm font-bold text-white uppercase tracking-tight">{child.name}</td>
                      <td className="px-6 py-4 text-sm font-medium text-zinc-400 tracking-widest">{child.dni || "S/D"}</td>
                      <td className="px-6 py-4 text-sm font-bold text-white text-center">
                         <span className="bg-background px-3 py-1 rounded-full border border-border text-[10px]">{child.age}</span>
                      </td>
                      <td className="px-6 py-4 text-sm font-medium text-zinc-400">{child.birthDate || "S/D"}</td>
                      <td className="px-6 py-4">
                          <div className="flex flex-col gap-1.5">
                             {child.hasDisability && (
                               <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-danger/10 text-danger text-[9px] font-black border border-danger/30 uppercase w-fit">
                                  <WarningCircle size={10} weight="fill" />
                                  Discapacidad
                               </span>
                             )}
                             {hasBirthCert ? (
                               <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-success/10 text-success text-[9px] font-black border border-success/30 uppercase w-fit">
                                  <CheckCircle size={10} weight="fill" />
                                  Acta Vinculada
                               </span>
                             ) : (
                               <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-warning/10 text-warning text-[9px] font-black border border-warning/30 uppercase w-fit">
                                  <WarningCircle size={10} weight="fill" />
                                  Acta Pendiente
                               </span>
                             )}
                             {child.hasDisability && (
                               linkedCUD ? (
                                 <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-success/10 text-success text-[9px] font-black border border-success/30 uppercase w-fit">
                                    <CheckCircle size={10} weight="fill" />
                                    CUD Presentado
                                 </span>
                               ) : (
                                 <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-danger/10 text-danger text-[9px] font-black border border-danger/30 uppercase w-fit animate-pulse">
                                    <FirstAidKit size={10} weight="fill" />
                                    CUD Requerido
                                 </span>
                               )
                             )}
                          </div>
                       </td>
                    </tr>
                    );
                  })}
               </tbody>
             </table>
          </div>
        )}
      </section>
      
      {/* 4. Actas de Nacimiento */}
      <section>
        <div className="flex items-center gap-2 mb-8">
          <Certificate size={18} weight="fill" className="text-zinc-600" />
          <h6 className="text-[10px] font-black uppercase tracking-[0.2em] text-zinc-500">Prueba de Filiación: Actas de Nacimiento</h6>
          <span className={cn(
            "ml-auto px-2.5 py-1 rounded-lg text-[9px] font-black uppercase tracking-widest border",
            birthCerts.length > 0 
              ? "bg-warning/10 text-warning border-warning/20" 
              : "bg-danger/10 text-danger border-danger/20 shadow-lg shadow-danger/10"
          )}>
            {birthCerts.length > 0 ? `${birthCerts.length} acreditación${birthCerts.length > 1 ? "es" : ""}` : "Acción Requerida"}
          </span>
        </div>

        {birthCerts.length === 0 ? (
          <div className="p-12 rounded-3xl bg-surface/30 border-2 border-dashed border-border flex flex-col items-center justify-center gap-4 group hover:border-accent/20 transition-all">
            <Stamp size={40} weight="thin" className="text-zinc-700 opacity-50 group-hover:scale-110 transition-transform duration-500" />
            <div className="text-center">
              <p className="text-sm font-bold text-zinc-500">Sin partidas de nacimiento</p>
              <p className="text-[10px] uppercase tracking-widest text-zinc-700 mt-1 max-w-sm leading-relaxed">
                Es mandatorio adjuntar la partida de cada hijo declarado.<br />
                Podés solicitarla por el chat o subirla en &quot;Evidencia&quot;.
              </p>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {birthCerts.map((ev) => (
              <div key={ev.id} className="rounded-2xl bg-surface border border-border overflow-hidden flex flex-col group hover:border-border-strong transition-all hover:shadow-2xl hover:shadow-black/20">
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
                      className="object-cover group-hover:scale-110 transition-transform duration-1000"
                    />
                  ) : (
                    <div className="flex flex-col items-center gap-2 text-zinc-600 group-hover:text-accent transition-colors">
                      <Files size={40} weight="thin" />
                      <span className="text-[10px] uppercase tracking-widest font-black">DOCUMENTO PDF</span>
                    </div>
                  )}

                  <div className={cn(
                    "absolute top-3 right-3 px-2 py-1 rounded text-[8px] font-black uppercase tracking-widest border backdrop-blur-md",
                    ev.approved ? "bg-success/20 text-success border-success/30" :
                    ev.rejectionReason ? "bg-danger/20 text-danger border-danger/30" : "bg-warning/20 text-warning border-warning/30"
                  )}>
                    {ev.approved ? "Validada" : ev.rejectionReason ? "Observada" : "En Revisión"}
                  </div>
                </div>

                <div className="p-5 flex flex-col gap-4">
                  {ev.childFullName && (
                    <span className="text-[9px] font-black uppercase tracking-widest text-accent bg-accent/10 border border-accent/20 px-2.5 py-1 rounded-lg w-fit">
                      {ev.childFullName}
                    </span>
                  )}
                  <p className="text-xs font-bold text-white truncate uppercase tracking-tight" title={ev.fileName}>{ev.fileName}</p>

                  <div className="flex gap-2">
                    {!ev.approved && !ev.rejectionReason && (
                      <>
                        <button
                          onClick={() => onEvidenceStatusUpdate(ev.id, true)}
                          className="flex-1 px-3 py-2.5 rounded-xl bg-success/10 border border-success/20 text-success text-[10px] font-black uppercase tracking-widest hover:bg-success/20 transition-all active:scale-95"
                        >
                          Aprobar
                        </button>
                        <button
                          onClick={() => setObservingEvidence(ev)}
                          className="flex-1 px-3 py-2.5 rounded-xl bg-danger/10 border border-danger/20 text-danger text-[10px] font-black uppercase tracking-widest hover:bg-danger/20 transition-all active:scale-95"
                        >
                          Observar
                        </button>
                      </>
                    )}
                    <a
                      href={`http://localhost:8081/api/divorce/evidence/download/${ev.id}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="p-2.5 rounded-xl bg-background border border-border text-zinc-500 hover:text-white hover:border-border-strong transition-all"
                      title="Descargar acta"
                    >
                      <DownloadSimple size={18} weight="bold" />
                    </a>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* 4.b Certificados de Discapacidad (CUD) */}
      {childrenWithDisability.length > 0 && (
        <section>
          <div className="flex items-center gap-2 mb-8">
            <FirstAidKit size={18} weight="fill" className="text-zinc-600" />
            <h6 className="text-[10px] font-black uppercase tracking-[0.2em] text-zinc-500">Certificados de Discapacidad (CUD)</h6>
            <span className={cn(
              "ml-auto px-2.5 py-1 rounded-lg text-[9px] font-black uppercase tracking-widest border",
              disabilityCerts.length >= childrenWithDisability.length
                ? "bg-success/10 text-success border-success/20" 
                : "bg-danger/10 text-danger border-danger/20 shadow-lg shadow-danger/10"
            )}>
              {disabilityCerts.length >= childrenWithDisability.length 
                ? `${disabilityCerts.length} presentado${disabilityCerts.length > 1 ? "s" : ""}` 
                : `${disabilityCerts.length}/${childrenWithDisability.length} — Faltante`}
            </span>
          </div>

          {disabilityCerts.length === 0 ? (
            <div className="p-12 rounded-3xl bg-surface/30 border-2 border-dashed border-danger/30 flex flex-col items-center justify-center gap-4 group hover:border-danger/50 transition-all">
              <FirstAidKit size={40} weight="thin" className="text-danger/50 group-hover:scale-110 transition-transform duration-500" />
              <div className="text-center">
                <p className="text-sm font-bold text-zinc-500">CUD no presentado</p>
                <p className="text-[10px] uppercase tracking-widest text-zinc-700 mt-1 max-w-sm leading-relaxed">
                  Se requiere el Certificado Único de Discapacidad para:<br />
                  {childrenWithDisability.map(c => c.name).join(", ")}
                </p>
              </div>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {disabilityCerts.map((ev) => (
                <div key={ev.id} className="rounded-2xl bg-surface border border-border overflow-hidden flex flex-col group hover:border-border-strong transition-all hover:shadow-2xl hover:shadow-black/20">
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
                        className="object-cover group-hover:scale-110 transition-transform duration-1000"
                      />
                    ) : (
                      <div className="flex flex-col items-center gap-2 text-zinc-600 group-hover:text-accent transition-colors">
                        <Files size={40} weight="thin" />
                        <span className="text-[10px] uppercase tracking-widest font-black">DOCUMENTO PDF</span>
                      </div>
                    )}

                    <div className={cn(
                      "absolute top-3 right-3 px-2 py-1 rounded text-[8px] font-black uppercase tracking-widest border backdrop-blur-md",
                      ev.approved ? "bg-success/20 text-success border-success/30" :
                      ev.rejectionReason ? "bg-danger/20 text-danger border-danger/30" : "bg-warning/20 text-warning border-warning/30"
                    )}>
                      {ev.approved ? "Validada" : ev.rejectionReason ? "Observada" : "En Revisión"}
                    </div>
                  </div>

                  <div className="p-5 flex flex-col gap-4">
                    {ev.childFullName && (
                      <span className="text-[9px] font-black uppercase tracking-widest text-danger bg-danger/10 border border-danger/20 px-2.5 py-1 rounded-lg w-fit">
                        {ev.childFullName}
                      </span>
                    )}
                    <p className="text-xs font-bold text-white truncate uppercase tracking-tight" title={ev.fileName}>{ev.fileName}</p>

                    <div className="flex gap-2">
                      {!ev.approved && !ev.rejectionReason && (
                        <>
                          <button
                            onClick={() => onEvidenceStatusUpdate(ev.id, true)}
                            className="flex-1 px-3 py-2.5 rounded-xl bg-success/10 border border-success/20 text-success text-[10px] font-black uppercase tracking-widest hover:bg-success/20 transition-all active:scale-95"
                          >
                            Aprobar
                          </button>
                          <button
                            onClick={() => setObservingEvidence(ev)}
                            className="flex-1 px-3 py-2.5 rounded-xl bg-danger/10 border border-danger/20 text-danger text-[10px] font-black uppercase tracking-widest hover:bg-danger/20 transition-all active:scale-95"
                          >
                            Observar
                          </button>
                        </>
                      )}
                      <a
                        href={`http://localhost:8081/api/divorce/evidence/download/${ev.id}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="p-2.5 rounded-xl bg-background border border-border text-zinc-500 hover:text-white hover:border-border-strong transition-all"
                        title="Descargar CUD"
                      >
                        <DownloadSimple size={18} weight="bold" />
                      </a>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      )}

      {/* 5. Lightbox Previsualización */}
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
                    {previewEvidence.documentType === "BIRTH_CERT" ? "Partida de Nacimiento" : 
                     previewEvidence.documentType === "DISABILITY_CERT" ? "Certificado de Discapacidad" :
                     previewEvidence.documentType === "MARRIAGE_CERT" ? "Acta de Matrimonio" : previewEvidence.documentType}
                  </span>
                  {previewEvidence.childFullName && (
                    <span className="px-3 py-1 bg-accent/10 rounded-lg border border-accent/20 text-[10px] font-black text-accent uppercase tracking-widest">
                      {previewEvidence.childFullName}
                    </span>
                  )}
                  {previewEvidence.approved && (
                    <span className="px-3 py-1 bg-success/10 rounded-lg border border-success/20 text-[10px] font-black text-success uppercase tracking-widest flex items-center gap-2">
                      <CheckCircle size={12} weight="fill" />
                      Validada
                    </span>
                  )}
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
                      onClick={() => {
                        setObservingEvidence(previewEvidence);
                      }}
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
