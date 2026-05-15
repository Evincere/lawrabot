# Plan de Implementación - Sistema de Observaciones

## FASE 1: Backend (Java/Spring Boot)

### Paso 1.1: Crear Entidades JPA

**ObservationJpaEntity.java**
```java
@Entity
@Table(name = "observations")
public class ObservationJpaEntity {
    @Id
    private UUID id;
    
    @Column(name = "expediente_id")
    private UUID expedienteId;
    
    @Column(name = "field_name")
    private String fieldName; // ej: "spouse.dni", "agreement.alimonyAmount"
    
    @Enumerated(EnumType.STRING)
    private ObservationSeverity severity; // ERROR, WARNING, INFO
    
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "suggested_value")
    private String suggestedValue;
    
    @Enumerated(EnumType.STRING)
    private ObservationStatus status; // PENDING, ASSIGNED_TO_BOT, RESOLVED, DISMISSED
    
    @Column(name = "created_by")
    private UUID createdBy; // operatorId
    
    @OneToOne(mappedBy = "observation", cascade = CascadeType.ALL)
    private TaskJpaEntity task;
    
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String resolutionNotes;
}
```

**TaskJpaEntity.java**
```java
@Entity
@Table(name = "tasks")
public class TaskJpaEntity {
    @Id
    private UUID id;
    
    @OneToOne
    @JoinColumn(name = "observation_id")
    private ObservationJpaEntity observation;
    
    @Enumerated(EnumType.STRING)
    private TaskType type; // CLARIFY_DATA, REQUEST_DOCUMENT, NOTIFY_APPOINTMENT, CORRECT_ERROR
    
    @Column(name = "message_template", columnDefinition = "TEXT")
    private String messageTemplate;
    
    @Enumerated(EnumType.STRING)
    private TaskStatus status; // PENDING, SENT_TO_BOT, IN_PROGRESS, COMPLETED, FAILED
    
    @Column(name = "assigned_to")
    private String assignedTo; // "LAWRA_BOT"
    
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime completedAt;
    private String responseData; // JSON con respuesta del ciudadano
}
```

### Paso 1.2: Crear Enums

```java
public enum ObservationSeverity {
    ERROR,    // Bloquea generación de documentos
    WARNING,  // Advertencia pero permite continuar
    INFO      // Informativo
}

public enum ObservationStatus {
    PENDING,          // Creada, no asignada
    ASSIGNED_TO_BOT,  // Enviada a LawraBot
    RESOLVED,         // Ciudadano respondió, operador validó
    DISMISSED         // Operador decidió ignorar
}

public enum TaskType {
    CLARIFY_DATA,      // Aclarar dato confuso
    REQUEST_DOCUMENT,  // Solicitar documento
    NOTIFY_APPOINTMENT,// Notificar cita
    CORRECT_ERROR      // Corregir error detectado
}

public enum TaskStatus {
    PENDING,      // Creada, esperando envío
    SENT_TO_BOT,  // Enviada a LawraBot
    IN_PROGRESS,  // LawraBot procesando
    COMPLETED,    // Ciudadano respondió
    FAILED        // Error en envío o respuesta
}
```

### Paso 1.3: Crear Repositorios

```java
@Repository
public interface ObservationJpaRepository extends JpaRepository<ObservationJpaEntity, UUID> {
    List<ObservationJpaEntity> findByExpedienteId(UUID expedienteId);
    List<ObservationJpaEntity> findByExpedienteIdAndStatus(UUID expedienteId, ObservationStatus status);
    long countByExpedienteIdAndStatusIn(UUID expedienteId, List<ObservationStatus> statuses);
}

@Repository
public interface TaskJpaRepository extends JpaRepository<TaskJpaEntity, UUID> {
    List<TaskJpaEntity> findByStatus(TaskStatus status);
    List<TaskJpaEntity> findByObservationExpedienteId(UUID expedienteId);
}
```

### Paso 1.4: Crear Servicio

