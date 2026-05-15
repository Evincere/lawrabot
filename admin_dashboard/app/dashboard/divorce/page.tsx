"use client";

import { useState, useEffect, useRef } from "react";
import { CaseInbox } from "@/components/divorce/CaseInbox";
import { CaseCaratula } from "@/components/divorce/CaseCaratula";
import { UtilityBar, UtilityAction } from "@/components/shared/UtilityBar";
import { FloatingChat } from "@/components/shared/FloatingChat";
import { ProfileTab } from "@/components/divorce/tabs/ProfileTab";
import { MarriageTab } from "@/components/divorce/tabs/MarriageTab";
import { EvidenceTab } from "@/components/divorce/tabs/EvidenceTab";
import { BlsgTab } from "@/components/divorce/tabs/BlsgTab";
import { AgreementTab } from "@/components/divorce/tabs/AgreementTab";
import { ProcessTab } from "@/components/divorce/tabs/ProcessTab";
import { HistoryTab } from "@/components/divorce/tabs/HistoryTab";
import { ObservationPanel } from "@/components/observations/ObservationPanel";
import { Expediente, EvidenceItem, UpdateCaseDataRequest } from "@/lib/types/divorce";
import { useToast } from "@/lib/contexts/ToastContext";
import { usePolledData } from "@/lib/hooks/usePolledData";
import { API_CONFIG } from "@/lib/config/api";
import { 
  CheckCircle,
  Archive,
  PaperPlaneTilt
} from "@phosphor-icons/react";
import { cn } from "@/lib/utils";
import { useMemo } from "react";

