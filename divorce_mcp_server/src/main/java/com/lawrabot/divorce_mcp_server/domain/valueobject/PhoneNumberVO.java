package com.lawrabot.divorce_mcp_server.domain.valueobject;

import java.util.Objects;

/**
 * Value Object representing a validated phone number.
 *
 * WHY A VALUE OBJECT?
 * - Immutable: once created, cannot change (thread-safe, predictable)
 * - Validation at creation: impossible to have an invalid phone number
 * - Self-equals: two phone numbers with same value are equal
 * - Reusable across entities (Spouse, Expediente, Notification)
 *
 * DESIGN DECISIONS:
 * - Stored normalized (only digits, no spaces/symbols)
 * - Supports Argentina format: +549261XXXXXXX or 261XXXXXXX
 * - Immutable: final class, final fields, no setters
 */
public final class PhoneNumberVO {

    private final String value;

    /**
     * Private constructor to force use of factory method 'of()'.
     * This ensures validation always runs before object creation.
     */
    private PhoneNumberVO(String value) {
        this.value = value;
    }

    /**
     * Factory method - the ONLY way to create a PhoneNumberVO.
     *
     * WHY FACTORY METHOD?
     * - Named method reveals intent: PhoneNumberVO.of("...") vs new PhoneNumberVO("...")
     * - Can return null or Optional in future if needed
     * - Can cache instances (flyweight pattern) if needed later
     *
     * @param rawValue Phone number in any format
     * @return Validated PhoneNumberVO
     * @throws IllegalArgumentException if format is invalid
     */
    public static PhoneNumberVO of(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }

        // Normalize: remove all non-digit characters
        String normalized = rawValue.replaceAll("[^0-9]", "");

        // Validate: Argentina phone numbers are 10 digits (without country code)
        // Accepts: 261XXXXXXX (10 digits) or +549261XXXXXXX (13 digits with 549)
        if (normalized.length() == 13 && normalized.startsWith("549")) {
            normalized = normalized.substring(3); // Remove country code
        }

        // RELAXED VALIDATION: Accept IDs between 7 and 15 digits to support LIDs (Linked Identity)
        // Argentine local numbers are 10 digits. LIDs observed are ~15 digits.
        if (normalized.length() < 7 || normalized.length() > 15) {
            throw new IllegalArgumentException(
                "Invalid phone number format. Expected 7-15 digits (e.g., 2614123456 or LID format), got: " + rawValue
            );
        }

        return new PhoneNumberVO(normalized);
    }

    /**
     * Returns the normalized phone number (10 digits, no formatting).
     * Example: "2614123456"
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns formatted for WhatsApp Business API.
     * Format: 5492614123456 (with country code, no +)
     */
    public String toWhatsAppFormat() {
        // If it looks like a standard Argentine number (10 digits), prepend country code
        if (value.length() == 10) {
            return "549" + value;
        }
        // Otherwise, return as is (LID format)
        return value;
    }

    /**
     * Returns formatted for display.
     * Format: +54 9 261 412-3456 or raw value if not standard.
     */
    public String toDisplayFormat() {
        if (value.length() == 10) {
            return String.format("+54 9 %s %s-%s",
                value.substring(0, 3),
                value.substring(3, 6),
                value.substring(6, 10)
            );
        }
        return value;
    }

    /**
     * Value Objects are equal if their values are equal.
     * This enables: phone1.equals(phone2) even if different objects in memory.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;  // Same reference
        if (o == null || getClass() != o.getClass()) return false;  // Null or different type
        PhoneNumberVO that = (PhoneNumberVO) o;
        return value.equals(that.value);  // Compare by value
    }

    /**
     * Hash code based on value for consistent hashing in HashMap, HashSet, etc.
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * ToString for debugging/logging (shows value, not memory address).
     */
    @Override
    public String toString() {
        return "PhoneNumberVO{" + "value='" + toDisplayFormat() + '\'' + '}';
    }
}
