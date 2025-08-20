package com.example.incometaxcalculator.data

import kotlin.math.max
import kotlin.math.min
import java.lang.IllegalStateException

// Progressive slab and surcharge band models
private data class Slab(val upTo: Double?, val rate: Double) // upTo is exclusive upper cap; null means no upper cap
private data class SurchargeBand(
    val thresholdIncomeExclusive: Double, // if taxable income > threshold, this band’s rate applies
    val rate: Double
)

// Per-AY configuration
private data class YearConfig(
    val ay: AssessmentYear,
    val regimeAvailable: Set<Regime>, // which regimes exist for the AY
    val slabsByStatusAndRegime: Map<Pair<TaxStatus, Regime>, List<Slab>>,
    val cessRate: Double, // apply to (baseTax + surcharge)
    val rebate87A: Rebate87A?, // rebate rules if applicable for the AY and regime
    val surchargeBandsByStatusAndRegime: Map<Pair<TaxStatus, Regime>, List<SurchargeBand>>,
    val marginalRelief: MarginalReliefRule? = null // optional per-AY marginal relief logic
)

// Section 87A rebate model (simple threshold-based)
private data class Rebate87A(
    val applicableStatuses: Set<TaxStatus>,
    val applicableRegimes: Set<Regime>,
    val incomeThreshold: Double, // total income threshold for rebate eligibility
    val rebateCap: Double // maximum rebate amount (applies to base tax net of slab, generally before cess)
)

// Marginal relief hook; implement per AY if needed
private data class MarginalReliefRule(
    val apply: (taxableIncome: Double, baseTax: Double, surchargeBeforeRelief: Double, bands: List<SurchargeBand>) -> Double
)

