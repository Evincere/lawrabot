"use client";

import { useState } from "react";
import { CaseCard } from "@/components/shared/CaseCard";
import { 
  ChatCircleDots, 
  Files, 
  NotePencil, 
  PaperPlaneTilt,
  CheckCircle,
  WarningCircle,
  DownloadSimple,
  PencilSimple,
  PlusCircle,
  Eye
} from "@phosphor-icons/react";
import { motion, AnimatePresence } from "framer-motion";
import { cn } from "@/lib/utils";

const MOCK_CASES = [
  { id: "DIV-2024-001", name: "García s/ Divorcio", status: "Review", lastActivity: "hace 15m", location: "S. Rafael" },
  { id: "DIV-2024-002", name: "Martínez c/ Soler", status: "Intake", lastActivity: "hace 1h", location: "Mendoza Cap." },
  { id: "DIV-2024-003", name: "Lopez s/ Divorcio", status: "Approved", lastActivity: "hace 4h", location: "G. Cruz" },
  { id: "DIV-2024-004", name: "Rodriguez c/ Paz", status: "Action Required", lastActivity: "hace 10m", location: "Maipú" },
];

const MOCK_MESSAGES = [
  { role: "bot", content: "Hola, soy Lawra. Para iniciar el trámite necesito su DNI.", time: "10:15 AM" },
  { role: "user", content: "Hola, aquí tiene mi DNI.", time: "10:17 AM" },
  { role: "user", image: "/dni_mock.jpg", content: "Adjunto foto.", time: "10:17 AM" },
  { role: "bot", content: "Gracias. He extraído los datos: Juan Pérez, DNI 30.123.456. ¿Es correcto?", time: "10:18 AM" },
];

