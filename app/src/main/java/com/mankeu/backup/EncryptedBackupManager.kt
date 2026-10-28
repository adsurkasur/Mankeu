package com.mankeu.backup

import com.mankeu.sync.SyncSchema
import android.content.ContentValues
import android.database.Cursor
import androidx.room.withTransaction
import com.mankeu.data.AppDatabase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

sealed interface BackupValue {
    data object Null : BackupValue
    data class Integer(val value: Long) : BackupValue
    data class Real(val value: Double) : BackupValue
    data class Text(val value: String) : BackupValue
    data class Blob(val value: ByteArray) : BackupValue
}

data class BackupTable(
    val name: String,
    val columns: List<String>,
    val rows: List<List<BackupValue>>,
)

data class PortableBackup(
    val schemaVersion: Int,
    val createdAtEpochMillis: Long,
    val tables: List<BackupTable>,
)

data class BackupPreview(
    val schemaVersion: Int,
    val createdAtEpochMillis: Long,
    val tableCount: Int,
    val rowCount: Int,
    val businessCount: Int,
    val projectCount: Int,
    val budgetItemCount: Int,
    val financialEventCount: Int,
)

internal fun PortableBackup.toPreview(): BackupPreview {
    val counts = tables.associate { it.name to it.rows.size }
    return BackupPreview(
        schemaVersion = schemaVersion,
        createdAtEpochMillis = createdAtEpochMillis,
        tableCount = tables.size,
        rowCount = tables.sumOf { it.rows.size },
        businessCount = counts["businesses"] ?: 0,
        projectCount = counts["projects"] ?: 0,
        budgetItemCount = counts["budget_items"] ?: 0,
        financialEventCount = counts["financial_events"] ?: 0,
    )
}

class PreparedRestore internal constructor(
    internal val snapshot: PortableBackup,
    val preview: BackupPreview,
)

data class RestoreResult(val tableCount: Int, val rowCount: Int, val recoveryCreated: Boolean)

object PortableBackupCodec {
    fun encode(snapshot: PortableBackup): ByteArray = ByteArrayOutputStream().use { output ->
        DataOutputStream(output).use { data ->
            data.write(MAGIC)
            data.writeInt(FORMAT_VERSION)
            data.writeInt(snapshot.schemaVersion)
            data.writeLong(snapshot.createdAtEpochMillis)
            data.writeInt(snapshot.tables.size)
            snapshot.tables.forEach { table ->
                data.writeText(table.name)
                data.writeInt(table.columns.size)
                table.columns.forEach { column -> data.writeText(column) }
                data.writeInt(table.rows.size)
                table.rows.forEach { row ->
                    require(row.size == table.columns.size) { "Jumlah nilai dan kolom backup tidak sama." }
                    row.forEach { value -> data.writeValue(value) }
                }
            }
        }
        output.toByteArray()
    }

    fun decode(bytes: ByteArray): PortableBackup {
        require(bytes.size <= MAX_PAYLOAD_BYTES) { "Isi backup terlalu besar." }
        return DataInputStream(ByteArrayInputStream(bytes)).use { data ->
            val magic = ByteArray(MAGIC.size).also(data::readFully)
            require(magic.contentEquals(MAGIC) || magic.contentEquals(LEGACY_PORTABLE_MAGIC)) { "Format isi backup tidak dikenali." }
            require(data.readInt() == FORMAT_VERSION) { "Versi format backup belum didukung." }
            val schemaVersion = data.readInt()
            val createdAt = data.readLong()
            val tableCount = data.readCount(MAX_TABLES, "Jumlah tabel backup tidak wajar.")
            val tables = List(tableCount) {
                val name = data.readText(MAX_NAME_BYTES)
                val columnCount = data.readCount(MAX_COLUMNS, "Jumlah kolom backup tidak wajar.")
                val columns = List(columnCount) { data.readText(MAX_NAME_BYTES) }
                val rowCount = data.readCount(MAX_ROWS_PER_TABLE, "Jumlah baris backup tidak wajar.")
                val rows = List(rowCount) { List(columnCount) { data.readValue() } }
                BackupTable(name, columns, rows)
            }
            require(data.available() == 0) { "Backup memuat data tambahan yang tidak dikenali." }
            PortableBackup(schemaVersion, createdAt, tables)
        }
    }

