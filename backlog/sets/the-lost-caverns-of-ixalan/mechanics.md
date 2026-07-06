# The Lost Caverns of Ixalan (LCI) — Mechanics

Counts are over the 286 booster cards (excluding basic lands), by front-face + back-face
oracle text. "Unimpl" = not yet implemented as of this document (72 cards done).

Engine-support column reflects whether the SDK/rules-engine already models the mechanic
(so cards using *only* supported mechanics need **no backend change** — pure `add-card` work).

## Set mechanics

| Mechanic | Total | Unimpl | Engine support | Notes |
|----------|------:|-------:|----------------|-------|
| **Explore** | 25 | 24 | ✅ `ExploreEffect` | Creature reveals top card; land → hand, nonland → +1/+1 counter + may mill. |
| **Discover N** | 11 | 11 | ❌ **NOT supported** | No SDK type exists (no effect / dynamic amount / condition). **Needs `add-feature`** — all 11 cards blocked until then. |
| **Craft** | 19 | 19 | ✅ `Craft*` + DFC transform | Activate from battlefield (exile materials), transform to back face. All Craft cards are DFCs. |
| **Descend (4 / 8 / fathomless)** | 28 | 27 | ✅ `descended` tracking | Cares about permanent cards in your graveyard; "descend N" / "fathomless descent". |
| **Map token** | 7 | 6 | ✅ `MapToken` | Artifact token; `{1}, {T}, Sacrifice: target creature you control explores.` |
| **Transform / DFC** | 35 | 35 | ✅ `TransformEffects` | Includes Craft backs, MDFC lands (front spell // back land), god // land flips. |
| **Treasure** | 16 | 12 | ✅ Treasure token | Standard artifact token, sac for one mana of any color. |
| **Vehicle / Crew** | 6 | 5 | ✅ Crew | Standard vehicles. |
| **Cave (land subtype)** | 12 | 10 | ✅ (subtype only) | New land subtype; some cards care "you control a Cave". |
| **Finality counter** | 5 | 4 | ✅ finality counters | -1/-1-ish removal-on-death counter; used by a few cards. |

## Evergreen / returning keywords present

Flying (25), Trample (14), Vigilance (10), Ward (9), Menace (6), Lifelink (5), Deathtouch (4),
Haste (4), Reach (4), First strike (2), Double strike (1), Flash (12), Defender (1),
Indestructible (1), Hexproof (1). Plus Equip (12), Cycling / typecycling / landcycling (13),
Mill (12), Scry (11), Surveil (1), Enchant (7), Fight (1). **All engine-supported.**

## Backend-change assessment

Every headline LCI mechanic **except Discover** is already modelled by the engine (72 LCI cards,
including a Craft DFC — `SaheelisLattice.kt` — are implemented). Therefore **most remaining cards
are pure `add-card` authoring**. The known backend gap:

- **Discover N (11 cards)** — no SDK primitive exists. Needs `add-feature` (a `DiscoverEffect`
  modelling "exile from top until nonland MV ≤ N; cast free or hand; rest to bottom random").
  Until then every Discover card is blocked, e.g. Primordial Gnawer, Geological Appraiser,
  Daring Discovery, Chimil the Inner Sun, Hurl into History, Walk with the Ancestors.

Beyond that, individual cards may need `add-feature` for a novel one-off ability — flagged
per-card during implementation. The 214 unimplemented cards break down as 179 single-faced + 35 DFCs.

Implementation order: single-faced cards using only supported mechanics first (explore / Map /
discover / standard effects), then DFCs (Craft, MDFC lands, god flips), deferring any card that
turns out to need a new SDK primitive.
