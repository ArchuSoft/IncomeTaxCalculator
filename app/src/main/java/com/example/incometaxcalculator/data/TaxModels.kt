
package com.example.incometaxcalculator.data

import java.time.LocalDate

/**
 * Core data models for the IncomeTaxCalculator app.
 *
 * This file provides:
 * - Full AssessmentYear enum entries from AY 1981-82 (FY 1980-81) to AY 2026-27 (FY 2025-26).
 * - TaxStatus categories.
 * - Regime selector (OLD vs NEW).
 * - Input models for tax and interest computations.
 * - Output models for detailed tax and interest breakdowns.
 *
 * Populate slabs, cess, surcharge, rebates, and marginal relief per AY in your rules provider
 * using official CBDT/Income Tax Department charts/tables[1][17], and cess history references[10][7][13].
 */

/* =========================
   Enums
   ========================= */

enum class Regime { OLD, NEW }

/**
 * Assessment year is the year following the financial year.
 * Example: FY 2024-25 => AY 2025-26, so fyStartYear = 2024.
 *
 * Entries below enumerate AY 1981-82 through AY 2026-27 inclusive.
 * Ensure your rules provider (TaxRules/HistoricalRates) contains authoritative slab, cess,
 * surcharge, rebate, and marginal relief logic per AY and regime[1][17].
 */
enum class AssessmentYear(val label: String, val fyStartYear: Int) {
    // 1980s (FY start year -> AY)
    AY_1981_82("AY 1981-82", 1980),
    AY_1982_83("AY 1982-83", 1981),
    AY_1983_84("AY 1983-84", 1982),
    AY_1984_85("AY 1984-85", 1983),
    AY_1985_86("AY 1985-86", 1984),
    AY_1986_87("AY 1986-87", 1985),
    AY_1987_88("AY 1987-88", 1986),
    AY_1988_89("AY 1988-89", 1987),
    AY_1989_90("AY 1989-90", 1988),

    // 1990s
    AY_1990_91("AY 1990-91", 1989),
    AY_1991_92("AY 1991-92", 1990),
    AY_1992_93("AY 1992-93", 1991),
    AY_1993_94("AY 1993-94", 1992),
    AY_1994_95("AY 1994-95", 1993),
    AY_1995_96("AY 1995-96", 1994),
    AY_1996_97("AY 1996-97", 1995),
    AY_1997_98("AY 1997-98", 1996),
    AY_1998_99("AY 1998-99", 1997),
    AY_1999_00("AY 1999-00", 1998),

    // 2000s
    AY_2000_01("AY 2000-01", 1999),
    AY_2001_02("AY 2001-02", 2000),
    AY_2002_03("AY 2002-03", 2001),
    AY_2003_04("AY 2003-04", 2002),
    AY_2004_05("AY 2004-05", 2003),
    AY_2005_06("AY 2005-06", 2004),
    AY_2006_07("AY 2006-07", 2005),
    AY_2007_08("AY 2007-08", 2006),
    AY_2008_09("AY 2008-09", 2007),
    AY_2009_10("AY 2009-10", 2008),

    // 2010s
    AY_2010_11("AY 2010-11", 2009),
    AY_2011_12("AY 2011-12", 2010),
    AY_2012_13("AY 2012-13", 2011),
    AY_2013_14("AY 2013-14", 2012),
    AY_2014_15("AY 2014-15", 2013),
    AY_2015_16("AY 2015-16", 2014),
    AY_2016_17("AY 2016-17", 2015),
    AY_2017_18("AY 2017-18", 2016),
    AY_2018_19("AY 2018-19", 2017),
    AY_2019_20("AY 2019-20", 2018),

    // 2020s
    AY_2020_21("AY 2020-21", 2019),
    AY_2021_22("AY 2021-22", 2020),
    AY_2022_23("AY 2022-23", 2021),
    AY_2023_24("AY 2023-24", 2022),
    AY_2024_25("AY 2024-25", 2023),
    AY_2025_26("AY 2025-26", 2024),
    AY_2026_27("AY 2026-27", 2025);

    companion object {
        /**
         * Returns all AYs sorted by their FY start (oldest first).
         */
        fun allOrdered(): List<AssessmentYear> = values().toList().sortedBy { it.fyStartYear }
    }
}

/**
 * Taxpayer status categories.
 * For individuals, senior/super senior thresholds differ by AY under old regime.
 * Companies/firms typically have flat/base rates with surcharge/cess variations per AY.
 */
enum class TaxStatus(val label: String) {
    INDIVIDUAL("Individual"),
    SENIOR_CITIZEN("Senior Citizen"),
    SUPER_SENIOR("Super Senior Citizen"),
    HUF("HUF"),
    FIRM("Firm"),
    COMPANY("Company")
}

/* =========================
   Inputs
   ========================= */

/**
 * A dated payment entry (used for advance tax or self-assessment tax).
 */
data class TaxPayment(
    val date: LocalDate,
    val amount: Double
)

/**
 * Primary input to the tax engine.
 *
 * Fields:
 * - assessmentYear, status, regime: drive selection of slabs/rates/surcharge bands.
 * - totalIncome: total income before regime-allowed deductions; rules apply per AY.
 * - deductions80C/deductionsOther: only apply when law permits for selected AY/regime/status.
 * - advanceTaxPayments: list of advance-tax installments with dates/amounts.
 * - tdsTcs: aggregate TDS/TCS credit relevant to the FY of the AY.
 * - selfAssessmentTaxPaid: list of self-assessment tax payments (with dates) for interest calcs.
 * - returnFilingDate, dueDateForReturn: for 234A interest period.
 * - interest234CExemptIncomes: set true if shortfall is solely due to incomes that get relief from 234C.
 */
data class TaxInput(
    val assessmentYear: AssessmentYear,
    val status: TaxStatus,
    val regime: Regime,
    val totalIncome: Double,
    val deductions80C: Double = 0.0,
    val deductionsOther: Double = 0.0,
    val advanceTaxPayments: List<TaxPayment> = emptyList(),
    val tdsTcs: Double = 0.0,
    val selfAssessmentTaxPaid: List<TaxPayment> = emptyList(),
    val returnFilingDate: LocalDate? = null,
    val dueDateForReturn: LocalDate? = null,
    val interest234CExemptIncomes: Boolean = false
)

/* =========================
   Outputs
   ========================= */

/**
 * Breakup of tax (excluding interest).
 * - taxableIncome: income after permissible deductions per AY/regime/status.
 * - baseTax: tax from slabs/flat rates before surcharge/cess.
 * - surcharge: surcharge amount after applying bands and marginal relief logic (if any).
 * - cess: health & education/education cess (per AY) on tax+surcharge.
 * - totalBeforeInterest: sum of baseTax + surcharge + cess.
 */
data class TaxBreakup(
    val taxableIncome: Double,
    val baseTax: Double,
    val surcharge: Double,
    val cess: Double,
    val totalBeforeInterest: Double
)

/**
 * Interest under sections 234A/234B/234C.
 * Computation uses 1% per month or part thereof and installment logic for 234C
 * (15%/45%/75%/100% milestones) as per department guidance; ensure AY-specific
 * nuances are implemented in your utilities[1][17].
 */
data class InterestBreakup(
    val i234A: Double,
    val i234B: Double,
    val i234C: Double
)

/**
 * Final result combining tax and interest.
 */
data class FinalComputation(
    val tax: TaxBreakup,
    val interest: InterestBreakup,
    val totalPayable: Double
)