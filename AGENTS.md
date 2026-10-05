# AGENTS.md

## CI handoff — 2026-10-05
- Build and quality workflows now use `concurrency` with `cancel-in-progress: true` per ref.
- Main/master release behavior is unchanged; only stale queued/running jobs are cancelled.
