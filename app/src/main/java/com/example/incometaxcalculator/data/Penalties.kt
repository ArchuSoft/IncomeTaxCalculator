package com.example.incometaxcalculator.data

data class PenaltyItem(val section: String, val title: String, val description: String)

object PenaltiesData {
    val penalties = listOf(
        PenaltyItem("234F", "Late filing fee", "Fee for delay in filing return[6]."),
        PenaltyItem("270A", "Under-reporting/misreporting", "Penalty for under-reporting or misreporting of income[12]."),
        PenaltyItem("271A", "Failure to keep books", "Penalty for not maintaining books of account[6]."),
        PenaltyItem("271B", "Failure to get accounts audited", "Penalty for not auditing accounts when required[6][12]."),
        PenaltyItem("271C", "Failure to deduct TDS", "Penalty for failure to deduct or pay TDS[6]."),
        PenaltyItem("271D/271E", "Loan/deposit in cash", "Penalties for contravention of ss.269SS/269T[6][12]."),
        PenaltyItem("271AA", "Transfer pricing documentation", "Penalty for failure/inaccuracy in TP documentation[6]."),
        PenaltyItem("271J", "Incorrect report by professionals", "Penalty on accountants/merchants bankers/registered valuer[6]."),
        PenaltyItem("140A(3)", "Default in self-assessment tax", "Penalty for default u/s 140A(3)[18]."),
        PenaltyItem("275A/275B", "Search-related offences", "Contravention/failure during search 132/132(3)[6]."),
        PenaltyItem("276B/276BB", "Failure to pay TDS/TCS to Govt", "Prosecution for failure to deposit TDS/TCS[6]."),
        PenaltyItem("276CC", "Failure to furnish return", "Prosecution for willful failure u/s 139/142/148 etc.[6].")
        // Extend list as needed using official Act[15]
    )
}
