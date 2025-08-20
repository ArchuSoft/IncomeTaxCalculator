package com.example.incometaxcalculator.data

import java.time.LocalDate

object Compute {

    fun computeAll(input: TaxInput): FinalComputation {
        // 1) Base tax + surcharge + cess
        val tax = TaxRules.computeTaxBreakup(input)

        // 2) Assessed tax for interest computations
        val assessed = InterestUtils.assessedTax(tax.totalBeforeInterest, input.tdsTcs)

        // 3) Sums for payments
        val advPaidBy31Mar = input.advanceTaxPayments
            .filter { it.date <= LocalDate.of(input.assessmentYear.fyStartYear + 1, 3, 31) }
            .sumOf { it.amount }
        val selfPaidTotal = input.selfAssessmentTaxPaid.sumOf { it.amount }
        val selfPaidByFiling = input.selfAssessmentTaxPaid
            .filter { input.returnFilingDate != null && it.date <= input.returnFilingDate }
            .sumOf { it.amount }

        // 4) Interest components
        val iA = InterestUtils.interest234A(
            assessedTax = assessed,
            advanceTaxPaid = advPaidBy31Mar,
            selfAssessmentPaidByFiling = selfPaidByFiling,
            dueDate = input.dueDateForReturn,
            filingDate = input.returnFilingDate
        )

        val finalPayDate = (input.selfAssessmentTaxPaid.maxByOrNull { it.date }?.date)
            ?: input.returnFilingDate
            ?: LocalDate.of(input.assessmentYear.fyStartYear + 1, 7, 31) // fallback

        val iB = InterestUtils.interest234B(
            ayStartYear = input.assessmentYear.fyStartYear,
            assessedTax = assessed,
            advanceTaxPaidBy31Mar = advPaidBy31Mar,
            finalPaymentDate = finalPayDate
        )

        val iC = InterestUtils.interest234C(
            ayStartYear = input.assessmentYear.fyStartYear,
            assessedTax = assessed,
            advancePayments = input.advanceTaxPayments,
            exemptFrom234C = input.interest234CExemptIncomes
        )

        val interest = InterestBreakup(iA, iB, iC)
        val totalPayable = tax.totalBeforeInterest + iA + iB + iC

        return FinalComputation(tax, interest, totalPayable)
    }
}
