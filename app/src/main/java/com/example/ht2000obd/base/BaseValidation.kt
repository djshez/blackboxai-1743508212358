package com.example.ht2000obd.base

/**
 * Interface for validation rules
 */
interface ValidationRule<T> {
    fun validate(value: T): ValidationResult
}

/**
 * Sealed class representing validation result
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()

    val isValid: Boolean
        get() = this is Valid

    val errorMessage: String?
        get() = if (this is Invalid) message else null
}

/**
 * Abstract class for form validation
 */
abstract class BaseFormValidator {
    private val validationResults = mutableMapOf<String, ValidationResult>()

    /**
     * Validate all fields
     */
    fun validateAll(): Boolean {
        performValidation()
        return validationResults.values.all { it.isValid }
    }

    /**
     * Get error message for a field
     */
    fun getError(fieldName: String): String? {
        return validationResults[fieldName]?.errorMessage
    }

    /**
     * Check if a field is valid
     */
    fun isFieldValid(fieldName: String): Boolean {
        return validationResults[fieldName]?.isValid ?: true
    }

    /**
     * Clear validation results
     */
    fun clearValidation() {
        validationResults.clear()
    }

    /**
     * Add validation result
     */
    protected fun addValidationResult(fieldName: String, result: ValidationResult) {
        validationResults[fieldName] = result
    }

    /**
     * Abstract method to perform validation
     */
    protected abstract fun performValidation()
}

/**
 * Common validation rules
 */
object ValidationRules {
    /**
     * Required field validation
     */
    class Required<T>(private val message: String = "This field is required") : ValidationRule<T> {
        override fun validate(value: T): ValidationResult {
            return when {
                value == null -> ValidationResult.Invalid(message)
                value is String && value.isBlank() -> ValidationResult.Invalid(message)
                value is Collection<*> && value.isEmpty() -> ValidationResult.Invalid(message)
                else -> ValidationResult.Valid
            }
        }
    }

    /**
     * Email validation
     */
    class Email(private val message: String = "Invalid email address") : ValidationRule<String> {
        override fun validate(value: String): ValidationResult {
            return if (android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid(message)
            }
        }
    }

    /**
     * Minimum length validation
     */
    class MinLength(
        private val minLength: Int,
        private val message: String = "Minimum length is $minLength"
    ) : ValidationRule<String> {
        override fun validate(value: String): ValidationResult {
            return if (value.length >= minLength) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid(message)
            }
        }
    }

    /**
     * Maximum length validation
     */
    class MaxLength(
        private val maxLength: Int,
        private val message: String = "Maximum length is $maxLength"
    ) : ValidationRule<String> {
        override fun validate(value: String): ValidationResult {
            return if (value.length <= maxLength) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid(message)
            }
        }
    }

    /**
     * Pattern validation
     */
    class Pattern(
        private val pattern: Regex,
        private val message: String = "Invalid format"
    ) : ValidationRule<String> {
        override fun validate(value: String): ValidationResult {
            return if (pattern.matches(value)) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid(message)
            }
        }
    }

    /**
     * Range validation
     */
    class Range<T : Comparable<T>>(
        private val min: T,
        private val max: T,
        private val message: String = "Value must be between $min and $max"
    ) : ValidationRule<T> {
        override fun validate(value: T): ValidationResult {
            return if (value in min..max) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid(message)
            }
        }
    }

    /**
     * Custom validation
     */
    class Custom<T>(
        private val validator: (T) -> Boolean,
        private val message: String = "Validation failed"
    ) : ValidationRule<T> {
        override fun validate(value: T): ValidationResult {
            return if (validator(value)) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid(message)
            }
        }
    }
}

/**
 * Extension function to validate multiple rules
 */
fun <T> T.validate(vararg rules: ValidationRule<T>): ValidationResult {
    rules.forEach { rule ->
        val result = rule.validate(this)
        if (result is ValidationResult.Invalid) {
            return result
        }
    }
    return ValidationResult.Valid
}

/**
 * Extension function to validate with custom message
 */
fun <T> T.validateWithMessage(message: String, validator: (T) -> Boolean): ValidationResult {
    return if (validator(this)) {
        ValidationResult.Valid
    } else {
        ValidationResult.Invalid(message)
    }
}