"use client";

import { Expediente } from "@/lib/types/divorce";
import { 
  ShieldCheck, 
  Clock,
  Circle
} from "@phosphor-icons/react";
import { cn } from "@/lib/utils";

interface HistoryTabProps {
  expediente: Expediente;
}

export function HistoryTab({ expediente }: HistoryTabProps) {
  const formatLastActivity = (isoString: string) => {
    if (!isoString) return "Recientemente";
    const date = new Date(isoString);
    return date.toLocaleString('es-AR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  // Mock timeline events based on case data for now
  const events = [
    {
      id: "1",
      title: "Expediente Iniciado",
      description: "LawraBot comenzó la recolección de datos vía WhatsApp.",
      timestamp: expediente.createdAt,
      type: "system",
      icon: Clock,
      color: "text-accent"
    },
    {
      id: "2",
      title: "Recolección Automatizada Completada",
      description: "Se han recolectado los datos básicos de los cónyuges y el convenio sugerido.",
      timestamp: expediente.createdAt, // Mocking same as created for demo
      type: "bot",
      icon: ShieldCheck,
      color: "text-success"
    }
  ];

  return (
    <div className="space-y-10">
      <section>
        <div className="flex items-center gap-3 mb-8">
           <div className="p-2 rounded-xl bg-accent/10 border border-accent/20">
             <Clock size={20} weight="duotone" className="text-accent" />
           </div>
           <h5 className="text-[10px] uppercase tracking-[0.2em] text-zinc-500 font-black">Historial de Movimientos y Auditoría</h5>
        </div>

        <div className="relative pl-8 space-y-12 before:absolute before:left-[11px] before:top-2 before:bottom-2 before:w-[2px] before:bg-border">
          {events.map((event, idx) => (
            <div key={event.id} className="relative">
              {/* Dot */}
              <div className={cn(
                "absolute -left-[30px] top-1.5 w-4 h-4 rounded-full border-4 bg-background z-10",
                event.color.replace('text-', 'border-')
              )} />
              
              <div className="flex flex-col gap-1">
                <div className="flex items-center gap-3">
                   <span className="text-xs font-black text-white uppercase tracking-widest">{event.title}</span>
                   <span className="text-[10px] font-bold text-zinc-600 legal-text">{formatLastActivity(event.timestamp)}</span>
                </div>
                <p className="text-sm text-zinc-400 mt-1 leading-relaxed max-w-2xl">
                  {event.description}
                </p>
                <div className="mt-2 flex items-center gap-2">
                   <div className="px-2 py-0.5 rounded bg-surface border border-border text-[8px] font-black uppercase tracking-widest text-zinc-500">
                     SISTEMA
                   </div>
                   {idx === events.length - 1 && (
                     <div className="flex items-center gap-1.5 text-[9px] text-accent font-bold">
                        <Circle size={8} weight="fill" className="animate-pulse" />
                        Estado Actual
                     </div>
                   )}
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Empty history message if needed */}
      {events.length === 0 && (
        <div className="p-20 flex flex-col items-center justify-center text-zinc-700 gap-4">
           <Clock size={48} weight="thin" className="opacity-20" />
           <p className="text-xs font-bold uppercase tracking-widest">No hay eventos registrados</p>
        </div>
      )}
    </div>
  );
}