object TaxRules {
    // IMPORTANT: Populate this map for ALL AYs from AY_1981_82 through AY_2026_27 with authoritative data.
// The example includes filled configs for AY_2024_25 and AY_2025_26 only, to provide a working baseline while you fill the rest.
    private val CONFIGS: Map<AssessmentYear, YearConfig> = buildMap {

        // EXAMPLE: AY 2024-25 (FY 2023-24) — fill verified tables before use in production
        // Old regime individual/HUF typical slabs; New regime revised slabs from Budget 2023.
        // Cess: 4% H&EC. Surcharge bands per department charts; 87A rebate new regime threshold 7 lakh (old regime 5 lakh).
        put(
            AssessmentYear.AY_2024_25,
            YearConfig(
                ay = AssessmentYear.AY_2024_25,
                regimeAvailable = setOf(Regime.OLD, Regime.NEW),
                slabsByStatusAndRegime = mapOf(
                    (TaxStatus.INDIVIDUAL to Regime.OLD) to listOf(
                        Slab(250000.0, 0.0),
                        Slab(500000.0, 0.05),
                        Slab(1000000.0, 0.20),
                        Slab(null, 0.30)
                    ),
                    (TaxStatus.HUF to Regime.OLD) to listOf(
                        Slab(250000.0, 0.0),
                        Slab(500000.0, 0.05),
                        Slab(1000000.0, 0.20),
                        Slab(null, 0.30)
                    ),
                    (TaxStatus.SENIOR_CITIZEN to Regime.OLD) to listOf(
                        Slab(300000.0, 0.0),
                        Slab(500000.0, 0.05),
                        Slab(1000000.0, 0.20),
                        Slab(null, 0.30)
                    ),
                    (TaxStatus.SUPER_SENIOR to Regime.OLD) to listOf(
                        Slab(500000.0, 0.0),
                        Slab(1000000.0, 0.20),
                        Slab(null, 0.30)
                    ),
                    (TaxStatus.INDIVIDUAL to Regime.NEW) to listOf(
                        Slab(300000.0, 0.0),
                        Slab(600000.0, 0.05),
                        Slab(900000.0, 0.10),
                        Slab(1200000.0, 0.15),
                        Slab(1500000.0, 0.20),
                        Slab(null, 0.30)
                    ),
                    (TaxStatus.HUF to Regime.NEW) to listOf(
                        Slab(300000.0, 0.0),
                        Slab(600000.0, 0.05),
                        Slab(900000.0, 0.10),
                        Slab(1200000.0, 0.15),
                        Slab(1500000.0, 0.20),
                        Slab(null, 0.30)
                    ),
                    (TaxStatus.FIRM to Regime.OLD) to listOf(Slab(null, 0.30)),
                    (TaxStatus.FIRM to Regime.NEW) to listOf(Slab(null, 0.30)),
                    (TaxStatus.COMPANY to Regime.OLD) to listOf(
                        Slab(
                            null,
                            0.25
                        )
                    ), // adjust per chosen corporate regime if supporting options
                    (TaxStatus.COMPANY to Regime.NEW) to listOf(Slab(null, 0.25))
                ),
                cessRate = 0.04, // H&EC 4%[2][3][7]
                rebate87A = Rebate87A(
                    applicableStatuses = setOf(
                        TaxStatus.INDIVIDUAL,
                        TaxStatus.HUF,
                        TaxStatus.SENIOR_CITIZEN,
                        TaxStatus.SUPER_SENIOR
                    ),
                    applicableRegimes = setOf(Regime.NEW, Regime.OLD),
                    // For AY 2024-25: general understanding — 87A up to ₹5L in Old; up to ₹7L in New regime (subject to fine print).
                    incomeThreshold = 700000.0,
                    rebateCap = 25000.0 // map regime-specific cap in compute step below
                ),
                surchargeBandsByStatusAndRegime = mapOf(
                    // Individuals/HUFs: common tiers; special caps for certain gains/dividends and marginal relief apply in practice[5][6][7][4]
                    (TaxStatus.INDIVIDUAL to Regime.OLD) to listOf(
                        SurchargeBand(50_00_000.0, 0.10),
                        SurchargeBand(1_00_00_000.0, 0.15),
                        SurchargeBand(2_00_00_000.0, 0.25),
                        SurchargeBand(5_00_00_000.0, 0.37)
                    ),
                    (TaxStatus.INDIVIDUAL to Regime.NEW) to listOf(
                        SurchargeBand(50_00_000.0, 0.10),
                        SurchargeBand(1_00_00_000.0, 0.15),
                        SurchargeBand(2_00_00_000.0, 0.25),
                        SurchargeBand(
                            5_00_00_000.0,
                            0.25
                        ) // new regime cap at 25% for highest tier in recent years[6][7]
                    ),
                    (TaxStatus.HUF to Regime.OLD) to listOf(
                        SurchargeBand(50_00_000.0, 0.10),
                        SurchargeBand(1_00_00_000.0, 0.15),
                        SurchargeBand(2_00_00_000.0, 0.25),
                        SurchargeBand(5_00_00_000.0, 0.37)
                    ),
                    (TaxStatus.HUF to Regime.NEW) to listOf(
                        SurchargeBand(50_00_000.0, 0.10),
                        SurchargeBand(1_00_00_000.0, 0.15),
                        SurchargeBand(2_00_00_000.0, 0.25),
                        SurchargeBand(5_00_00_000.0, 0.25)
                    ),
                    (TaxStatus.SENIOR_CITIZEN to Regime.OLD) to listOf(
                        SurchargeBand(50_00_000.0, 0.10),
                        SurchargeBand(1_00_00_000.0, 0.15),
                        SurchargeBand(2_00_00_000.0, 0.25),
                        SurchargeBand(5_00_00_000.0, 0.37)
                    ),
                    (TaxStatus.SENIOR_CITIZEN to Regime.NEW) to listOf(
                        SurchargeBand(50_00_000.0, 0.10),
                        SurchargeBand(1_00_00_000.0, 0.15),
                        SurchargeBand(2_00_00_000.0, 0.25),
                        SurchargeBand(5_00_00_000.0, 0.25)
                    ),
                    (TaxStatus.SUPER_SENIOR to Regime.OLD) to listOf(
                        SurchargeBand(50_00_000.0, 0.10),
                        SurchargeBand(1_00_00_000.0, 0.15),
                        SurchargeBand(2_00_00_000.0, 0.25),
                        SurchargeBand(5_00_00_000.0, 0.37)
                    ),
                    (TaxStatus.SUPER_SENIOR to Regime.NEW) to listOf(
                        SurchargeBand(50_00_000.0, 0.10),
                        SurchargeBand(1_00_00_000.0, 0.15),
                        SurchargeBand(2_00_00_000.0, 0.25),
                        SurchargeBand(5_00_00_000.0, 0.25)
                    ),
                    // Firms (illustrative 12% above ₹1Cr; verify per AY)
                    (TaxStatus.FIRM to Regime.OLD) to listOf(
                        SurchargeBand(1_00_00_000.0, 0.12)
                    ),
                    (TaxStatus.FIRM to Regime.NEW) to listOf(
                        SurchargeBand(1_00_00_000.0, 0.12)
                    ),
                    // Domestic company surcharge bands (7% >₹1Cr, 12% >₹10Cr typical for standard regime; verify per AY and special regimes)[12][4]
                    (TaxStatus.COMPANY to Regime.OLD) to listOf(
                        SurchargeBand(1_00_00_000.0, 0.07),
                        SurchargeBand(10_00_00_000.0, 0.12)
                    ),
                    (TaxStatus.COMPANY to Regime.NEW) to listOf(
                        SurchargeBand(1_00_00_000.0, 0.07),
                        SurchargeBand(10_00_00_000.0, 0.12)
                    )
                ),
                marginalRelief = MarginalReliefRule { taxableIncome, baseTax, scBefore, bands ->
                    // Hook for precise marginal relief per AY and category.
                    // For now, return surcharge before relief; implement per threshold math when populating data.
                    scBefore
                }
            )
        )

        // EXAMPLE: AY 2025-26 (FY 2024-25) — similar to AY 2024-25 unless official changes; verify all values
        put(
            AssessmentYear.AY_2025_26,
            YearConfig(
                ay = AssessmentYear.AY_2025_26,
                regimeAvailable = setOf(Regime.OLD, Regime.NEW),
                slabsByStatusAndRegime = mapOf(
                    (TaxStatus.INDIVIDUAL to Regime.OLD) to listOf(
                        Slab(250000.0, 0.0),
                        Slab(500000.0, 0.05),
                        Slab(1000000.0, 0.20),
                        Slab(null, 0.30)
                    ),
                    (TaxStatus.HUF to Regime.OLD) to listOf(
                        Slab(250000.0, 0.0),
                        Slab(500000.0, 0.05),
                        Slab(1000000.0, 0.20),
                        Slab(null, 0.30)
                    ),
                    (TaxStatus.SENIOR_CITIZEN to Regime.OLD) to listOf(
                        Slab(300000.0, 0.0),
                        Slab(500000.0, 0.05),
                        Slab(1000000.0, 0.20),
                        Slab(null, 0.30)
                    ),
                    (TaxStatus.SUPER_SENIOR to Regime.OLD) to listOf(
                        Slab(500000.0, 0.0),
                        Slab(1000000.0, 0.20),
                        Slab(null, 0.30)
                    ),
                    (TaxStatus.INDIVIDUAL to Regime.NEW) to listOf(
                        Slab(300000.0, 0.0),
                        Slab(600000.0, 0.05),
                        Slab(900000.0, 0.10),
                        Slab(1200000.0, 0.15),
                        Slab(1500000.0, 0.20),
                        Slab(null, 0.30)
                    ),
                    (TaxStatus.HUF to Regime.NEW) to listOf(
                        Slab(300000.0, 0.0),
                        Slab(600000.0, 0.05),
                        Slab(900000.0, 0.10),
                        Slab(1200000.0, 0.15),
                        Slab(1500000.0, 0.20),
                        Slab(null, 0.30)
                    ),
                    (TaxStatus.FIRM to Regime.OLD) to listOf(Slab(null, 0.30)),
                    (TaxStatus.FIRM to Regime.NEW) to listOf(Slab(null, 0.30)),
                    (TaxStatus.COMPANY to Regime.OLD) to listOf(Slab(null, 0.25)),
                    (TaxStatus.COMPANY to Regime.NEW) to listOf(Slab(null, 0.25))
                ),
                cessRate = 0.04, // H&EC 4%[7][2]
                rebate87A = Rebate87A(
                    applicableStatuses = setOf(
                        TaxStatus.INDIVIDUAL,
                        TaxStatus.HUF,
                        TaxStatus.SENIOR_CITIZEN,
                        TaxStatus.SUPER_SENIOR
                    ),
                    applicableRegimes = setOf(Regime.NEW, Regime.OLD),
                    incomeThreshold = 700000.0,
                    rebateCap = 25000.0
                ),
                surchargeBandsByStatusAndRegime = mapOf(
                    (TaxStatus.INDIVIDUAL to Regime.OLD) to listOf(
                        SurchargeBand(50_00_000.0, 0.10),
                        SurchargeBand(1_00_00_000.0, 0.15),
                        SurchargeBand(2_00_00_000.0, 0.25),
                        SurchargeBand(5_00_00_000.0, 0.37)
                    ),
                    (TaxStatus.INDIVIDUAL to Regime.NEW) to listOf(
                        SurchargeBand(50_00_000.0, 0.10),
                        SurchargeBand(1_00_00_000.0, 0.15),
                        SurchargeBand(2_00_00_000.0, 0.25),
                        SurchargeBand(5_00_00_000.0, 0.25)
                    ),
                    (TaxStatus.HUF to Regime.OLD) to listOf(
                        SurchargeBand(50_00_000.0, 0.10),
                        SurchargeBand(1_00_00_000.0, 0.15),
                        SurchargeBand(2_00_00_000.0, 0.25),
                        SurchargeBand(5_00_00_000.0, 0.37)
                    ),
                    (TaxStatus.HUF to Regime.NEW) to listOf(
                        SurchargeBand(50_00_000.0, 0.10),
                        SurchargeBand(1_00_00_000.0, 0.15),
                        SurchargeBand(2_00_00_000.0, 0.25),
                        SurchargeBand(5_00_00_000.0, 0.25)
                    ),
                    (TaxStatus.SENIOR_CITIZEN to Regime.OLD) to listOf(
                        SurchargeBand(50_00_000.0, 0.10),
                        SurchargeBand(1_00_00_000.0, 0.15),
                        SurchargeBand(2_00_00_000.0, 0.25),
                        SurchargeBand(5_00_00_000.0, 0.37)
                    ),
                    (TaxStatus.SENIOR_CITIZEN to Regime.NEW) to listOf(
                        SurchargeBand(50_00_000.0, 0.10),
                        SurchargeBand(1_00_00_000.0, 0.15),
                        SurchargeBand(2_00_00_000.0, 0.25),
                        SurchargeBand(5_00_00_000.0, 0.25)
                    ),
                    (TaxStatus.SUPER_SENIOR to Regime.OLD) to listOf(
                        SurchargeBand(50_00_000.0, 0.10),
                        SurchargeBand(1_00_00_000.0, 0.15),
                        SurchargeBand(2_00_00_000.0, 0.25),
                        SurchargeBand(5_00_00_000.0, 0.37)
                    ),
                    (TaxStatus.SUPER_SENIOR to Regime.NEW) to listOf(
                        SurchargeBand(50_00_000.0, 0.10),
                        SurchargeBand(1_00_00_000.0, 0.15),
                        SurchargeBand(2_00_00_000.0, 0.25),
                        SurchargeBand(5_00_00_000.0, 0.25)
                    ),
                    (TaxStatus.FIRM to Regime.OLD) to listOf(SurchargeBand(1_00_00_000.0, 0.12)),
                    (TaxStatus.FIRM to Regime.NEW) to listOf(SurchargeBand(1_00_00_000.0, 0.12)),
                    (TaxStatus.COMPANY to Regime.OLD) to listOf(
                        SurchargeBand(1_00_00_000.0, 0.07),
                        SurchargeBand(10_00_00_000.0, 0.12)
                    ),
                    (TaxStatus.COMPANY to Regime.NEW) to listOf(
                        SurchargeBand(1_00_00_000.0, 0.07),
                        SurchargeBand(10_00_00_000.0, 0.12)
                    )
                ),
                marginalRelief = MarginalReliefRule { _, _, scBefore, _ -> scBefore }
            )
        )

        // PLACEHOLDERS FOR ALL OTHER YEARS:
        // For each AY from AY_1981_82 to AY_2023_24 and AY_2026_27:
        // - Add regimeAvailable (most older years: Regime.OLD only).
        // - Add slabsByStatusAndRegime for statuses applicable in that AY.
        // - Set cessRate for that AY (e.g., 0.00 prior to 2004; 0.02 from 2004-05; 0.03 from 2007-08; 0.04 from 2018-19 onward).
        // - Configure rebate87A where applicable (introduced in 2013-14/2014-15 era; thresholds changed over time).
        // - Configure surchargeBands per AY and taxpayer with correct thresholds and rates; add marginal relief rule.
        // If not populated, compute() will throw with a message prompting to add the AY.
    }

