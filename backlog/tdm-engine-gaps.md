# Tarkir: Dragonstorm — Engine Gap Analysis

Cross-reference of the **252 remaining (unimplemented) TDM cards** against the engine's actual
capabilities (SDK reference + source verification, May 2026). Generated to scope what must be
built before the set can be completed.

**Status:** 19 / 271 implemented (7%). All 19 implemented are reprints (taplands, Evolving Wilds,
Snakeskin Veil) plus two simple spells (Awaken the Honored Dead, Strategic Betrayal). **No
new-mechanic card is implemented yet.** Card list comes from `scripts/card-status --list --set TDM`.
Oracle text pulled from Scryfall (`set:tdm`, 277 printings → 271 unique cards).

## Bottom line

The set is built around **five wedge clan keywords** plus an across-the-board Dragons-matter theme.
Two of the five clan mechanics and the DFC layout are **already supported**; the rest are the real
work. Once the Tier-1 keywords land, the large majority of remaining cards are buildable today
(standard creatures, dual lands, monuments, sieges, exhale/behold spells, planeswalkers).

### Already supported — no new engine work

- **Behold a Dragon** (Temur additional cost) — full handling in `CostHandler` + cast enumerators:
  reveal-from-hand / choose-controlled, optional (`you may behold`) and `behold or pay {1}` forms,
  and the `if a Dragon was beheld` downstream conditional. (Caustic/Dispelling/Molten/Osseous/
  Piercing Exhale, Eshki-adjacent cards, etc.)
