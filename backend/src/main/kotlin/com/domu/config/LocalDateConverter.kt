package com.domu.config

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Converter(autoApply = true)
class LocalDateConverter : AttributeConverter<LocalDate, String> {

    override fun convertToDatabaseColumn(attribute: LocalDate?): String? {
        return attribute?.let { DateTimeFormatter.ISO_LOCAL_DATE.format(it) }
    }

    override fun convertToEntityAttribute(dbData: String?): LocalDate? {
        if (dbData.isNullOrBlank()) return null

        return try {
            LocalDate.parse(dbData, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            null
        }
    }
}
