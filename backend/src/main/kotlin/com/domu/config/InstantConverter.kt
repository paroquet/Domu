package com.domu.config

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.time.Instant
import java.time.format.DateTimeFormatter

@Converter(autoApply = true)
class InstantConverter : AttributeConverter<Instant, String> {

    override fun convertToDatabaseColumn(attribute: Instant?): String? {
        return attribute?.let { DateTimeFormatter.ISO_INSTANT.format(it) }
    }

    override fun convertToEntityAttribute(dbData: String?): Instant? {
        if (dbData.isNullOrBlank()) return null

        return try {
            // Try parsing as ISO-8601 format first
            Instant.parse(dbData)
        } catch (e: Exception) {
            try {
                // Fallback: try parsing as milliseconds (for legacy data)
                Instant.ofEpochMilli(dbData.toLong())
            } catch (e2: Exception) {
                null
            }
        }
    }
}
