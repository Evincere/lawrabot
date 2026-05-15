package com.lawrabot.divorce_mcp_server.domain.enums;

/**
 * Enum representing all possible states of a divorce case (expediente).
 *
 * WHY AN ENUM?
 * - Closed set of values that never changes at runtime
 * - Type safety: can't pass an invalid state
 * - Self-documenting: readable code like ExpedienteStatusEnum.BLSG_PRECONSULTA
 *
 * STATE MACHINE:
 * BLSG_PRECONSULTA -> IN_PROGRESS -> DATA_COMPLETE -> DOCUMENTS_GENERATED
 * | |
 * v v
 * BLSG_RECHAZADO (end) UNDER_REVIEW
 * |
 * +-----------------------------------+
 * |
 * v
 * OBSERVATIONS_PENDING -> IN_PROGRESS (loop)
 * |
 * v
 * SUBMITTED -> IN_PROCEDURE -> FINALIZED
 *
 * NAMING CONVENTION:
 * - Suffix "Enum" per CODING_STANDARDS.md
 * - Values in UPPER_SNAKE_CASE (English, descriptive)
 * - Stored as VARCHAR in DB, not numbers (more readable, order-independent)
 */
public enum ExpedienteStatusEnum {

    // Initial BLSG check
    BLSG_PRECONSULTA, // BLSG check passed, ready to create expediente
    BLSG_RECHAZADO, // BLSG check failed, case rejected early

    // Data collection phase
    IN_DATA_COLLECTION_PROGRESS, // Expediente created, collecting data
    DATA_COMPLETE, // All required data collected

    // Document generation
    DOCUMENTS_GENERATED, // Drafts created, ready for lawyer review

    // Review and Signature phase (File Watcher Flow)
    WAITING_SIGNATURE, // Documents generated in /drafts, waiting for lawyer to sign
    READY_FOR_PORTAL, // Signed PDF detected in /signed, ready to upload to judicial portal
    OBSERVATIONS_PENDING, // Lawyer sent observations, waiting for user

    // Final phases
    SUBMITTED, // Lawyer approved, presented to court
    IN_PROCEDURE, // Court accepted, in judicial process
    FINALIZED, // Divorce finalized/sentenced

    // Admin / Cleanup
    ARCHIVED // Soft-deleted, hidden from main view
}
