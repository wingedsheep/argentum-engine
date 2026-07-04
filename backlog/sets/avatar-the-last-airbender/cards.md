# Avatar: The Last Airbender (TLA) — Card Checklist

**Set Size:** 286 draft/booster cards (excluding basic lands beyond the set's own, tokens, and special variants)
**Release Date:** November 21, 2025
**Implemented:** 284 / 286
**Engine gap analysis:** [`tla-engine-gaps.md`](tla-engine-gaps.md)

> **Status (July 2026):** 284/286 implemented — **2 cards remain** in the checklist:
> **Firebender Ascension** and **Koh, the Face Stealer**. All four **"bending" keyword families** are
> built (Earthbend incl. dynamic X, Waterbend as activated/spell/Exhaust cost incl. waterbend {X},
> Firebending, and Airbend — permanent form plus the *airbend-a-spell* stack branch), along with
> **Exhaust**, the **four-bend event system** (`Triggers.YouBend` + `TurnTracker.DISTINCT_BENDS`, which
> with `CostModification.ReduceColoredPerUnit` completed **Avatar Aang**), the **Vigilance/double-strike
> keyword counters**, the **Nth-card-drawn** and **Surveil** triggers, **Sagas**, and the recent
> reconciliation batch (Avatar's Wrath, The Legend of Yangchen, The Rise of Sozin, Bumi Unleashed, …).
>
> The remaining 3 are each blocked by a *distinct* engine gap (see [`tla-engine-gaps.md`](tla-engine-gaps.md)),
> not by a bending keyword. Broader still-open primitives that no longer block a *specific* TLA card but
> are noted there: **granting/conditional Firebending**, remaining **Waterbend cost shapes**
> (Ward—Waterbend, in-resolution may-pay, waterbend-as-alternative-cast), **Foretell**, and the **Fire
> counter** type.

## Mechanics needed to complete the set

The set is built around four **"bending" keyword families** plus a returning **Exhaust** keyword.
Counts below are over the 286 draft cards — *total* with the mechanic / *remaining* still
unimplemented. Remaining counts overlap: a still-missing card may be blocked by a *different*
mechanic than the row it appears under (e.g. a Firebending creature that also uses Airbend is held
up by Airbend, not Firebending).

### Signature / set-defining mechanics

| Mechanic | Total | Remaining | Notes |
|----------|------:|----------:|-------|
| Earthbend | 28 | 0 | Target land you control becomes a 0/0 haste creature-land; put N +1/+1 counters on it. ✅ built (`Effects.Earthbend`, incl. dynamic X). All implemented. |
| Waterbend | 25 | 0 | Convoke+improvise-style alt cost (tap artifacts/creatures to help pay). ✅ **activated-ability** cost (`hasWaterbend = true`), **spell-level additional cost** (incl. **waterbend {X}**), **Exhaust—Waterbend**, and the **four-bend "whenever you waterbend" trigger** (CR 701.67c, emitted at cost payment) all built. **Secret of Bloodbending** (the last waterbend card) is now implemented — its *take-control-during-your-opponent's-turn* payoff is the new combat-phase-scoped hijack (`Effects.HijackNextCombatPhase`, waterbend upgrades it to a whole turn). |
| Firebending | 28 | 1 | Attack-triggered combat-duration red mana. ✅ built — `firebending(n)` keyword + dynamic versions + the "whenever you firebend" trigger. ❌ the 1 remaining firebending card, **Firebender Ascension**, is blocked by its *copy-an-attacker's-triggered-ability quest* mechanic (the token's firebending itself is supported). Broader still-open: **granting/conditional** firebending. |
| Airbend | 11 | 0 | Exile target permanent; owner may recast it for {2}. ✅ built — `Effects.Airbend` / `Effects.AirbendAll` + the spell stack branch (`Effects.AirbendSpell` + `Conditions.TargetIsSpellOnStack` — *exile* from the stack, not a counter). Both the permanent and spell branches fire the "whenever you airbend" trigger once ≥1 object is exiled (CR 701.65b). All 11 implemented (Avatar's Wrath and Yangchen since resolved). |
| Exhaust | 8 | 0 | Activated ability usable only once (per object, CR 702.177). ✅ built — `isExhaust = true` on `activatedAbility` desugars to `ActivationRestriction.Once` (the existing per-object tracker is rules-correct; **not** once-per-game) and renders the "Exhaust — " prefix. **All 8 implemented**: Hog-Monkey, Rough Rhino Cavalry, Rebellious Captives, Bitter Work, plus Jeong Jeong (copy-next-Lesson rider), Invasion Submersible (Exhaust—Waterbend → becomes-artifact-creature via `AddCardType`), The Legend of Kuruk (Saga DFC + Exhaust—Waterbend {20} extra turn), and Mai (new **double strike** keyword counter). |

### Other keywords present (evergreen + returning)

| Keyword | Total | Remaining |
|---------|------:|----------:|
| Flying | 26 | 9 |
| Vigilance | 15 | 2 |
| Scry | 10 | 2 |
| Flash | 9 | 5 |
| Transform | 8 | 5 |
| Mill | 8 | 1 |
| Reach | 8 | 1 |
| Prowess | 7 | 1 |
| Menace | 6 | 2 |
| Equip | 5 | 1 |
| Food | 5 | 1 |
| Enchant | 5 | 1 |
| Landcycling | 5 | 0 |
| Typecycling | 5 | 0 |
| Cycling | 5 | 0 |
| Crew | 5 | 1 |
| Trample | 5 | 3 |
| Kicker | 4 | 0 |
| Defender | 4 | 0 |
| Ward | 3 | 1 |
| Deathtouch | 3 | 0 |
| Raid | 3 | 0 |
| Flashback | 3 | 0 |
| Haste | 3 | 1 |
| First strike | 2 | 0 |
| Landfall | 2 | 0 |
| Lifelink | 2 | 0 |
| Fight | 2 | 1 |
| Plainscycling | 1 | 0 |
| Islandcycling | 1 | 0 |
| Swampcycling | 1 | 0 |
| Surveil | 1 | 0 |
| Mountaincycling | 1 | 0 |
| Foretell | 1 | 1 |
| Affinity | 1 | 0 |
| Forestcycling | 1 | 0 |

**Sagas:** 7 total / 3 remaining (chapter abilities + final-chapter transform — ✅ engine-supported).

---

## Card checklist

- [x] Aang's Iceberg
- [x] Aang's Journey
- [x] Aang, Swift Savior
- [x] Aang, at the Crossroads
- [x] Aang, the Last Airbender
- [x] Abandon Attachments
- [x] Abandoned Air Temple
- [x] Accumulate Wisdom
- [x] Agna Qel'a
- [x] Air Nomad Legacy
- [x] Airbender Ascension
- [x] Airbender's Reversal
- [x] Airbending Lesson
- [x] Airship Engine Room
- [x] Allies at Last
- [x] Appa, Loyal Sky Bison
- [x] Appa, Steadfast Guardian
- [x] Avatar Aang
- [x] Avatar Destiny
- [x] Avatar Enthusiasts
- [x] Avatar's Wrath
- [x] Azula Always Lies
- [x] Azula, Cunning Usurper
- [x] Azula, On the Hunt
- [x] Ba Sing Se
- [x] Badgermole
- [x] Badgermole Cub
- [x] Barrels of Blasting Jelly
- [x] Beetle-Headed Merchants
- [x] Beifong's Bounty Hunters
- [x] Bender's Waterskin
- [x] Benevolent River Spirit
- [x] Bitter Work
- [x] Boar-q-pine
- [x] Boiling Rock Prison
- [x] Boiling Rock Rioter
- [x] Boomerang Basics
- [x] Bumi Bash
- [x] Bumi, King of Three Trials
- [x] Bumi, Unleashed
- [x] Buzzard-Wasp Colony
- [x] Callous Inspector
- [x] Canyon Crawler
- [x] Cat-Gator
- [x] Cat-Owl
- [x] Combustion Man
- [x] Combustion Technique
- [x] Compassionate Healer
- [x] Corrupt Court Official
- [x] Crashing Wave
- [x] Crescent Island Temple
- [x] Cruel Administrator
- [x] Cunning Maneuver
- [x] Curious Farm Animals
- [x] Cycle of Renewal
- [x] Dai Li Agents
- [x] Dai Li Indoctrination
- [x] Day of Black Sun
- [x] Deadly Precision
- [x] Deserter's Disciple
- [x] Destined Confrontation
- [x] Diligent Zookeeper
- [x] Dragonfly Swarm
- [x] Earth King's Lieutenant
- [x] Earth Kingdom General
- [x] Earth Kingdom Jailer
- [x] Earth Kingdom Protectors
- [x] Earth Kingdom Soldier
- [x] Earth Rumble
- [x] Earth Rumble Wrestlers
- [x] Earth Village Ruffians
- [x] Earthbender Ascension
- [x] Earthbending Lesson
- [x] Earthen Ally
- [x] Elemental Teachings
- [x] Ember Island Production
- [x] Energybending
- [x] Enter the Avatar State
- [x] Epic Downfall
- [x] Fancy Footwork
- [x] Fatal Fissure
- [x] Fated Firepower
- [x] Fire Lord Azula
- [x] Fire Lord Zuko
- [x] Fire Nation Attacks
- [x] Fire Nation Cadets
- [x] Fire Nation Engineer
- [x] Fire Nation Palace
- [x] Fire Nation Raider
- [x] Fire Nation Warship
- [x] Fire Navy Trebuchet
- [x] Fire Sages
- [ ] Firebender Ascension
- [x] Firebending Lesson
- [x] Firebending Student
- [x] First-Time Flyer
- [x] Flexible Waterbender
- [x] Flopsie, Bumi's Buddy
- [x] Foggy Bottom Swamp
- [x] Foggy Swamp Hunters
- [x] Foggy Swamp Spirit Keeper
- [x] Foggy Swamp Vinebender
- [x] Foggy Swamp Visions
- [x] Forecasting Fortune Teller
- [x] Forest
- [x] Gather the White Lotus
- [x] Geyser Leaper
- [x] Giant Koi
- [x] Glider Kids
- [x] Glider Staff
- [x] Gran-Gran
- [x] Great Divide Guide
- [x] Guru Pathik
- [x] Hakoda, Selfless Commander
- [x] Hama, the Bloodbender
- [x] Haru, Hidden Talent
- [x] Heartless Act
- [x] Hei Bai, Spirit of Balance
- [x] Hermitic Herbalist
- [x] Hog-Monkey
- [x] Honest Work
- [x] How to Start a Riot
- [x] Iguana Parrot
- [x] Invasion Reinforcements
- [x] Invasion Submersible
- [x] Invasion Tactics
- [x] Iroh's Demonstration
- [x] Iroh, Grand Lotus
- [x] Iroh, Tea Master
- [x] Island
- [x] It'll Quench Ya!
- [x] Jasmine Dragon Tea Shop
- [x] Jeong Jeong's Deserters
- [x] Jeong Jeong, the Deserter
- [x] Jet's Brainwashing
- [x] Jet, Freedom Fighter
- [x] Joo Dee, One of Many
- [x] June, Bounty Hunter
- [x] Katara, Bending Prodigy
- [x] Katara, Water Tribe's Hope
- [x] Katara, the Fearless
- [x] Knowledge Seeker
- [ ] Koh, the Face Stealer
- [x] Kyoshi Battle Fan
- [x] Kyoshi Island Plaza
- [x] Kyoshi Village
- [x] Kyoshi Warriors
- [x] Leaves from the Vine
- [x] Lightning Strike
- [x] Lo and Li, Twin Tutors
- [x] Long Feng, Grand Secretariat
- [x] Lost Days
- [x] Mai, Jaded Edge
- [x] Mai, Scornful Striker
- [x] Master Pakku
- [x] Master Piandao
- [x] Meditation Pools
- [x] Merchant of Many Hats
- [x] Messenger Hawk
- [x] Meteor Sword
- [x] Misty Palms Oasis
- [x] Momo, Friendly Flier
- [x] Momo, Playful Pet
- [x] Mongoose Lizard
- [x] Mountain
- [x] North Pole Gates
- [x] North Pole Patrol
- [x] Northern Air Temple
- [x] Obsessive Pursuit
- [x] Octopus Form
- [x] Omashu City
- [x] Origin of Metalbending
- [x] Ostrich-Horse
- [x] Otter-Penguin
- [x] Ozai's Cruelty
- [x] Ozai, the Phoenix King
- [x] Path to Redemption
- [x] Phoenix Fleet Airship
- [x] Pillar Launch
- [x] Pirate Peddlers
- [x] Plains
- [x] Planetarium of Wan Shi Tong
- [x] Platypus-Bear
- [x] Pretending Poxbearers
- [x] Price of Freedom
- [x] Professor Zei, Anthropologist
- [x] Rabaroo Troop
- [x] Ran and Shaw
- [x] Raucous Audience
- [x] Raven Eagle
- [x] Razor Rings
- [x] Realm of Koh
- [x] Rebellious Captives
- [x] Redirect Lightning
- [x] Rockalanche
- [x] Rocky Rebuke
- [x] Rough Rhino Cavalry
- [x] Rowdy Snowballers
- [x] Ruinous Waterbending
- [x] Rumble Arena
- [x] Saber-Tooth Moose-Lion
- [x] Sandbender Scavengers
- [x] Sandbenders' Storm
- [x] Secret Tunnel
- [x] Secret of Bloodbending
- [x] Seismic Sense
- [x] Serpent of the Pass
- [x] Serpent's Pass
- [x] Shared Roots
- [x] Sokka's Haiku
- [x] Sokka, Bold Boomeranger
- [x] Sokka, Lateral Strategist
- [x] Sokka, Tenacious Tactician
- [x] Sold Out
- [x] Solstice Revelations
- [x] South Pole Voyager
- [x] Southern Air Temple
- [x] Sozin's Comet
- [x] Sparring Dummy
- [x] Spirit Water Revival
- [x] Suki, Courageous Rescuer
- [x] Suki, Kyoshi Warrior
- [x] Sun Warriors
- [x] Sun-Blessed Peak
- [x] Swamp
- [x] Swampsnare Trap
- [x] Team Avatar
- [x] Teo, Spirited Glider
- [x] The Boulder, Ready to Rumble
- [x] The Cave of Two Lovers
- [x] The Earth King
- [x] The Fire Nation Drill
- [x] The Last Agni Kai
- [x] The Legend of Kuruk
- [x] The Legend of Kyoshi
- [x] The Legend of Roku
- [x] The Legend of Yangchen
- [x] The Lion-Turtle
- [x] The Mechanist, Aerial Artisan
- [x] The Rise of Sozin
- [x] The Spirit Oasis
- [x] The Unagi of Kyoshi Island
- [x] The Walls of Ba Sing Se
- [x] Tiger-Dillo
- [x] Tiger-Seal
- [x] Tolls of War
- [x] Toph, Hardheaded Teacher
- [x] Toph, the Blind Bandit
- [x] Toph, the First Metalbender
- [x] Treetop Freedom Fighters
- [x] True Ancestry
- [x] Trusty Boomerang
- [x] Tundra Tank
- [x] Turtle-Duck
- [x] Twin Blades
- [x] Ty Lee, Artful Acrobat
- [x] Ty Lee, Chi Blocker
- [x] Uncle Iroh
- [x] United Front
- [x] Unlucky Cabbage Merchant
- [x] Vengeful Villagers
- [x] Vindictive Warden
- [x] Walltop Sentries
- [x] Wan Shi Tong, Librarian
- [x] Wandering Musicians
- [x] War Balloon
- [x] Wartime Protestors
- [x] Water Tribe Captain
- [x] Water Tribe Rallier
- [x] Waterbender Ascension
- [x] Waterbending Lesson
- [x] Waterbending Scroll
- [x] Watery Grasp
- [x] White Lotus Hideout
- [x] White Lotus Reinforcements
- [x] White Lotus Tile
- [x] Wolfbat
- [x] Yip Yip!
- [x] Yue, the Moon Spirit
- [x] Yuyan Archers
- [x] Zhao, Ruthless Admiral
- [x] Zhao, the Moon Slayer
- [x] Zuko's Conviction
- [x] Zuko's Exile
- [x] Zuko, Conflicted
- [x] Zuko, Exiled Prince