```java
@Service
@RequiredArgsConstructor
public class ObservationService {
    private final ObservationJpaRepository observationRepo;
    private final TaskJpaRepository taskRepo;
    private final LawraBotNotificationService lawraBotService;
    
    @Transactional
    public ObservationResponse createObservation(CreateObservationCommand cmd) {
        // 1. Crear observación
        ObservationJpaEntity obs = new ObservationJpaEntity();
        obs.setId(UUID.randomUUID());
        obs.setExpedienteId(cmd.getExpedienteId());
        obs.setFieldName(cmd.getFieldName());
        obs.setSeverity(cmd.getSeverity());
        obs.setMessage(cmd.getMessage());
        obs.setSuggestedValue(cmd.getSuggestedValue());
        obs.setStatus(ObservationStatus.PENDING);
        obs.setCreatedBy(cmd.getOperatorId());
        obs.setCreatedAt(LocalDateTime.now());
        
        observationRepo.save(obs);
        
        // 2. Si createTask = true, crear tarea automáticamente
        if (cmd.isCreateTask()) {
            TaskJpaEntity task = createTaskForObservation(obs, cmd.getTaskType());
            obs.setTask(task);
            observationRepo.save(obs);
            
            // 3. Enviar a LawraBot
            sendTaskToLawraBot(task);
        }
        
        return mapToResponse(obs);
    }
    
    private TaskJpaEntity createTaskForObservation(ObservationJpaEntity obs, TaskType type) {
        TaskJpaEntity task = new TaskJpaEntity();
        task.setId(UUID.randomUUID());
        task.setObservation(obs);
        task.setType(type);
        task.setMessageTemplate(buildMessageTemplate(obs, type));
        task.setStatus(TaskStatus.PENDING);
        task.setAssignedTo("LAWRA_BOT");
        task.setCreatedAt(LocalDateTime.now());
        return taskRepo.save(task);
    }
    
    private String buildMessageTemplate(ObservationJpaEntity obs, TaskType type) {
        // Templates según tipo
        switch (type) {
            case CORRECT_ERROR:
                return String.format(
                    "Hemos detectado un posible error en %s: %s. " +
                    "El valor sugerido es: %s. ¿Puede confirmar o corregir este dato?",
                    obs.getFieldName(), obs.getMessage(), obs.getSuggestedValue()
                );
            case CLARIFY_DATA:
                return String.format(
                    "Necesito aclaración sobre %s: %s",
                    obs.getFieldName(), obs.getMessage()
                );
            // ... otros templates
        }
    }
    
    public List<ObservationResponse> getObservationsByExpediente(UUID expedienteId) {
        return observationRepo.findByExpedienteId(expedienteId)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public ObservationResponse resolveObservation(UUID observationId, ResolveObservationCommand cmd) {
        ObservationJpaEntity obs = observationRepo.findById(observationId)
            .orElseThrow(() -> new ObservationNotFoundException(observationId));
            
        obs.setStatus(ObservationStatus.RESOLVED);
        obs.setResolvedAt(LocalDateTime.now());
        obs.setResolutionNotes(cmd.getNotes());
        
        if (obs.getTask() != null) {
            obs.getTask().setStatus(TaskStatus.COMPLETED);
            obs.getTask().setCompletedAt(LocalDateTime.now());
            obs.getTask().setResponseData(cmd.getResponseData());
        }
        
        observationRepo.save(obs);
        return mapToResponse(obs);
    }
    
    // Método llamado por LawraBot cuando ciudadano responde
    @Transactional
    public void markTaskAsCompleted(UUID taskId, String responseData) {
        TaskJpaEntity task = taskRepo.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));
            
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        task.setResponseData(responseData);
        taskRepo.save(task);
        
        // Notificar dashboard vía WebSocket/SSE
        notificationService.notifyTaskCompleted(task.getObservation().getExpedienteId(), taskId);
    }
}
```

### Paso 1.5: Crear REST Controller

