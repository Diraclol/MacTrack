# MacTrack docs

Index of the project's documentation. New here? Read **MACTRACK_STATE** for where things stand, then
**ENGINEERING_SUMMARY** for how the app is built; everything else is reference.

## Status & architecture

| Doc | What it is |
|---|---|
| [MACTRACK_STATE.md](MACTRACK_STATE.md) | Project status: what is built, what is in flight, and what is next. Start here. |
| [ENGINEERING_SUMMARY.md](ENGINEERING_SUMMARY.md) | Architecture overview: layers, conventions, the hard rules, and the key design decisions (and why). |
| [BACKLOG.md](BACKLOG.md) | The task backlog — open work in detail, shipped work as a checklist. |

## Product & design

| Doc | What it is |
|---|---|
| [MACROFACTOR_REFERENCE.md](MACROFACTOR_REFERENCE.md) | The visual and interaction reference the UI targets, captured as notes. |
| [AI4_PLAN.md](AI4_PLAN.md) | Design + staged build for turning an ingredient list into a saved recipe or meal. |

## Backend & platform (future work)

| Doc | What it is |
|---|---|
| [BACKEND_RESEARCH.md](BACKEND_RESEARCH.md) | Backend evaluation (Supabase vs Neon vs Convex) and the chosen path: Supabase, cloud-free first then self-host. |
| [SUPABASE_PLAN.md](SUPABASE_PLAN.md) | Concrete design for accounts + per-user sync + a shared food database: schema, RLS, the sync engine, and the phased rollout. Parked until after the public release. |
| [PWA_IOS_SPIKE.md](PWA_IOS_SPIKE.md) | Feasibility spike for iOS (Compose Multiplatform) and web (a thin PWA). |

## Security

| Doc | What it is |
|---|---|
| [SECURITY.md](SECURITY.md) | Security and threat model for the current app and the planned accounts/roles/sync backend. Written in Supabase terms. |

---

The public-facing overview is the repo-root [README.md](../README.md).
