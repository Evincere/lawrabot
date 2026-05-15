"use client";

import { useState, useEffect, useCallback } from "react";
import { usePolledData } from "./usePolledData";
import { API_CONFIG } from "../config/api";
import type {
  Observation,
  CreateObservationInput,
  ResolveObservationInput,
} from "@/lib/types/observation";

export function useObservations(expedienteId: string | null, enabled = true) {
  const { 
    data: observations, 
    isLoading, 
    isRefreshing,
    refetch,
    error: pollError 
  } = usePolledData<Observation[]>({
    fetcher: () => expedienteId 
      ? fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.OBSERVATIONS(expedienteId)}`).then(res => {
          if (!res.ok) throw new Error("Failed to fetch observations");
          return res.json();
        })
      : Promise.resolve([]),
    interval: 5000,
    enabled: enabled && !!expedienteId,
    initialData: []
  });

  const [error, setError] = useState<string | null>(null);

  // Sync internal error state with polling error
  useEffect(() => {
    if (pollError) setError(pollError);
  }, [pollError]);

  const fetchObservations = useCallback(async () => {
    await refetch();
  }, [refetch]);

  const createObservation = async (
    input: CreateObservationInput
  ): Promise<Observation | null> => {
    try {
      const res = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.CASES.replace("/cases", "/observations")}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(input),
      });
      if (!res.ok) throw new Error("Failed to create observation");
      const data = await res.json();
      await refetch();
      return data;
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
      return null;
    }
  };

  const resolveObservation = async (
    observationId: string,
    input: ResolveObservationInput
  ): Promise<Observation | null> => {
    try {
      const res = await fetch(
        `${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.RESOLVE_OBSERVATION(observationId)}`,
        {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(input),
        }
      );
      if (!res.ok) throw new Error("Failed to resolve observation");
      const data = await res.json();
      await refetch();
      return data;
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
      return null;
    }
  };

  const dismissObservation = async (
    observationId: string
  ): Promise<Observation | null> => {
    try {
      const res = await fetch(
        `${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.DISMISS_OBSERVATION(observationId)}`,
        { method: "PUT" }
      );
      if (!res.ok) throw new Error("Failed to dismiss observation");
      const data = await res.json();
      await refetch();
      return data;
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
      return null;
    }
  };

  const deleteObservation = async (
    observationId: string
  ): Promise<boolean> => {
    try {
      const res = await fetch(
        `${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.OBSERVATION_DETAIL(observationId)}`,
        { method: "DELETE" }
      );
      if (!res.ok) throw new Error("Failed to delete observation");
      await refetch();
      return true;
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
      return false;
    }
  };

  return {
    observations,
    isLoading,
    error,
    createObservation,
    resolveObservation,
    dismissObservation,
    deleteObservation,
    refetch: fetchObservations,
  };
}