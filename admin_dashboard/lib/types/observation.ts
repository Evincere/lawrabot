export type ObservationSeverity = 'ERROR' | 'WARNING' | 'INFO';
export type ObservationStatus = 'PENDING' | 'ASSIGNED_TO_BOT' | 'RESOLVED' | 'DISMISSED';
export type TaskType = 'CLARIFY_DATA' | 'REQUEST_DOCUMENT' | 'NOTIFY_APPOINTMENT' | 'CORRECT_ERROR';
export type TaskStatus = 'PENDING' | 'SENT_TO_BOT' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';

export interface ObservationTask {
  id: string;
  type: TaskType;
  status: TaskStatus;
  assignedTo: string;
  messageTemplate: string;
  createdAt: string;
  sentAt?: string;
  completedAt?: string;
  responseData?: string;
}

export interface Observation {
  id: string;
  expedienteId: string;
  fieldName: string;
  severity: ObservationSeverity;
  message: string;
  suggestedValue?: string;
  status: ObservationStatus;
  createdBy?: string;
  createdAt: string;
  resolvedAt?: string;
  resolutionNotes?: string;
  task?: ObservationTask;
}

export interface CreateObservationInput {
  expedienteId: string;
  fieldName: string;
  severity: ObservationSeverity;
  message: string;
  suggestedValue?: string;
  createTask: boolean;
  taskType?: TaskType;
  isImmediate?: boolean;
  operatorId?: string;
}

export interface ResolveObservationInput {
  notes: string;
  responseData?: string;
}