    // Public API: compute tax breakup for given input
    fun computeTaxBreakup(input: TaxInput): TaxBreakup {
        val cfg = CONFIGS[input.assessmentYear]
            ?: throw IllegalStateException("Rates for ${input.assessmentYear.label} are not populated yet. Please add YearConfig for this AY using official charts.")

        if (input.regime !in cfg.regimeAvailable) {
            throw IllegalStateException("Selected regime ${input.regime} is not available for ${input.assessmentYear.label}.")
        }

        val slabs = cfg.slabsByStatusAndRegime[Pair(input.status, input.regime)]
            ?: throw IllegalStateException("Slabs not defined for ${input.status} in ${input.regime} for ${input.assessmentYear.label}.")

        // Regime-wise deductions:
        // Old regime allows deductions such as 80C/other (subject to AY-specific caps);
        // New regime generally disallows most deductions except permitted ones—handle when you populate data.
        val allowedDeductions = when (input.regime) {
            Regime.OLD -> min(150000.0, input.deductions80C) + input.deductionsOther
            Regime.NEW -> 0.0
        }

        val taxable = max(0.0, input.totalIncome - allowedDeductions)
        val base = computeBaseTaxFromSlabs(taxable, slabs)

        // Apply Section 87A rebate where applicable and eligible
        val baseAfter87A = apply87AIfEligible(base, taxable, input, cfg)

        // Surcharge rate by last crossed threshold
        val bands =
            cfg.surchargeBandsByStatusAndRegime[Pair(input.status, input.regime)] ?: emptyList()
        val scRate = computeSurchargeRate(taxable, bands)
        val scBeforeRelief = baseAfter87A * scRate

        // Marginal relief hook
        val surcharge =
            cfg.marginalRelief?.apply?.invoke(taxable, baseAfter87A, scBeforeRelief, bands)
                ?: scBeforeRelief

        // Cess on tax + surcharge
        val cess = (baseAfter87A + surcharge) * cfg.cessRate

        val total = baseAfter87A + surcharge + cess
        return TaxBreakup(
            taxableIncome = taxable,
            baseTax = baseAfter87A,
            surcharge = surcharge,
            cess = cess,
            totalBeforeInterest = total
        )
    }

