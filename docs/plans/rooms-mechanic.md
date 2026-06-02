# Rooms Mechanic — Design Plan

Design for adding the **Rooms** card-shape and unlock mechanic from *Duskmourn: House of Horror*
(DSK) to the Argentum engine. Driving requirement: ship enough of the mechanic to implement
[`Unholy Annex // Ritual Chamber`](https://scryfall.com/card/dsk/118), the last unimplemented card in
[`backlog/magezero-coverage.md`](../../backlog/magezero-coverage.md) for the Standard-MonoB training deck.

## 1. Motivation

The MageZero coverage backlog has one card outstanding: `Unholy Annex // Ritual Chamber`. It's a
split-layout Room with door-unlock costs and per-door triggered abilities. It's also the only
representative of an entire card *layout* and an entire *mechanic*, so the implementation cost is
well above a normal card. This plan exists so we can land it deliberately rather than as part of
an `add-card` skill invocation.

**Goal:** ship a general-purpose Rooms implementation — not bespoke for Unholy Annex. Future DSK
Rooms (and any Room reprints) should drop in as data-only `cardDef { }` definitions.

## 2. Rules summary

Rules verified against the Comprehensive Rules at
<https://yawgatog.com/resources/magic-rules/>. Rooms are governed under **rule 709 — Split
Cards**, specifically **709.5** (split cards with a shared type line). The "Room" subtype is
listed in **205.3h**. "Door" is defined in **709.5j** as a half of a Room permanent.

- **Layout:** `split`. Both halves print on one card. Each half has its own name, mana cost, and
  rules text. Both halves share a single type line (`Enchantment — Room`) per **709.5a**, and the
  shared type line means both halves share the same types and subtypes.
- **In zones other than the battlefield/stack** (709.4): the card has the combined characteristics
  of both halves — combined name (`"X // Y"`), combined mana cost (sum of both halves' costs), and
  every ability from both halves' text boxes (709.4c). Mana value = sum of both halves (709.4b).
- **On the stack** (709.3): only the chosen half's characteristics exist (709.3b). Exception per
  709.5b: the *existence* of each half is part of the object's copiable values, even on the stack
  — relevant for clones/copies of the cast half.
- **Casting** (709.3): you cast one half. Only that half is evaluated for legality and put on the
  stack (709.3a).
- **Entering the battlefield** (709.5d):
  - If the left half was cast: enters with `"left half unlocked"` designation, right half locked.
  - If the right half was cast: enters with `"right half unlocked"`, left half locked.
  - **If neither half was cast** (e.g. reanimated, put onto the battlefield by another effect):
    enters with **neither** designation — both halves locked. This is the defined CR behavior, not
    a corner case.
- **Permanent characteristics on the battlefield** (709.5):
  - Type line is always shared (`"Enchantment — Room"`).
  - Locked halves are suppressed by two intrinsic static abilities (printed in 709.5 verbatim):
    > "As long as this permanent doesn't have the 'left half unlocked' designation, it doesn't
    > have the name, mana cost, or rules text of this object's left half"
    >
    > (and the symmetric one for the right half)

    So a Room with only the right half unlocked has *only the right half's name, mana cost, and
    rules text*. The permanent's name is **not** `"X // Y"` while one half is locked — it's the
    unlocked half's name. Both halves' name/cost/text appear only when both are unlocked.
  - These suppression abilities and which half a characteristic belongs to are part of copiable
    values (709.5).
- **Unlocking a locked door** (709.5e): the controller may pay a locked half's **mana cost** to
  give the permanent that half's `"unlocked"` designation. This is called an "unlock cost" and is
  a **special action** (rule 116) — not an activated ability. It does not use the stack, can't be
  responded to between declaration and effect, and can't be countered. Timing: any time the
  controller has priority **and the stack is empty during a main phase of their turn**.
- **Door-unlock triggers** (709.5h): "When you unlock this door, …" fires whenever a particular
  half is given the `"unlocked"` designation, **regardless** of whether it was given the
  designation while entering the battlefield or after. So the cast-door's "when unlocked" trigger
  fires from the ETB unlock too.
- **Fully-unlock triggers** (709.5i): "When you fully unlock this Room" / Eerie's "Whenever you
  fully unlock a Room" fires when the permanent has one of the two unlocked designations and gets
  the other, **or when it has neither and gains both** simultaneously.
- **Locking** (709.5g): some effects say "lock" a half — removes the `"unlocked"` designation.
  Not used by Unholy Annex; out of scope for v1 but trivial follow-up given the same component.

Subtleties worth keeping on the radar (still v1-relevant, just not the happy path):

- 709.5b means a copy of a Room spell on the stack still has both halves' existence as copiable
  values. Engine clone/copy implementations need to preserve `RoomComponent` shape on copies.
- "May cast either half" interacts with cast-from-zone permissions (flashback, casting from
  graveyard, etc.). Each face has its own mana cost, so each face is its own castable spell.

## 3. State of the codebase

What already exists:

- `mtg-sdk/.../scripting/EventPattern.kt` defines `EventPattern.RoomFullyUnlockedEvent(player)` (SDK-side
  trigger event with a `Player` filter).
- `rules-engine/.../core/GameEvent.kt` defines `RoomFullyUnlockedEvent(roomId, roomName, controllerId)`
  (engine-side concrete event).
- `mtg-sdk/.../dsl/Triggers.kt` exposes `Triggers.RoomFullyUnlocked` (with `TriggerBinding.ANY`).
- `mtg-sdk/.../core/Keyword.kt` mentions Rooms in a comment for Eerie.

What does **not** exist (full list of gaps):

| Gap | Where |
|-----|-------|
| Split-card layout in `CardDefinition` | `mtg-sdk/.../model/CardDefinition.kt` is single-face only |
| Per-half `manaCost` / `typeLine` / `oracleText` / abilities | Same |
| `Room` subtype | `mtg-sdk/.../core/Subtype.kt` — verify; may already exist as a string subtype |
| `RoomComponent` (door state on battlefield) | Not present |
| `DoorUnlockedEvent` (per-door event) | Not present |
| Trigger event detection / emission for both door events | `TriggerDetector` does not call anything for Rooms |
| `UnlockRoomDoorAction` + handler | Not present |
| Legal-action enumeration for unlock costs | Not present |
| Cast-time "choose which half" decision | Not present |
| Filtered ability projection (only unlocked halves apply) | `StateProjector` does not gate on door state |
| Game-server DTOs for the new events/actions/state | Not present |
| Frontend split-card rendering and unlock-door buttons | Not present |

## 4. Design

### 4.1 SDK — split-card shape and Room metadata

Two options for representing split layouts on `CardDefinition`. The choice matters because it
affects every consumer of `CardDefinition` (engine, server, client).

**Option A — `cardFaces: List<CardFace>` on `CardDefinition`.** Single `CardDefinition` with a
list of faces; existing single-face cards have `cardFaces = [self]` or the fields are mirrored.
Closely matches Scryfall's data model and generalizes beyond Rooms (split, fuse, aftermath, MDFC,
adventure, prototype). Larger refactor.

**Option B — `RoomDefinition` (or `SplitDefinition`) as a sibling of `CardDefinition`.** Keeps the
single-face contract intact; adds a parallel data class. Smaller initial diff, but every system
that takes `CardDefinition` (registry, card builder, serialization, game state) gains a second
branch. Likely to grow worse as more split-shaped layouts appear.

**Recommendation:** Option A. We'll introduce `CardFace` carrying `(name, manaCost, typeLine,
oracleText, colors, abilities, spell?)` and have `CardDefinition` own one or more faces plus a
`layout: CardLayout` enum (`NORMAL`, `SPLIT`, `TRANSFORM`, `MODAL_DFC`, `ADVENTURE`, …). Existing
DFC support already smuggles a back face through `DoubleFacedComponent`; we can refactor that to
use `cardFaces` in a follow-up but don't have to in v1.

DSL surface for Room cards:

```kotlin
val UnholyAnnexRitualChamber = card("Unholy Annex // Ritual Chamber") {
    layout = CardLayout.SPLIT
    metadata { /* ...image, set, collector, rarity... */ }

    face("Unholy Annex") {
        manaCost = "{2}{B}"
        typeLine = "Enchantment — Room"
        oracleText = "At the beginning of your end step, draw a card. If you control a Demon, ..."

        triggeredAbility {
            trigger = Triggers.OnEndStep
            // Only active while this door (Unholy Annex) is unlocked — this is implicit
            // for face-scoped abilities; the engine projects them out when the door is locked.
            effect = /* ... */
        }
    }

    face("Ritual Chamber") {
        manaCost = "{3}{B}{B}"
        typeLine = "Enchantment — Room"
        oracleText = "When you unlock this door, create a 6/6 black Demon creature token..."

        triggeredAbility {
            trigger = Triggers.OnDoorUnlocked    // NEW — see §4.4
            effect = Effects.CreatePredefinedToken(/* 6/6 flying Demon */)
        }
    }
}
```

Key DSL decisions:

- Abilities defined inside a `face { }` block are **face-scoped** by default. The builder tags
  them so the engine knows to suppress them while that face's door is locked.
- A `Triggers.OnDoorUnlocked` constant with `TriggerBinding.SELF_FACE` means "fires when *this
  face's* door becomes unlocked," matching the printed reading "When you unlock this door". The
  engine resolves which face the trigger belongs to from the trigger's source.

### 4.2 Engine state — `RoomComponent`

```kotlin
@Serializable
data class RoomComponent(
    val faces: List<RoomFace>,
    val unlocked: Set<RoomFaceId>     // subset of face IDs in `faces`
) : Component {
    val isFullyUnlocked: Boolean get() = unlocked.size == faces.size
    fun isUnlocked(faceId: RoomFaceId): Boolean = faceId in unlocked
}