    private fun DataOutputStream.writeValue(value: BackupValue) = when (value) {
        BackupValue.Null -> writeByte(0)
        is BackupValue.Integer -> { writeByte(1); writeLong(value.value) }
        is BackupValue.Real -> { writeByte(2); writeDouble(value.value) }
        is BackupValue.Text -> { writeByte(3); writeText(value.value) }
        is BackupValue.Blob -> { writeByte(4); writeBytesWithLength(value.value) }
    }

    private fun DataInputStream.readValue(): BackupValue = when (val type = readUnsignedByte()) {
        0 -> BackupValue.Null
        1 -> BackupValue.Integer(readLong())
        2 -> BackupValue.Real(readDouble())
        3 -> BackupValue.Text(readText(MAX_TEXT_BYTES))
        4 -> BackupValue.Blob(readBytesWithLength(MAX_BLOB_BYTES))
        else -> error("Tipe nilai backup $type tidak dikenali.")
    }

    private fun DataOutputStream.writeText(value: String) = writeBytesWithLength(value.toByteArray(Charsets.UTF_8))

    private fun DataOutputStream.writeBytesWithLength(value: ByteArray) {
        writeInt(value.size)
        write(value)
    }

    private fun DataInputStream.readText(maxBytes: Int): String = readBytesWithLength(maxBytes).toString(Charsets.UTF_8)

    private fun DataInputStream.readBytesWithLength(maxBytes: Int): ByteArray {
        val size = readCount(maxBytes, "Panjang nilai backup tidak wajar.")
        return ByteArray(size).also(::readFully)
    }

    private fun DataInputStream.readCount(max: Int, message: String): Int = readInt().also { require(it in 0..max) { message } }

    private val MAGIC = "MANKEU-PORTABLE-DB".toByteArray(Charsets.US_ASCII)
    private val LEGACY_PORTABLE_MAGIC = "ARINA-PORTABLE-DB".toByteArray(Charsets.US_ASCII)
    private const val FORMAT_VERSION = 1
    private const val MAX_PAYLOAD_BYTES = 128 * 1024 * 1024
    private const val MAX_TABLES = 64
    private const val MAX_COLUMNS = 256
    private const val MAX_ROWS_PER_TABLE = 1_000_000
    private const val MAX_NAME_BYTES = 8 * 1024
    private const val MAX_TEXT_BYTES = 8 * 1024 * 1024
    private const val MAX_BLOB_BYTES = 64 * 1024 * 1024
}

object PasswordBackupCipher {
    fun encrypt(plain: ByteArray, password: CharArray, iterations: Int = DEFAULT_ITERATIONS): ByteArray {
        require(password.size >= 8) { "Kata sandi cadangan minimal 8 karakter." }
        require(iterations >= 100_000) { "Penguatan kata sandi terlalu lemah." }
        val salt = ByteArray(16).also(secureRandom::nextBytes)
        val iv = ByteArray(12).also(secureRandom::nextBytes)
        val key = deriveKey(password, salt, iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
            updateAAD(ENVELOPE_MAGIC)
        }
        val encrypted = cipher.doFinal(plain)
        return ByteArrayOutputStream().use { output ->
            DataOutputStream(output).use { data ->
                data.write(ENVELOPE_MAGIC)
                data.writeInt(ENVELOPE_VERSION)
                data.writeInt(iterations)
                data.writeInt(salt.size)
                data.write(salt)
                data.writeInt(iv.size)
                data.write(iv)
                data.writeInt(encrypted.size)
                data.write(encrypted)
            }
            output.toByteArray()
        }
    }

