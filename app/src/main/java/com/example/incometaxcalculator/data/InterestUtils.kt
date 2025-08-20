package com.example.incometaxcalculator.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max

object InterestUtils {

    // 1% simple interest per month or part thereof.[15][12][6]
    private const val RATE = 0.01

    private fun monthsInclusive(startExclusive: LocalDate, endInclusive: LocalDate): Int {
        // If any part of a month counts as a full month
        var months = 0
        var cursor = startExclusive.plusDays(1)
        // Shortcut: compute months diff by calendar and count partial as 1
        // Simpler: days difference; any partial month bump to next.
        val days = ChronoUnit.DAYS.between(cursor, endInclusive) + 1
        if (days <= 0) return 0
        // Approximate by month boundaries: count months by stepping months while <= end
        // For reliability, compute by year-month difference plus partial month bump:
        val ymdStart = cursor.withDayOfMonth(1)
        val ymdEnd = endInclusive.withDayOfMonth(1)
        months = (ymdEnd.year - ymdStart.year) * 12 + (ymdEnd.monthValue - ymdStart.monthValue)
        // If there is any day in the last month, add 1
        months += 1
        return max(0, months)
    }

    // Assessed tax = tax on returned income + surcharge + cess – TDS/TCS – MAT/AMT credit – relief u/s 90/90A/91 (if any).
    // Here we approximate with TDS/TCS provided; extend as needed.
    fun assessedTax(totalTaxBeforeInterest: Double, tdsTcs: Double): Double {
        return max(0.0, totalTaxBeforeInterest - tdsTcs)
    }

    // 234A: late filing interest from day after due date to actual filing date on unpaid self-assessment amount.[15]
    fun interest234A(
        assessedTax: Double,
        advanceTaxPaid: Double,
        selfAssessmentPaidByFiling: Double,
        dueDate: LocalDate?,
        filingDate: LocalDate?
    ): Double {
        if (dueDate == null || filingDate == null || !filingDate.isAfter(dueDate)) return 0.0
        val unpaidAtDue = max(0.0, assessedTax - advanceTaxPaid)
        val months = monthsInclusive(dueDate, filingDate)
        return (unpaidAtDue - selfAssessmentPaidByFiling).coerceAtLeast(0.0) * RATE * months
    }

    // 234B: if advance tax paid < 90% of assessed tax, interest @1% p.m. from 1 Apr of AY to date of payment/assessment.[15][12]
    fun interest234B(
        ayStartYear: Int,
        assessedTax: Double,
        advanceTaxPaidBy31Mar: Double,
        finalPaymentDate: LocalDate // date SA tax finally paid
    ): Double {
        val threshold = 0.9 * assessedTax
        if (advanceTaxPaidBy31Mar >= threshold) return 0.0
        val shortfall = assessedTax - advanceTaxPaidBy31Mar
        val start = LocalDate.of(ayStartYear + 1, 4, 1) // 1 April of AY
        val months = monthsInclusive(start.minusDays(1), finalPaymentDate)
        return shortfall * RATE * months
    }

    // 234C: installment shortfalls for non-presumptive taxpayers.[12][6]
    // Milestones: 15% by Jun 15, 45% by Sep 15, 75% by Dec 15, 100% by Mar 15.
    // Interest: 1% per month for 3,3,3,1 months respectively on the shortfall.
    fun interest234C(
        ayStartYear: Int,
        assessedTax: Double,
        advancePayments: List<TaxPayment>,
        exemptFrom234C: Boolean
    ): Double {
        if (exemptFrom234C) return 0.0
        val fyY = ayStartYear
        val jun15 = LocalDate.of(fyY, 6, 15)
        val sep15 = LocalDate.of(fyY, 9, 15)
        val dec15 = LocalDate.of(fyY, 12, 15)
        val mar15 = LocalDate.of(fyY + 1, 3, 15)

        val paidBy = fun(cutoff: LocalDate) = advancePayments
            .filter { !it.date.isAfter(cutoff) }
            .sumOf { it.amount }

        var interest = 0.0

        // 15% by Jun 15 -> charge 3 months
        run {
            val need = 0.15 * assessedTax
            val paid = paidBy(jun15)
            val short = max(0.0, need - paid)
            if (short > 0) interest += short * RATE * 3
        }
        // 45% by Sep 15 -> 3 months
        run {
            val need = 0.45 * assessedTax
            val paid = paidBy(sep15)
            val short = max(0.0, need - paid)
            if (short > 0) interest += short * RATE * 3
        }
        // 75% by Dec 15 -> 3 months
        run {
            val need = 0.75 * assessedTax
            val paid = paidBy(dec15)
            val short = max(0.0, need - paid)
            if (short > 0) interest += short * RATE * 3
        }
        // 100% by Mar 15 -> 1 month
        run {
            val need = 1.00 * assessedTax
            val paid = paidBy(mar15)
            val short = max(0.0, need - paid)
            if (short > 0) interest += short * RATE * 1
        }
        return interest
    }
}
