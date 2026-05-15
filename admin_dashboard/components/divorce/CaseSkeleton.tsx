"use client";

import { motion } from "framer-motion";

export function CaseSkeleton() {
  return (
    <div className="space-y-10 animate-pulse">
      {/* 1. Header Area */}
      <section className="space-y-6">
        <div className="flex items-center gap-3">
           <div className="w-8 h-8 rounded-xl bg-white/5" />
           <div className="h-3 w-40 bg-white/5 rounded" />
        </div>
        
        <div className="grid grid-cols-12 gap-4">
           <div className="col-span-12 lg:col-span-6 h-24 bg-white/5 rounded-2xl" />
           <div className="col-span-6 lg:col-span-3 h-24 bg-white/5 rounded-2xl" />
           <div className="col-span-6 lg:col-span-3 h-24 bg-white/5 rounded-2xl" />
        </div>
      </section>

      {/* 2. Secondary Area */}
      <section className="space-y-6">
        <div className="flex items-center gap-3">
           <div className="w-8 h-8 rounded-xl bg-white/5" />
           <div className="h-3 w-32 bg-white/5 rounded" />
        </div>

        <div className="grid grid-cols-12 gap-4">
           <div className="col-span-12 lg:col-span-6 h-24 bg-white/5 rounded-2xl" />
           <div className="col-span-12 lg:col-span-6 h-24 bg-white/5 rounded-2xl" />
        </div>
      </section>

      {/* 3. Third Area */}
      <section className="space-y-6">
        <div className="flex items-center gap-3">
           <div className="w-8 h-8 rounded-xl bg-white/5" />
           <div className="h-3 w-48 bg-white/5 rounded" />
        </div>

        <div className="grid grid-cols-3 gap-4">
           <div className="h-24 bg-white/5 rounded-2xl" />
           <div className="h-24 bg-white/5 rounded-2xl" />
           <div className="h-24 bg-white/5 rounded-2xl" />
        </div>
      </section>
    </div>
  );
}