    fun decrypt(envelope: ByteArray, password: CharArray): ByteArray {
        require(password.isNotEmpty()) { "Kata sandi cadangan wajib diisi." }
        require(envelope.size <= MAX_ENVELOPE_BYTES) { "Berkas cadangan terlalu besar." }
        return try {
            DataInputStream(ByteArrayInputStream(envelope)).use { data ->
                val magic = ByteArray(10).also(data::readFully)
                val isValid = magic.contentEquals(ENVELOPE_MAGIC) || magic.contentEquals(LEGACY_MAGIC)
                require(isValid) { "Berkas ini bukan cadangan Mankeu." }
                require(data.readInt() == ENVELOPE_VERSION) { "Versi enkripsi cadangan belum didukung." }
                val iterations = data.readInt().also { require(it in 100_000..2_000_000) { "Parameter enkripsi tidak valid." } }
                val salt = data.readSized(64)
                val iv = data.readSized(32)
                require(salt.size >= 16 && iv.size == 12) { "Parameter enkripsi tidak valid." }
                val encrypted = data.readSized(MAX_ENVELOPE_BYTES)
                require(data.available() == 0) { "Berkas cadangan memuat data tambahan." }
                val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                    init(Cipher.DECRYPT_MODE, deriveKey(password, salt, iterations), GCMParameterSpec(128, iv))
                    updateAAD(magic)
                }
                cipher.doFinal(encrypted)
            }
        } catch (_: AEADBadTagException) {
            error("Kata sandi salah atau berkas cadangan telah berubah/rusak.")
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val specification = PBEKeySpec(password, salt, iterations, 256)
        return try {
            val encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(specification).encoded
            SecretKeySpec(encoded, "AES")
        } finally {
            specification.clearPassword()
        }
    }

    private fun DataInputStream.readSized(max: Int): ByteArray {
        val size = readInt().also { require(it in 0..max) { "Panjang bagian cadangan tidak valid." } }
        return ByteArray(size).also(::readFully)
    }

    private val secureRandom = SecureRandom()
    private val ENVELOPE_MAGIC = "MANKEU-BKP".toByteArray(Charsets.US_ASCII)
    private val LEGACY_MAGIC = "ARINA-AABK".toByteArray(Charsets.US_ASCII)
    private const val ENVELOPE_VERSION = 1
    private const val DEFAULT_ITERATIONS = 310_000
    private const val MAX_ENVELOPE_BYTES = 160 * 1024 * 1024
}

