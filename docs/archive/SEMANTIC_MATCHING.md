---
status: mvp-fallback-implemented-model-experiment-pending
last_verified: 2026-08-15
audience: maintainers-and-ai
sensitivity: internal
---

> **Status: superseded (2026-10-20, DEC-012).** Mankeu now imports only its own Excel template; see `docs/audit/2026-10-20_RENCANA_PERBAIKAN.md` F-01. Kept as historical research evidence.

# Semantic Matching Strategy

## Current MVP

`SemanticMatcher` is an implementation boundary. The shipped MVP uses a deterministic Indonesian normalizer with stop-word removal, reviewed aliases, Jaccard/containment scoring, exact amount checks, aggregation checks for split ledger rows, and coordinated-sheet evidence.

Similarity alone never authorizes an automatic financial mutation. Automatic enrichment additionally requires:

1. the fact to be absent from direct ledger representation, including compatible split rows;
2. matching amount and meaning in the RAB;
3. independent matching amount and meaning in profit/loss;
4. independent matching amount and meaning in cash flow;
5. no unresolved conflict;
6. provenance and a persistent user-visible notice.

## Embedding experiment

`all-MiniLM-L6-v2` or a stronger small multilingual/Indonesian sentence encoder may later replace or complement fuzzy candidate retrieval on-device. It is an experiment, not a promised dependency. Before inclusion:

- convert and quantize through a reproducible ONNX/TFLite pipeline with recorded model license and checksum;
- benchmark APK size, cold start, latency, memory, and battery on the agreed low/mid-range device matrix;
- evaluate the fixed train/validation/test splits in `../arina-finance-workbook-fixtures/datasets/semantic-label-pairs.jsonl`;
- add sanitized labels from future widyaiswara sources only through provenance/privacy review;
- measure retrieval separately from final classification and financial reconciliation;
- define abstention thresholds from false-automatic-mutation cost, not cosmetic accuracy;
- retain the deterministic fallback when the model is unavailable or unsuitable;
- keep all inference local unless a later owner-approved privacy decision explicitly changes that boundary.

Training a model from scratch is not justified by the starter dataset. A likely progression is reviewed aliases → benchmark existing sentence encoders → fine-tune only when a sufficiently large, licensed, source-separated dataset exists.
