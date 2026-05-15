"use client";

import React, { createContext, useContext, useState, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { 
  CheckCircle, 
  WarningCircle, 
  Info, 
  X 
} from "@phosphor-icons/react";
import { cn } from "@/lib/utils";

type ToastType = "success" | "error" | "info" | "warning";

interface Toast {
  id: string;
  message: string;
  type: ToastType;
}

interface ToastContextType {
  showToast: (message: string, type?: ToastType) => void;
}

const ToastContext = createContext<ToastContextType | undefined>(undefined);

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const showToast = useCallback((message: string, type: ToastType = "info") => {
    const id = Math.random().toString(36).substring(2, 9);
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 5000);
  }, []);

  const removeToast = (id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <div className="fixed bottom-8 right-8 z-[100] flex flex-col gap-3 pointer-events-none">
        <AnimatePresence>
          {toasts.map((toast) => (
            <motion.div
              key={toast.id}
              initial={{ opacity: 0, y: 20, scale: 0.9 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, scale: 0.9, transition: { duration: 0.2 } }}
              className={cn(
                "pointer-events-auto flex items-center gap-3 px-6 py-4 rounded-2xl border shadow-2xl min-w-[320px] bg-surface",
                toast.type === "success" && "border-success/30 text-success",
                toast.type === "error" && "border-danger/30 text-danger",
                toast.type === "warning" && "border-warning/30 text-warning",
                toast.type === "info" && "border-accent/30 text-accent"
              )}
            >
              <div className="shrink-0">
                {toast.type === "success" && <CheckCircle size={24} weight="bold" />}
                {toast.type === "error" && <WarningCircle size={24} weight="bold" />}
                {toast.type === "warning" && <WarningCircle size={24} weight="bold" />}
                {toast.type === "info" && <Info size={24} weight="bold" />}
              </div>
              <p className="flex-1 text-xs font-black uppercase tracking-widest leading-relaxed">
                {toast.message}
              </p>
              <button 
                onClick={() => removeToast(toast.id)}
                className="opacity-50 hover:opacity-100 transition-opacity"
              >
                <X size={16} weight="bold" />
              </button>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error("useToast must be used within a ToastProvider");
  }
  return context;
}
