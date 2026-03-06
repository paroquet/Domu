package com.domu.repository

import com.domu.model.Order
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface OrderRepository : JpaRepository<Order, Long> {
    fun findByFamily_IdOrderByCreatedAtDesc(familyId: Long): List<Order>
    fun findByFamily_IdAndPlannedDate(familyId: Long, date: LocalDate): List<Order>
    fun findByFamily_IdAndPlannedDateAndStatusNot(familyId: Long, date: LocalDate, status: String): List<Order>
}
