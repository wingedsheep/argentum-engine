# Antiquities (ATQ) — Mechanics Backlog

Antiquities (March 1994) is the first **"artifact-matters"** set: the Brothers' War between
Urza and Mishra. It has no named keyword mechanics in the modern sense, but it introduces a
dense web of artifact-centric sub-mechanics that recur throughout the set. This document
catalogues every mechanic the set leans on, what it means, and which cards use it. The
card-by-card *implementation* triage (composes-from-primitives vs. needs-engine-work) lives in
[`TODO.md`](TODO.md).

Data source: [`atq_set.json`](atq_set.json) (full Scryfall dump, 100 prints / 85 unique
cards). Rules references: verify against `MagicCompRules_20260417.pdf` / `.txt` in the repo
root — never a web source.

---

## 1. Artifact hate / artifact removal
The set's defining axis: nearly every color gets tools to punish or destroy artifacts.
- **Destroy artifact(s):** Crumble, Detonate, Shatterstorm, Gate to Phyrexia, Golgothian Sylex.
- **Counter artifact spells:** Artifact Blast, Goblin Artisans.
- **Bounce all artifacts:** Hurkyl's Recall.
- **Tax / sacrifice artifacts:** Energy Flux ("sacrifice unless you pay {2}").
- **Anti-artifact combat/damage:** Argothian Pixies, Argothian Treefolk, Artifact Ward,
  Martyrs of Korlis, Circle of Protection: Artifacts, Reverse Polarity.

## 2. "Tapping / activating an artifact" punisher triggers
A recurring template: a trigger that fires *"whenever an artifact becomes tapped, or a player
activates an artifact's ability without {T} in its cost."*
- Haunting Wind (1 dmg to controller), Powerleech (you gain 1 life — opponents' artifacts
  only), Artifact Possession (2 dmg — single enchanted artifact).

## 3. Cast-an-artifact-spell triggers
- Citanul Druid (grow on opponent's artifact spell), Urza's Chalice (gain 1 life on any
  artifact spell), Goblin Artisans (counter your own artifact spell on a lost coin flip).

## 4. Sacrifice-an-artifact as a cost / fuel
The "Atog archetype." An activated ability whose cost includes sacrificing an artifact:
- Atog, Orcish Mechanics, Dwarven Weaponsmith, Sage of Lat-Nam (done), Priest of Yawgmoth,
  Yawgmoth Demon (upkeep "sac or take damage"), Transmute Artifact (sac to fetch).

## 5. Sacrifice-a-creature as a cost / fuel
- Ashnod's Altar (sac creature → {C}{C}), Gate to Phyrexia (sac creature → destroy artifact).

## 6. "Artifact dies" triggers
*"Whenever an artifact you control is put into a graveyard from the battlefield…"*
- Tablet of Epityr (pay {1} → gain 1), Urza's Miter (pay {3} → draw, if it wasn't sacrificed).

## 7. Damage prevention & redirection
A very prevention-heavy set.
- **Prevent next N damage:** Argivian Blacksmith, Amulet of Kroog, Rakalite.
- **Prevent all damage from artifact sources:** Argothian Pixies, Argothian Treefolk,
  Artifact Ward.
- **Circle-of-Protection style (prevent next from a chosen source):** Circle of Protection:
  Artifacts.
- **Redirect / replace damage:** Martyrs of Korlis ("damage by artifacts is dealt to this
  creature instead").
- **Life gain off damage taken:** Reverse Polarity (gain 2× artifact damage taken this turn).

## 8. Banding (and granting banding)
Antiquities is a banding-heavy set.
- Static banding: Mishra's War Machine.
- Gains banding (begin-combat): Battering Ram.
- Gains a *chosen* keyword incl. banding: Urza's Avenger.

## 9. Coin flips
- Goblin Artisans (win → draw, lose → counter your own artifact spell).

## 10. Animate artifact / "becomes a creature"
Turning noncreature permanents into artifact creatures with P/T tied to mana value.
- Xenic Poltergeist (target artifact, until your next upkeep), Titania's Song (all noncreature
  artifacts, continuously), Ashnod's Transmogrant (creature also becomes an artifact),
  Mishra's Factory (land → 2/2 Assembly-Worker).