- **Omen** double-faced cards (13 Dragon // Omen-spell DFCs) — supported since Bloomburrow; the Omen
  face shuffles its card into the library on resolution. (Bloomvine Regent, Marang River Regent,
  Scavenger Regent, the Stormbrood cycle, …)
- **Sieges** (5 cards) — `EntersWithChoice(ChoiceType.MODE)` gating two ability sets ("As this
  enters, choose [clan A] or [clan B]"). Proven by Outpost Siege. (Barrensteppe / Frostcliff /
  Glacierwood / Hollowmurk / Windcrag / Cori Mountain Monastery siege-style enchantments.)
- **Planeswalkers** — loyalty framework fully supported (many existing PWs incl. a Sarkhan).
  **Ugin, Eye of the Storms**, **Elspeth, Storm Slayer**, **Sarkhan, Dragon Ascendant** need only
  their specific abilities, not new framework.
- **Affinity** (`affinity for creatures`) and **Delve** — `CostCalculator` reductions. (Salt Road
  Packbeast; Teval grants Delve to your spells.)
- Building blocks that several gaps below compose: tapped-and-attacking tokens
  (`ZonePlacement.TappedAndAttacking`), "next end step" delayed triggers
  (`DelayedTriggerTiming.NextEndStep`), distribute-counters, copy-spell-on-stack, mass/dynamic P/T,
  divided damage, exile-until-leaves, impulse `MayPlayFromExile`, keyword counters (flying/first
  strike/lifelink/indestructible/stun/finality), `ExileFromGraveyard` ability cost.

What follows are the **genuine gaps** — elements no current SDK primitive expresses.

---

## Tier 1 — Headline clan keywords (~50 cards, highest leverage)

Four of the five wedge mechanics plus Harmonize are missing. These unlock the overwhelming
majority of the set.

1. **Mobilize N** (Mardu, 12 cards) — *Lowest risk; all building blocks exist.*
   "Whenever this creature attacks, create N tapped and attacking 1/1 red Warrior creature tokens.
   Sacrifice them at the beginning of the next end step." Compose attack trigger +
   `ZonePlacement.TappedAndAttacking` token + `DelayedTriggerTiming.NextEndStep` sacrifice. Needs a
   keyword + a token-shape helper; no new engine subsystem.
   → Zurgo's Vanguard, Voice of Victory, Marshal of the Lost, Shock Brigade, Stadium Headliner,
     War Effort, Sagu Pummeler, Zurgo Thunder's Decree, …

2. **Endure N** (Abzan, 10 cards) — modal player choice: **put N +1/+1 counters on it OR create an
   N/N white Spirit creature token.** Both halves exist (`AddCounters`, `CreateToken`); needs the
   keyword + the binary choice prompt (and the dynamic N path for "endure X").
   → Anafenza Unyielding Lineage, Descendant of Storms, Inspirited Vanguard, Krumar Initiate, …

3. **Flurry** (Jeskai, 10 cards) — ✅ **DONE.** "Whenever you cast your **second** spell each turn, …"
   The `GameEvent.NthSpellCastEvent` trigger already fired on the Nth cast of the turn (it powers the
   EOE/BLB second-spell cards), so no new trigger type was needed — the gap note above was stale. Added
   the display-only `Keyword.FLURRY` plus a `card { flurry { … } }` builder helper (mirrors
   `prowess()` / `rampage()`) that forces `Triggers.NthSpellCast(2, Player.You)` and prefixes the
   "Flurry — Whenever you cast your second spell each turn," reminder text.
   Implemented: Cori Mountain Stalwart, Devoted Duelist, Jeskai Devotee, Poised Practitioner,
   Wingblade Disciple, Wayspeaker Bodyguard, Equilibrium Adept.
   Still blocked elsewhere: **Taigam, Master Opportunist** (needs Suspend, Tier 3 §14),
   **Cori-Steel Cutter** + **A-Cori-Steel Cutter** (need "create a token, then attach this Equipment
   to it" from a trigger — deferred).

4. **Renew** (Sultai, 12 cards) — graveyard-activated ability:
   `{cost}, Exile this card from your graveyard: <effect>. Activate only as a sorcery.`
   The SDK **already models** a per-ability `activateFromZone` field and `AbilityCost.ExileFromGraveyard`,
   but `ActivatedAbilityEnumerator.kt:99` only enumerates abilities on **battlefield** permanents.
   **Needs the enumerator extended to surface graveyard-zone activated abilities** + sorcery-timing
   gate. Most architecturally involved of the clan mechanics.
   → Qarsi Revenant, Sage of the Fang, Kheru Goldkeeper, Naga Fleshcrafter, Rot-Curse Rakshasa,
     Lasyd Prowler, Hundred-Battle Veteran, …

5. **Harmonize** (10 cards) — alternative cast-from-graveyard cost, **may tap a creature to reduce
   that cost by its power (X)**, then exile the spell. Flashback + a Convoke-style power-based
   reduction. Needs a new alternative-cost path (graveyard cast + tap-for-power reduction + exile
   on resolution).
   → Channeled Dragonfire, Glacial Dragonhunt, Mammoth Bellow, Wail of War, …

6. **Decayed** (1 card) — "can't block; when it attacks, sacrifice it at end of combat." Minor
   keyword; not currently in the Keyword enum.
   → Rot-Curse Rakshasa (also a Renew card).

---

## Tier 2 — Small recurring primitives

7. **New keyword counter types.** The engine has flying / first-strike / lifelink / indestructible /
   stun / finality counters, but is **missing `deathtouch`, `trample`, `hexproof`, `decayed`**.
   Needs the `CounterType` constants + their keyword-counter mapping in `StateProjector`.
   → Champion of Dusan (trample), Qarsi Revenant (deathtouch), Kheru Goldkeeper (flying — exists),
     Perennation (hexproof), Rot-Curse Rakshasa (decayed)

8. **Leaves-graveyard trigger.** "Whenever one or more cards leave your graveyard during your turn."
   A graveyard-departure *condition/tracker* exists, but there is **no `CardsLeftGraveyard` trigger
   event** (only cards-entering and battlefield-only LTB). Needs a new trigger keyed to
   graveyard-exit batching + "once each turn / during your turn" gating.
   → Attuned Hunter, Kishla Skimmer, Kheru Goldkeeper

9. **Double existing +1/+1 counters (one-shot).** `DoubleCounterPlacement` is a *replacement* on
   future placement, not a one-shot doubling of counters already present.
   → Sage of the Fang

10. ~~**Double power and toughness until end of turn (group).**~~ ✅ **Done.** Modeled as a
    fixed +X/+Y layer-7 modification read per-entity from projected state
    (`DynamicAmount.EntityProperty(EntityReference.IterationEntity, …)`), surfaced as the
    reusable `EffectPatterns.doublePowerAndToughnessForAll(filter, duration)` helper.
    → Roar of Endless Song (and Unnatural Growth, refactored onto the same helper)

---

## Tier 3 — One-off complex cards (each needs unique new functionality)

11. **Copy a card in a zone, then cast the copy.** All copy primitives operate on stack objects
    (`CopyTargetSpellEffect`, `ChainCopyEffect`, `CopyNextSpellCast`) or make token copies of
    permanents — none copies a card *in the graveyard* into a castable spell copy.
    → **Shiko, Paragon of the Way**

12. **Cost-linked relative mana value target.** Sacrifice a creature of MV X → return a creature of
    MV **X+1** from graveyard. No predicate for "MV exactly the sacrificed cost-object's MV + 1".
    → **Sidisi, Regent of the Mire**

13. **Choose-and-exile from a target opponent's revealed hand.** Existing primitives reveal /
    look-at hands or force self-discard, but none lets the controller pick a card from the opponent's
    hand and exile it (with a linked LTB payoff).
    → **Severance Priest**

14. **Suspend.** Time counters on a card in exile + cast-when-last-counter-removed + haste payoff.
    Impending/Vanishing use time counters on the battlefield, not the Suspend exile-cast flow.
    → **Taigam, Master Opportunist** (Flurry copies a spell, exiles it with 4 time counters + Suspend)

15. **Event-based "when you next attack this turn" delayed trigger.** Current delayed triggers are
    step/turn-based only (`DelayedTriggerTiming`); there is no one-shot trigger keyed to the next
    attack-declaration event.
    → **All-Out Assault**

16. **Prevent-and-redirect-and-draw.** Prevent the next damage from a chosen source, then deal the
    *prevented amount* to that source's controller and draw that many cards. Prevention shields don't
    capture/forward the prevented amount.
    → **New Way Forward**

17. **Free-cast-from-exile gated by a dynamic MV cap.** Exile top X of opponent's library (X =
    combat damage) and cast any number of those with MV ≤ X for free. The free-cast-from-exile grants
    exist, but not a dynamic MV-≤-X cap on the playable set.
    → **Kotis, the Fangkeeper**

18. **Per-activation cost reduction by a target's color count.** Equip cost "costs {1} less to
    activate for each color of the creature it targets." Cost statics target spells; none reduces an
    activated (equip) cost by the chosen target's color count.
    → **Dragonfire Blade**

19. **Opponent-scoped continuous cast prohibition.** "Your opponents can't cast spells during your
    turn." Current cast restrictions are self-controller-scoped (`RestrictSpellsCastPerTurn`); needs a
    player-group prohibition gated on your turn.
    → **Voice of Victory**

20. **Sacrifice-a-token as a cost** (minor — verify a token-only sacrifice-cost filter exists).
    → **Hardened Tactician**

---

## Recommended build order

1. **Mobilize → Endure → Flurry** — every primitive for Mobilize already exists; Endure/Flurry are
   modest. Unlocks ~30 cards quickly.
2. **Renew** (enumerator extension) + **Harmonize** (new alt-cost) — bigger lifts, ~22 cards.
3. **Keyword counters + Decayed + leaves-graveyard trigger** (Tier 2) — small, scattered unlocks.
4. **Tier-3 one-offs** as the relevant legendaries / rares come up.

The clan keywords (Tier 1) cover the bulk of the remaining cards. Behold and Omen already being done
means Temur spells and the 13 DFC dragons are essentially ready once the shared supporting effects
are filled in.
