# Avatar: The Last Airbender (TLA) — Card Checklist

**Set Size:** 286 draft/booster cards (excluding basic lands beyond the set's own, tokens, and special variants)
**Release Date:** November 21, 2025
**Implemented:** 232 / 286
**Engine gap analysis:** [`tla-engine-gaps.md`](tla-engine-gaps.md)

> **Status (June 2026):** 232/286 implemented. Every card buildable on the *current* engine has been
> added — the 54 remaining all need new engine/SDK work first (see the gap analysis). Since the
> original gap doc was written, **Firebending**, the **Vigilance keyword counter**, the
> **Nth-card-drawn** and **Surveil** triggers, **`PERMANENTS_SACRIFICED`**, **dynamic Earthbend**,
> and the **spell-level Waterbend additional cost** (including **waterbend {X}**) all landed, which
> unlocked the bulk of the set. The headline holdouts are now **Airbend**, **Exhaust**, **Foretell**,
> the **Fire counter** type, the remaining **Waterbend cost shapes** (Ward—Waterbend, Exhaust—Waterbend,
> waterbend-as-alternative-cast), **granting/conditional Firebending**, and a handful of Tier-3 one-offs.

## Mechanics needed to complete the set

The set is built around four **"bending" keyword families** plus a returning **Exhaust** keyword.
Counts below are over the 286 draft cards — *total* with the mechanic / *remaining* still
unimplemented. Remaining counts overlap: a still-missing card may be blocked by a *different*
mechanic than the row it appears under (e.g. a Firebending creature that also uses Airbend is held
up by Airbend, not Firebending).

### Signature / set-defining mechanics

| Mechanic | Total | Remaining | Notes |
|----------|------:|----------:|-------|
| Earthbend | 28 | 6 | Target land you control becomes a 0/0 haste creature-land; put N +1/+1 counters on it. ✅ built (`Effects.Earthbend`, incl. dynamic X). |
| Waterbend | 25 | 10 | Convoke+improvise-style alt cost (tap artifacts/creatures to help pay). ✅ **activated-ability** cost (`hasWaterbend = true`) and **spell-level additional cost** (incl. **waterbend {X}**) both built. ❌ still needed: **Ward—Waterbend**, **Exhaust—Waterbend**, and **waterbend-as-alternative-cast** (Hama). |
| Firebending | 28 | 14 | Attack-triggered combat-duration red mana. ✅ built — `firebending(n)` keyword + dynamic versions hand-wired via `AddManaEffect(…, ManaExpiry.END_OF_COMBAT)`. ❌ still missing: **granting** firebending to others / **conditional** "has firebending as long as …" / "gains firebending until EOT". |
| Airbend | 11 | 11 | Exile target nonland permanent; owner may recast it for {2}. ❌ keyword **not built**. |
| Exhaust | 8 | 8 | Activated ability usable only once per game. ❌ keyword **not built**. |

### Other keywords present (evergreen + returning)

| Keyword | Total | Remaining |
|---------|------:|----------:|
| Flying | 26 | 10 |
| Vigilance | 15 | 2 |
| Scry | 10 | 2 |
| Flash | 9 | 5 |
| Transform | 8 | 5 |
| Mill | 8 | 1 |
| Reach | 8 | 1 |
| Prowess | 7 | 2 |
| Menace | 6 | 2 |
| Equip | 5 | 2 |
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
- [ ] Aang, Swift Savior
- [x] Aang, at the Crossroads
- [x] Aang, the Last Airbender
- [x] Abandon Attachments
- [x] Abandoned Air Temple
- [x] Accumulate Wisdom
- [x] Agna Qel'a
- [x] Air Nomad Legacy
- [ ] Airbender Ascension
- [ ] Airbender's Reversal
- [x] Airbending Lesson
- [x] Airship Engine Room
- [x] Allies at Last
- [ ] Appa, Loyal Sky Bison
- [ ] Appa, Steadfast Guardian
- [ ] Avatar Aang
- [x] Avatar Destiny
- [x] Avatar Enthusiasts
- [ ] Avatar's Wrath
- [x] Azula Always Lies
- [ ] Azula, Cunning Usurper
- [x] Azula, On the Hunt
- [x] Ba Sing Se
- [x] Badgermole
- [x] Badgermole Cub
- [x] Barrels of Blasting Jelly
- [x] Beetle-Headed Merchants
- [x] Beifong's Bounty Hunters
- [ ] Bender's Waterskin
- [x] Benevolent River Spirit
- [ ] Bitter Work
- [x] Boar-q-pine
- [x] Boiling Rock Prison
- [x] Boiling Rock Rioter
- [x] Boomerang Basics
- [x] Bumi Bash
- [ ] Bumi, King of Three Trials
- [ ] Bumi, Unleashed
- [x] Buzzard-Wasp Colony
- [x] Callous Inspector
- [x] Canyon Crawler
- [x] Cat-Gator
- [x] Cat-Owl
- [ ] Combustion Man
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
- [ ] Destined Confrontation
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
- [ ] Earthen Ally
- [x] Elemental Teachings
- [x] Ember Island Production
- [x] Energybending
- [x] Enter the Avatar State
- [x] Epic Downfall
- [x] Fancy Footwork
- [x] Fatal Fissure
- [ ] Fated Firepower
- [x] Fire Lord Azula
- [x] Fire Lord Zuko
- [x] Fire Nation Attacks
- [ ] Fire Nation Cadets
- [x] Fire Nation Engineer
- [ ] Fire Nation Palace
- [x] Fire Nation Raider
- [x] Fire Nation Warship
- [x] Fire Navy Trebuchet
- [x] Fire Sages
- [ ] Firebender Ascension
- [x] Firebending Lesson
- [ ] Firebending Student
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
- [ ] Hama, the Bloodbender
- [x] Haru, Hidden Talent
- [ ] Heartless Act
- [x] Hei Bai, Spirit of Balance
- [x] Hermitic Herbalist
- [ ] Hog-Monkey
- [ ] Honest Work
- [x] How to Start a Riot
- [x] Iguana Parrot
- [x] Invasion Reinforcements
- [ ] Invasion Submersible
- [x] Invasion Tactics
- [x] Iroh's Demonstration
- [ ] Iroh, Grand Lotus
- [ ] Iroh, Tea Master
- [x] Island
- [x] It'll Quench Ya!
- [x] Jasmine Dragon Tea Shop
- [x] Jeong Jeong's Deserters
- [ ] Jeong Jeong, the Deserter
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
- [ ] Lo and Li, Twin Tutors
- [x] Long Feng, Grand Secretariat
- [x] Lost Days
- [ ] Mai, Jaded Edge
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
- [ ] North Pole Patrol
- [x] Northern Air Temple
- [x] Obsessive Pursuit
- [x] Octopus Form
- [x] Omashu City
- [x] Origin of Metalbending
- [x] Ostrich-Horse
- [x] Otter-Penguin
- [x] Ozai's Cruelty
- [ ] Ozai, the Phoenix King
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
- [ ] Ran and Shaw
- [x] Raucous Audience
- [x] Raven Eagle
- [x] Razor Rings
- [x] Realm of Koh
- [ ] Rebellious Captives
- [x] Redirect Lightning
- [x] Rockalanche
- [x] Rocky Rebuke
- [ ] Rough Rhino Cavalry
- [x] Rowdy Snowballers
- [x] Ruinous Waterbending
- [x] Rumble Arena
- [x] Saber-Tooth Moose-Lion
- [x] Sandbender Scavengers
- [x] Sandbenders' Storm
- [ ] Secret Tunnel
- [ ] Secret of Bloodbending
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
- [ ] Sozin's Comet
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
- [ ] The Last Agni Kai
- [ ] The Legend of Kuruk
- [x] The Legend of Kyoshi
- [x] The Legend of Roku
- [ ] The Legend of Yangchen
- [x] The Lion-Turtle
- [x] The Mechanist, Aerial Artisan
- [ ] The Rise of Sozin
- [x] The Spirit Oasis
- [ ] The Unagi of Kyoshi Island
- [x] The Walls of Ba Sing Se
- [x] Tiger-Dillo
- [x] Tiger-Seal
- [x] Tolls of War
- [x] Toph, Hardheaded Teacher
- [x] Toph, the Blind Bandit
- [x] Toph, the First Metalbender
- [x] Treetop Freedom Fighters
- [x] True Ancestry
- [ ] Trusty Boomerang
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
- [ ] Wan Shi Tong, Librarian
- [x] Wandering Musicians
- [ ] War Balloon
- [x] Wartime Protestors
- [x] Water Tribe Captain
- [x] Water Tribe Rallier
- [x] Waterbender Ascension
- [ ] Waterbending Lesson
- [x] Waterbending Scroll
- [x] Watery Grasp
- [x] White Lotus Hideout
- [x] White Lotus Reinforcements
- [ ] White Lotus Tile
- [x] Wolfbat
- [x] Yip Yip!
- [x] Yue, the Moon Spirit
- [x] Yuyan Archers
- [ ] Zhao, Ruthless Admiral
- [ ] Zhao, the Moon Slayer
- [x] Zuko's Conviction
- [x] Zuko's Exile
- [x] Zuko, Conflicted
- [x] Zuko, Exiled Prince