```java
@RestController
@RequestMapping("/api/divorce/observations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ObservationRestController {
    private final ObservationService observationService;
    
    @PostMapping
    public ResponseEntity<ObservationResponse> createObservation(
            @RequestBody @Valid CreateObservationRequest request) {
        ObservationResponse response = observationService.createObservation(
            CreateObservationCommand.builder()
                .expedienteId(request.getExpedienteId())
                .fieldName(request.getFieldName())
                .severity(request.getSeverity())
                .message(request.getMessage())
                .suggestedValue(request.getSuggestedValue())
                .createTask(request.isCreateTask())
                .taskType(request.getTaskType())
                .operatorId(request.getOperatorId())
                .build()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/expediente/{expedienteId}")
    public ResponseEntity<List<ObservationResponse>> getObservations(
            @PathVariable UUID expedienteId) {
        return ResponseEntity.ok(observationService.getObservationsByExpediente(expedienteId));
    }
    
    @PutMapping("/{observationId}/resolve")
    public ResponseEntity<ObservationResponse> resolveObservation(
            @PathVariable UUID observationId,
            @RequestBody @Valid ResolveObservationRequest request) {
        ObservationResponse response = observationService.resolveObservation(
            observationId,
            ResolveObservationCommand.builder()
                .notes(request.getNotes())
                .responseData(request.getResponseData())
                .build()
        );
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<Void> completeTask(
            @PathVariable UUID taskId,
            @RequestBody TaskCompletionRequest request) {
        // Endpoint llamado por LawraBot cuando ciudadano responde
        observationService.markTaskAsCompleted(taskId, request.getResponseData());
        return ResponseEntity.ok().build();
    }
}
```

## FASE 2: Frontend (Next.js/React)

### Paso 2.1: Crear Tipos TypeScript

```typescript
// types/observation.ts

export type ObservationSeverity = 'ERROR' | 'WARNING' | 'INFO';
export type ObservationStatus = 'PENDING' | 'ASSIGNED_TO_BOT' | 'RESOLVED' | 'DISMISSED';
export type TaskType = 'CLARIFY_DATA' | 'REQUEST_DOCUMENT' | 'NOTIFY_APPOINTMENT' | 'CORRECT_ERROR';
export type TaskStatus = 'PENDING' | 'SENT_TO_BOT' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';

export interface Observation {
  id: string;
  expedienteId: string;
  fieldName: string;
  fieldLabel: string; // Label amigable ej: "DNI del Cónyuge"
  severity: ObservationSeverity;
  message: string;
  suggestedValue?: string;
  status: ObservationStatus;
  createdBy: string;
  createdByName: string;
  createdAt: string;
  resolvedAt?: string;
  resolutionNotes?: string;
  task?: Task;
}

export interface Task {
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

export interface CreateObservationInput {
  expedienteId: string;
  fieldName: string;
  severity: ObservationSeverity;
  message: string;
  suggestedValue?: string;
  createTask: boolean;
  taskType?: TaskType;
  operatorId: string;
}

export interface ResolveObservationInput {
  notes: string;
  responseData?: string;
}
```

### Paso 2.2: Crear Hook useObservations

```typescript
// hooks/useObservations.ts

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Observation, CreateObservationInput, ResolveObservationInput } from '@/types/observation';

const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081';

export function useObservations(expedienteId: string) {
  return useQuery({
    queryKey: ['observations', expedienteId],
    queryFn: async (): Promise<Observation[]> => {
      const res = await fetch(`${API_BASE}/api/divorce/observations/expediente/${expedienteId}`);
      if (!res.ok) throw new Error('Failed to fetch observations');
      return res.json();
    },
    refetchInterval: 5000, // Refrescar cada 5 segundos para ver cambios
  });
}

export function useCreateObservation() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (input: CreateObservationInput): Promise<Observation> => {
      const res = await fetch(`${API_BASE}/api/divorce/observations`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(input),
      });
      if (!res.ok) throw new Error('Failed to create observation');
      return res.json();
    },
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['observations', variables.expedienteId] });
    },
  });
}

export function useResolveObservation(expedienteId: string) {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ observationId, input }: { observationId: string; input: ResolveObservationInput }) => {
      const res = await fetch(`${API_BASE}/api/divorce/observations/${observationId}/resolve`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(input),
      });
      if (!res.ok) throw new Error('Failed to resolve observation');
      return res.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['observations', expedienteId] });
    },
  });
}
```

### Paso 2.3: Crear Componente ObservationCard

