"use client";

import { Sidebar } from "@/components/shared/Sidebar";
import { DoubleBezel } from "@/components/shared/DoubleBezel";
import { motion, AnimatePresence } from "framer-motion";
import { 
  Bell, 
  MagnifyingGlass, 
  Command,
  Plus
} from "@phosphor-icons/react";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen bg-background p-6 gap-6 overscroll-none overflow-hidden">
      {/* Background Micro-noise */}
      <div className="noise" />
      
      {/* Sidebar - Positioned via fixed in component */}
      <Sidebar />

      {/* Main Content Area */}
      <main className="flex-1 ml-20 md:ml-64 flex flex-col gap-6 h-[calc(100vh-3rem)]">
        
        {/* Global Header / Search Bar */}
        <header className="h-20 glass rounded-[2.5rem] px-8 flex items-center justify-between">
          <div className="flex items-center gap-4 w-1/3">
            <div className="relative w-full group">
              <MagnifyingGlass size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500 group-focus-within:text-accent transition-colors" />
              <input 
                type="text" 
                placeholder="Buscar casos, documentos, ciudadanos..."
                className="w-full bg-white/5 border border-white/5 rounded-2xl py-2.5 pl-12 pr-4 text-sm focus:outline-none focus:ring-1 focus:ring-accent/50 focus:bg-white/10 transition-all placeholder:text-zinc-600"
              />
              <div className="absolute right-4 top-1/2 -translate-y-1/2 flex items-center gap-1 opacity-50 text-[10px] font-bold text-zinc-500">
                <Command size={12} />
                <span>K</span>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-4">
            <button className="p-3 rounded-2xl hover:bg-white/5 text-zinc-400 hover:text-white transition-all relative">
              <Bell size={24} weight="duotone" />
              <span className="absolute top-3 right-3 w-2 h-2 bg-rose-500 rounded-full border border-background" />
            </button>
            <button className="flex items-center gap-2 bg-white text-zinc-950 px-5 py-2.5 rounded-2xl font-bold text-sm tracking-tight hover:scale-105 active:scale-95 transition-all">
              <Plus size={18} weight="bold" />
              Nuevo Caso
            </button>
          </div>
        </header>

        {/* Content Wrapper with DoubleBezel */}
        <div className="flex-1 min-h-0">
          <DoubleBezel className="h-full w-full" innerClassName="p-0">
            {children}
          </DoubleBezel>
        </div>
      </main>
    </div>
  );
}
