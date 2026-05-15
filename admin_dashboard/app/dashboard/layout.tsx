"use client";

import { Sidebar } from "@/components/shared/Sidebar";
import { DoubleBezel } from "@/components/shared/DoubleBezel";
import { SidebarProvider, useSidebar } from "@/lib/contexts/SidebarContext";
import { cn } from "@/lib/utils";
import { 
  Bell, 
  MagnifyingGlass, 
  Command,
  Plus
} from "@phosphor-icons/react";

function DashboardContent({ children }: { children: React.ReactNode }) {
  const { isCollapsed } = useSidebar();

  return (
    <div className="flex min-h-screen bg-background p-6 gap-6 overscroll-none overflow-hidden relative">
      {/* Background Micro-noise */}
      <div className="noise" />
      
      <Sidebar />

      {/* Main Content Area */}
      <main className={cn(
        "flex-1 flex flex-col gap-0 h-[calc(100vh-3rem)] transition-all duration-500 ease-in-out",
        isCollapsed ? "ml-20" : "ml-64"
      )}>
        
        {/* Global Header / Search Bar */}
        <header className="h-14 glass rounded-[2rem] px-6 flex items-center justify-between shadow-lg">
          <div className="flex items-center gap-4 w-1/3">
            <div className="relative w-full group">
              <MagnifyingGlass size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500 group-focus-within:text-accent transition-colors" />
              <input 
                type="text" 
                placeholder="Buscar casos o ciudadanos..."
                className="w-full bg-white/5 border border-white/5 rounded-xl py-2 pl-10 pr-4 text-xs focus:outline-none focus:ring-1 focus:ring-accent/50 focus:bg-white/10 transition-all placeholder:text-zinc-600"
              />
              <div className="absolute right-4 top-1/2 -translate-y-1/2 flex items-center gap-1 opacity-50 text-[9px] font-bold text-zinc-500">
                <Command size={10} />
                <span>K</span>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <button className="p-2 rounded-xl hover:bg-white/5 text-zinc-400 hover:text-white transition-all relative group">
              <Bell size={20} weight="duotone" className="group-hover:rotate-12 transition-transform" />
              <span className="absolute top-2.5 right-2.5 w-1.5 h-1.5 bg-rose-500 rounded-full border border-zinc-900" />
            </button>
            <button className="flex items-center gap-2 bg-accent text-white px-4 py-2 rounded-xl font-bold text-xs tracking-tight hover:scale-105 active:scale-95 transition-all shadow-md shadow-accent/20">
              <Plus size={14} weight="bold" />
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

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <SidebarProvider>
      <DashboardContent>{children}</DashboardContent>
    </SidebarProvider>
  );
}
