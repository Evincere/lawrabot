export const API_CONFIG = {
  BASE_URL: "http://localhost:8081",
  ENDPOINTS: {
    CASES: "/api/divorce/cases",
    EVIDENCE: (caseId: string) => `/api/divorce/evidence/${caseId}`,
    OBSERVATIONS: (expedienteId: string) => `/api/divorce/observations/expediente/${expedienteId}`,
    OBSERVATION_DETAIL: (id: string) => `/api/divorce/observations/${id}`,
    RESOLVE_OBSERVATION: (id: string) => `/api/divorce/observations/${id}/resolve`,
    DISMISS_OBSERVATION: (id: string) => `/api/divorce/observations/${id}/dismiss`,
    APPROVE_CASE: (caseId: string) => `/api/divorce/cases/${caseId}/approve`,
    DRAFTING: (caseId: string) => `/api/divorce/drafting/generate/${caseId}`,
    APPOINTMENTS: {
      SLOTS: "/api/divorce/appointments/slots",
      ACTIVE: (caseId: string) => `/api/divorce/appointments/case/${caseId}/active`,
      ALL: (caseId: string) => `/api/divorce/appointments/case/${caseId}`,
      BOOK: "/api/divorce/appointments",
    }
  }
};
