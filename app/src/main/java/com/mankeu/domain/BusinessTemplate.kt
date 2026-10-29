package com.mankeu.domain

/**
 * The categories a new business starts with.
 *
 * This used to be four sector templates behind a `BusinessSector` enum (D-048), each with its own
 * category list and its own unit list. It was removed in round 9 for three reasons, all of them
 * visible in the code rather than argued from taste:
 *
 * 1. **Nobody could pick one for most of its life.** The picker was never wired up until round 8,
 *    so every business ever created was `GENERAL_MSME` regardless. Four lists were maintained,
 *    tested and localised; one was ever used.
 * 2. **The choice is unrecoverable.** A sector was applied once, at creation, and there was no way
 *    to change it afterwards — an owner who picked wrong had to delete the business or rebuild the
 *    category list by hand. A starting list that can be edited freely does not need to be right.
 * 3. **Categories are user data anyway.** Every category is editable, addable and archivable from
 *    Pengaturan, and an import creates whatever a workbook names with the role its section implies
 *    (D-060). The starting list is a convenience, not a schema.
 *
 * What remains is one general list that suits a warung, a bengkel, a kitchen or a farm equally,
 * because every name in it is a role rather than a trade. See D-063.
 */
data class DefaultCategoryTemplate(
    val name: String,
    val role: CategoryRole,
    /** Expense categories only (DEC-014 B-1). */
    val costNature: CostNature? = null,
    /** "Biaya produksi (sudah dihitung di HPP)" (F2-8): spend here is represented by HPP. */
    val includedInHpp: Boolean = false,
)

object DefaultMasterData {
    /** Basic only (DEC-011 K-5); anything trade-specific is a custom category. */
    val categories: List<DefaultCategoryTemplate> = listOf(
        DefaultCategoryTemplate("Penjualan & Jasa", CategoryRole.REVENUE),
        DefaultCategoryTemplate("Pendapatan Lain", CategoryRole.REVENUE),
        DefaultCategoryTemplate("Bahan Baku & Stok", CategoryRole.EXPENSE, CostNature.VARIABLE, includedInHpp = true),
        DefaultCategoryTemplate("Gaji & Upah", CategoryRole.EXPENSE, CostNature.FIXED),
        DefaultCategoryTemplate("Sewa Tempat", CategoryRole.EXPENSE, CostNature.FIXED),
        DefaultCategoryTemplate("Listrik, Air & Internet", CategoryRole.EXPENSE, CostNature.FIXED),
        DefaultCategoryTemplate("Transportasi", CategoryRole.EXPENSE, CostNature.VARIABLE),
        DefaultCategoryTemplate("Biaya Lain-lain", CategoryRole.EXPENSE, CostNature.FIXED),
        DefaultCategoryTemplate("Setoran Modal Pemilik", CategoryRole.CAPITAL_IN),
        DefaultCategoryTemplate("Ambil untuk Pribadi (Prive)", CategoryRole.DRAWING),
        DefaultCategoryTemplate("Beli Peralatan & Aset", CategoryRole.ASSET),
    )

    /** Normalised name of the default materials category, the one counted in HPP from the start (F2-8). */
    const val MATERIALS_KEY = "bahan baku stok"

    /** A category created by hand, import or migration starts counted in HPP only if it is the materials one. */
    fun includedInHppByDefault(role: CategoryRole, normalizedName: String) = role == CategoryRole.EXPENSE && normalizedName == MATERIALS_KEY
}