export default function DivorceWorkspace() {
  const [activeCase, setActiveCase] = useState(MOCK_CASES[0]);

  return (
    <div className="flex h-full w-full overflow-hidden">
      
      {/* 1. Panel Izquierdo: Lista de Casos */}
      <div className="w-80 border-r border-white/5 flex flex-col h-full bg-black/20">
        <div className="p-6 border-b border-white/5">
          <h3 className="text-xl font-bold tracking-tight mb-1">Divorcios</h3>
          <p className="text-xs text-zinc-500 font-medium">12 Casos Activos</p>
        </div>
        <div className="flex-1 overflow-y-auto p-4 scrollbar-hide">
          {MOCK_CASES.map((doc) => (
            <CaseCard 
              key={doc.id}
              {...doc as any}
              isActive={activeCase.id === doc.id}
              onClick={() => setActiveCase(doc as any)}
            />
          ))}
        </div>
      </div>

      {/* 2. Panel Central: Chat & HITL */}
      <div className="flex-1 flex flex-col border-r border-white/5 h-full relative">
        <header className="h-16 px-6 border-b border-white/5 flex items-center justify-between">
          <div className="flex items-center gap-3">
             <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
             <h4 className="text-sm font-bold tracking-tight">Conversación en Vivo</h4>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-[10px] uppercase tracking-widest text-zinc-500 font-bold">Modo:</span>
            <div className="glass-pill text-[10px] font-bold text-accent px-2 py-1 bg-accent/10 border-accent/20">
              Asistido por IA
            </div>
          </div>
        </header>

        {/* Chat Stream */}
        <div className="flex-1 overflow-y-auto p-8 flex flex-col gap-6 scrollbar-hide">
          {MOCK_MESSAGES.map((msg, idx) => (
            <div 
              key={idx} 
              className={cn(
                "flex flex-col max-w-[80%] gap-1",
                msg.role === "user" ? "self-end items-end" : "self-start items-start"
              )}
            >
              <div className={cn(
                "p-4 rounded-3xl text-sm leading-relaxed shadow-sm",
                msg.role === "user" 
                  ? "bg-accent text-white rounded-tr-none" 
                  : "bg-white/5 border border-white/10 rounded-tl-none"
              )}>
                {msg.content}
              </div>
              <span className="text-[10px] text-zinc-500 font-medium px-2">{msg.time}</span>
            </div>
          ))}
        </div>

        {/* Human-in-the-loop: Task Injection */}
        <div className="p-6 border-t border-white/5 bg-white/[0.02] backdrop-blur-xl">
           <div className="relative group">
              <input 
                type="text" 
                placeholder="Inyectar tarea al agente (ej: 'Pide el acta de matrimonio')..."
                className="w-full bg-white/5 border border-white/10 rounded-2xl py-4 pl-6 pr-14 text-sm focus:outline-none focus:ring-2 focus:ring-accent/50 focus:bg-white/10 transition-all"
              />
              <button className="absolute right-3 top-1/2 -translate-y-1/2 p-2.5 bg-accent text-white rounded-xl shadow-lg shadow-accent/20 hover:scale-105 active:scale-95 transition-all">
                <PaperPlaneTilt size={20} weight="bold" />
              </button>
           </div>
        </div>
      </div>

      {/* 3. Panel Derecho: Datos & Documentos */}
      <div className="w-[420px] h-full flex flex-col bg-black/10">
        <div className="h-16 px-6 border-b border-white/5 flex items-center">
           <h4 className="text-sm font-bold tracking-tight">Expediente Digital</h4>
        </div>

        <div className="flex-1 overflow-y-auto p-6 scrollbar-hide">
          
          {/* Perfil del Ciudadano (MCI) */}
          <section className="mb-8">
            <div className="flex items-center justify-between mb-4">
              <h5 className="text-[11px] uppercase tracking-[0.2em] text-zinc-500 font-bold">Datos del Ciudadano</h5>
              <button className="text-accent text-[11px] font-bold flex items-center gap-1 hover:underline">
                <NotePencil size={14} /> Corregir
              </button>
            </div>
            <div className="grid grid-cols-2 gap-4">
              {[
                { label: "Nombre Completo", value: "Juan Ignacio Pérez" },
                { label: "DNI", value: "30.123.456" },
                { label: "Domicilio", value: "Mitre 123, S. Rafael" },
                { label: "Elegibilidad BLSG", value: "Apto", status: "success" },
              ].map((item, i) => (
                <div key={i} className="p-3 rounded-2xl bg-white/5 border border-white/5">
                  <p className="text-[10px] text-zinc-500 font-medium mb-1">{item.label}</p>
                  <div className="flex items-center gap-2">
                    {item.status === 'success' && <CheckCircle size={14} weight="fill" className="text-emerald-500" />}
                    <p className="text-xs font-bold">{item.value}</p>
                  </div>
                </div>
              ))}
            </div>
          </section>

          {/* Galería de Documentos */}
          <section>
            <div className="flex items-center justify-between mb-4">
              <h5 className="text-[11px] uppercase tracking-[0.2em] text-zinc-500 font-bold">Documentación Aportada</h5>
              <div className="flex items-center gap-2">
                 <button className="p-1.5 rounded-lg hover:bg-white/5 text-zinc-400">
                    <PlusCircle size={18} />
                 </button>
              </div>
            </div>
            <div className="space-y-3">
              {[
                { name: "Acta de Matrimonio.pdf", type: "PDF", size: "1.2 MB", status: "verified" },
                { name: "DNI_Frente.jpg", type: "IMG", size: "800 KB", status: "verified" },
                { name: "Certificado_Ingresos.pdf", type: "PDF", size: "2.1 MB", status: "pending" },
              ].map((doc, idx) => (
                <div key={idx} className="group p-4 rounded-2xl bg-white/5 border border-white/5 hover:border-white/20 transition-all flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="p-2 rounded-xl bg-zinc-800 text-zinc-400 group-hover:text-white transition-colors">
                      <Files size={20} weight="duotone" />
                    </div>
                    <div>
                      <p className="text-xs font-bold truncate w-40">{doc.name}</p>
                      <p className="text-[10px] text-zinc-500">{doc.type} • {doc.size}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2 overflow-hidden">
                    {doc.status === 'pending' ? (
                      <div className="p-1.5 rounded-lg text-yellow-500 shadow-lg shadow-yellow-500/10">
                         <Eye size={18} />
                      </div>
                    ) : (
                      <div className="p-1.5 rounded-lg text-emerald-500 shadow-lg shadow-emerald-500/10">
                         <DownloadSimple size={18} />
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </section>

          {/* Action Footer */}
          <div className="mt-8 flex flex-col gap-3">
            <button className="w-full bg-emerald-500 text-white py-4 rounded-2xl font-bold text-sm shadow-lg shadow-emerald-500/20 hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center justify-center gap-2">
              <CheckCircle size={20} weight="bold" />
              Aprobar y Generar Borrador
            </button>
            <button className="w-full bg-white/5 border border-white/10 text-zinc-200 py-3 rounded-2xl font-bold text-xs hover:bg-white/10 transition-all">
              Marcar como 'Requiere Acción'
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