@Serializable data class RoomFace(val id: RoomFaceId, val name: String, val manaCost: ManaCost)
@Serializable @JvmInline value class RoomFaceId(val value: String)
```

Lives on the permanent entity alongside `CardComponent`. Stable across the card's lifetime on the
battlefield. Wiped on zone change like any other battlefield-only component.

### 4.3 Casting flow

Two integration points:

1. **Legal-action enumeration** (`LegalActionEnumerator`): a Room card in the appropriate zone
   produces *one cast action per face*, each with that face's mana cost and target requirements.
   Existing single-face logic becomes "iterate faces; for `NORMAL` layout there's one face."
2. **`CastSpellAction`** gains an optional `faceIndex: Int?`. `CastSpellHandler.validate` resolves
   the face, uses that face's mana cost and abilities, and on resolution emits an ETB with a
   freshly-built `RoomComponent` whose `unlocked` set contains the cast face.

### 4.4 Unlock action and trigger emission

New action and event types in the engine:

- `UnlockRoomDoorAction(room: EntityId, faceId: RoomFaceId)` — sorcery-speed special action.
- `DoorUnlockedEvent(room: EntityId, roomName: String, faceId: RoomFaceId, faceName: String,
  controllerId: EntityId, becameFullyUnlocked: Boolean)` — emitted on every transition from
  locked → unlocked (including the cast-time ETB unlock and reanimation-edge cases).
- `RoomFullyUnlockedEvent` (already exists) — emitted once when the second door unlocks, by the
  same handler that would emit the second `DoorUnlockedEvent`.

Handler shape (`UnlockRoomDoorHandler`), per **CR 709.5e** + **rule 116** (special actions):

1. Validate it's the controller's turn, they have priority, the stack is empty, and it's a main
   phase. Reuse existing sorcery-timing helpers, but tighten "active player" — the controller of
   the *Room*, not just any sorcery-speed actor, must be the one taking the action.
2. Validate `faceId` is currently locked (i.e. not in `RoomComponent.unlocked`).
3. Resolve and pay the face's mana cost via the existing cost pipeline. Note this is *only* the
   mana cost — additional costs from the locked half's text don't apply to the unlock action; the
   unlock cost is the printed mana cost (709.5e).
4. Update `RoomComponent.unlocked` immutably.
5. Emit `DoorUnlockedEvent`. If `unlocked` now contains every face id, also emit
   `RoomFullyUnlockedEvent`.
6. Run trigger detection over the new events.

Because special actions don't use the stack (rule 116), the unlock is *not* an `ActionType` that
goes through cast-spell-style stack frames. It mutates state directly and immediately runs
trigger detection — closer to how `TurnManager` handles morph face-up turns or `Crew` etc.
Players gain priority again after triggers resolve, before any further action.

Legal-action enumeration adds an `UnlockRoomDoor` enumerator that walks battlefield Room
permanents the active player controls, finds locked doors, and emits one action per locked door
whose cost the player can pay (mana availability check via `ManaSolver`).

`Triggers.OnDoorUnlocked` (new) and `Triggers.RoomFullyUnlocked` (existing) are detected in
`TriggerDetector.detectTriggers()` against the new events. Door-scoped triggers match only when
the source permanent matches the trigger source *and* the unlocked face id matches the face the
trigger was authored on (carried via the trigger binding metadata from §4.1).

### 4.5 Filtered ability projection

CR 709.5 models locked-half suppression as two intrinsic static abilities that strip the locked
half's **name, mana cost, and rules text**. We can implement that effect either declaratively (as
real `ContinuousEffect`s applied by `StateProjector`) or imperatively (a projection-time filter
keyed off `RoomComponent.unlocked`). The filter approach is simpler and one chokepoint covers all
the consumers:

- **Abilities** (static / triggered / activated): the projector drops abilities tagged to a
  locked face from the projected ability set. Triggered abilities never see events because
  `TriggerDetector` reads from projected abilities; activated abilities don't appear in legal
  actions; static abilities don't contribute to layers.
- **Name and mana cost**: the projected `CardComponent` returns only the unlocked halves' name
  and mana cost. While both halves are locked the permanent has no name and no mana cost — match
  the CR literally rather than papering over with `"X // Y"`.
- **Rules text** (oracle text shown to players): same — only the unlocked halves' text.
- **Type line**: always shared per 709.5a; not gated on lock state.

Care needed: cards that reference name (e.g. "named X") need to read the projected name. The
copiable-values exception in 709.5b means copy effects still capture the full two-half shape, so
`Effects.CopyPermanent` etc. must copy `RoomComponent` (the lock state, like counter state, isn't
copiable — only the structural face list is).

### 4.6 Server / DTO / masking

- Add `DoorUnlockedEvent` and `RoomFullyUnlockedEvent` to the `ClientEvent.kt` exhaustive `when`.
- `RoomComponent` needs a client-facing DTO; door state is public information (per CR Rooms are
  visible to all players), so no masking concerns.
- `UnlockRoomDoorAction` is a new client → server action — add to the action DTO sum type.

### 4.7 Frontend

- **Card rendering** in hand / stack / graveyard / library: split layout. The Scryfall image is
  already a horizontal split; reuse the existing card image as-is. No new image assets needed.
- **On the battlefield:** the Room is one card. Show two door indicators (lock/unlock icons) on
  the card frame. Unlocked doors visually highlighted; locked doors greyed.
- **Casting from hand:** clicking a Room in hand presents a "Cast Unholy Annex / Cast Ritual
  Chamber" mode picker (existing modal-spell decision UI may suffice, or a small dedicated
  `SplitCardCastUI`).
- **Unlocking on battlefield:** `legalActions` on a Room permanent includes one entry per
  unlockable door. The action menu shows "Unlock Ritual Chamber ({3}{B}{B})". Mana payment
  flows through the existing cost-payment UI.
- **Stack display:** stack item shows the cast-half name only.

### 4.8 Subtype

Verify `Subtype.ROOM` exists. If not, add to `mtg-sdk/.../core/Subtype.kt` and to the frontend
`Subtype` enum. The Eerie text already references "Room" so the string is in use; we just need
the typed enum.

## 5. Phased rollout

Phasing chosen to keep diffs reviewable and to land a working subset early.

1. **Phase 1 — SDK split-card shape.** Add `CardLayout`, `CardFace`, the `face { }` DSL block, and
   serialization. No engine wiring yet. Existing single-face cards untouched (default
   `layout = NORMAL`, single-face list). Add `Subtype.ROOM` if missing.
2. **Phase 2 — Cast-a-half.** Legal-action enumeration produces per-face cast actions;
   `CastSpellAction` carries a face index. A Room enters the battlefield with the cast door
   unlocked, the other locked. `RoomComponent` is populated, but no triggers fire yet and locked
   halves' abilities are not yet suppressed. Test: a Room can be cast and sits on the
   battlefield with the right door state.
3. **Phase 3 — Unlock action + trigger emission.** `UnlockRoomDoorHandler`,
   `Triggers.OnDoorUnlocked`, `RoomFullyUnlockedEvent` emission, ability projection filter for
   locked halves. Test: cast Unholy Annex, take 2 life loss on end step (no Demon yet); pay
   {3}{B}{B} to unlock Ritual Chamber, get the 6/6 flying Demon token; subsequent end step gives
   2-life-gain branch.
4. **Phase 4 — Server DTOs + frontend.** `ClientEvent` mapping, action DTOs, split-card card
   rendering, lock indicators on battlefield, unlock action menu. E2E test for the Unholy Annex
   flow.
5. **Phase 5 — Cleanup.** Implement Unholy Annex // Ritual Chamber as data, register in DSK set
   file (new set scaffolding), check off the magezero backlog item.

Phase 1+2 can land independently and are useful even without unlock — they unblock other
split-layout cards (Aftermath, Fuse) later.

## 6. Test plan

Per phase:

- **Phase 1:** SDK serialization round-trip for a two-face Room definition; DSL test that
  face-scoped abilities are tagged correctly.
- **Phase 2:** scenario test — cast each half, assert `RoomComponent` and ETB events. Reanimation
  (if Phase 2 extends to the both-locked case) — a Room reanimated via `Effects.MoveToZone`
  battlefield drop arrives with both doors locked.
- **Phase 3:** scenario tests for door-unlock trigger, fully-unlock trigger, and locked-ability
  suppression. One test exercises both halves; one exercises a card with face-scoped static
  abilities (placeholder card if no real one exists yet).
- **Phase 4:** Playwright E2E covering "cast Unholy Annex → end step trigger → unlock Ritual
  Chamber → demon token created → subsequent end step uses Demon-branch."
- **Phase 5:** verification against Scryfall for Unholy Annex // Ritual Chamber per the standard
  add-card Step 10 checklist.

## 7. Open questions

1. **Both-doors-locked behavior.** CR 709.5d defines this as the canonical case for non-cast
   battlefield drops (reanimation, Replenish, etc.). Phase 3 must support it, since legal-action
   enumeration over a both-locked Room produces *two* unlock candidates and `RoomComponent` must
   serialize that state correctly. No corner case to defer — just a consequence of doing the rule
   right.
2. **Adventure / split / aftermath generalization.** The `CardLayout` enum should accommodate
   these; the `face { }` DSL can grow `castableFromZone`, `castOnceThenExile`, etc. We don't have
   to build them now — just don't paint the design into a Rooms-only corner.
3. **Frontend card-image strategy for Rooms vs Aftermath.** Both are split layouts but read
   differently (horizontal vs vertical). The current Scryfall normal image is correct for Rooms.
   Aftermath would need a rotation or a different layout pass — out of scope here.
4. **Copy-effect handling.** 709.5b makes the *existence* of each half part of copiable values
   even on the stack. Existing copy-spell logic (`Effects.CopyPermanent`, `ChainCopyExecutor`)
   needs a small audit to confirm it carries `RoomComponent`'s `faces` list (not the dynamic
   `unlocked` set, which is like counters — not copiable).

## 8. Out of scope

- Other DSK Rooms (we only need Unholy Annex for the magezero backlog).
- Eerie (the mechanic that *consumes* `RoomFullyUnlockedEvent` and `Triggers.RoomFullyUnlocked`)
  — the trigger fires once Phase 3 lands, but no cards in the magezero coverage use Eerie.
- Generalizing single-face DFC code to use the new `cardFaces` model. Worthwhile follow-up,
  unrelated to magezero coverage.
- Aftermath, Fuse, Adventure layouts. Same shape, different rules — punt.
