"use client";

import { usePathname } from "next/navigation";
import Link from "next/link";
import { cn } from "@/lib/utils";
import { motion } from "framer-motion";
import { 
  Gavel, 
  Users, 
  Pulse, 
  Files, 
  ChatCircleText, 
  Gear,
  ChartBar
} from "@phosphor-icons/react";

const NAV_ITEMS = [
  { name: "Divorcios", icon: Gavel, href: "/dashboard/divorce", badge: "12" },
  { name: "Ciudadanos (MCI)", icon: Users, href: "/dashboard/mci" },
  { name: "Expedientes", icon: Files, href: "/dashboard/records" },
  { name: "Analíticas", icon: ChartBar, href: "/dashboard/stats" },
  { name: "Salud del Sistema", icon: Pulse, href: "/dashboard/health" },
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="fixed left-6 top-6 bottom-6 w-20 md:w-64 z-40">
      <div className="h-full w-full glass rounded-[2.5rem] flex flex-col p-4">
        {/* Logo Section */}
        <div className="flex items-center gap-3 px-2 py-4 mb-8">
          <div className="w-10 h-10 rounded-2xl bg-accent flex items-center justify-center shadow-[0_0_20px_rgba(59,130,246,0.3)]">
            <ChatCircleText size={24} weight="bold" className="text-white" />
          </div>
          <div className="hidden md:block">
            <h2 className="font-bold tracking-tighter text-lg leading-none">LawraBot</h2>
            <p className="text-[10px] uppercase tracking-[0.2em] text-zinc-500 font-medium">Ops Center</p>
          </div>
        </div>

        {/* Navigation */}
        <nav className="flex-1 flex flex-col gap-2">
          {NAV_ITEMS.map((item) => {
            const isActive = pathname === item.href;
            const Icon = item.icon;
            
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "group relative flex items-center gap-3 px-3 py-3 rounded-2xl transition-all duration-300",
                  isActive 
                    ? "bg-accent/10 text-accent" 
                    : "text-zinc-500 hover:text-zinc-200 hover:bg-white/5"
                )}
              >
                <Icon size={24} weight={isActive ? "fill" : "regular"} />
                <span className="hidden md:block font-medium text-sm">{item.name}</span>
                
                {item.badge && (
                  <span className="absolute right-3 hidden md:flex w-5 h-5 rounded-full bg-accent text-[10px] font-bold text-white items-center justify-center">
                    {item.badge}
                  </span>
                )}

                {/* Active Indicator */}
                {isActive && (
                  <motion.div
                    layoutId="active-nav"
                    className="absolute left-0 w-1 h-6 bg-accent rounded-full"
                    transition={{ type: "spring", stiffness: 300, damping: 30 }}
                  />
                )}
              </Link>
            );
          })}
        </nav>

        {/* Bottom Actions */}
        <div className="mt-auto pt-4 border-t border-white/5">
          <Link
            href="/dashboard/settings"
            className="flex items-center gap-3 px-3 py-3 rounded-2xl text-zinc-500 hover:text-zinc-200 hover:bg-white/5 transition-all"
          >
            <Gear size={24} />
            <span className="hidden md:block font-medium text-sm">Configuración</span>
          </Link>
          
          <div className="mt-4 p-3 rounded-2xl bg-white/5 flex items-center gap-3">
            <div className="w-8 h-8 rounded-full bg-zinc-800 border border-white/10 overflow-hidden">
              <img 
                src="https://api.dicebear.com/7.x/avataaars/svg?seed=Admin" 
                alt="User"
                className="w-full h-full object-cover"
              />
            </div>
            <div className="hidden md:block overflow-hidden">
              <p className="text-xs font-bold truncate">Defensora Oficial</p>
              <p className="text-[10px] text-zinc-500 truncate">S. Rafael, Mendoza</p>
            </div>
          </div>
        </div>
      </div>
    </aside>
  );
}
