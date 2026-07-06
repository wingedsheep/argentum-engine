# EOE bug list

**Status: closed** — every playtest bug reported for Edge of Eternities is resolved. All entries
below are either fixed (with a covering scenario test) or verified not-a-bug.

## Not bugs
- Chrome Companion: should keep the bottom of the library visible.
- Intrepid Tenderfoot, stacks pumps, but it's sorcery.

## Fixed
- **Rust Harvester** — "{2}, {T}, Exile an artifact card from your graveyard: …" picked the exiled
  card automatically instead of letting the controller choose. Now the exile cost prompts for a
  choice from the graveyard. Covered by `RustHarvesterExileCostChoiceTest`.
- **Auxiliary Boosters** — the ETB "create a 2/2 Robot token and attach this Equipment to it" created
  the token but never attached. The token id lands in `pipeline.storedCollections[CREATED_TOKENS]`
  rather than `context.targets[0]`, so the follow-up attach targeted nothing; the chain now attaches
  to the created token. Covered by `AuxiliaryBoostersAttachOnEtbTest`.
- **Larval Scoutlander** — ETB "sacrifice a land or Lander, then search for up to two basics, put
  them onto the battlefield tapped, then shuffle" fired the sacrifice prompt but the search payoff
  never delivered (no lands entered, library not shuffled). Fixed so the full pipeline resolves.
  Covered by `LarvalScoutlanderScenarioTest`.
- **Artifact Equipment attacks, gives separate damage** (Atomic Microsizer 3/3 attached to Chrome
  Companion — Tezzeret, Cruel Captain). Equipment that becomes a creature now unattaches
  (CR 301.5c / 704.5n), so it no longer both buffs its host AND attacks. Covered by
  `EquipmentAsCreatureUnattachTest` (combat case).
- **Wedgelight Rammer** did not turn to creature.
- **Spacecraft can attack 1 turn after being played.**
- **Glacier Godmaw** — Landfall not working.