## 11. Modal / chosen P/T as it enters
- Primal Clay (choose 3/3, 2/2 flyer, or 1/6 defender Wall on ETB), Shapeshifter (choose a
  number 0–7 on ETB and each upkeep; P/T derived from it).

## 12. \*/\* dynamic power & toughness
- Gaea's Avenger (1 + artifacts opponents control), Primal Clay / Shapeshifter (from choice),
  animated artifacts (= mana value).

## 13. +1/+0 and +1/+1 counters as a built-in resource
- Clockwork Avian (enters w/ four +1/+0; sheds one per combat; refill in upkeep, cap 4),
  Tetravus (+1/+1 counters ↔ Tetravite tokens), Triskelion (remove +1/+1 → 1 damage),
  Dwarven Weaponsmith / Ashnod's Transmogrant (place counters), Citanul Druid (grows).

## 14. Token generation
- Tetravus (1/1 flying Tetravite artifact tokens, with reabsorb).

## 15. "Doesn't untap" / "you may choose not to untap"
A signature Antiquities clause, in two flavours:
- **Self, optional:** Phyrexian Gremlins, Ashnod's Battle Gear, Tawnos's Coffin,
  Tawnos's Weaponry ("you may choose not to untap this…").
- **Self, mandatory + pay to untap:** Colossus of Sardia ("doesn't untap; {9} in upkeep").
- **Imposed on others:** Phyrexian Gremlins (tap target artifact, stays tapped while this is
  tapped), Damping Field ("can't untap more than one artifact per untap step").

## 16. "Tap-locked" buffs (effect lasts while the source stays tapped)
- Ashnod's Battle Gear (+2/-2), Tawnos's Weaponry (+1/+1) — bonus persists *for as long as
  this artifact remains tapped*, tied to mechanic #15.

## 17. Mana production specials
- Mana = sacrificed artifact's mana value (Priest of Yawgmoth).
- Death → mana (Su-Chi: {C}{C}{C}{C} on death).
- Restricted mana (Mishra's Workshop: only for artifact spells).
- Location/assembly mana (Urza's Mine / Power Plant / Tower "tron": more mana if you control
  the other two).
- Untap helpers (Candelabra of Tawnos: untap X lands).

## 18. Activated-ability cost reduction
- Power Artifact (enchanted artifact's activated abilities cost {2} less, floor of 1 mana).

## 19. Maximum-hand-size & "hand-size matters"
- Cursed Rack (set chosen player's max hand size to 4), Ivory Tower (gain life for cards over
  4), The Rack (damage = 3 − cards in chosen player's hand), Mishra's War Machine /
  Coral Helm / Jalum Tome (discard as cost/effect).

## 20. "Choose an opponent as it enters" stored on the permanent
- The Rack, Cursed Rack — the chosen player is referenced by later abilities.

## 21. Accruing-counter damage engine
- Armageddon Clock (doom counters accrue each upkeep, deal damage to all in draw step,
  any player may pay {4} to remove one).

## 22. Set-membership ("originally printed in Antiquities")
- Golgothian Sylex (sacrifice every nontoken permanent originally printed in this expansion),
  echoing Arabian Nights' City in a Bottle.

## 23. Exile-and-return with state preservation
- Tawnos's Coffin (blink a creature, remembering its counters and re-attaching its Auras),
  Obelisk of Undoing (bounce a permanent you own and control), Feldon's Cane (shuffle
  graveyard into library), Drafna's Restoration / Reconstruction / Argivian Archaeologist
  (recur artifact cards).

## 24. Ante (banned / house-ruled)
- Bronze Tablet (swaps ownership of permanents via the ante zone). The engine has no ante
  support — postpone, consistent with the project's ARN ante stance.

## 25. Library manipulation
- Millstone (mill), Feldon's Cane (graveyard → library), Drafna's Restoration (graveyard →
  top of library), Transmute Artifact (search + cheat into play).