```typescript
// components/observations/ObservationCard.tsx

import { Observation } from '@/types/observation';
import { cn } from '@/lib/utils';
import { 
  AlertCircle, 
  AlertTriangle, 
  Info, 
  CheckCircle, 
  Clock,
  Bot,
  User
} from '@phosphor-icons/react';
import { formatDistanceToNow } from 'date-fns';
import { es } from 'date-fns/locale';

interface ObservationCardProps {
  observation: Observation;
  onResolve?: () => void;
  onDismiss?: () => void;
}

const severityConfig = {
  ERROR: { 
    icon: AlertCircle, 
    color: 'text-rose-400', 
    bg: 'bg-rose-500/10',
    border: 'border-rose-500/20',
    label: 'Error'
  },
  WARNING: { 
    icon: AlertTriangle, 
    color: 'text-amber-400', 
    bg: 'bg-amber-500/10',
    border: 'border-amber-500/20',
    label: 'Advertencia'
  },
  INFO: { 
    icon: Info, 
    color: 'text-blue-400', 
    bg: 'bg-blue-500/10',
    border: 'border-blue-500/20',
    label: 'Info'
  },
};

const statusConfig = {
  PENDING: { icon: Clock, label: 'Pendiente', color: 'text-zinc-400' },
  ASSIGNED_TO_BOT: { icon: Bot, label: 'Enviado a LawraBot', color: 'text-accent' },
  RESOLVED: { icon: CheckCircle, label: 'Resuelto', color: 'text-emerald-400' },
  DISMISSED: { icon: User, label: 'Descartado', color: 'text-zinc-500' },
};

export function ObservationCard({ observation, onResolve, onDismiss }: ObservationCardProps) {
  const severity = severityConfig[observation.severity];
  const status = statusConfig[observation.status];
  const SeverityIcon = severity.icon;
  const StatusIcon = status.icon;
  
  return (
    <div className={cn(
      "p-4 rounded-2xl border transition-all",
      severity.bg,
      severity.border,
      observation.status === 'RESOLVED' && 'opacity-60'
    )}>
      {/* Header */}
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center gap-2">
          <div className={cn("p-1.5 rounded-lg", severity.bg)}>
            <SeverityIcon size={16} className={severity.color} weight="fill" />
          </div>
          <span className={cn("text-xs font-bold uppercase tracking-wider", severity.color)}>
            {severity.label}
          </span>
        </div>
        <div className="flex items-center gap-1.5">
          <StatusIcon size={14} className={status.color} />
          <span className={cn("text-xs font-medium", status.color)}>{status.label}</span>
        </div>
      </div>
      
      {/* Field Name */}
      <div className="mb-2">
        <span className="text-[10px] uppercase tracking-widest text-zinc-500 font-bold">
          Campo: {observation.fieldLabel}
        </span>
      </div>
      
      {/* Message */}
      <p className="text-sm text-zinc-200 mb-3 leading-relaxed">
        {observation.message}
      </p>
      
      {/* Suggested Value */}
      {observation.suggestedValue && (
        <div className="p-2.5 rounded-xl bg-white/5 border border-white/10 mb-3">
          <span className="text-[10px] uppercase tracking-wider text-zinc-500 font-bold block mb-1">
            Valor Sugerido
          </span>
          <span className="text-sm font-mono text-emerald-300">
            {observation.suggestedValue}
          </span>
        </div>
      )}
      
      {/* Task Info */}
      {observation.task && observation.status === 'ASSIGNED_TO_BOT' && (
        <div className="flex items-center gap-2 p-2.5 rounded-xl bg-accent/5 border border-accent/10 mb-3">
          <Bot size={16} className="text-accent" />
          <span className="text-xs text-accent">
            Tarea enviada a LawraBot • Esperando respuesta del ciudadano
          </span>
        </div>
      )}
      
      {/* Footer */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 text-zinc-500">
          <User size={12} />
          <span className="text-xs">
            {observation.createdByName} • {formatDistanceToNow(new Date(observation.createdAt), { locale: es, addSuffix: true })}
          </span>
        </div>
        
        {/* Actions */}
        {observation.status === 'PENDING' && (
          <div className="flex items-center gap-2">
            <button
              onClick={onDismiss}
              className="px-3 py-1.5 rounded-lg text-xs font-medium text-zinc-400 hover:text-white hover:bg-white/5 transition-all"
            >
              Descartar
            </button>
            <button
              onClick={onResolve}
              className="px-3 py-1.5 rounded-lg text-xs font-medium bg-emerald-500/10 text-emerald-400 hover:bg-emerald-500/20 transition-all"
            >
              Resolver
            </button>
          </div>
        )}
        
        {observation.status === 'ASSIGNED_TO_BOT' && (
          <button
            onClick={onResolve}
            className="px-3 py-1.5 rounded-lg text-xs font-medium bg-accent/10 text-accent hover:bg-accent/20 transition-all"
          >
            Marcar Resuelto
          </button>
        )}
      </div>
      
      {/* Resolution Notes */}
      {observation.resolutionNotes && (
        <div className="mt-3 pt-3 border-t border-white/5">
          <span className="text-[10px] uppercase tracking-wider text-zinc-500 font-bold block mb-1">
            Resolución
          </span>
          <p className="text-xs text-zinc-400">{observation.resolutionNotes}</p>
        </div>
      )}
    </div>
  );
}
```

