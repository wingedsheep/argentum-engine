These public-safe synthetic gameplay traces were executed and verified by the
unchanged parent engine at `fba4b704cb213843a1420809e0cf6aee656045c9`.
They cover nested may decisions with an automatic outer tail, repeat-while,
a mana-payment suspension nested under a Birds of Paradise color choice,
free Naturalize target selection, and Secluded Steppe cycling with Astral Slide.

The saved initial and after-action states here are frozen current-format
representations of those captures. The reader at
`6f222eb05a1d9e61f540217b2b372960ddf051c7` converted only suspension storage,
then serialized with `encodeDefaults = true`. Conversion did not rerun gameplay.
Each manifest retains the original `sourceRevision`, witness, and verification,
and separately records `representationReaderRevision` and encoding configuration.
The original actions and events are unchanged. Original wire-format captures and
the general migration reader belong to the companion compatibility change.

`SuspensionTraceTest` loads these files using only the current-format reader and
replays the recorded choices. It compares every resulting state's gameplay data,
normalizing only the routing counter and owned question IDs: automatic work no
longer spends question IDs, so later responses are rebound to the current question
without changing player or choice payload. Cycling also compares the exact event
list, final hand/library sizes, and drained stack. Current-format roundtrips are
checked throughout. This preserves before-and-after behavioral evidence whether
or not runtime support for old snapshots is deployed.

Re-captured on 2026-09-07 when this branch merged `main`'s object-identity feature
(`objectIdentities`, `nextObjectGeneration`, and `objectReferences` on continuations).
The recorded `state.json` and `actions.json` are unchanged; the expected `after-N.json`
were re-derived by replaying those same actions. `events-N.json` was rewritten only where
the new feature genuinely emits more: the LIBRARY->HAND draw in `compact-cycling`, and the
HAND->STACK cast plus the stack-origin on the graveyard move in `free-cast-target`. The
`nested-may`, `repeat-while` and `suspended-mana-window` event lists still match the
original parent capture byte for byte, which is what shows the suspension change itself
did not alter behaviour.

Schema-refreshed on 2026-09-08 when a `triggerClashWon` slot (CR 701.30d, the "if you won"
rider on a "Whenever you clash" trigger) joined the triggered-ability stack component, the
effect continuation, and `EffectContext`. Because these files are encoded with
`encodeDefaults = true`, a new field appears in every object that carries it; the only edit
was adding `"triggerClashWon": null` — the field's default — alongside each existing
`triggerScryCount`. No state, action, or event payload changed, and no gameplay was rerun.

Schema-refreshed for immediate zone returns: current state captures now include the empty
`zoneReturns` list alongside `departedLinkedExile`. Actions and events are unchanged; none
of these suspension traces has an outstanding zone-return effect.

Re-encoded on 2026-09-23 without defaults. Every `state.json` and `after-N.json` was decoded
with the current reader and written back with `encodeDefaults = false` (and
`SuspensionTraceTest` now encodes the same way), so a field added with a default no longer
appears in these files and no longer requires a schema refresh like the two above. In the same
change the per-fact trigger fields (`triggerScryCount`, `triggerClashWon`, `lastKnownPower`, …)
on `EffectContext`, the triggered-ability stack object and its target-selection frames moved
into one nested `triggerContext` record; every one of them was null in these traces, so they
simply dropped out. No gameplay was rerun, and `actions.json` / `events-N.json` are unchanged.
The replay in `SuspensionTraceTest` is the check. Each manifest records this as
`representationEncodeDefaults: false` and `representationReencodedOn`.

Representation-refreshed on 2026-09-25 when card target declarations stopped carrying
author-written names: the DSL now mints each target's binding id (`t0`, `t1`, … unique per card)
and the targeting prompt is derived from the requirement. The only edits were the recorded
binding name `"target"` → `"t0"` (requirement `id` and the `BoundVariable` reading it) and the
recorded prompt `"target"` → `"target artifact or enchantment"` (Naturalize's derived wording).
No action or event payload changed, and no gameplay was rerun.

Edited on 2026-10-08 when the battlefield-exit `EntitySnapshot` gained `copiableCard` (the
departed permanent's copiable values, read by a token copy of a triggering creature that has
left — CR 608.2h). The one snapshot in these traces — Naturalize's Sol Ring in
`free-cast-target/after-3.json` — now carries Sol Ring's `CardComponent` there, exactly the
component the card keeps in the graveyard (it was no copy). No action or event payload changed,
and no gameplay was rerun.