    // Progressive slabs
    private fun computeBaseTaxFromSlabs(income: Double, slabs: List<Slab>): Double {
        var remaining = income
        var prev = 0.0
        var tax = 0.0
        for (s in slabs) {
            val cap = s.upTo ?: Double.MAX_VALUE
            val amt = max(0.0, min(remaining, cap - prev))
            if (amt > 0) tax += amt * s.rate
            remaining -= amt
            prev = cap
            if (remaining <= 0) break
        }
        return max(0.0, tax)
    }

    private fun computeSurchargeRate(taxable: Double, bands: List<SurchargeBand>): Double {
        var rate = 0.0
        for (b in bands) {
            if (taxable > b.thresholdIncomeExclusive) rate = b.rate
        }
        return rate
    }

    private fun apply87AIfEligible(
        baseTax: Double,
        taxableIncome: Double,
        input: TaxInput,
        cfg: YearConfig
    ): Double {
        val r = cfg.rebate87A ?: return baseTax
        if (input.status !in r.applicableStatuses) return baseTax
        if (input.regime !in r.applicableRegimes) return baseTax

        // Note: 87A details vary by AY and regime (threshold and cap differ). For AYs where Old vs New differ,
        // you can encode regime-specific thresholds/caps by extending Rebate87A or handling in code here.
        // For the illustrated years:
        // - Old regime commonly offered rebate up to ₹12,500 when total income ≤₹5,00,000.
        // - New regime commonly offered rebate up to ₹25,000 when total income ≤₹7,00,000 (post 2023 reforms).
        val threshold = when (input.regime) {
            Regime.OLD -> 500000.0
            Regime.NEW -> r.incomeThreshold // e.g., 700000.0 for AY 2024-25/2025-26 illustrations
        }
        val cap = when (input.regime) {
            Regime.OLD -> 12500.0
            Regime.NEW -> r.rebateCap // e.g., 25000.0
        }

        if (taxableIncome <= threshold) {
            val rebate = min(cap, baseTax)
            return max(0.0, baseTax - rebate)
        }
        return baseTax
    }
}