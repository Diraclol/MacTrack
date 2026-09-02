# MacTrack docs — knowledge base

Index of the project's documentation. New here? Read **MACTRACK_STATE** then **ENGINEERING_SUMMARY**
first; everything else is reference you can reach for as needed.

## Start here — orientation

| Doc | What it is |
|---|---|
| [MACTRACK_STATE.md](MACTRACK_STATE.md) | Where the project is right now: current position, the data-model rationale, and the ordered task queue (the exact next task). The single "state of the world" doc. |
| [ENGINEERING_SUMMARY.md](ENGINEERING_SUMMARY.md) | Architecture overview for an engineer picking the app up: layers, conventions, and the load-bearing rules. |

## Product & design

| Doc | What it is |
|---|---|
| [MACROFACTOR_REFERENCE.md](MACROFACTOR_REFERENCE.md) | The visual and interaction reference the app targets, captured as concrete notes. |

## Roadmap & feature plans

| Doc | What it is |
|---|---|
| [BACKLOG.md](BACKLOG.md) | The full task backlog — everything shipped and everything open, with rationale. The working task list. |
| [AI4_PLAN.md](AI4_PLAN.md) | Design + staged build for turning an ingredient list into a saved recipe or meal (the AI assistant's build flow). |

## Backend & platform (future work)

| Doc | What it is |
|---|---|
| [BACKEND_RESEARCH.md](BACKEND_RESEARCH.md) | Backend evaluation (Supabase vs Neon vs Convex) and the chosen path: Supabase, Cloud-Free-first then self-host. |
| [SUPABASE_PLAN.md](SUPABASE_PLAN.md) | Concrete design for accounts + per-user cloud sync + the shared food database: schema, RLS, the sync engine, and the phased rollout. Parked until after the public release. |
| [PWA_IOS_SPIKE.md](PWA_IOS_SPIKE.md) | Feasibility spike for iOS and web: recommendation (Compose Multiplatform for iOS, a thin PWA for web) and the one discipline to hold now. |

## Security

| Doc | What it is |
|---|---|
| [SECURITY.md](SECURITY.md) | Security and threat model. NOTE: its principles are authoritative, but it predates the backend decision and still names Firebase in places — see [SUPABASE_PLAN.md](SUPABASE_PLAN.md) §8 for the Firebase to Supabase mapping. |

---

The public-facing project overview is the repo-root [README.md](../README.md).
