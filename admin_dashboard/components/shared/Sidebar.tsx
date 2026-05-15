"use client";

import { usePathname } from "next/navigation";
import Link from "next/link";
import Image from "next/image";
import { cn } from "@/lib/utils";
import { motion, AnimatePresence } from "framer-motion";
import { useSidebar } from "@/lib/contexts/SidebarContext";
import { 
  Gavel, 
  Users, 
  Pulse, 
  Files, 
  ChatCircleText, 
  Gear,
  ChartBar,
  CaretLeft,
  CaretRight,
  ShieldCheck
} from "@phosphor-icons/react";

const NAV_ITEMS = [
  { name: "Divorcios", icon: Gavel, href: "/dashboard/divorce" },
  { name: "Ciudadanos (MCI)", icon: Users, href: "/dashboard/mci" },
  { name: "Expedientes", icon: Files, href: "/dashboard/records" },
  { name: "Analíticas", icon: ChartBar, href: "/dashboard/stats" },
  { name: "Salud del Sistema", icon: Pulse, href: "/dashboard/health" },
];

export function Sidebar() {
  const pathname = usePathname();
  const { isCollapsed, toggleSidebar } = useSidebar();

  return (
    <aside className={cn(
      "fixed left-6 top-6 bottom-6 z-40 transition-all duration-500 ease-in-out",
      isCollapsed ? "w-20" : "w-64"
    )}>
      <div className="h-full w-full bg-surface border border-border rounded-[1.5rem] flex flex-col p-4 relative overflow-hidden shadow-2xl">
        {/* Subtle noise and texture */}
        <div className="absolute inset-0 noise pointer-events-none opacity-20" />
        
        {/* Toggle Button */}
        <button 
          onClick={toggleSidebar}
          className="absolute -right-3 top-24 w-6 h-6 rounded-full bg-surface border border-border-strong flex items-center justify-center text-zinc-400 shadow-xl hover:text-white hover:scale-110 active:scale-95 transition-all z-50 bg-background"
          aria-label={isCollapsed ? "Expand Sidebar" : "Collapse Sidebar"}
        >
          {isCollapsed ? <CaretRight size={10} weight="bold" /> : <CaretLeft size={10} weight="bold" />}
        </button>

        {/* Logo Section */}
        <div className="flex items-center gap-3 px-2 py-4 mb-8 overflow-hidden relative z-10">
          <div className="w-10 h-10 min-w-[2.5rem] rounded-xl bg-accent flex items-center justify-center shadow-[0_0_25px_rgba(37,99,235,0.4)]">
            <ShieldCheck size={24} weight="bold" className="text-white" />
          </div>
          <AnimatePresence mode="wait">
            {!isCollapsed && (
              <motion.div
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -10 }}
                transition={{ duration: 0.2 }}
                className="whitespace-nowrap"
              >
                <h2 className="font-black tracking-tighter text-lg leading-none text-white italic">LawraBot</h2>
                <p className="text-[9px] uppercase tracking-[0.3em] text-zinc-600 font-black mt-1">Ops Center</p>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        {/* Navigation */}
        <nav className="flex-1 flex flex-col gap-1 relative z-10">
          {NAV_ITEMS.map((item) => {
            const isActive = pathname === item.href;
            const Icon = item.icon;
            
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "group relative flex items-center gap-3 px-3 py-3 rounded-xl transition-all duration-300",
                  isActive 
                    ? "bg-accent/10 text-accent border border-accent/20" 
                    : "text-zinc-500 hover:text-zinc-200 hover:bg-white/5 border border-transparent"
                )}
              >
                <div className="min-w-[1.5rem] flex items-center justify-center">
                  <Icon size={22} weight={isActive ? "bold" : "duotone"} />
                </div>
                
                <AnimatePresence mode="wait">
                  {!isCollapsed && (
                    <motion.span
                      initial={{ opacity: 0, x: -10 }}
                      animate={{ opacity: 1, x: 0 }}
                      exit={{ opacity: 0, x: -10 }}
                      className="font-bold text-[11px] uppercase tracking-widest whitespace-nowrap"
                    >
                      {item.name}
                    </motion.span>
                  )}
                </AnimatePresence>
                
                {isActive && (
                  <motion.div
                    layoutId="active-nav-glow"
                    className="absolute inset-0 bg-accent/5 rounded-xl pointer-events-none"
                    transition={{ type: "spring", stiffness: 300, damping: 30 }}
                  />
                )}
              </Link>
            );
          })}
        </nav>

        {/* Bottom Actions */}
        <div className="mt-auto pt-4 border-t border-border relative z-10">
          <Link
            href="/dashboard/settings"
            className="flex items-center gap-3 px-3 py-3 rounded-xl text-zinc-500 hover:text-zinc-200 hover:bg-white/5 transition-all text-[11px] font-bold uppercase tracking-widest"
          >
            <div className="min-w-[1.5rem] flex items-center justify-center">
              <Gear size={22} weight="duotone" />
            </div>
            {!isCollapsed && <span>Configuración</span>}
          </Link>
          
          <div className={cn(
            "mt-4 p-3 rounded-xl bg-background border border-border flex items-center gap-3 transition-all",
            isCollapsed ? "justify-center" : ""
          )}>
            <div className="w-8 h-8 min-w-[2rem] rounded-lg bg-zinc-900 border border-border overflow-hidden relative">
              <Image 
                src="https://api.dicebear.com/7.x/avataaars/svg?seed=Admin" 
                alt="User"
                fill
                className="object-cover grayscale"
              />
            </div>
            {!isCollapsed && (
              <div className="overflow-hidden">
                <p className="text-[10px] font-black truncate text-white uppercase tracking-tighter">S. Rafael, MZA</p>
                <p className="text-[9px] text-zinc-600 font-bold uppercase tracking-widest truncate">Defensora Oficial</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </aside>
  );
}
