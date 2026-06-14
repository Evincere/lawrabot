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
  regulatoryAgreement?: RegulatoryAgreement;
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
  childFullName: string | null;
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

// ============================================
// Convenio Regulador (Art. 439 CCyC)
// ============================================

export type AgreementStatus = "PROPOSED" | "ACCEPTED" | "REJECTED" | "HOMOLOGATED";

export type PersonalCareType =
  | "SHARED_INDISTINCT"
  | "SHARED_ALTERNATED"
  | "UNILATERAL_PETITIONER"
  | "UNILATERAL_RESPONDENT"
  | "OTHER";

export type MainResidence = "PETITIONER" | "RESPONDENT" | "BOTH_EQUITABLE" | "OTHER";

export type CommunicationRegimeType =
  | "BROAD_AND_FLEXIBLE"
  | "SPECIFIC_SCHEDULE"
  | "RESTRICTED_SUPERVISED"
  | "OTHER";

export type ProvisionType = "MONETARY" | "IN_KIND" | "MIXED" | "OTHER";

export type PaymentFrequency = "MONTHLY" | "FORTNIGHTLY" | "WEEKLY" | "ONE_OFF" | "OTHER";

export type PaymentMethod =
  | "BANK_TRANSFER"
  | "JUDICIAL_DEPOSIT"
  | "CASH"
  | "EMPLOYER_WITHHOLDING"
  | "OTHER";

export type UpdateMechanism = "IPC_INDEX" | "SALARY_PARITY" | "SMVM_PERCENTAGE" | "NONE" | "OTHER";

export type CurrencyParameter =
  | "ARS"
  | "USD"
  | "SALARY_PERCENTAGE"
  | "SMVM_PERCENTAGE"
  | "JUS_PERCENTAGE"
  | "OTHER";

export type HomeAttribution = "PETITIONER" | "RESPONDENT" | "BOTH_SALE" | "OTHER";

export type SpouseRole = "PETITIONER" | "RESPONDENT" | "BOTH" | "OTHER";

export type CompensationPayment = "SINGLE_PAYMENT" | "INSTALLMENTS" | "USUFRUCT" | "OTHER";

export interface AlimonyAmount {
  value?: number;
  currencyOrParameter?: CurrencyParameter;
  customParameter?: string;
}

export interface PersonalCare {
  id?: string;
  careType?: PersonalCareType;
  customCareType?: string;
  mainResidence?: MainResidence;
  customMainResidence?: string;
}

export interface CommunicationRegime {
  id?: string;
  regimeType?: CommunicationRegimeType;
  customRegimeType?: string;
  regularSchedule?: string;
  holidaySchedule?: string;
  pickUpLocationDescription?: string;
  supervisorName?: string;
}

export interface AlimonyProvision {
  id?: string;
  provisionType?: ProvisionType;
  customProvisionType?: string;
  amount?: AlimonyAmount;
  paymentFrequency?: PaymentFrequency;
  customPaymentFrequency?: string;
  paymentMethod?: PaymentMethod;
  customPaymentMethod?: string;
  paymentDetails?: string;
  updateMechanism?: UpdateMechanism;
  customUpdateMechanism?: string;
}

export interface AssetDistribution {
  id?: string;
  homeAttributionTo?: HomeAttribution;
  customHomeAttributionTo?: string;
  homeAttributionTerm?: string;
  assetsSummary?: string;
  liabilitiesSummary?: string;
}

export interface EconomicCompensation {
  id?: string;
  appliesEconomicCompensation?: boolean;
  beneficiary?: SpouseRole;
  imbalanceJustification?: string;
  paymentMethod?: CompensationPayment;
  customPaymentMethod?: string;
  compensationAmount?: AlimonyAmount;
  installmentsCount?: number;
  updateMechanism?: UpdateMechanism;
  customUpdateMechanism?: string;
  inKindPaymentDescription?: string;
}

export interface RegulatoryAgreement {
  id?: string;
  status?: AgreementStatus;
  includesChildrenProvisions?: boolean;
  personalCare?: PersonalCare;
  communicationRegime?: CommunicationRegime;
  alimonyProvision?: AlimonyProvision;
  assetDistribution?: AssetDistribution;
  economicCompensation?: EconomicCompensation;
}