export default function DivorceWorkspace() {
  const [activeCaseId, setActiveCaseId] = useState<string | null>(null);
  const [activeTool, setActiveTool] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<string | null>(null);
  const [isInboxCollapsed, setIsInboxCollapsed] = useState(false);
  const scrollContainerRef = useRef<HTMLDivElement>(null);

  const { 
    data: cases, 
    isLoading: isCasesLoading, 
    isRefreshing: isCasesRefreshing,
    refetch: refetchCases 
  } = usePolledData<Expediente[]>({
    fetcher: () => fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.CASES}`).then(r => r.json()),
    interval: 10000,
    enabled: true,
    initialData: []
  });

  const activeCase = useMemo(() => {
    if (!activeCaseId) return cases?.[0] || null;
    return cases?.find(c => c.id === activeCaseId) || null;
  }, [cases, activeCaseId]);

  const viewMode = !activeCaseId ? "idle" : activeTab === null ? "overview" : "focus";

  useEffect(() => {
    if (viewMode === "focus") {
      setIsInboxCollapsed(true);
    } else {
      setIsInboxCollapsed(false);
    }
  }, [viewMode]);

  const handleToggleCollapse = () => {
    const newValue = !isInboxCollapsed;
    setIsInboxCollapsed(newValue);
    if (!newValue && viewMode === "focus") {
      setActiveTab(null);
    }
  };

  const handleTabClick = (tabId: string) => {
    if (activeTab === tabId) {
      setActiveTab(null);
    } else {
      setActiveTab(tabId);
    }
  };

  const handleCaseSelect = (doc: Expediente) => {
    setActiveCaseId(doc.id);
    setActiveTab(null);
  };

  // Reset scroll when tab changes
  useEffect(() => {
    if (scrollContainerRef.current) {
      scrollContainerRef.current.scrollTo({ top: 0, behavior: "smooth" });
    }
  }, [activeTab, activeCaseId]);
  
  // Evidence state
  const { 
    data: evidences, 
    refetch: refetchEvidences 
  } = usePolledData<EvidenceItem[]>({
    fetcher: () => activeCaseId 
      ? fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.EVIDENCE(activeCaseId)}`).then(r => r.json())
      : Promise.resolve([]),
    interval: 5000,
    enabled: !!activeCaseId && (activeTab === "marriage" || activeTab === "evidence" || activeTab === "profile" || activeTab === "blsg"),
    initialData: []
  });
  const [isUploading, setIsUploading] = useState(false);
  const [selectedDocType, setSelectedDocType] = useState("DNI_FRONT");

  // Agreement state
  const [isEditingAgreement, setIsEditingAgreement] = useState(false);
  const [draftAgreementText, setDraftAgreementText] = useState("");
  const [isSavingAgreement, setIsSavingAgreement] = useState(false);

  const { showToast } = useToast();

  const handleFileUpload = async (file: File) => {
    if (!activeCaseId) return;
    setIsUploading(true);
    try {
      const formData = new FormData();
      formData.append("file", file);
      formData.append("documentType", selectedDocType);
      const res = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.EVIDENCE(activeCaseId)}`, {
        method: "POST",
        body: formData,
      });
      if (res.ok) {
        refetchEvidences();
        showToast("Evidencia subida correctamente", "success");
      } else {
        showToast("Error al subir evidencia", "error");
      }
    } catch (err) {
      console.error("Upload error:", err);
      showToast("Error crítico en la subida", "error");
    } finally {
      setIsUploading(false);
    }
  };

  const handleEvidenceStatus = async (evidenceId: string, approved: boolean, reason?: string) => {
    try {
      const params = new URLSearchParams({ approved: String(approved) });
      if (reason) params.append("reason", reason);
      const res = await fetch(`${API_CONFIG.BASE_URL}/api/divorce/evidence/${evidenceId}/status?${params}`, {
        method: "PUT",
      });
      if (res.ok && activeCaseId) {
        refetchEvidences();
        showToast(approved ? "Evidencia validada" : "Evidencia impugnada", approved ? "success" : "warning");
      }
    } catch (err) {
      console.error("Error updating evidence status:", err);
      showToast("Error al actualizar estado", "error");
    }
  };

  const handleDeleteEvidence = async (evidenceId: string) => {
    if (!confirm("¿Está seguro de eliminar esta evidencia? Esta acción es irreversible.")) return;
    try {
      const res = await fetch(`${API_CONFIG.BASE_URL}/api/divorce/evidence/${evidenceId}`, {
        method: "DELETE",
      });
      if (res.ok) {
        refetchEvidences();
        showToast("Evidencia eliminada correctamente", "success");
      }
    } catch (err) {
      console.error("Error deleting evidence:", err);
      showToast("Error al eliminar evidencia", "error");
    }
  };

  const handleReclassifyEvidence = async (evidenceId: string, newType: string) => {
    try {
      const params = new URLSearchParams({ documentType: newType });
      const res = await fetch(`${API_CONFIG.BASE_URL}/api/divorce/evidence/${evidenceId}/type?${params}`, {
        method: "PUT",
      });
      if (res.ok) {
        refetchEvidences();
        showToast("Documento reclasificado", "success");
      }
    } catch (err) {
      console.error("Error reclassifying evidence:", err);
      showToast("Error al reclasificar", "error");
    }
  };

  const handleBlsgDecision = async (approved: boolean, observations: string) => {
    if (!activeCaseId) return;
    try {
      const params = new URLSearchParams({ approved: String(approved), observations });
      const res = await fetch(`${API_CONFIG.BASE_URL}/api/divorce/cases/${activeCaseId}/blsg-decision?${params}`, {
        method: "PUT",
      });
      if (res.ok) {
        showToast(approved ? "BLSG Aprobado con éxito" : "BLSG Rechazado", approved ? "success" : "warning");
      }
    } catch (err) {
      console.error("Error updating BLSG decision:", err);
      showToast("Error al procesar decisión", "error");
    }
  };

  const handleSaveAgreement = async () => {
    if (!activeCaseId) return;
    setIsSavingAgreement(true);
    try {
      const response = await fetch(`${API_CONFIG.BASE_URL}/api/divorce/cases/${activeCaseId}/raw-agreement`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ rawAgreementText: draftAgreementText })
      });
      if (response.ok) {
        showToast("Convenio guardado correctamente", "success");
        refetchCases(); 
        setIsEditingAgreement(false);
      }
    } catch (error) {
      console.error("Error saving agreement:", error);
      showToast("Error al guardar convenio", "error");
    } finally {
      setIsSavingAgreement(false);
    }
  };

  const handleUpdateCaseData = async (updateData: UpdateCaseDataRequest) => {
    if (!activeCaseId) return;
    try {
      const response = await fetch(`${API_CONFIG.BASE_URL}/api/divorce/cases/${activeCaseId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(updateData)
      });
      if (response.ok) {
        // Just refetch cases to update local state cleanly
        await refetchCases();
      } else {
        // El GlobalExceptionHandler devuelve { status, error, message, path, timestamp }
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || "Failed to update case");
      }
    } catch (error) {
      console.error("Error updating case data:", error);
      throw error;
    }
  };

  const handleArchive = async () => {
    if (!activeCaseId) return;
    // We still use confirm() for destructive actions as it's a native blocker, 
    // but the result feedback is now a toast.
    if (!confirm("¿Está seguro de archivar este expediente? Esta acción es irreversible.")) return;

    try {
      const response = await fetch(`${API_CONFIG.BASE_URL}/api/divorce/cases/${activeCaseId}`, {
        method: 'DELETE'
      });
      if (response.ok) {
        showToast("Expediente archivado", "info");
        setActiveCaseId(null);
        refetchCases();
      }
    } catch (error) {
      console.error("Error archiving case:", error);
      showToast("Error al archivar", "error");
    }
  };

  const generateDraft = async (asPdf: boolean) => {
    if (!activeCaseId) return;
    try {
      const response = await fetch(`${API_CONFIG.BASE_URL}/api/divorce/drafting/generate/${activeCaseId}?asPdf=${asPdf}`, {
        method: 'POST'
      });
      if (response.ok) {
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = asPdf ? "Demanda.pdf" : "Demanda.docx";
        a.click();
      }
    } catch (error) {
      console.error("Error drafting:", error);
    }
  };

  useEffect(() => {
    if (activeCase) {
      setDraftAgreementText(activeCase.rawAgreementText || "");
    }
  }, [activeCase]);

  const barActions: UtilityAction[] = [
    { 
      id: "consolidate", 
      icon: CheckCircle, 
      label: "Consolidar Expediente", 
      onClick: () => {},
      variant: "success"
    },
    { 
      id: "transfer", 
      icon: PaperPlaneTilt, 
      label: "Transferir a Defensoría", 
      onClick: () => {},
      variant: "accent"
    },
    { 
      id: "archive", 
      icon: Archive, 
      label: "Archivar Expediente", 
      onClick: handleArchive,
      variant: "danger"
    },
  ];

  const TABS = [
    { id: "profile", label: "Sujetos e Hijos" },
    { id: "marriage", label: "Matrimonio" },
    { id: "blsg", label: "Evaluación BLSG" },
  { id: "evidence", label: "Prueba Documental" },
    { id: "agreement", label: "Convenio Regulador" },
    { id: "process", label: "Gestión Procesal" },
    { id: "history", label: "Historial" },
    { id: "observations", label: "Observaciones" }
  ];

  return (
    <div className="flex h-full w-full overflow-hidden bg-background">
      
      <CaseInbox 
        cases={cases}
        isLoading={isCasesLoading}
        isRefreshing={isCasesRefreshing}
        activeCaseId={activeCaseId}
        onCaseSelect={handleCaseSelect}
        isCollapsed={isInboxCollapsed}
        onToggleCollapse={handleToggleCollapse}
      />

      <div className="flex-1 flex flex-col h-full overflow-hidden">
        
        <CaseCaratula expediente={activeCase} viewMode={viewMode} />

        {/* Tab Navigation */}
        <div className="flex items-center gap-1 px-10 py-4 border-b border-border bg-surface/[0.02]">
          {activeCase && TABS.map(tab => (
            <button
              key={tab.id}
              onClick={() => handleTabClick(tab.id)}
              className={cn(
                "px-5 py-2.5 rounded-xl text-[10px] font-black transition-all uppercase tracking-[0.15em] border",
                activeTab === tab.id 
                  ? "bg-accent/10 text-accent border-accent/20" 
                  : "text-zinc-500 hover:text-zinc-300 hover:bg-white/5 border-transparent"
              )}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Scrollable Content Area */}
        <div 
          ref={scrollContainerRef}
          className="flex-1 overflow-y-auto p-10 scrollbar-hide"
        >
          {activeCase ? (
            <div className="max-w-6xl mx-auto">
              {activeTab === "profile" && (
                <ProfileTab 
                  expediente={activeCase} 
                  evidences={evidences}
                  onUpdate={handleUpdateCaseData} 
                  onEvidenceStatusUpdate={handleEvidenceStatus}
                />
              )}
              
              {activeTab === "marriage" && (
                <MarriageTab 
                  expediente={activeCase}
                  evidences={evidences}
                  onUpdate={handleUpdateCaseData}
                  onEvidenceStatusUpdate={handleEvidenceStatus}
                />
              )}
              
              {activeTab === "blsg" && (
                <BlsgTab 
                  expediente={activeCase} 
                  onDecision={handleBlsgDecision}
                />
              )}

              {activeTab === "evidence" && (
                <EvidenceTab 
                  expediente={activeCase}
                  evidences={evidences}
                  isUploading={isUploading}
                  selectedDocType={selectedDocType}
                  setSelectedDocType={setSelectedDocType}
                  onFileUpload={handleFileUpload}
                  onEvidenceStatusUpdate={handleEvidenceStatus}
                  onDeleteEvidence={handleDeleteEvidence}
                  onReclassifyEvidence={handleReclassifyEvidence}
                />
              )}

              {activeTab === "agreement" && (
                <AgreementTab 
                  expediente={activeCase}
                  isEditing={isEditingAgreement}
                  setIsEditing={setIsEditingAgreement}
                  draftText={draftAgreementText}
                  setDraftText={setDraftAgreementText}
                  isSaving={isSavingAgreement}
                  onSave={handleSaveAgreement}
                />
              )}

              {activeTab === "process" && (
                <ProcessTab 
                  expediente={activeCase}
                  onGenerateDraft={generateDraft}
                  onFetchCases={refetchCases}
                />
              )}

              {activeTab === "history" && <HistoryTab expediente={activeCase} />}

              {activeTab === "observations" && (
                <ObservationPanel 
                  expedienteId={activeCase.id} 
                  operatorId="00000000-0000-0000-0000-000000000001" 
                  enabled={activeTab === "observations"}
                />
              )}
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center h-full opacity-30 select-none text-zinc-600">
               <Archive size={64} weight="thin" className="mb-4" />
               <p className="text-xl font-bold tracking-tighter">No hay casos seleccionados</p>
               <p className="text-sm">Inicie una gestión desde el panel lateral</p>
            </div>
          )}
        </div>
      </div>

      <UtilityBar
        activeTool={activeTool}
        onToolToggle={setActiveTool}
        actions={barActions}
      />

      <FloatingChat
        isOpen={activeTool === "chat"}
        onClose={() => setActiveTool(null)}
        caseId={activeCase?.id.substring(0, 8).toUpperCase() || "..."}
      />
    </div>
  );
}
