# Interaction Feedback - Tooltip dan Haptic

Cara Mankeu memberi tahu pengguna bahwa sentuhannya diterima: tooltip tekan lama dari Material 3 dan lima jenis getar. Aturannya ada di `docs/UX_AND_ACCESSIBILITY.md` ("Touch targets, tooltips and haptics") dan keputusan D-084 sampai D-087, diselaraskan dengan Room **schema 11** (DEC-019).

## 1. Dari peristiwa ke getar

```mermaid
flowchart TD
    subgraph eventSources ["Sumber peristiwa"]
        longPress["Tekan lama pada ikon, badge, atau kartu angka"]
        discrete["Ganti tab, segmen, filter, tanggal, atau tutup sheet"]
        destructive["Mulai hapus atau arsipkan"]
        formInvalid["Validasi formulir gagal"]
        vmMessage["Pesan ViewModel atau hasil ekspor dan cadangan"]
    end

    longPress --> tooltipBox["TooltipBox<br/>state.isVisible menjadi true"]
    tooltipBox --> evLong["LONG_PRESS"]
    discrete --> sameChoice{"Pilihan yang sama dengan sekarang?"}
    sameChoice -->|"ya"| noFeedback(["Tanpa getar"])
    sameChoice -->|"tidak"| evTap["TAP"]
    destructive --> evWarn["WARN"]
    formInvalid --> evReject["REJECT"]
    vmMessage --> isError{"UiText.isError atau TransferUiState.Failure?"}
    isError -->|"ya"| evReject
    isError -->|"tidak"| evConfirm["CONFIRM"]

    evLong --> feedback["MankeuFeedback"]
    evTap --> feedback
    evWarn --> feedback
    evReject --> feedback
    evConfirm --> feedback

    feedback --> hapticSwitch{"Sakelar getar di Pengaturan aktif?"}
    hapticSwitch -->|"tidak"| silent(["Tanpa getar"])
    hapticSwitch -->|"ya"| composeHaptic["LocalHapticFeedback<br/>performHapticFeedback"]
    composeHaptic --> platformHaptic["Getar platform<br/>mengikuti setelan getar ponsel"]
    feedback --> soundSwitch{"Sakelar suara aktif dan event bersuara?"}
    soundSwitch -->|"ya"| soundEffect["View.playSoundEffect"]
```

| Event | `HapticFeedbackType` | Contoh |
| --- | --- | --- |
| TAP | SegmentTick | Tab Laporan, Proyeksi atau Realisasi, filter Catatan, tombol Ubah, tanggal di kalender |
| LONG_PRESS | LongPress | Tooltip ikon Data, tombol Ubah catatan, kartu Uang masuk bersih |
| WARN | GestureThresholdActivate | Ketuk ikon Hapus pada catatan |
| CONFIRM | Confirm | Catatan tersimpan atau diperbarui, impor disimpan, cadangan dibuat |
| REJECT | Reject | Jumlah kosong, stok keluar melebihi stok tersedia |

## 2. Siklus tooltip

```mermaid
sequenceDiagram
    autonumber
    actor Owner as Pemilik usaha
    participant Anchor as Ikon, badge, atau kartu
    participant TipBox as TooltipBox
    participant Tip as MankeuTooltip
    participant Fb as MankeuFeedback

    Owner->>Anchor: Tekan dan tahan
    Anchor->>TipBox: Ambang tekan lama tercapai
    TipBox->>TipBox: Tampilkan di atas anchor, pindah ke bawah bila tidak muat
    TipBox-->>Tip: snapshotFlow isVisible bernilai true
    Tip->>Fb: LONG_PRESS
    Fb-->>Owner: Getar tekan lama
    Owner->>Anchor: Lepas jari
    TipBox->>TipBox: Pelepasan dikonsumsi, aksi tombol tidak ikut berjalan
    alt Jari diangkat, ketuk di luar, atau waktu habis
        TipBox->>TipBox: Tooltip ditutup
    else Pembaca layar aktif
        TipBox-->>Owner: Teks tooltip dibacakan sebagai deskripsi anchor
    end
```

## 3. Tampilan kartu catatan (`EventCard`)

Sejak DEC-019 kartu tidak punya status pembalikan: warnanya hanya mengikuti arah uang, dan setiap kartu di Catatan memiliki Ubah dan Hapus (target sentuh 48dp).

```mermaid
stateDiagram-v2
    state "Uang masuk, hijau dengan tanda plus" as Incoming
    state "Uang keluar, koral dengan tanda minus" as Outgoing
    state "Formulir terisi, Mengubah catatan" as Editing
    state "Sheet konfirmasi Hapus" as ConfirmDelete

    [*] --> Incoming : jenis masuk tersimpan
    [*] --> Outgoing : jenis keluar tersimpan
    Incoming --> Editing : Ubah, getar TAP
    Outgoing --> Editing : Ubah, getar TAP
    Editing --> Incoming : simpan sebagai masuk
    Editing --> Outgoing : simpan sebagai keluar
    Incoming --> ConfirmDelete : Hapus, getar WARN
    Outgoing --> ConfirmDelete : Hapus, getar WARN
    ConfirmDelete --> Incoming : batal
    ConfirmDelete --> Outgoing : batal
    ConfirmDelete --> [*] : Hapus dikonfirmasi

    note right of Editing
        Ubah dapat mengganti arah uang.
        Kategori menentukan jenis kas.
    end note
```