### Paso 2.4: Crear Componente ObservationComposer

```typescript
// components/observations/ObservationComposer.tsx

import { useState } from 'react';
import { CreateObservationInput, ObservationSeverity, TaskType } from '@/types/observation';
import { useCreateObservation } from '@/hooks/useObservations';
import { cn } from '@/lib/utils';
import { 
  AlertCircle, 
  AlertTriangle, 
  Info,
  PaperPlaneRight,
  Bot
} from '@phosphor-icons/react';

interface ObservationComposerProps {
  expedienteId: string;
  operatorId: string;
  availableFields: Array<{ name: string; label: string }>;
  onSuccess?: () => void;
}

const severityOptions: Array<{ value: ObservationSeverity; label: string; icon: any; color: string }> = [
  { value: 'ERROR', label: 'Error', icon: AlertCircle, color: 'text-rose-400' },
  { value: 'WARNING', label: 'Advertencia', icon: AlertTriangle, color: 'text-amber-400' },
  { value: 'INFO', label: 'Info', icon: Info, color: 'text-blue-400' },
];

const taskTypeOptions: Array<{ value: TaskType; label: string; description: string }> = [
  { value: 'CLARIFY_DATA', label: 'Aclarar dato', description: 'Solicitar aclaración sobre información confusa' },
  { value: 'CORRECT_ERROR', label: 'Corregir error', description: 'Notificar error detectado con sugerencia' },
  { value: 'REQUEST_DOCUMENT', label: 'Solicitar documento', description: 'Pedir envío de documentación' },
  { value: 'NOTIFY_APPOINTMENT', label: 'Notificar cita', description: 'Convocar a firma de documentos' },
];

export function ObservationComposer({ expedienteId, operatorId, availableFields, onSuccess }: ObservationComposerProps) {
  const [fieldName, setFieldName] = useState('');
  const [severity, setSeverity] = useState<ObservationSeverity>('WARNING');
  const [message, setMessage] = useState('');
  const [suggestedValue, setSuggestedValue] = useState('');
  const [createTask, setCreateTask] = useState(true);
  const [taskType, setTaskType] = useState<TaskType>('CORRECT_ERROR');
  
  const createObservation = useCreateObservation();
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    await createObservation.mutateAsync({
      expedienteId,
      fieldName,
      severity,
      message,
      suggestedValue: suggestedValue || undefined,
      createTask,
      taskType: createTask ? taskType : undefined,
      operatorId,
    });
    
    // Reset form
    setFieldName('');
    setSeverity('WARNING');
    setMessage('');
    setSuggestedValue('');
    onSuccess?.();
  };
  
  return (
    <form onSubmit={handleSubmit} className="p-6 rounded-3xl bg-zinc-950/50 border border-white/10">
      <div className="flex items-center gap-3 mb-6">
        <div className="w-10 h-10 rounded-2xl bg-amber-500/10 flex items-center justify-center">
          <AlertTriangle size={24} className="text-amber-400" />
        </div>
        <div>
          <h3 className="text-lg font-bold text-white">Nueva Observación</h3>
          <p className="text-xs text-zinc-500">Crear observación y asignar tarea a LawraBot</p>
        </div>
      </div>
      
      <div className="space-y-4">
        {/* Field Selection */}
        <div>
          <label className="text-[10px] uppercase tracking-widest text-zinc-500 font-bold block mb-2">
            Campo Afectado
          </label>
          <select
            value={fieldName}
            onChange={(e) => setFieldName(e.target.value)}
            className="w-full bg-zinc-900 border border-white/10 rounded-xl px-4 py-3 text-sm text-white focus:outline-none focus:border-accent/50"
            required
          >
            <option value="">Seleccionar campo...</option>
            {availableFields.map((field) => (
              <option key={field.name} value={field.name}>{field.label}</option>
            ))}
          </select>
        </div>
        
        {/* Severity */}
        <div>
          <label className="text-[10px] uppercase tracking-widest text-zinc-500 font-bold block mb-2">
            Severidad
          </label>
          <div className="flex gap-2">
            {severityOptions.map((opt) => {
              const Icon = opt.icon;
              return (
                <button
                  key={opt.value}
                  type="button"
                  onClick={() => setSeverity(opt.value)}
                  className={cn(
                    "flex-1 flex items-center justify-center gap-2 px-4 py-3 rounded-xl border transition-all",
                    severity === opt.value
                      ? cn(opt.bg, opt.border, opt.color)
                      : "border-white/10 text-zinc-500 hover:border-white/20"
                  )}
                >
                  <Icon size={18} weight={severity === opt.value ? "fill" : "regular"} />
                  <span className="text-sm font-medium">{opt.label}</span>
                </button>
              );
            })}
          </div>
        </div>
        
        {/* Message */}
        <div>
          <label className="text-[10px] uppercase tracking-widest text-zinc-500 font-bold block mb-2">
            Descripción del Problema
          </label>
          <textarea
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            placeholder="Ej: El valor ingresado parece incorrecto..."
            className="w-full bg-zinc-900 border border-white/10 rounded-xl px-4 py-3 text-sm text-white focus:outline-none focus:border-accent/50 resize-none h-24"
            required
          />
        </div>
        
        {/* Suggested Value */}
        <div>
          <label className="text-[10px] uppercase tracking-widest text-zinc-500 font-bold block mb-2">
            Valor Sugerido (opcional)
          </label>
          <input
            type="text"
            value={suggestedValue}
            onChange={(e) => setSuggestedValue(e.target.value)}
            placeholder="Ej: 50000"
            className="w-full bg-zinc-900 border border-white/10 rounded-xl px-4 py-3 text-sm text-white focus:outline-none focus:border-accent/50"
          />
        </div>
        
        {/* Create Task Toggle */}
        <div className="flex items-center gap-3 p-4 rounded-xl bg-zinc-900/50 border border-white/5">
          <input
            type="checkbox"
            id="createTask"
            checked={createTask}
            onChange={(e) => setCreateTask(e.target.checked)}
            className="w-5 h-5 rounded border-white/20 accent-accent"
          />
          <label htmlFor="createTask" className="flex items-center gap-2 cursor-pointer">
            <Bot size={18} className="text-accent" />
            <span className="text-sm text-zinc-300">Crear tarea y enviar a LawraBot</span>
          </label>
        </div>
        
        {/* Task Type (conditional) */}
        {createTask && (
          <div>
            <label className="text-[10px] uppercase tracking-widest text-zinc-500 font-bold block mb-2">
              Tipo de Tarea
            </label>
            <div className="grid grid-cols-2 gap-2">
              {taskTypeOptions.map((opt) => (
                <button
                  key={opt.value}
                  type="button"
                  onClick={() => setTaskType(opt.value)}
                  className={cn(
                    "p-3 rounded-xl border text-left transition-all",
                    taskType === opt.value
                      ? "border-accent bg-accent/10"
                      : "border-white/10 hover:border-white/20"
                  )}
                >
                  <span className={cn(
                    "text-sm font-medium block",
                    taskType === opt.value ? "text-accent" : "text-zinc-300"
                  )}>
                    {opt.label}
                  </span>
                  <span className="text-xs text-zinc-500 block mt-1">
                    {opt.description}
                  </span>
                </button>
              ))}
            </div>
          </div>
        )}
        
        {/* Submit */}
        <button
          type="submit"
          disabled={createObservation.isPending || !fieldName || !message}
          className="w-full flex items-center justify-center gap-2 py-4 rounded-xl bg-accent text-white font-bold hover:bg-accent/90 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
        >
          {createObservation.isPending ? (
            <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
          ) : (
            <>
              <PaperPlaneRight size={20} weight="bold" />
              Crear Observación
            </>
          )}
        </button>
      </div>
    </form>
  );
}
```

