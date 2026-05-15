"use client";

import { useState, useEffect, useRef, useCallback } from "react";

interface UsePolledDataOptions<T> {
  fetcher: () => Promise<T>;
  interval?: number;
  enabled?: boolean;
  initialData?: T;
}

interface UsePolledDataResult<T> {
  data: T;
  isLoading: boolean;
  isRefreshing: boolean;
  error: string | null;
  refetch: () => Promise<void>;
}

/**
 * Hook para manejar polling síncrono con deduplicación y optimización de visibilidad.
 * 
 * @param options Configuración del polling
 * @returns Estado de los datos y controles
 */
export function usePolledData<T>({
  fetcher,
  interval = 10000,
  enabled = true,
  initialData,
}: UsePolledDataOptions<T>): UsePolledDataResult<T> {
  const [data, setData] = useState<T>(initialData as T);
  const [isLoading, setIsLoading] = useState(enabled);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchIdRef = useRef(0);
  const isFirstLoadRef = useRef(true);
  const timeoutRef = useRef<NodeJS.Timeout | null>(null);
  const fetcherRef = useRef(fetcher);

  // Mantener el ref actualizado con la última versión de la función
  useEffect(() => {
    fetcherRef.current = fetcher;
  }, [fetcher]);

  const performFetch = useCallback(async (isManual = false) => {
    const fetchId = ++fetchIdRef.current;
    
    if (isFirstLoadRef.current || isManual) {
      if (!isManual) setIsLoading(true);
    } else {
      setIsRefreshing(true);
    }

    try {
      const result = await fetcherRef.current();
      
      // Solo actualizar si este es el fetch más reciente (evita race conditions)
      if (fetchId === fetchIdRef.current) {
        setData(prev => {
          // Deduplicación simple pero efectiva para objetos JSON
          if (JSON.stringify(prev) === JSON.stringify(result)) {
            return prev;
          }
          return result;
        });
        setError(null);
      }
    } catch (err) {
      if (fetchId === fetchIdRef.current) {
        setError(err instanceof Error ? err.message : "Error desconocido");
      }
    } finally {
      if (fetchId === fetchIdRef.current) {
        setIsLoading(false);
        setIsRefreshing(false);
        isFirstLoadRef.current = false;
      }
    }
  }, []);

  // Manejador del ciclo de polling
  useEffect(() => {
    if (!enabled) {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
      return;
    }

    const tick = async () => {
      // Solo ejecutar si la pestaña está visible
      if (document.visibilityState === "visible") {
        await performFetch();
      }
      
      // Programar el siguiente tick
      timeoutRef.current = setTimeout(tick, interval);
    };

    // Carga inicial inmediata
    performFetch();
    
    // Iniciar el ciclo de polling
    timeoutRef.current = setTimeout(tick, interval);

    return () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    };
  }, [enabled, interval, performFetch]);

  // Escuchar cambios de visibilidad para retomar polling inmediatamente al volver
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible" && enabled) {
        performFetch();
      }
    };

    document.addEventListener("visibilitychange", handleVisibilityChange);
    return () => document.removeEventListener("visibilitychange", handleVisibilityChange);
  }, [enabled, performFetch]);

  return {
    data,
    isLoading,
    isRefreshing,
    error,
    refetch: () => performFetch(true)
  };
}
