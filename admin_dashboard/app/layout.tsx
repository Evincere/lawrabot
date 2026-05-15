import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "LawraBot Ops Center — Ministerio Público de la Defensa",
  description: "Centro de operaciones y gestión de expedientes judiciales automatizados.",
};

import { SidebarProvider } from "@/lib/contexts/SidebarContext";
import { ToastProvider } from "@/lib/contexts/ToastContext";

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="es" className="h-full antialiased">
      <body className={`${geistSans.variable} ${geistMono.variable} min-h-full bg-background text-foreground flex flex-col`}>
        <ToastProvider>
          <SidebarProvider>
            {children}
          </SidebarProvider>
        </ToastProvider>
      </body>
    </html>
  );
}
