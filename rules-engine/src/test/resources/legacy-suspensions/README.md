These synthetic fixtures were exported by executing the unchanged engine at
`fba4b704cb213843a1420809e0cf6aee656045c9`, before changing suspension storage.
They were not constructed by reversing the new representation.

Each directory contains the original paused `state.json`, the actual subsequent
`actions.json`, the resulting `after-N.json` states and `events-N.json` events,
the captured step's `creation-events.json`, and a verified `manifest.json`.
The exporter decoded the saved initial state before applying the actions and
asserted each scenario's final observable outcome. The original four exports passed with `encodeDefaults = true`.

The examples cover nested may decisions and an automatic outer tail, a repeat
loop awaiting its continue answer, an outstanding mana-payment question hidden
under a real Birds of Paradise color choice, and a free Naturalize awaiting
targets. All entities and gameplay state come from public-safe synthetic setups.

Migration tests preserve the saved counter exactly on load. Later transitions
may allocate fewer routing IDs because automatic work no longer allocates IDs;
tests explicitly rebind subsequent replies to the current question when needed.

`compact-cycling` was separately exported and reloaded at the same parent revision
with `encodeDefaults = false`. It cycles Secluded Steppe with Astral Slide and
Grizzly Bears on the battlefield. The paused stack contains an automatic cycling
draw whose default `decisionId = "cycle-draw"` is absent from the encoded JSON.
Declining the trigger resumes that draw; the parent exporter verifies one card in
hand, one remaining in the library, and a drained continuation stack. The migration
regression compares the complete resumed state and event list with this capture.

Re-captured on 2026-09-07 when this branch merged `main`'s object-identity feature
(`objectIdentities`, `nextObjectGeneration`, and `objectReferences` on continuations).
The recorded `state.json` and `actions.json` are unchanged; the expected `after-N.json`
were re-derived by replaying those same actions. `events-N.json` was rewritten only where
the new feature genuinely emits more: the LIBRARY->HAND draw in `compact-cycling`, and the
HAND->STACK cast plus the stack-origin on the graveyard move in `free-cast-target`. The
`nested-may`, `repeat-while` and `suspended-mana-window` event lists still match the
original parent capture byte for byte, which is what shows the suspension change itself
did not alter behaviour.

Edited on 2026-09-24 when a `ForEach` loop's current object moved from
`PipelineState.iterationTarget` into `ObjectReferenceEnvironment.iteration` (replacing
`selfBinding`). Every capture held both as `null`, so the obsolete `iterationTarget` keys were
dropped and `selfBinding` renamed in place; no other byte of `state.json`, `actions.json` or
`after-N.json` changed.

Representation-refreshed on 2026-09-25 when card target declarations stopped carrying
author-written names: the DSL now mints each target's binding id (`t0`, `t1`, … unique per card)
and the targeting prompt is derived from the requirement. The only edits were the recorded
binding name `"target"` → `"t0"` (requirement `id` and the `BoundVariable` reading it) and the
recorded prompt `"target"` → `"target artifact or enchantment"` (Naturalize's derived wording).
No action or event payload changed, and no gameplay was rerun.

Edited on 2026-09-28 when mana provenance gained a producing-source card-type axis
(`ManaPoolComponent.manaByCardType`, `SpellOnStackComponent.manaSpentByCardType`,
`RestrictedManaEntry.source`). The new keys were inserted with their defaults in the
`encodeDefaults = true` captures; the one tagged pool — Birds of Paradise's floating mana in
`suspended-mana-window/after-1.json` — carries `{"CREATURE": 1}`, as the engine now records.