### Paso 2.5: Crear Panel Principal ObservationPanel

```typescript
// components/observations/ObservationPanel.tsx

import { useState } from 'react';
import { useObservations } from '@/hooks/useObservations';
import { ObservationCard } from './ObservationCard';
import { ObservationComposer } from './ObservationComposer';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { cn } from '@/lib/utils';
import { AlertCircle, CheckCircle, Clock, Bot } from '@phosphor-icons/react';

interface ObservationPanelProps {
  expedienteId: string;
  operatorId: string;
}

const availableFields = [
  { name: 'petitioner.name', label: 'Nombre del Peticionante' },
  { name: 'petitioner.dni', label: 'DNI del Peticionante' },
  { name: 'petitioner.address', label: 'Domicilio del Peticionante' },
  { name: 'respondent.name', label: 'Nombre del Demandado' },
  { name: 'respondent.dni', label: 'DNI del Demandado' },
  { name: 'respondent.address', label: 'Domicilio del Demandado' },
  { name: 'marriage.date', label: 'Fecha de Matrimonio' },
  { name: 'marriage.separationDate', label: 'Fecha de Separación' },
  { name: 'agreement.alimonyAmount', label: 'Cuota Alimentaria' },
  { name: 'agreement.personalCare', label: 'Cuidado Personal' },
  { name: 'agreement.communication', label: 'Régimen Comunicacional' },
  { name: 'children.info', label: 'Datos de Hijos' },
  { name: 'documents.missing', label: 'Documentación Faltante' },
];

export function ObservationPanel({ expedienteId, operatorId }: ObservationPanelProps) {
  const { data: observations, isLoading } = useObservations(expedienteId);
  const [activeTab, setActiveTab] = useState('all');
  const [showComposer, setShowComposer] = useState(false);
  
  const filteredObservations = observations?.filter((obs) => {
    if (activeTab === 'all') return true;
    if (activeTab === 'pending') return obs.status === 'PENDING' || obs.status === 'ASSIGNED_TO_BOT';
    if (activeTab === 'resolved') return obs.status === 'RESOLVED' || obs.status === 'DISMISSED';
    if (activeTab === 'errors') return obs.severity === 'ERROR';
    return true;
  });
  
  const stats = {
    total: observations?.length || 0,
    pending: observations?.filter(o => o.status === 'PENDING' || o.status === 'ASSIGNED_TO_BOT').length || 0,
    errors: observations?.filter(o => o.severity === 'ERROR' && o.status !== 'RESOLVED').length || 0,
    withBot: observations?.filter(o => o.status === 'ASSIGNED_TO_BOT').length || 0,
  };
  
  return (
    <div className="flex flex-col h-full">
      {/* Header Stats */}
      <div className="grid grid-cols-4 gap-3 mb-4">
        <StatCard 
          icon={AlertCircle} 
          label="Errores" 
          value={stats.errors} 
          color="text-rose-400" 
          bg="bg-rose-500/10"
        />
        <StatCard 
          icon={Clock} 
          label="Pendientes" 
          value={stats.pending} 
          color="text-amber-400" 
          bg="bg-amber-500/10"
        />
        <StatCard 
          icon={Bot} 
          label="Con LawraBot" 
          value={stats.withBot} 
          color="text-accent" 
          bg="bg-accent/10"
        />
        <StatCard 
          icon={CheckCircle} 
          label="Total" 
          value={stats.total} 
          color="text-zinc-400" 
          bg="bg-white/5"
        />
      </div>
      
      {/* Tabs */}
      <Tabs value={activeTab} onValueChange={setActiveTab} className="flex-1 flex flex-col">
        <TabsList className="grid grid-cols-4 mb-4">
          <TabsTrigger value="all">Todas</TabsTrigger>
          <TabsTrigger value="pending">Pendientes</TabsTrigger>
          <TabsTrigger value="errors">Errores</TabsTrigger>
          <TabsTrigger value="resolved">Resueltas</TabsTrigger>
        </TabsList>
        
        <TabsContent value="all" className="flex-1 overflow-y-auto space-y-3">
          {isLoading ? (
            <LoadingState />
          ) : filteredObservations?.length === 0 ? (
            <EmptyState onCreate={() => setShowComposer(true)} />
          ) : (
            filteredObservations?.map((obs) => (
              <ObservationCard key={obs.id} observation={obs} />
            ))
          )}
        </TabsContent>
        
        <TabsContent value="pending" className="flex-1 overflow-y-auto space-y-3">
          {/* Similar... */}
        </TabsContent>
        
        {/* Other tabs... */}
      </Tabs>
      
      {/* Create Button */}
      <button
        onClick={() => setShowComposer(!showComposer)}
        className="mt-4 w-full py-3 rounded-xl border border-dashed border-white/20 text-zinc-400 hover:text-white hover:border-white/40 transition-all flex items-center justify-center gap-2"
      >
        <AlertCircle size={18} />
        {showComposer ? 'Cancelar' : 'Nueva Observación'}
      </button>
      
      {/* Composer Modal / Inline */}
      {showComposer && (
        <div className="mt-4">
          <ObservationComposer
            expedienteId={expedienteId}
            operatorId={operatorId}
            availableFields={availableFields}
            onSuccess={() => setShowComposer(false)}
          />
        </div>
      )}
    </div>
  );
}

function StatCard({ icon: Icon, label, value, color, bg }: any) {
  return (
    <div className="p-3 rounded-2xl bg-white/5 border border-white/10">
      <div className="flex items-center gap-2 mb-1">
        <div className={cn("p-1.5 rounded-lg", bg)}>
          <Icon size={14} className={color} />
        </div>
        <span className="text-[10px] uppercase tracking-wider text-zinc-500 font-bold">{label}</span>
      </div>
      <span className={cn("text-2xl font-bold", color)}>{value}</span>
    </div>
  );
}

function LoadingState() {
  return (
    <div className="flex flex-col items-center justify-center py-12">
      <div className="w-8 h-8 border-2 border-accent/30 border-t-accent rounded-full animate-spin mb-4" />
      <p className="text-sm text-zinc-500">Cargando observaciones...</p>
    </div>
  );
}

function EmptyState({ onCreate }: { onCreate: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center py-12 text-center">
      <div className="w-16 h-16 rounded-full bg-white/5 flex items-center justify-center mb-4">
        <CheckCircle size={32} className="text-zinc-600" />
      </div>
      <h4 className="text-white font-bold mb-2">Sin observaciones</h4>
      <p className="text-sm text-zinc-500 mb-4">No hay observaciones registradas para este caso</p>
      <button
        onClick={onCreate}
        className="px-4 py-2 rounded-xl bg-accent/10 text-accent text-sm font-medium hover:bg-accent/20 transition-all"
      >
        Crear primera observación
      </button>
    </div>
  );
}
```

