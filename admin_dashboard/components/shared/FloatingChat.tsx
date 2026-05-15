"use client";

import { useState, useEffect, useRef } from "react";
import {
  ChatCircleDots,
  PaperPlaneTilt,
  UserFocus,
  X,
  DotsThreeVertical,
  Circle
} from "@phosphor-icons/react";
import { motion, AnimatePresence } from "framer-motion";
import ReactMarkdown from "react-markdown";
import { cn } from "@/lib/utils";

interface FloatingChatProps {
  isOpen: boolean;
  onClose: () => void;
  caseId: string;
}

type MessageRole = "user" | "assistant" | "system" | "hitl";

interface ChatMessage {
  id: string;
  role: MessageRole;
  content: string;
  timestamp: number;
}

const MOCK_MESSAGES: ChatMessage[] = [
  { 
    id: "1", 
    role: "user", 
    content: "hola lawrabot, quiero iniciar mi divorcio, mi dni es 26598410", 
    timestamp: Date.now() - 3600000 
  },
  { 
    id: "2", 
    role: "assistant", 
    content: "Hola, soy **LawraBot**, la asistente legal automatizada del **Ministerio Público de la Defensa de Mendoza**. 🤝\n\nPuedo ayudarte con el proceso de divorcio. Primero, necesito verificar si posees el **Beneficio de Litigar Sin Gastos (BLSG)**. \n\n¿Deseas iniciar la consulta ahora o prefieres que te informe sobre los requisitos previos?", 
    timestamp: Date.now() - 3500000 
  },
  { 
    id: "3", 
    role: "hitl", 
    content: "Sistema detectó intención de inicio. Validando DNI...", 
    timestamp: Date.now() - 3400000 
  },
  { 
    id: "4", 
    role: "assistant", 
    content: "He verificado tu DNI. Actualmente tu estado en el sistema es **PENDIENTE**. Para continuar, por favor adjunta una foto de tu acta de matrimonio.", 
    timestamp: Date.now() - 3300000 
  }
];

