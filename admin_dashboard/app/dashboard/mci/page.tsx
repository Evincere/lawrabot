"use client";

import { useState } from "react";
import { 
  MagnifyingGlass, 
  User, 
  ClockCounterClockwise, 
  IdentificationCard,
  Phone,
  EnvelopeSimple,
  MapPin,
  ArrowRight,
  Fingerprint
} from "@phosphor-icons/react";
import { motion, AnimatePresence } from "framer-motion";
import { cn } from "@/lib/utils";

// Mock data while we connect to real backend
const MOCK_HISTORY = [
  { id: "EXP-2023-991", role: "Alimentante", type: "Familia", status: "Cerrado", date: "12/05/2023" },
  { id: "EXP-2024-102", role: "Peticionante", type: "Divorcio", status: "En Curso", date: "02/02/2024" },
];

export default function MciLookupPage() {
  const [searchDni, setSearchDni] = useState("");
  const [citizen, setCitizen] = useState<any>(null);
  const [isSearching, setIsSearching] = useState(false);

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!searchDni) return;
    
    setIsSearching(true);
    try {
      const response = await fetch(`http://localhost:8081/api/mci/citizen/${searchDni}`);
      if (response.status === 404) {
        alert("Ciudadano no encontrado en el Master Client Index.");
        setCitizen(null);
        return;
      }
      if (!response.ok) throw new Error("Error en la búsqueda");
      
      const data = await response.json();
      setCitizen(data);
    } catch (error) {
      console.error("Search error:", error);
      alert("Error al conectar con el servidor MCI.");
    } finally {
      setIsSearching(false);
    }
  };

  return (
    <div className="flex-1 flex flex-col p-8 gap-8 overflow-y-auto scrollbar-hide">
      
      {/* Header Section */}
      <header className="flex flex-col gap-2">
        <div className="flex items-center gap-2">
           <div className="px-3 py-1 rounded-full bg-accent/10 border border-accent/20">
              <span className="text-[10px] uppercase tracking-[0.2em] font-bold text-accent">Master Client Index</span>
           </div>
        </div>
        <h1 className="text-4xl font-bold tracking-tighter text-white">Buscador de Identidad</h1>
        <p className="text-zinc-500 text-sm max-w-xl">
          Consulte la ficha única del ciudadano y su historial de intervenciones en el Ministerio Público de la Defensa.
        </p>
      </header>

      {/* Search Bar - Double Bezel Architecture */}
      <section className="w-full max-w-2xl">
        <form onSubmit={handleSearch} className="group relative p-2 rounded-[2rem] bg-white/5 border border-white/10 focus-within:border-accent/50 transition-all duration-500">
           <div className="flex items-center gap-4 bg-zinc-950/50 rounded-[calc(2rem-0.5rem)] px-6 py-4 shadow-[inset_0_1px_1px_rgba(255,255,255,0.05)]">
              <MagnifyingGlass size={22} className="text-zinc-500 group-focus-within:text-accent transition-colors" />
              <input 
                type="text" 
                value={searchDni}
                onChange={(e) => setSearchDni(e.target.value)}
                placeholder="Ingrese DNI o CUIL del ciudadano..."
                className="flex-1 bg-transparent text-lg font-medium outline-none text-white placeholder:text-zinc-600"
              />
              <button 
                type="submit"
                disabled={isSearching}
                className="flex items-center gap-2 px-6 py-2.5 rounded-full bg-accent text-white font-bold text-sm hover:scale-105 active:scale-95 transition-all shadow-lg shadow-accent/20 disabled:opacity-50"
              >
                {isSearching ? <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" /> : "Buscar"}
                <ArrowRight size={16} weight="bold" />
              </button>
           </div>
        </form>
      </section>

      {/* Results - Bento Layout */}
      <AnimatePresence mode="wait">
        {citizen && (
          <motion.div 
            initial={{ opacity: 0, y: 20, filter: "blur(10px)" }}
            animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
            exit={{ opacity: 0, y: -20, filter: "blur(10px)" }}
            className="grid grid-cols-12 gap-6"
          >
            
            {/* Citizen Core Card (Bento span-8) */}
            <div className="col-span-12 lg:col-span-8 p-1.5 rounded-[2.5rem] bg-white/5 border border-white/10 shadow-2xl">
               <div className="h-full bg-zinc-950/40 rounded-[calc(2.5rem-0.375rem)] p-8 flex flex-col gap-8 shadow-[inset_0_1px_1px_rgba(255,255,255,0.1)]">
                  <div className="flex items-start justify-between">
                    <div className="flex items-center gap-6">
                      <div className="w-20 h-20 rounded-3xl bg-gradient-to-br from-accent/20 to-accent/5 border border-accent/20 flex items-center justify-center">
                        <User size={40} weight="duotone" className="text-accent" />
                      </div>
                      <div>
                        <h2 className="text-3xl font-bold tracking-tight text-white">{citizen.fullName}</h2>
                        <div className="flex items-center gap-2 mt-1">
                          <IdentificationCard size={16} className="text-zinc-500" />
                          <span className="text-sm font-medium text-zinc-500">DNI {citizen.dni} • CUIL {citizen.cuil}</span>
                        </div>
                      </div>
                    </div>
                    <div className="flex flex-col items-end">
                      <span className="text-[10px] uppercase tracking-widest text-zinc-600 font-bold mb-1">MCI ID</span>
                      <code className="text-[10px] bg-white/5 px-2 py-1 rounded-md text-zinc-400 border border-white/5 font-mono">{citizen.id}</code>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {[
                      { icon: Phone, label: "Teléfono", value: citizen.phoneNumber },
                      { icon: EnvelopeSimple, label: "Email", value: citizen.email },
                      { icon: MapPin, label: "Domicilio Declarado", value: citizen.address, full: true },
                      { icon: Fingerprint, label: "Biometría GNA", value: "Verificada (2024)", status: "verified" },
                    ].map((item, i) => (
                      <div key={i} className={cn("p-5 rounded-3xl bg-white/[0.02] border border-white/5", item.full && "md:col-span-2")}>
                        <div className="flex items-center gap-3 mb-1">
                          <item.icon size={16} className="text-accent" />
                          <span className="text-[11px] uppercase tracking-widest text-zinc-500 font-bold">{item.label}</span>
                        </div>
                        <p className="text-sm font-medium text-zinc-200">{item.value}</p>
                      </div>
                    ))}
                  </div>
               </div>
            </div>

            {/* History Card (Bento span-4) */}
            <div className="col-span-12 lg:col-span-4 p-1.5 rounded-[2.5rem] bg-white/5 border border-white/10 shadow-2xl">
               <div className="h-full bg-zinc-950/40 rounded-[calc(2.5rem-0.375rem)] p-8 flex flex-col gap-6">
                  <div className="flex items-center gap-3">
                    <ClockCounterClockwise size={24} weight="bold" className="text-accent" />
                    <h3 className="text-xl font-bold tracking-tight text-white">Historial MPD</h3>
                  </div>
                  
                  <div className="flex flex-col gap-3">
                    {citizen.history.map((item: any, idx: number) => (
                      <div key={idx} className="group p-4 rounded-2xl bg-white/5 border border-white/5 hover:border-white/20 transition-all cursor-pointer">
                        <div className="flex justify-between items-start mb-2">
                           <span className="text-[10px] font-bold bg-white/10 px-2 py-0.5 rounded-full text-zinc-300 uppercase tracking-wider">{item.type}</span>
                           <span className="text-[10px] font-medium text-zinc-500">{item.date}</span>
                        </div>
                        <p className="text-sm font-bold text-white group-hover:text-accent transition-colors">{item.id}</p>
                        <p className="text-xs text-zinc-500 font-medium">Rol: {item.role} • {item.status}</p>
                      </div>
                    ))}
                  </div>

                  <button className="mt-auto w-full py-4 rounded-2xl bg-white/5 border border-white/10 text-zinc-400 font-bold text-xs hover:bg-accent/10 hover:text-accent hover:border-accent/20 transition-all flex items-center justify-center gap-2">
                    Ver Expedientes Completos
                    <ArrowRight size={14} />
                  </button>
               </div>
            </div>

          </motion.div>
        )}
      </AnimatePresence>

      {/* Empty State */}
      {!citizen && !isSearching && (
        <div className="flex-1 flex flex-col items-center justify-center text-center opacity-30 select-none">
          <MagnifyingGlass size={64} weight="thin" className="mb-4" />
          <p className="text-lg font-medium">Inicie una búsqueda para visualizar los datos</p>
          <p className="text-sm">Buscando en registros centrales del MPD y base consolidada MCI</p>
        </div>
      )}

    </div>
  );
}
