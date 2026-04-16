# MageZero Coverage

Cards we need to implement for [MageZero](https://github.com/WillWroble/MageZero) to run its
training workloads on Argentum. MageZero is an AlphaZero-shaped MTG RL project that currently
runs on XMage; `gym-trainer` was designed to fit its shape, so getting its three test decks
playable is the concrete coverage target.

## Training decks

Referenced from `configs/game.yml` (`player_a.deckPath` / `player_b.deckPath`) and the
MageZero README. `.dck` files are not checked into the MageZero repo — the canonical
source is Moxfield.

| Deck | Role | Format | Source |
|------|------|--------|--------|
| **UWTempo** | Primary RL training mirror | Modern | https://moxfield.com/decks/Bl76TS_q6E-HZ4G-s9_dlQ |
| **Standard-MonoU** | Secondary RL training deck | Modern (mono-blue) | https://moxfield.com/decks/Okxs-whgSkapj5kIXUgMPg |
| **Standard-MonoB** | Minimax opponent pool | Modern (mono-black) | https://moxfield.com/decks/R3zCVSK78kWyOwzAUyHUJg |

The README also mentions MonoG / MonoR / MonoW starter decks shipped in `xmage/decks/` as
additional baseline opponents. Those deck lists aren't public; start with the three above.

## Coverage summary

**Implemented** (6 / 44 unique cards): Kitsa, Otterball Elite · Iridescent Vinelasher ·
Malcolm, Alluring Scoundrel · Negate · Island · Swamp.

**Missing** (38 unique cards — 6 creatures + 1 planeswalker + 13 instants + 1 sorcery +
4 enchantments + 6 nonbasic lands + 7 more creatures). One card (Cecil, Dark Knight) and
one enchantment (Unholy Annex) are double-faced / modal DFCs and depend on DFC support.

## UWTempo (60 cards)

### Creatures (14)
- [x] Malcolm, Alluring Scoundrel ×4 *(Lost Caverns of Ixalan)*
- [ ] Skrelv, Defector Mite ×4
- [ ] Sleep-Cursed Faerie ×2
- [x] Kitsa, Otterball Elite ×4 *(Bloomburrow)*

### Instants (14)
- [ ] Bounce Off ×4
- [x] Negate ×2 *(Foundations)*
- [ ] No More Lies ×4
- [ ] Soul Partition ×2
- [ ] Spell Pierce ×2

### Enchantments (10)
- [ ] Combat Research ×4
- [ ] Shardmage's Rescue ×2
- [ ] Sheltered by Ghosts ×4

### Lands (22)
- [ ] Adarkar Wastes ×4
- [ ] Floodfarm Verge ×3
- [ ] Meticulous Archive ×4
- [ ] Seachrome Coast ×4
- [x] Island ×7

## Standard-MonoU (60 cards)

### Planeswalkers (1)
- [ ] Teferi, Temporal Pilgrim ×1

### Creatures (10)
- [ ] Chrome Host Seedshark ×1
- [ ] Haughty Djinn ×4
- [ ] Hullbreaker Horror ×2
- [ ] Tolarian Terror ×3

### Sorceries (1)
- [ ] Blue Sun's Twilight ×1

### Instants (25)
- [ ] Consider ×4
- [ ] Dissipate ×4
- [ ] Essence Scatter ×2
- [ ] Fading Hope ×4
- [ ] Flow of Knowledge ×2
- [ ] Impulse ×2
- [ ] Memory Deluge ×1
- [x] Negate ×2 *(shared with UWTempo)*
- [ ] Spell Pierce ×2 *(shared with UWTempo)*
- [ ] Thirst for Discovery ×2

### Lands (23)
- [x] Island ×23

## Standard-MonoB (60 cards)

### Creatures (30)
- [ ] Bloodletter of Aclazotz ×4
- [ ] Cecil, Dark Knight // Cecil, Redeemed Paladin ×3 *(DFC — requires DFC support)*
- [ ] Deep-Cavern Bat ×4
- [ ] Forsaken Miner ×4
- [ ] Gatekeeper of Malakir ×4
- [ ] Mai, Scornful Striker ×3
- [ ] Unstoppable Slasher ×4
- [x] Iridescent Vinelasher ×4 *(Bloomburrow)*

### Instants (2)
- [ ] Shoot the Sheriff ×2

### Enchantments (4)
- [ ] Unholy Annex // Ritual Chamber ×4 *(DFC modal room — requires DFC + rooms support)*

### Lands (24)
- [ ] Realm of Koh ×4
- [ ] Soulstone Sanctuary ×2
- [x] Swamp ×18

## Mechanic gaps to watch

A handful of these cards pull in mechanics that may not yet exist engine-side and will
dominate the implementation cost:

- **Double-faced cards (DFC)** — Cecil (creature DFC), Unholy Annex (room DFC)
- **Rooms (Duskmourn)** — Unholy Annex // Ritual Chamber
- **Prowess / spell-count triggers** — Haughty Djinn, Kitsa, Otterball Elite (already in)
- **Delirium** — Thirst for Discovery
- **Bargain** — Shoot the Sheriff
- **Convoke / Toxic / Backup** — Skrelv, Defector Mite (toxic + pump)
- **Stun counters** — Sleep-Cursed Faerie, Unstoppable Slasher, No More Lies
- **Exile-with-play-it (Oblivion Ring family)** — Sheltered by Ghosts, Soul Partition
- **Modal dual lands / fastlands / checklands / slowlands** — Adarkar Wastes (checkland-ish
  painland), Seachrome Coast (fastland), Floodfarm Verge (slowland), Meticulous Archive
  (surveil land), Realm of Koh / Soulstone Sanctuary (lesson/channel-style utility lands)

Checking these against existing mechanics support should happen before scheduling the
first batch of card implementations.
