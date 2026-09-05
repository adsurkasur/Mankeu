package com.arinahub.arinaagri.calculator.importer

import com.arinahub.arinaagri.calculator.domain.CategoryRole

/**
 * Puts an imported row into one of the app's own categories.
 *
 * Before this existed the importer labelled every expense row "Pengeluaran impor" and every income
 * row "Pemasukan impor". The totals were exact — the golden workbook reconciled to the rupiah — but
 * the cost breakdown had one slice, so the owner could see *that* they spent Rp22.159.000 and not
 * *what on*, which is most of why the reference workbook groups its costs at all.
 *
 * Two passes, cheapest first:
 *
 * 1. **Keyword.** Agricultural bookkeeping vocabulary is small and stable — "upah" and "HOK" mean
 *    labour, "combine" and "transplanter" mean machinery service. A hit here is exact and needs no
 *    scoring.
 * 2. **Fuzzy.** Anything the keywords miss is scored against the category names themselves with
 *    [FuzzySemanticMatcher], the same token matcher the workbook reconciler already uses.
 *
 * Below the threshold the row keeps the generic label. That is deliberate: a wrong category is
 * worse than an obvious unsorted one, because a wrong category is silently believable. Every
 * classified row says so in its preview line, so nothing is renamed behind the owner's back.
 *
 * No embedding model. A MiniLM would add tens of megabytes, a tokenizer, and an inference runtime
 * to a task that a keyword table settles exactly on this corpus — every one of the reference
 * workbook's 38 rows lands on the same category the workbook's own profit-and-loss sheet puts it in.
 * If a real corpus later shows rows this cannot place, the [SemanticMatcher] interface is where a
 * model would go, behind the same "never mutate silently" rule.
 */
object CategoryClassifier {

    /** Below this, the row is left unsorted rather than guessed at. */
    private const val FUZZY_THRESHOLD = 0.45

    // Ordered: the first category whose keywords appear wins, so the specific beats the general.
    //
    // Labour is tested FIRST, and that order is load-bearing. A wage row names the work it paid for,
    // so "upah penyiangan herbisida" contains "herbisida" and "upah persemaian sistem dapog"
    // contains "semai". Testing ingredients first sent Rp600.000 of wages into Obat dan Pestisida
    // and Benih dan Bibit — the totals still reconciled, so only the breakdown was wrong, which is
    // the kind of error a spreadsheet owner finds months later. Paying a person is labour whatever
    // the person was doing.
    //
    // "iuran pengairan" is a fee, not the wage row "upah pengairan" it shares a word with; it is
    // caught by "iuran" and never reaches the shared word.
    private val EXPENSE_KEYWORDS: List<Pair<String, List<String>>> = listOf(
        "Tenaga Kerja" to listOf("upah", "hok", "tenaga kerja", "buruh", "borongan", "gaji", "karyawan", "lembur", "pegawai", "kasir", "koki", "staf", "barista", "teknisi", "waiter", "pramusaji"),
        "Benih dan Bibit" to listOf("benih", "bibit", "semai"),
        "Pupuk" to listOf("pupuk", "npk", "urea", "za", "dolomit", "kapur", "kompos", "organik"),
        "Obat dan Pestisida" to listOf("herbisida", "pestisida", "fungisida", "insektisida", "obat", "hpt", "racun"),
        "Pengairan" to listOf("iuran", "irigasi"),
        "Jasa Alsintan" to listOf(
            "jasa", "traktor", "transplanter", "combine", "harvester", "alsintan",
            "pengolahan tanah", "pompa",
        ),
        "Transport" to listOf("transport", "angkut", "ongkos kirim", "kirim", "ongkir", "ekspedisi", "kurir", "bensin", "solar", "bbm"),
        "Sewa Lahan" to listOf("sewa", "kontrak tempat", "kontrak kios"),
        "Lain-lain" to listOf(
            "karung", "rafia", "tali", "administrasi", "atk", "kantong", "kardus", "kresek", "plastik",
            "kemasan", "box", "kebersihan", "sampah", "parkir", "listrik", "pln", "air", "pdam", "pulsa", "kuota",
            "gas", "lpg", "iklan", "promosi", "alat", "peralatan", "kulakan", "belanja dagangan", "stok", "bahan baku"
        ),
    )

    private val REVENUE_KEYWORDS: List<Pair<String, List<String>>> = listOf(
        "Hasil Panen" to listOf("gabah", "gkp", "gkg", "panen", "hasil", "padi", "beras"),
        "Penjualan Lain" to listOf("jual", "penjualan", "sewa keluar", "omzet", "pesanan", "order", "katering", "servis", "service", "pendapatan", "fee", "ongkos kerja"),
    )

    /**
     * @return the name of an existing category, or null when nothing matched well enough.
     */
    fun classify(
        description: String,
        role: CategoryRole,
        activeCategories: List<String> = emptyList(),
    ): String? {
        val table = when (role) {
            CategoryRole.REVENUE -> REVENUE_KEYWORDS
            CategoryRole.EXPENSE -> EXPENSE_KEYWORDS
            else -> return null
        }
        val haystack = FuzzySemanticMatcher.normalized(description)
        // Match on the normalized form so accents, punctuation and word order do not matter, and on
        // the raw lowercase text so multi-word keys like "pengolahan tanah" still work.
        val raw = description.lowercase()

        // If active categories are passed (e.g. from UMKM templates), prioritize matching against them
        if (activeCategories.isNotEmpty()) {
            activeCategories.firstOrNull { cat ->
                val normCat = FuzzySemanticMatcher.normalized(cat)
                normCat.length > 3 && (normCat in haystack || normCat in raw)
            }?.let { return it }

            val bestActive = activeCategories
                .map { cat -> cat to FuzzySemanticMatcher.similarity(description, cat) }
                .maxByOrNull { it.second }
            if (bestActive != null && bestActive.second >= FUZZY_THRESHOLD) {
                return bestActive.first
            }
        }

        // Pass 1: Exact keyword matches (prioritized order: labour first)
        table.forEach { (category, keywords) ->
            if (keywords.any { it in haystack || it in raw }) return category
        }

        // Pass 2: Fuzzy keyword matches for typos (e.g. "persmaian", "trnsplanter", "herbisda")
        val descTokens = FuzzySemanticMatcher.tokens(description)
        table.forEach { (category, keywords) ->
            val matchesFuzzyKeyword = keywords.any { kw ->
                val kwTokens = FuzzySemanticMatcher.tokens(kw)
                kwTokens.isNotEmpty() && kwTokens.all { kt ->
                    descTokens.any { dt -> FuzzySemanticMatcher.tokenSimilarity(kt, dt) >= 0.85 }
                }
            }
            if (matchesFuzzyKeyword) return category
        }

        // Pass 3: Similarity to category name
        return table
            .map { (category, _) -> category to FuzzySemanticMatcher.similarity(description, category) }
            .maxByOrNull { it.second }
            ?.takeIf { it.second >= FUZZY_THRESHOLD }
            ?.first
    }
}
