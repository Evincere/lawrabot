export interface Expediente {
  id: string;
  status: string;
  divorceType?: string;
  marriageDate?: string;
  deFactoSeparationDate?: string;
  
  // Datos de Acta de Matrimonio
  marriageCertificateNumber?: string;
  marriageRegistryBook?: string;
  marriageRegistryPage?: string;
  marriageRegistryOffice?: string;
  marriagePlace?: string;
  marriageCertificateId?: string;
  marriageCertificateIssuanceDate?: string;
  petitioner?: {
    fullName?: { fullName: string };
    dni?: string;
    cuil?: string;
    phoneNumber?: string;
    nationality?: string;
    email?: string;
    profession?: string;
    birthDate?: string;
    address?: {
      street: string;
      number: string;
      locality: string;
      floorAppartment?: string;
      neighborhood?: string;
      province?: string;
      zipCode?: string;
    };
  };
  respondent?: {
    fullName?: { fullName: string };
    dni?: string;
    cuil?: string;
    phoneNumber?: string;
    nationality?: string;
    email?: string;
    profession?: string;
    birthDate?: string;
    residentialAddress?: {
      street: string;
      number: string;
      locality: string;
      floorAppartment?: string;
      neighborhood?: string;
      province?: string;
      zipCode?: string;
    };
  };
  lastConjugalResidence?: {
    locality: string;
    street?: string;
    number?: string;
  };
  regulatoryAgreement?: Record<string, unknown>;
  rawAgreementText?: string;
  children?: { 
    id?: string;
    name: string; 
    dni: string;
    birthDate: string;
    age: number; 
    hasDisability: boolean; 
    birthCertificateId?: string;
  }[];
  socioEconomicProfile?: {
    avgMonthlyIncome: number;
    housingType: string;
    occupation: string;
    blsgScrapingResult: string;
    blsgObservations: string;
    
    // Fase 1: Scraping Externo
    scrapingFullName?: string;
    scrapingDni?: string;
    scrapingCuil?: string;
    scrapingBirthDate?: string;
    scrapingProvince?: string;
    scrapingSex?: string;
    scrapingJustification?: string;
    certificatePath?: string;

    // Fase 2: Recolección Activa
    vehiclesRegistered?: number;
    hasFormalEmployment?: boolean;

    // Fase 3: Evaluación Defensoría
    blsgApprovedByDefensoria?: boolean;
  };
  createdAt: string;
}

export interface UpdateCaseDataRequest {
  petitioner?: SpouseUpdateData;
  respondent?: SpouseUpdateData;
  marriageDate?: string;
  deFactoSeparationDate?: string;
  lastConjugalResidence?: { locality: string; street?: string; number?: string; };
  children?: ChildUpdateData[];
  divorceType?: string;
  
  // Datos del acta
  marriageCertificateId?: string;
  marriageCertificateIssuanceDate?: string;
  marriageCertificateNumber?: string;
  marriageRegistryBook?: string;
  marriageRegistryPage?: string;
  marriageRegistryOffice?: string;
  marriagePlace?: string;
}

export interface SpouseUpdateData {
  fullName: string;
  dni?: string;
  cuil?: string;
  phoneNumber?: string;
  email?: string;
  profession?: string;
  nationality?: string;
  birthDate?: string;
  address?: AddressData;
}

export interface ChildUpdateData {
  id?: string;
  name: string;
  dni?: string;
  birthDate?: string;
  hasDisability?: boolean;
  birthCertificateId?: string;
}

export interface AddressData {
  street: string;
  number: string;
  locality: string;
  floorAppartment?: string;
  neighborhood?: string;
  province?: string;
  zipCode?: string;
}

export interface EvidenceItem {
  id: string;
  documentType: string;
  fileName: string;
  filePath: string;
  mimeType: string;
  approved: boolean;
  rejectionReason: string | null;
  createdAt: string;
}

export type DashboardStatus = "Intake" | "Review" | "Approved" | "Action Required";

export interface AppointmentSlot {
  date: string;
  startTime: string;
  endTime: string;
}

export interface SignatureAppointment {
  id: string;
  expedienteId: string;
  status: "SCHEDULED" | "CONFIRMED" | "COMPLETED" | "CANCELLED" | "NO_SHOW";
  scheduledDateTime: string;
  location: string;
  contactPhone: string;
}

export interface MarriageCertificateRequest {
  certificateNumber: string;
  registryBook: string;
  registryPage: string;
  registryOffice: string;
  place: string;
}