## FASE 3: Integración y Testing

### Paso 3.1: Integrar en DivorceWorkspace

Modificar `divorce/page.tsx` para incluir el panel de observaciones en una pestaña nueva o en el panel lateral.

### Paso 3.2: WebSocket para Tiempo Real

Implementar conexión SSE/WebSocket para notificar al dashboard cuando LawraBot completa una tarea.

```typescript
// hooks/useObservationEvents.ts

import { useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';

export function useObservationEvents(expedienteId: string) {
  const queryClient = useQueryClient();
  
  useEffect(() => {
    const eventSource = new EventSource(
      `${process.env.NEXT_PUBLIC_API_URL}/api/divorce/events?expedienteId=${expedienteId}`
    );
    
    eventSource.onmessage = (event) => {
      const data = JSON.parse(event.data);
      
      if (data.type === 'TASK_COMPLETED') {
        // Refrescar observaciones
        queryClient.invalidateQueries({ queryKey: ['observations', expedienteId] });
        
        // Mostrar notificación
        toast.success(`LawraBot completó una tarea: ${data.taskType}`);
      }
    };
    
    return () => eventSource.close();
  }, [expedienteId, queryClient]);
}
```

## Criterios de Aceptación

### Backend
- [x] Entidades JPA creadas y mapeadas correctamente
- [x] Repositorios funcionando con queries correctas
- [x] Service con lógica de negocio completa
- [x] Controller REST con endpoints funcionales
- [x] Integración con LawraBot (endpoint para notificar tareas)

### Frontend
- [x] Hook useObservations funcionando (adaptado a fetch directo)
- [x] Componente ObservationCard visualizando correctamente
- [x] ObservationComposer permitiendo crear observaciones
- [x] ObservationPanel integrando todo
- [/] UI se actualiza en tiempo real (polling implementado, SSE pendiente)

### Integración
- [x] Flujo completo: Crear observación → Generar tarea → LawraBot notifica → Dashboard actualiza
- [x] Manejo de errores (network, validation, etc.)
- [x] Estados de loading y error
- [x] Responsive design

## Tiempo Estimado

- Fase 1 (Backend): 3-4 horas
- Fase 2 (Frontend): 4-5 horas
- Fase 3 (Integración): 2-3 horas
- **Total: 9-12 horas**