function MessageBubble({ message }: { message: ChatMessage }) {
  const isLawra = message.role === "assistant";
  const isUser = message.role === "user";
  const isSystem = message.role === "system" || message.role === "hitl";

  if (isSystem) {
    return (
      <div className="flex justify-center my-4">
        <div className="px-4 py-1.5 rounded-full bg-white/5 border border-white/10 text-[9px] uppercase tracking-widest font-bold text-zinc-500">
          {message.content}
        </div>
      </div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 10, scale: 0.95 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      className={cn(
        "flex w-full mb-6",
        isUser ? "justify-end" : "justify-start"
      )}
    >
      <div className={cn(
        "max-w-[85%] flex flex-col gap-1.5",
        isUser ? "items-end" : "items-start"
      )}>
        {/* Avatar/Badge for Lawra */}
        {!isUser && (
          <div className="flex items-center gap-2 mb-1 pl-1">
            <div className="w-5 h-5 rounded-lg bg-accent flex items-center justify-center shadow-lg shadow-accent/20">
              <ChatCircleDots size={12} weight="bold" className="text-white" />
            </div>
            <span className="text-[10px] font-bold text-zinc-400 uppercase tracking-tighter">LawraBot</span>
          </div>
        )}

        <div className={cn(
          "px-5 py-4 rounded-3xl text-sm leading-relaxed shadow-2xl",
          isUser 
            ? "bg-zinc-900 border border-white/10 text-white rounded-tr-none" 
            : "glass border border-white/5 text-zinc-200 rounded-tl-none relative overflow-hidden"
        )}>
          {/* Subtle glow for Lawra messages */}
          {!isUser && <div className="absolute top-0 left-0 w-1 h-full bg-accent/20" />}
          
          <ReactMarkdown 
            components={{
              p: ({children}) => <p className="mb-0">{children}</p>,
              strong: ({children}) => <strong className="text-accent font-bold">{children}</strong>,
              ul: ({children}) => <ul className="list-disc ml-4 my-2">{children}</ul>,
            }}
          >
            {message.content}
          </ReactMarkdown>
        </div>
        
        <span className="text-[10px] text-zinc-600 font-medium px-2">
          {new Date(message.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
        </span>
      </div>
    </motion.div>
  );
}

export function FloatingChat({ isOpen, onClose, caseId }: FloatingChatProps) {
  const [messages, setMessages] = useState<ChatMessage[]>(MOCK_MESSAGES);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTo({
        top: scrollRef.current.scrollHeight,
        behavior: "smooth"
      });
    }
  }, [messages, isOpen]);

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.aside
          initial={{ x: "100%", opacity: 0 }}
          animate={{ x: 0, opacity: 1 }}
          exit={{ x: "100%", opacity: 0 }}
          transition={{ type: "spring", damping: 25, stiffness: 200 }}
          className="absolute inset-y-0 right-16 w-[480px] z-30 flex flex-col bg-zinc-950/80 backdrop-blur-3xl border-l border-white/10 shadow-[-20px_0_40px_rgba(0,0,0,0.5)]"
        >
          {/* Chat Header */}
          <header className="h-20 px-8 border-b border-white/5 flex items-center justify-between bg-black/20 shrink-0">
            <div className="flex items-center gap-4">
              <div className="w-10 h-10 rounded-2xl bg-accent/10 border border-accent/20 flex items-center justify-center">
                <ChatCircleDots
                  size={24}
                  weight="duotone"
                  className="text-accent"
                />
              </div>
              <div>
                <h4 className="text-sm font-bold tracking-tight text-white italic">
                  Conversación Activa
                </h4>
                <p className="text-[10px] text-zinc-500 font-medium uppercase tracking-widest">
                  Caso: {caseId}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <button className="p-2 rounded-xl hover:bg-white/5 text-zinc-500 transition-all">
                <DotsThreeVertical size={20} />
              </button>
              <button
                onClick={onClose}
                className="p-2 rounded-xl hover:bg-rose-500/10 text-zinc-500 hover:text-rose-400 transition-all"
              >
                <X size={20} />
              </button>
            </div>
          </header>

          {/* Chat Stream Area */}
          <div 
            ref={scrollRef}
            className="flex-1 overflow-y-auto p-8 scrollbar-hide"
          >
            {messages.length === 0 ? (
              <div className="flex flex-col items-center justify-center h-full opacity-20 text-center">
                <ChatCircleDots size={48} weight="thin" />
                <p className="text-xs mt-4">
                  Cargando historial de WhatsApp...
                </p>
              </div>
            ) : (
              <div className="flex flex-col">
                {messages.map((msg) => (
                  <MessageBubble key={msg.id} message={msg} />
                ))}
                
                {/* Typing Indicator */}
                <motion.div 
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="flex items-center gap-2 pl-1 mb-12 opacity-40 shrink-0"
                >
                   <div className="w-5 h-5 rounded-lg bg-zinc-800 flex items-center justify-center">
                      <Circle size={8} weight="fill" className="text-zinc-500 animate-pulse" />
                   </div>
                   <span className="text-[10px] font-bold text-zinc-600 uppercase tracking-tighter italic">Lawra está escribiendo...</span>
                </motion.div>
              </div>
            )}
          </div>

          {/* HITL Injection Bar */}
          <div className="p-6 border-t border-white/5 bg-gradient-to-t from-black to-black/40 shrink-0">
            <div className="p-1.5 rounded-[2rem] bg-white/5 border border-white/10 focus-within:border-accent/50 transition-all duration-500">
              <div className="relative flex items-center bg-zinc-950/80 rounded-[calc(2rem-0.5rem)] shadow-[inset_0_1px_1px_rgba(255,255,255,0.05)]">
                <div className="pl-4 text-zinc-500">
                  <UserFocus size={20} />
                </div>
                <input
                  type="text"
                  placeholder="Intervenir como operador..."
                  className="flex-1 bg-transparent py-4 px-3 text-xs font-medium outline-none text-white placeholder:text-zinc-700"
                />
                <div className="pr-3">
                  <button className="flex items-center gap-2 px-4 py-2 rounded-full bg-accent text-white font-bold text-[10px] hover:scale-105 active:scale-95 transition-all shadow-lg shadow-accent/20 group">
                    <PaperPlaneTilt
                      size={14}
                      weight="bold"
                    />
                  </button>
                </div>
              </div>
            </div>
            <p className="text-center text-[8px] text-zinc-600 font-bold uppercase tracking-[0.2em] mt-4">
               Canal Seguro • Encriptado de punto a punto
            </p>
          </div>
        </motion.aside>
      )}
    </AnimatePresence>
  );
}