class EncryptedBackupManager(
    private val database: AppDatabase,
    private val recoveryFile: File,
) {
    suspend fun create(password: CharArray): ByteArray {
        val snapshot = database.withTransaction { capture() }
        return PasswordBackupCipher.encrypt(PortableBackupCodec.encode(snapshot), password)
    }

    suspend fun prepareRestore(envelope: ByteArray, password: CharArray): PreparedRestore {
        val snapshot = BackupUpgrade.toCurrent(PortableBackupCodec.decode(PasswordBackupCipher.decrypt(envelope, password)))
        validate(snapshot)
        return PreparedRestore(snapshot, snapshot.toPreview())
    }

    suspend fun restore(prepared: PreparedRestore, recoveryPassword: CharArray): RestoreResult {
        validate(prepared.snapshot)
        val recovery = database.withTransaction { capture() }
        writeRecovery(PasswordBackupCipher.encrypt(PortableBackupCodec.encode(recovery), recoveryPassword))
        return database.withTransaction {
            val writable = database.openHelper.writableDatabase
            // A restore is a new baseline, not a pile of local edits: change capture stays off, the
            // outbox is cleared, and the next sync is told to push a full snapshot (DEC-013).
            SyncSchema.quietly(writable) {
            TABLES.asReversed().forEach { table -> writable.execSQL("DELETE FROM `$table`") }
            var rowCount = 0
            prepared.snapshot.tables.associateBy { it.name }.let { tables ->
                TABLES.forEach { tableName ->
                    // A table absent from the snapshot is left empty.
                    val table = tables[tableName] ?: return@forEach
                    val liveColumns = writable.query("PRAGMA table_info(`$tableName`)").use { cursor ->
                        buildSet { while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name"))) }
                    }
                    table.rows.forEach { row ->
                        val values = ContentValues(table.columns.size)
                        table.columns.zip(row)
                            .filter { (column, _) -> column in liveColumns }
                            .forEach { (column, value) -> values.putBackupValue(column, value) }
                        check(writable.insert(tableName, 0, values) != -1L) { "Gagal memulihkan tabel $tableName." }
                        rowCount += 1
                    }
                }
            }
            writable.execSQL("DELETE FROM sync_outbox")
            writable.execSQL("INSERT OR REPLACE INTO sync_meta(`key`, value) VALUES ('${SyncSchema.REQUIRES_FULL_SNAPSHOT}', '1')")
            RestoreResult(TABLES.size, rowCount, recoveryCreated = true)
            }
        }
    }

    fun hasLatestRecovery(): Boolean = recoveryFile.isFile && recoveryFile.length() > 0

    /** Part of "Hapus semua data" (DEC-020): the recovery copy is a full snapshot of the old data. */
    fun deleteLatestRecovery() {
        check(!recoveryFile.exists() || recoveryFile.delete()) { "Salinan pemulihan otomatis tidak dapat dihapus." }
    }

    fun readLatestRecovery(): ByteArray {
        check(hasLatestRecovery()) { "Belum ada salinan otomatis sebelum pemulihan." }
        require(recoveryFile.length() <= MAX_RECOVERY_BYTES) { "Salinan pemulihan otomatis melebihi batas aman." }
        return recoveryFile.readBytes()
    }

    private fun validate(snapshot: PortableBackup) {
        val current = currentSchemaVersion()
        // Exact-match would make every backup taken before an app update unrestorable after it;
        // see OLDEST_RESTORABLE_SCHEMA.
        require(snapshot.schemaVersion in OLDEST_RESTORABLE_SCHEMA..current) {
            "Versi basis data cadangan (${snapshot.schemaVersion}) tidak dapat dipulihkan ke aplikasi ini ($current)."
        }
        val names = snapshot.tables.map { it.name }.toSet()
        require(names.containsAll(TABLES) && names.size == TABLES.size) {
            "Daftar tabel cadangan tidak lengkap atau tidak dikenali."
        }
    }

    private fun writeRecovery(bytes: ByteArray) {
        recoveryFile.parentFile?.let { parent -> check(parent.exists() || parent.mkdirs()) { "Folder pemulihan otomatis tidak dapat dibuat." } }
        val temporary = File(recoveryFile.parentFile, ".${recoveryFile.name}.tmp")
        FileOutputStream(temporary).use { output ->
            output.write(bytes)
            output.flush()
            output.fd.sync()
        }
        if (recoveryFile.exists()) check(recoveryFile.delete()) { "Salinan pemulihan lama tidak dapat diganti." }
        check(temporary.renameTo(recoveryFile)) { "Salinan pemulihan otomatis tidak dapat disimpan." }
    }

    private fun capture(): PortableBackup {
        val writable = database.openHelper.writableDatabase
        val tables = TABLES.map { tableName ->
            writable.query("SELECT * FROM `$tableName`").use { cursor ->
                BackupTable(
                    name = tableName,
                    columns = cursor.columnNames.toList(),
                    rows = buildList {
                        while (cursor.moveToNext()) add(cursor.readBackupRow())
                    },
                )
            }
        }
        return PortableBackup(currentSchemaVersion(), System.currentTimeMillis(), tables)
    }

    private fun currentSchemaVersion(): Int = database.openHelper.writableDatabase.query("PRAGMA user_version").use { cursor ->
        check(cursor.moveToFirst()) { "Versi basis data tidak dapat dibaca." }
        cursor.getInt(0)
    }

    private fun Cursor.readBackupRow(): List<BackupValue> = (0 until columnCount).map { index ->
        when (getType(index)) {
            Cursor.FIELD_TYPE_NULL -> BackupValue.Null
            Cursor.FIELD_TYPE_INTEGER -> BackupValue.Integer(getLong(index))
            Cursor.FIELD_TYPE_FLOAT -> BackupValue.Real(getDouble(index))
            Cursor.FIELD_TYPE_STRING -> BackupValue.Text(getString(index))
            Cursor.FIELD_TYPE_BLOB -> BackupValue.Blob(getBlob(index))
            else -> error("Tipe kolom SQLite tidak dikenali.")
        }
    }

    private fun ContentValues.putBackupValue(column: String, value: BackupValue) = when (value) {
        BackupValue.Null -> putNull(column)
        is BackupValue.Integer -> put(column, value.value)
        is BackupValue.Real -> put(column, value.value)
        is BackupValue.Text -> put(column, value.value)
        is BackupValue.Blob -> put(column, value.value)
    }

    private companion object {
        const val MAX_RECOVERY_BYTES = 160 * 1024 * 1024

        /**
         * Nothing older than 12 is restorable (spec #1). A later migration that reshapes a table must
         * teach [BackupUpgrade] the same reshaping.
         */
        const val OLDEST_RESTORABLE_SCHEMA = 12
        val TABLES = AppDatabase.DATA_TABLES
    }
}
