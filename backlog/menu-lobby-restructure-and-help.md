# Menu / Lobby Restructure + Player Guidance

Reorganise the client's entry points around a coherent model of what the app actually does, and add
the first real help surface. Argentum grew feature-first — quick games, six limited formats, four
table shapes, tournaments, Momir, ranked, accounts, friends, stats, replays, scenario tooling — and
was never re-organised around a player arriving for the first time.

Two problems, one project:

1. **Mode organisation.** The landing screen's `Quick Game | Tournament` toggle cuts across three
   independent axes at once, which makes several *already-implemented* modes effectively unreachable
   by reasoning.
2. **Guidance.** There is **no help surface of any kind** in the client — no tutorial, no shortcut
   list, no FAQ, no `?` button. Every explanation in the app is a native `title=` tooltip.

This is an `add-feature` project (client capability, plus a tail of server work in Phase 5).

**Audience we optimise for: knows Magic, new to Argentum.** Not a rules tutorial — no teaching what
a phase or the stack is. Explicitly out of scope at the bottom.

## Status: **in progress** (2026-07-31) — Phases 0–4, 6a, 7 and 8 landed; 5, 6b and 9 open

| Phase | State |
|---|---|
| 0 — split `GameUI.tsx` | **done** |
| 1 — landing restructure | **done** — but its six-card grid is superseded by Phase 7; see Part 3 |
| 2 — axis renaming in both lobbies | **done** |
| 3 — help (`topics.ts`, `/help`, `HelpTip`, `shortcuts.ts`) | **done** |
| 4 — unified lobby over a view model | **done** |
| 7 — landing wizard: three questions, not six cards | **done** |
| — polish pass | **done** — see § *Polish pass* |
| 6a — wizard-step URLs | **done** — see § *Phase 6, the wizard half* |
| 8 — `LobbyRecipe`, saved setups, grouped lobby, vs-AI rematch | **done** — see § *Phase 8* |
| 5 — server gaps (4c) | not started — **next**, in the numbered order |
| 6b — `convertLobby`, URLs for the other in-`/` screens | not started |
| 9 — setups in the account (`V13__setups.sql`), human 1v1 rematch | not started — the server tail of Phase 8 |

Everything landed so far is **client-only**. Phase 5 is where this project first touches the server,
and it is deliberately a separate PR: each of the seven gaps stands alone and each one deletes a
disabled option from the wizard. Phase 6 split cleanly along the same line — the wizard's URLs needed
no server, `convertLobby` is nothing but server.

Phase 1 landed the **Cards / Table / Event** vocabulary as
[`web-client/src/components/lobby/axes.ts`](../web-client/src/components/lobby/axes.ts) and the six
presets as [`modePresets.ts`](../web-client/src/components/ui/modePresets.ts). Phase 2 wired both
lobbies' controls onto that vocabulary and gave `axes.ts` the server-mapping half it was missing.

Sequenced behind [`cube-draft-format.md`](cube-draft-format.md), which is the current next pick.
Phases 0–3 are independent of Cube and can land in any order relative to it; Phase 2's taxonomy is
what gives Cube a place to slot in without a seventh top-level button, so landing Phase 2 *before*
Cube ships is worth a little scheduling effort.

## Confirmed scope decisions

- **Keep the centred glass card.** No persistent nav bar. The landing stays a single
  `styles.contentBackdrop` panel; only its *contents* get restructured into tiers.
- ~~**Presets, not modes.** The home screen shows six named entry points that set lobby defaults.~~
  Superseded by Phase 7 (the six cards became three questions). Phase 8 reuses the *word* for
  something else entirely — a saved **setup** is a whole lobby you have already built, not a preset
  that seeds defaults — which is why its internal type is `LobbyRecipe` and not `Preset`: the lobby
  already renders a row literally labelled **Preset** (`commanderPreset`: Brawl / Commander / Pod).
- **Unify the lobby presentation first, the server second.** A full server-side lobby merge is a
  large project on its own (see § *The honest constraint*); the client unifies over a view model and
  the server gaps get closed behind it, individually.
- **Dev tools stay dev tools.** Scenario Builder, Set Completion and LLM Tournament group under a
  **Lab** heading with an explicit "debugging and content tools, not part of normal play" caption.
  They are *not* reframed as practice modes, and hotseat is not promoted to a real Table value —
  it's a debugging affordance, not a way people are meant to play.
- **Help is one content source, two surfaces.** A typed topic registry feeds both the `/help` page
  and the inline `HelpTip` popovers. This is the constraint that prevents the drift that made the
  existing scattered tooltips near-useless.

---

## Part 1 — Inventory of what the client does today

Captured 2026-07-26. This is reference material for the restructure; it is also the first time the
client's full surface has been written down in one place.

### Routes (`web-client/src/main.tsx:62–79`)

| Route | File | Reached from |
|---|---|---|
| `*` → `/` | `App.tsx` → `components/ui/GameUI.tsx` | default |
| `/deckbuilder`, `/deckbuilder/:deckId` | `components/deckbuilder/DeckbuilderPage.tsx` | home button |
| `/scenario` | `components/scenario/ScenarioBuilderPage.tsx` | home button |
| `/set-completion` | `components/setCompletion/SetCompletionPage.tsx` | home button |
| `/replay/:gameId` | `components/replay/ReplayPage.tsx` | game-over overlay, profile, admin |
| `/profile` | `pages/ProfilePage.tsx` | `AuthWidget` only |
| `/stats` | `pages/StatsPage.tsx` | **`/profile` only — orphaned** |
| `/u/:userId` | `pages/PublicProfilePage.tsx` | **opponent names in tables only — orphaned** |
| `/friends` | `pages/FriendsPage.tsx` | `AuthWidget`, `/profile` |
| `/admin` | `components/admin/AdminPage.tsx` | `AuthWidget` when `user.isAdmin` |
| `/llm-tournament`, `/llm-tournament/:id` | `components/llmTournament/LlmTournamentPage.tsx` | **DEV-only button; route itself ungated** |
| `/tournament/:lobbyId` | `components/tournament/TournamentEntryPage.tsx` | share link; also where a mid-lobby refresh lands |
| `/join/:lobbyId` | `components/lobby/JoinLobbyPage.tsx` | QR / share link (`utils/joinLink.ts`, `JoinQrModal.tsx`) |
| `/login/verify` | `pages/LoginVerifyPage.tsx` | magic-link email |

Query-param sub-modes with no route: `/?spectate=<id>` (`App.tsx:55–100`, only ever emitted by the
LLM tournament page — the normal spectate path uses store state, so **live games have no shareable
URL**), `/?token=<t>` (session assumption, how Scenario Builder hands you a seat),
`/deckbuilder?d=` / `?decks=open` / `?q=&sort=&view=&fmt=`, `/scenario?s=` / `?replay=&frame=`,
`/?profile=1` (render profiler).

### Screens with no URL at all

Everything inside `/` is a Zustand state-machine view selected in `App.tsx:280–299` and
`GameUI.tsx:81–297`: name entry, home, quick-game lobby, tournament lobby, premade deck picker,
booster/Winston/grid draft, limited deck builder, tournament standings, FFA standings, waiting for
opponent, replay browser overlay, match intro, mulligan, game board, game over, spectator board,
session-replaced.

Consequences: nothing here can be bookmarked, shared, or deep-linked, and **browser Back exits the
app** rather than stepping back a screen. `App.tsx:103–113` rewrites the address bar to
`/tournament/<id>` with raw `history.replaceState`, bypassing the router — so React Router's
location is stale relative to the address bar during lobby play.

> **Partly resolved (Phase 6a).** The landing wizard's three steps now have real URLs under `/play/...`
> and are bookmarkable and shareable, so Back steps back a question there. Everything else in this list
> still has no URL — see § *What is left of Phase 6* for why the remainder is not the same size of job.

### Duplicated surfaces

- **Two deckbuilders:** `/deckbuilder` (constructed, 4900 lines) vs
  `components/sealed/DeckBuilderOverlay.tsx` (limited). No cross-link.
- **Two replay viewers:** the `/replay/:gameId` route vs the `components/admin/ReplayViewer.tsx`
  overlay behind the home screen's "Game Replays" button. Same controls, different code.
- **Two lobbies:** `QuickGameLobbyOverlay.tsx` vs `LobbyOverlay` (inside `GameUI.tsx:763`).

### Every way to start a game

| Path | File | Notes |
|---|---|---|
| Quick Game vs human | `QuickGameLobbyOverlay.tsx` | private/public, casual/ranked, legality format, `DeckPicker` |
| Quick Game vs AI | same | one click from home when `aiEnabled` |
| Momir Basic | `QuickGameLobbyOverlay.tsx:293–409` (`CUSTOM_FORMATS`) | no deckbuilding, 60 basics |
| Sealed (Standard / Commander) | `LobbyOverlay` → `sealed/DeckBuilderOverlay.tsx` | 1–16 boosters, per-set distribution, chaos boosters |
| Booster Draft | `draft/DraftPickOverlay.tsx` | 3–8 players, 1–6 packs, pick timer, 1–2 picks/pack |
| Winston Draft | `draft/WinstonDraftOverlay.tsx` | exactly 2 players |
| Grid Draft | `draft/GridDraftOverlay.tsx` | 2–4 players |
| Commander Draft / Sealed | `LobbyOverlay` | 2 players, Brawl or Commander presets |
| Premade Decks event | `PremadeDeckPickerPanel` (`GameUI.tsx:1778`) | everyone brings their own deck |
| Free-for-All | `FreeForAllOverlay` (`GameUI.tsx:1883`) | 2–6, attack-mode any/left/right (CR 802/803) |
| Two-Headed Giant | lobby `gameMode` | exactly 4, shared 30 life (CR 810) |
| Team vs Team | lobby `gameMode` | 4/6/8, own life and turns (CR 808) |
| Scenario / hotseat | `scenario/ScenarioBuilderPage.tsx` | SELF / AI / TWO_PLAYER, 3–4 seat pods |
| Spectate | `LiveGameList`, `/?spectate=`, eliminated "Keep Watching" | `spectating/SpectatorGameBoard.tsx` |
| Replay | `/replay/:gameId`, `ReplayViewer` overlay | scrub, share-as-scenario, snapshot download |

No gauntlet mode exists (grep-verified).

### Existing help content

Zero dedicated surface. The one real help affordance in the whole app is the deckbuilder's
search-syntax popover (`DeckbuilderPage.tsx:2593` → `SearchHelp` at `:2618`).

Good copy that already exists and should be **harvested into topics, not rewritten**:

- `OpponentRail.tsx:211–290` — Overview / Follow camera. The best-written help in the app.
- `GameBoard.tsx:1400–1423` — Auto Tap / Manual Tap, and Auto / Stops / Full Control. The *only*
  place priority modes are explained anywhere.
- `GameCard.tsx:1514–1539` — Plotted ("CR 718 — cast it for free on a later turn"), Prepared, Warped.
- `SpeedGauge.tsx` — how speed rises and what max speed unlocks.
- `TheRingBadge.tsx:14–19` — the four Ring ability texts.
- `GameUI.tsx:989–1600` — ~30 lobby setting tooltips (game modes, ranked, FFA/2HG/team, attack
  left/right, Brawl vs Commander presets, singleton, AI assist, random teams).
- `ReplayPage.tsx:302` — share-as-scenario.
- `StepStrip.tsx:484,492` — "My turn stop" / "Opponent turn stop".
- `QuickGameLobbyOverlay.tsx:184–388` — host-only settings, ranked eligibility.

### Undocumented power features

Nothing in the UI hints at any of these:

- **Right-click / long-press a stack item → yield menu** (`StackZone.tsx:174` →
  `YieldContextMenu.tsx`): yield until end of turn, always yield, always answer Yes/No, revoke.
  `ActiveYieldsPanel.tsx` only appears *after* you've set one, so it can't teach the feature.
- **Stop dots** revealed by hovering a step pip (`StepStrip.tsx:469–497`), persisted to
  `localStorage['argentum-stop-overrides']`.
- `D` toggles the deck browser (`ZonePiles.tsx:149`) — documented only in a `title`.
- `F` flips a DFC while hovering (`CardPreview.tsx:44`, `useDfcHoverFlip.tsx:30`) — undocumented.
- `0` table overview, `1`–`9` opponent boards, `Esc` unpin (`useMultiplayerView.ts:63–73`).
- Draggable decision banners (`DraggableBanner.tsx`).
- Card-stack ungroup (`CardStack.tsx:27`), attachment browser (`Battlefield.tsx:340–390`).
- Swipe left/right on the opponent strip (`GameBoard.tsx:841–858`).
- Drag-to-cast from hand, drag-to-band attackers, drag-to-block (`GameCard.tsx:496–710`).

> **Found while surveying:** `useMultiplayerView.ts:64` has a comment claiming number keys activate
> abilities when a card's action menu is open. The guard is real; **no such handler exists anywhere
> in the codebase.** Either implement it or delete the comment.
>
> **Also found:** `components/spectating/SpectatorView.tsx` is dead — superseded by
> `SpectatorGameBoard.tsx`, imported nowhere.

---

## Part 2 — Mode taxonomy: three axes, not one toggle

> **Update (2026-07-31): there are four.** Everything below about Cards / Table / Event holds, but
> "this game runs Commander" turned out to be a value on none of them while being reachable through
> three unrelated fields (the pool-building `TournamentFormat`, the deck-legality `DeckFormat`, and the
> quick lobby's format). It is now its own axis — **Rules** (`GameRules`, `TournamentLobby.rules`),
> rendered between Cards and Table, with `usesCommanderRules` as the single authority every consumer
> reads. Reading order: what deck → under what rules → at what table → over how many games. See
> `backlog/commander-format.md` § Phase 3.

Everything implemented today is a point in this space:

| Axis | Values implemented | Where it lives now |
|---|---|---|
| **Cards** — where your deck comes from | Bring a deck · Random pool · Momir Basic · Sealed (Standard / Commander) · Draft (Booster / Winston / Grid / Commander) | split across **two unrelated controls**: the legality dropdown at `QuickGameLobbyOverlay.tsx:23` and the `Format` row at `GameUI.tsx:971` |
| **Table** — who is at it | 1v1 · Free-for-All (2–6) · Two-Headed Giant (4) · Team vs Team (4/6/8) | `GameUI.tsx:1056` — only visible *after* picking "Multiplayer" |
| **Event** — one game or a series | Single game · Round-robin bracket + standings | **not its own axis** — jammed onto Table as `Mode: Tournament \| Multiplayer` (`GameUI.tsx:998`) |

### The core diagnosis

**"Tournament" is not a peer of "Sealed" — it's a peer of "single game."**

Because Event was never separated out as its own axis, the word got promoted to the home screen,
where it now labels *all limited play*. Everything else follows from that one mistake:

- A **1v1 sealed game with one friend** requires clicking a button labelled **Tournament**.
- **"4-player free-for-all with my own deck"** = Tournament → Create Lobby → Format **Premade** →
  Mode **Multiplayer** → Variant **Free-for-All**. Four steps, the first of which is misleading.
  This combination is *fully supported server-side* — verified at `LobbyHandler.kt:1304–1335`
  (the `PREMADE_DECKS` start branch dispatches to `FreeForAllHandler.maybeStartGame`) and
  `FreeForAllHandler.kt:47–111` (premade + commander-shape + team stamping all handled). It is a
  discoverability failure, not a missing feature.
- **Momir Basic** is a card inside a dropdown inside the quick lobby — invisible from home.
- **"Format"** means deck legality in one lobby and pool type in the other. **"Mode"** means
  quick-vs-tournament on home but table shape in the lobby. Two words, four meanings.

### Renaming (worth doing even if nothing else ships)

| Old | New | Values |
|---|---|---|
| Format (quick) / `deckFormat` | folded into **Cards** → "Bring a deck" sub-option | Standard, Pioneer, Modern, Pauper, Legacy, Vintage, Commander, Brawl, Standard Brawl, Premodern |
| Format (tournament) | **Cards** | Bring a deck · Random · Sealed · Draft · Momir |
| Mode / Variant | **Table** | 1v1 · Free-for-All · Two-Headed Giant · Team vs Team |
| *(implicit in `gameMode`)* | **Event** | Single game · Round-robin bracket |

Sub-options hang off their own axis only: Draft → Booster / Winston / Grid / Commander; Sealed →
Standard / Commander; Bring a deck → the legality dropdown.

**Cube is the test that the taxonomy is right.** [`cube-draft-format.md`](cube-draft-format.md)
adds one new **Cards** value (with Pool Play as a sub-option), not a seventh top-level button. If a
new mode ever needs a new home-screen button, the taxonomy was wrong.

Naming the axes also makes the real holes *visible* instead of hiding them behind impossible click
paths: ranked is 1v1-bracket-only, there is no 2HG bracket, AI can't join premade or FFA. Those are
Phase 5.

---

## Part 3 — Landing screen

Keep the centred glass card. Three labelled tiers: **PLAY**, **BUILD & BROWSE**, **LAB**.

```
  ── BUILD & BROWSE ──────────────────────────────────
  Deckbuilder · Replays · Stats · Friends · Profile

  ── LAB (advanced) ──────────────────────────────────
  Scenario Builder[dev] · Set Completion · LLM Tournament[dev]
  "Debugging and content tools, not part of normal play."
```

- **BUILD & BROWSE finally gives `/stats` and `/profile` a home-screen entry.** They were orphaned
  — `/stats` was two clicks deep behind a non-obvious affordance.
- **Continue chip** — if a lobby is still live from a previous page load, surface it. Before this a
  refresh mid-lobby dumped you on `/tournament/:lobbyId` with no indication that's what happened.
- Unchanged and staying where they are: guest name entry, `AccountBenefitsCallout`,
  `DeckMigrationPrompt`, `AuthWidget`, `PublicLobbyList`, `LiveGameList`, the Scryfall / Mana Font
  attribution and the WotC fan-project disclaimer.

The PLAY tier is Phase 7 and the rest of this section.

### 3a. Why the six-card grid failed — Phase 1's own axis-mixing bug

Phase 1 shipped a PLAY tier of six preset cards: `vs AI · vs Friend · Draft & Sealed ·
Multiplayer · Tournament · Variants`. Reported unclear by Vincent on 2026-07-26 — *"the
distinctions are not clear… vs AI vs a friend: you can add AI to 1v1 single games, Momir, or
brackets… draft, multiplayer and tournament all seem to overlap."* Correct on both counts, and the
cause is exactly the mistake Part 2 diagnoses, reproduced one level down:

| Card | Question it actually answers |
|---|---|
| vs AI · vs Friend | who fills the other **seats** |
| Draft & Sealed · Variants | **Cards** |
| Multiplayer | **Table** |
| Tournament | **Event** |

Six cards drawn from four different questions. Because the three axes are *independent*, the
overlaps a player notices are real product truths rather than wording problems: AI can sit in a
bracket, a draft **is** a round-robin event, "Multiplayer" and "Tournament" differ only in Event,
and Momir is a Cards value that merely happens to be 1v1 today.

Two further diagnoses follow, and they're what the new design is built on.

**Seats are not a fourth axis — they aren't a mode dimension at all.** Who fills a seat is per
seat, decided by *Add AI* / an invite, and any Cards × Table × Event point can in principle be
played by any roster. `vs AI` and `vs Friend` were roster shortcuts wearing mode-card clothing,
which is why they read as peers of `Sealed` and aren't. The taxonomy was right to have three axes;
Part 2's rule (a new mode adds a *value*, never an axis) stands.

**Since Phase 4 the lobby already is the mode picker** — all three axes, all five Cards values, on
both backing kinds, with `HelpTip`s and `⇄` cross-kind switching. Six cards in front of that screen
were a second, worse mode picker speaking a different vocabulary. Deleting them removes a competing
taxonomy rather than a feature.

### 3b. What replaces it: three questions, asked one at a time

Decided 2026-07-26 (options weighed: a three-tile "how do the other seats get filled" grid; a
Cards-only grid with AI as a toggle; this). The landing screen asks the three questions a player can
actually answer, in the order that prunes hardest, and the *answers* compose into one
`(roster, Cards, Table, Event)` point that creates the right lobby kind first time.

```
                    Argentum Engine

  ── PLAY ────────────────────────────────────────────
  Play again → [Booster Draft · 8-player pod · bracket]   ← if there is a last time

  Step 1 of 3 — Who are you playing with?
  ┌──────────────┬──────────────┬──────────────┐
  │ Just me      │ A friend     │ A group      │
  │ you + the    │ one opponent,│ 3–8 players, │
  │ built-in AI  │ invite code  │ invite codes │
  └──────────────┴──────────────┴──────────────┘
  Been invited?  [invite code____] (Join)
  Continue → [Sealed lobby ABC12]        ← only if one is live

  Step 2 of 3 — What are you playing with?      Just me · ✎
  ┌────────────┬────────────┬────────────┬────────────┬────────────┐
  │ Bring a    │ Random     │ Momir      │ Sealed     │ Draft      │
  │ deck       │ pool       │ Basic      │            │            │
  └────────────┴────────────┴────────────┴────────────┴────────────┘
  [← Back]                        (no sub-shape row — see § 3e)

  Step 3 of 3 — How do you play it?     Just me · Booster Draft · ✎
  ┌──────────────────────┬──────────────────────┐
  │ Round-robin bracket  │ Free-for-All  ⃠      │
  │ 1v1 matches          │ AI can't sit in a    │
  │ pod of [8 ▾]         │ multiplayer pod yet  │
  └──────────────────────┴──────────────────────┘
  [← Back]                                  [Create lobby →]
```

**Each step offers only what is reachable, and says why for the rest.** Options nothing implements
render disabled with the reason attached — the same rule the lobby follows, for the same reason
(§ 4a). This is the first surface that shows a Phase 5 hole *at the moment the player asks the
question*, instead of after they've committed to a lobby.

**A step whose options collapse to one answer is skipped**, and the resolved value appears in the
recap line with its `HelpTip` so it is decided-and-visible rather than silently assumed. That is
what keeps the fast paths fast: `Just me → Bring a deck` skips step 3 entirely (a premade table
can't hold AI in a bracket or a pod, so "one game" is the only answer) — two clicks to a vs-AI
lobby, which is what the old `vs AI` card cost.

**The recap line is the Back button.** Every answered step shows as a chip; clicking one returns to
that step with later answers re-validated against the change.

**Nothing is final.** Create lands in the Phase 4 lobby with all three axes still editable, so the
wizard is a *creation* path, not a mode gate — and because it collects the whole triple before the
lobby exists, the initial setup never pays the `⇄` recreate the six presets forced (pick `vs
Friend`, then want Free-for-All → tear down and recreate).

### 3c. What is reachable today — the wizard's load-bearing data

Verified against the server, not inferred. Every ✗ is a Phase 5 gap in § 4c, and the wizard is
where each one now becomes visible.

| Cards | Just me (AI) | A friend | A group (3–8) |
|---|---|---|---|
| Bring a deck | 1v1, one game | 1v1 one game · 1v1 bracket | FFA · 2HG · Teams · bracket |
| Random pool | 1v1, one game | 1v1, one game | ✗ quick lobby is 2 seats (gap #3) |
| Momir Basic | 1v1, one game | 1v1, one game | ✗ no tournament-side Momir (gap #2) |
| Sealed | 1v1 bracket vs AI | 1v1 bracket | bracket · FFA · 2HG · Teams |
| Draft | **pod of up to 7 AI**, 1v1 bracket | 1v1 bracket | bracket · FFA · 2HG · Teams |

Three things this table settles that were guesses before:

- **Solo is 1v1-only, and that is gap #1, not a design choice.** `handleAddAiToLobby` rejects
  `PREMADE_DECKS` (`LobbyHandler.kt:1387`) and rejects `isFreeForAll`, which covers Free-for-All
  **and** Two-Headed Giant **and** Team vs. Team (`TournamentLobby.kt:343`). Same guard on the
  settings-update path at `:2080–2088`. So "Bring a deck vs AI" can only be the quick lobby's single
  game, and no AI can sit at any multiplayer table.
- **A solo draft pod against 7 AI drafters is fully supported and currently near-undiscoverable.**
  `handleAddAiToLobby` caps only on `isFull`, so a host can fill a `DRAFT` lobby with AI and play the
  bracket out. This is the same class of finding as Part 2's "4-player FFA with my own deck":
  implemented, reachable only by accident. The wizard's step 3 offers it as a pod-size control.
- **Limited at a multiplayer table works.** `SEALED` + `gameMode = FREE_FOR_ALL` is a legal
  tournament lobby, so Phase 4's note that "a limited pool always runs as a bracket" is only true at
  a *1v1* table, where the sole single-game path is the quick lobby and it can't do limited.

Sub-shape reachability was originally given the same treatment — Winston is exactly 2 players, Grid is
2–4, so both blocked-with-reason under **A group**. **Superseded by § 3e:** the sub-shape is no longer a
wizard question at all.

### 3d. Implementation rules

- **`src/components/lobby/modeMatrix.ts` — the cold-start reachability model.** The pre-lobby twin of
  `axisChoices.ts`: same question ("can I have this combination?"), different starting point (no
  lobby exists yet, so the answer is never `RECREATE`). Exports the `Roster` type, the reachable
  `Table × Event` shapes as one list of concrete named shapes rather than two dropdowns with dead
  combinations, per-value `disabledReason`s, and `resolveLaunch(roster, cards, shape, seats)` →
  the lobby-creation spec plus how many AI seats to add.
- **`lobbyKindFor(triple)` is the one fact both surfaces need.** `axisChoices.ts` currently
  re-derives it inside three hand-written switches; it should ask `modeMatrix` and compare the
  answer with the lobby's current kind (same kind → `DIRECT`, other kind → `RECREATE`, neither →
  `BLOCKED`). Do this only if it comes out genuinely smaller — the two surfaces legitimately phrase
  the *reasons* differently ("Switch the Table to 1v1 first" is meaningless in a wizard with no
  table yet), so the shared thing is the predicate, not the copy. If the phrasing split makes the
  unification worse than the duplication, leave the modules separate and cross-reference them in a
  comment. Either way there is a test asserting the two agree on every triple.
- **`src/components/ui/PlayWizard.tsx`** renders the steps in place inside `contentBackdrop` — no
  navigation, no route (home still has no URL; that's Phase 6). Reuses `presetCard*` styling for the
  option tiles, `SettingsLabel`, `axisSummary` for the recap, and the existing `cards-*` / `table-*`
  / `event-*` help topics, which already exist from Phase 2 and need no rewrite.
- **Repeat players skip the wizard.** `localStorage['argentum-last-launch']` feeds a `Play again →
  <recap>` chip above step 1 — one click, same lobby as last time. This is the honest answer to a
  wizard's real cost: it is excellent once and tedious the fifth time. Sits next to the Continue
  chip, which answers a different question (a lobby you are *already* in).
- **Join code, `PublicLobbyList` and `LiveGameList` sit outside the wizard.** Someone else already
  answered the three questions; the join row is not a step. Keep it visible under step 1 rather than
  gating it behind one.
- **Deleted:** `components/ui/modePresets.ts`, `ModePresetCard`, and the six `preset-*` help topics.
  `ModePreset.launch` — the six-way mapping onto the two server lobby kinds — is subsumed by
  `resolveLaunch`, which is the same seam with a smaller surface.
- **Test ids:** `wizard-roster-{solo,friend,group}`, `wizard-cards-{bring-a-deck,random,momir,sealed,draft}`,
  `wizard-shape-<id>`, `wizard-seats-<n>`, `wizard-back-<step>`, `wizard-create`, `wizard-play-again`.
  (There is no `wizard-subshape-*` — see § 3e.) The two e2e specs that click `mode-preset-draft-sealed`
  (`sealed-tournament.spec.ts`, `draft-tournament.spec.ts`) move onto a shared fixture helper — they
  have now been rewritten by two consecutive landing changes, which is the argument for the helper.
  Since Phase 6a a spec can also skip the clicking entirely and `page.goto('/play/group/draft/bracket')`.
- **Help:** new topics for the three roster values and for the wizard itself; `/help` § Game modes
  reorganised around the three questions; every disabled option's reason cross-links `axis-limits`.

### 3e. Sealed and draft shape are a lobby sub-option, not a question

Decided 2026-07-26 by Vincent, after seeing the wizard running. The sub-shape row is removed from step
2; picking Sealed or Draft commits on the *kind* at its default (Standard sealed, Booster draft) and
goes straight to step 3.

The reasoning that makes it obvious in hindsight: **the sub-shape is the same category of thing as deck
legality.** Both hang off the Cards axis as sub-options, both are already rendered by the lobby
(`LobbyAxes`' `Sealed shape` / `Draft shape` rows, gated by its own `shapeBlock`), and neither is one of
the three questions the landing screen exists to ask. "Booster, Winston, Grid or Commander?" is a
question only someone who has already decided to draft can have an opinion about — asking it before
they have said who they are playing with is asking it too early.

It also simplifies the URL scheme in a principled direction: slugs are the Cards kind
(`/play/group/draft`, not `/play/group/draft-booster`), because encoding a sub-shape nobody chose would
invent an answer — exactly the reason `legality` was never encoded either. One segment per answer.

`modeMatrix.subShapeChoices` (plus `subShapeLabel` / `subShapeCaption`) is deleted with it: the wizard
was its only production consumer, since the lobby's row is hand-written. The
`COMMANDER_LIMITED_HAS_NO_AI` reason it carried is still live at `LobbyAxes.tsx:292`, so nothing that
was being explained stopped being explained — it is explained where the control now is.

> **One constraint got weaker, and it is worth knowing.** The wizard used to refuse a group Winston
> selection up front ("Winston is exactly two players"). The lobby's `shapeBlock` gates on players
> *present*, not seats — `view.players.length > cardsSeatCap(cards)` — so a lone host in an 8-seat lobby
> can select Winston and no warning appears until people join. This is **not new** (create a Booster
> draft for 8, switch it to Winston: same outcome), and the underlying cause is server-side:
> `LobbyHandler.kt:2164–2175` re-clamps `maxPlayers` for Winston/Grid only when the message *carries*
> `maxPlayers`, so changing the format alone leaves an 8-seat cap on a 2-player format. The honest fix
> is a Phase 5 one — re-clamp on format change. A client-side stopgap would be to gate `shapeBlock` on
> `maxPlayers` rather than players present, at the cost of making the host lower Seats before Winston
> becomes selectable.


---

## Part 4 — One lobby, three axes

**Goal:** every preset lands in the same lobby, which always shows Cards / Table / Event. Someone who
entered via "vs AI" can add a human and switch to Free-for-All without backing out to the menu.

### The honest constraint

The server has **two unrelated lobby implementations with no shared interface**:

| | Quick game | Tournament |
|---|---|---|
| Model | `lobby/QuickGameLobby.kt` (125 lines) | `lobby/TournamentLobby.kt` (1884 lines) |
| Storage | in-memory `ConcurrentHashMap`, not persisted | Redis via `PersistentTournamentLobby` + `persistence/LobbyConverter.kt` |
| Players | hard `MAX_PLAYERS = 2` (4 for its unused 2HG path) | 2–8, host field, spectators |
| Shape | no state machine; flat DTO + per-field setters | `LobbyState` machine; nested `settings` + one 21-field `updateLobbySettings` |
| Handler | `handler/QuickGameLobbyHandler.kt` (678) | `handler/LobbyHandler.kt` (2412) + 5 sub-handlers |

There is no `kind` discriminator anywhere. So a *fully* unified lobby is **not** a client-only
change. This plan unifies the presentation first and closes server gaps behind it, individually.

One bridge already exists: `QuickGameLobbyHandler.handleJoin:241–248` delegates to the tournament
join handler when the code belongs to a tournament lobby, so the home Join field is already
kind-agnostic.

### 4a. Client: one lobby screen over a view model

- `src/components/lobby/lobbyViewModel.ts` — a `UnifiedLobbyView` type plus
  `fromQuickGameLobby(state)` and `fromTournamentLobby(state)`. Both produce
  `{ lobbyId, isHost, players[], axes: { cards, table, event }, capabilities, ready, canStart }`.
- `src/components/lobby/LobbyScreen.tsx` renders the view model. `GameUI.tsx:95`'s hard either/or
  switch becomes: build the view model from whichever slice is populated, render one screen.
- `src/components/lobby/LobbyAxes.tsx` renders the three axes from a declarative descriptor
  (`lobbyAxes.ts`) that also encodes which values the *current backing kind* supports. Unsupported
  values render **disabled with a HelpTip explaining why** — not hidden. Visible constraints beat
  invisible ones.
- **The CSS is already shared.** `QuickGameLobbyOverlay.tsx` imports `GameUI.module.css` and
  deliberately reuses `lobbyOverlay` / `lobbyContent` / `lobbyHeader` / `inviteBox` /
  `playerListPanel` / `actionsRow` / `settingsRow` / `variantGroup` / `variantCaption` — its header
  comment says so. The merge is structural, not visual.
- Reused untouched: `DeckPicker.tsx`, `SetPickerModal`, `BanListEditor.tsx`, `JoinQrModal.tsx`,
  `utils/joinLink.ts`, `PremadeDeckPickerPanel`.

### 4b. Switching axes across lobby kinds

When the host picks a value the current backing kind can't express (quick game → Free-for-All), the
client tears down and recreates on the other kind.

- **v1, no server work:** confirm first — *"Switching to Free-for-All creates a new lobby. Your
  invite code will change."* Recreate, re-copy the link. Acceptable because it only bites the host
  before anyone has joined, which is the common case.
- **v2, needs server:** a `convertLobby` message preserving `lobbyId` and joined players. Scope
  separately; **do not block v1 on it.**

### 4c. Server gaps that keep holes in the matrix

Each is independent. Ordered by value.

**All seven re-verified against the server on 2026-07-26**, after the phase-0–7 PR merged `main`
forward 26 commits — nothing in this list has been closed by other work, and the PR branch carries
**zero** `game-server/` changes. Line references below are current as of that check. Two of the
original evidence lines had drifted and are corrected in place: gap #5 gained the defaults finding
(note under the table), and gap #6's "0 hits in `web-client/`" is now 1 hit — a comment recording the
gap, not a use of it.

| # | Gap | Where | Why it matters |
|---|---|---|---|
| 1 | **AI rejected in `PREMADE_DECKS` and in all FFA/team modes** | `LobbyHandler.kt:1386–1393`, `:1395–1403`, `:2080–2088` | "Bring a deck + vs AI" and "FFA with AI seats" both read as obvious once the axes are visible, and both currently fail. Highest-value fix in the list. |
| 2 | **Momir exists only on quick lobbies** | `lobby/MomirBasicSetup.kt`, `QuickGameLobbyHandler`; `grep -i momir` over `TournamentLobby.kt` / `LobbyHandler.kt` / `FreeForAllHandler.kt` / `PersistentLobby.kt` = **zero hits** | Momir can't be a Cards value on the unified lobby until it exists tournament-side. Needs the flag, the `MomirBasicSetup` wiring in `TournamentMatchHandler` / `FreeForAllHandler`, and a `Ranked.modeForQuickGame` equivalent. |
| 3 | **No per-player "random pool" in premade** | `QuickGameLobbyPlayer.setCode` (empty `deckList` = server picks); tournament SEALED forces a `DECK_BUILDING` phase | "Random" is the zero-prep on-ramp — the fastest path from cold open to playing. It must survive the merge. |
| 4 | **No per-player ready in tournament `WAITING_FOR_PLAYERS`** | host presses `startTournamentLobby`; there is no per-player ready toggle | The 2-player "both ready → go" flow is what makes a quick game *feel* quick. |
| 5 | **Ranked gated to `gameMode == TOURNAMENT`, and the two kinds default it differently** | `TournamentLobby.rankedEligible` (`:335–358`) vs `QuickGameLobby.rankedEligible` + `Ranked.modeForQuickGame`; defaults at `ClientMessage.CreateTournamentLobby.ranked = true` vs `TournamentLobby.ranked = false` | Two different ranked paths need reconciling before ranked can appear on one axis panel. Both already silently downgrade to unranked at start, so the failure mode is safe but confusing. **See the note below** — the defaults disagreeing is now visible on the shared panel. |
| 6 | **Quick-lobby 2HG is phantom capability** | `QuickGameLobby.twoHeadedGiant` + `TWO_HEADED_GIANT_PLAYERS = 4` are implemented (`QuickGameLobby.kt:51,70,85,93`); missing from `QuickGameLobbyStateMessage` (`types/messages.ts:2712–2727`) along with `maxPlayers` and `QuickGameLobbyPlayerView.teamIndex`, so no client can read it. The only `twoHeadedGiant` in `web-client/` is the comment at `lobbyViewModel.ts:171` recording exactly that | Either wire it up or delete it. Right now it's server capability no client can reach. |
| 7 | **`PersistentTournamentLobby` missing fields** | `persistence/LobbyConverter.kt` — lacks `deckFormat`, `ranked`, `bannedCardNames`, `deckSizeMin`, `allowDuplicates`, `commanderPreset`, `ffaLastStandings` | Any new unified setting needs a converter pass or it won't survive a restart. Pre-existing bug, worth fixing while in here. **Partly closed:** the Rules axis (`rules`) is persisted, so a restored lobby at least still knows whether it runs Commander; the rest of the list stands. |

**Gap #5, sharpened — the two ranked defaults disagree, and it now shows.** Found while verifying the
wizard against the running stack. `ClientMessage.CreateTournamentLobby.ranked` defaults to **`true`**
(`ClientMessage.kt:179`) while the model's own `TournamentLobby.ranked` defaults to **`false`**
(`TournamentLobby.kt:331`); the client has never sent the field on create, on this branch or on main.
So every tournament-backed lobby opens on **Ranked** and every quick-backed lobby opens on **Casual**
— for the same "a friend, bring a deck" intent, decided only by which Event you picked. Before Phase 4
that was invisible, because the two kinds had different panels; now it is one shared Ranked row that
changes its own default when you cross the `⇄`.

Deliberately **not** fixed in the phase-0–7 PR. The one-line client fix (send `ranked: false`) and the
one-line server fix (flip the message default) are opposite product calls about whether ranked is
opt-in or opt-out, and that decision belongs with the rest of gap #5 rather than to a polish pass. It
is safe today either way: `LobbyHandler.kt:1202–1211` downgrades to unranked at start unless every
seat is a signed-in human, so nothing incorrect is recorded — the lobby just says something it may
not honour.

---

## Part 5 — Guidance

Two surfaces, **one content source**. This is the whole design.

### 5a. Content model — `src/help/topics.ts`

```ts
export type HelpSection = 'getting-started' | 'modes' | 'playing' | 'decks' | 'advanced'

export interface HelpTopic {
  id: string                 // 'priority-modes', 'table-free-for-all', 'yields'
  section: HelpSection
  title: string
  summary: string            // 1–2 sentences — what the popover shows
  body?: ReactNode           // longer prose, only rendered on /help
  related?: string[]         // other topic ids
  shortcuts?: string[]       // ids from shortcuts.ts
}
```

Typed TS, not markdown: there is no markdown pipeline in the client, `public/` ships no docs, and
the Dockerfile copies only `dist/` + `nginx.conf` — so the repo's `docs/` is not reachable from the
browser and never will be without new build machinery.

**Seed it by moving the good `title=` copy listed in Part 1 into topics**, then having those call
sites reference the topic id instead of holding the string. Net new prose is small; the win is that
there is now exactly one place each explanation lives.

### 5b. `/help` route — `src/pages/HelpPage.tsx`

Registered in `main.tsx`. Deep-linkable as `/help/playing#priority-modes`.

1. **Getting started** — pick a name, guest vs account, start your first game, where decks live.
2. **Game modes** — one entry per Part 2 axis value plus the six presets. Documents the three-axis
   model using the exact words the lobby uses.
3. **Playing a game** — phase bar and stops, priority modes (Auto / Stops / Full Control), passing
   and resolving, Auto vs Manual Tap, targeting and combat drag, yields, undo, the log, zone
   browsers, the deck tracker.
4. **Decks** — constructed deckbuilder, search syntax (link the existing `SearchHelp` popover rather
   than duplicating it), Arena import/export, share links, sealed/draft building, sideboard.
5. **Advanced** — keyboard shortcut table, replays and replay-to-scenario, spectating, multiplayer
   camera (Overview / Follow / pin), Lab tools.

### 5c. `HelpTip` — `src/components/help/HelpTip.tsx`

A `⃝?` button taking a `topicId`. Popover shows the topic's `summary` plus "Read more →" linking to
`/help/<section>#<id>`.

**Must be portal-rendered.** The app is `overflow: hidden` and the multiplayer strip uses a CSS
transform, which breaks `position: fixed` — see the existing portal overlays in `ZonePiles.tsx` for
the established pattern.

Minimum placement: every home mode card; every lobby axis row; the in-game priority-mode button,
Auto Tap button and step-strip stop dots; the stack yield menu; the ranked toggle; the Overview /
Follow buttons.

### 5d. Persistent `?` entry

No nav bar, so: a small fixed help button beside `FullscreenButton` / `AuthWidget` on the home
screen, and in the in-game top chrome. Home opens `/help`; in-game opens a **drawer** rather than
navigating, so it doesn't drop the WebSocket.

### 5e. Keyboard shortcut registry — `src/help/shortcuts.ts`

One declarative list. Shortcuts are currently scattered across `useMultiplayerView.ts`,
`ZonePiles.tsx`, `CardPreview.tsx`, `useDfcHoverFlip.tsx`, `ReplayPage.tsx`, `ActionMenu.tsx`,
`DeckbuilderPage.tsx`, with no index anywhere. The `/help` Advanced section renders it as a table.

Complete list as of 2026-07-26: `1`–`9` opponent boards · `0` overview · `Esc` unpin / close modal /
close zone browser / exit replay · `D` deck browser · `F` flip DFC on hover · `←`/`→` replay frame ·
`Space` replay play-pause · `Enter` submit · Shift-click / right-click removes a deckbuilder copy.

Resolve the phantom number-key comment (`useMultiplayerView.ts:64`) while doing this.

---

## Phasing

`GameUI.tsx` is 2992 lines holding five screens. Phase 0 is a prerequisite for everything else.

| Phase | Work | Ships value alone? |
|---|---|---|
| ~~**0**~~ | ~~Split `GameUI.tsx`~~ — **done.** `HomeScreen.tsx` (709), `components/lobby/LobbyOverlay.tsx` (1123), `components/tournament/{TournamentOverlay,FreeForAllOverlay}.tsx`, shared `FullscreenButton.tsx`. `GameUI.tsx` is now a 34-line router. | no — enabler |
| ~~**1**~~ | ~~Landing restructure~~ — **done.** `axes.ts` + `modePresets.ts`, three tiers, Lab caption, Continue chip, `/stats` `/friends` `/profile` surfaced. Its six-card PLAY grid is superseded by Phase 7; the tiers, chip and account entries stand. | **yes** |
| ~~**2**~~ | ~~Axis renaming across both lobbies~~ — **done.** `axes.ts` gained the server-mapping half; both lobbies show a `LobbyAxisSummary`; the tournament lobby's Format/Mode/Variant rows became Cards (+ sub-options) / Table / Event. | **yes** — kills the Format/Mode overloading |
| ~~**3**~~ | ~~Help~~ — **done.** `src/help/{topics,shortcuts,helpStore}.ts`, `/help/:section`, portal `HelpTip`, in-game drawer, `?` on home. | **yes** |
| ~~**4**~~ | ~~Unified lobby~~ — **done.** `lobbyViewModel.ts` + `axisChoices.ts` + `useLobbyCommands.ts` behind one `LobbyScreen`; both old overlays deleted; v1 recreate-on-switch confirm. | **yes** |
| ~~**7**~~ | ~~Landing wizard~~ — **done.** `modeMatrix.ts` + `PlayWizard.tsx` replace the six preset cards; `modePresets.ts` and the `preset-*` topics deleted; numbered stepper, commitment badges, flow line, open-by-default seats, `Play again` chip, `Seats` in the lobby, e2e fixture helper. | **yes** |
| **5** | Server gaps from 4c, in the numbered order. Each one deletes a disabled option from the wizard. | yes, each |
| ~~**6a**~~ | ~~A URL per wizard step~~ — **done.** `wizardUrl.ts`; the stepper's Back and the browser's Back finally agree, and a selection is shareable. Client-only. | **yes** |
| **6b** | `convertLobby` preserving the invite code (server); real URLs for the remaining in-`/` screens so Back works across them. | yes |

### Phase 8 — a game you want to play, written down

The three questions were answered well; what happened to the answers was not. The wizard produced a
three-field `Selection`, `resolveLaunch` widened it to a five-field `LaunchSpec`, and
`HomeScreen.launch` threw even that away — `createTournamentLobby(['ECL'], format, 6, maxPlayers, 45,
false, …)`. The other ~20 answers only ever existed as server-owned `LobbySettings` that die with the
lobby.

One missing object, three symptoms: repeat play was slow (nothing to repeat — the `Play again` chip
carried four of ~24 knobs), the lobby was a wall (the only place those answers could live), and there
was no 1v1 rematch (the thing to replay had never been recorded).

**[`components/lobby/lobbyRecipe.ts`](../web-client/src/components/lobby/lobbyRecipe.ts)** is that
object; [`useApplyRecipe.ts`](../web-client/src/components/lobby/useApplyRecipe.ts) replays it. What
landed on top of it:

- **Saved setups** — auto-captured when you press Start or Ready, plus a named `★ Save setup`, on a
  rail above the wizard ([`SetupRail.tsx`](../web-client/src/components/ui/SetupRail.tsx),
  [`store/setupLibrary.ts`](../web-client/src/store/setupLibrary.ts)). The rail does not render until
  you have played something, so a first-time player sees exactly the wizard described in Part 3. The
  old `argentum-last-play-selection` key migrates into it on first read.
- **Five collapsible settings groups** ([`settingsGroups.ts`](../web-client/src/components/lobby/settingsGroups.ts))
  named for the four axes plus "This lobby". ~20 rows / ~1500px → 5 headers / ~320px.
- **A vs-AI rematch** on the game-over overlay ([`useRematch.ts`](../web-client/src/components/lobby/useRematch.ts)).

Three server behaviours the applier depends on, all verified against `LobbyHandler`:

1. `handleUpdateLobbySettings` is **already ordered for a whole bag** — its own comments read "apply
   after format change" and "apply after boosterCount", and the format branch honours a `setCodes` in
   the same message. A `format` change resets `boosterCount`, `picksPerRound` and `chaosBoosters`, so
   **field-by-field replay would be wrong**, not merely slower.
2. `cubeCards` resolves immediately and `return`s on a card it can't find, discarding the rest of the
   message. The cube goes first, alone.
3. `boosterCount == 6` is a **sentinel** on the *create* message meaning "use the format default" (3
   packs for a draft). A captured 6 is restated in the bag, where the same field is read literally.

Queued settings flush on `onLobbyCreated` rather than firing straight after the create: the server
keys `updateLobbySettings` on `identity.currentLobbyId`, which is only set once `handleCreate` has run.

**Deviations from the plan, both deliberate.**

- The plan had a wizard-made lobby open with *no* sets, on the grounds that `'ECL'` was arbitrary. It
  is more honest but a regression on the goal — every draft lobby would then need a set picked before
  Start could be pressed, and it broke the `draft-tournament` spec, which is the
  wizard-straight-to-Start path. It now opens on the **newest complete, non-extension set**, which is
  named in the title and the Sets chip the moment the lobby opens.
- No Seats row was distributed into a group: it had already been removed, since the lobby holds as
  many players as its shape allows and people join until it is full.

**Notes for the phases still open.** `applyRecipe` performs the same leave→create→configure sequence
`recreate` does, so **6b's `convertLobby` becomes a cheaper path inside it** when it lands. And gap #5
(the two `ranked` defaults disagree — `CreateTournamentLobby.ranked` is true, `TournamentLobby.ranked`
is false) no longer *shows*, because a recipe always states `ranked` explicitly — but the gap itself is
untouched, and the opt-in/opt-out product call this project deferred is still deferred.

### Phase 6, the wizard half

**Landed.** The wizard's answers are the URL:
[`components/ui/wizardUrl.ts`](../web-client/src/components/ui/wizardUrl.ts).

```
/                                         who with?
/play/group                               what with?
/play/group/draft-booster                 how?
/play/group/draft-booster/bracket         ready
/play/group/draft-booster/bracket?seats=4 ready, narrowed
```

**The path carries the answers, not the step number**, so the step stays derived from which answers are
present — the same derivation `PlayWizard` already did from `useState` — and `history.back()` is by
construction "drop the last answer". One user action = one history entry is the invariant that keeps
Back honest; a seat narrowing *replaces*, since it refines the current answer rather than adding one
(clicking through 8·7·6·5 would otherwise cost four Backs to escape). The stepper's pencil and the
browser's Back are now exact inverses, which is what Phase 7 broke and this fixes.

A side effect worth having: **a selection is shareable.** Nothing exists server-side until Create, so
the link is "here's what we're playing", not a lobby.

**Decoding validates**, the same way `loadLastSelection` re-checks `localStorage`: each answer is
checked against the one before it, so `/play/group/momir` renders step 2 with Momir disabled and its
reason rather than a dead screen. It also auto-resolves a one-answer step, which is what makes
`/play/solo/bring-a-deck` complete without the shape being written down.

**The rule that took a bug to find: normalisation may only ever *complete* a path, never shorten one.**
Truncating is a reachability verdict, and reachability depends on `aiEnabled`, which arrives with the
connection — so a truncating normalise races the socket. A cold `/play/solo/bring-a-deck` was being
rewritten to `/` before the server had said whether AI exists, silently destroying a shared link.
Keeping the link and explaining why it can't be used is both safer and better. (The first diagnosis of
this blamed `connectionHandlers`' `aiEnabled: msg.aiEnabled ?? false`. That is *not* the cause —
`MessageSender` sets `encodeDefaults = true`, so the field is always on the wire. Worth writing down,
because a store probe via `import('/src/store/gameStore.ts')` reads a **different module instance**
than the app's and reported `aiEnabled: false` throughout; the UI is the only reliable witness.)

**`App.tsx`'s address-bar sync no longer erases in-`/` paths.** Its reset arm sent *any* non-`/` path
back to `/` on every lobby state change, which ate the wizard's steps; it is now scoped to
`/tournament/...`. It still deliberately uses raw `history.replaceState` rather than `navigate()`,
because `/tournament/:lobbyId` is a real route rendering `TournamentEntryPage` — routing there would
unmount the app and drop the WebSocket. That constraint is the reason the rest of Phase 6 is not free,
and the comment now says so at the call site.

`wizardUrl.test.ts` walks the same space `modeMatrix.test.ts` does and asserts the round trip, one
distinct path per selection, the default seat count staying out of the query, and that decoding is
defensive about every way a hand-typed URL can be wrong.

### What is left of Phase 6

Two things, both **re-verified 2026-07-26**.

**`convertLobby` does not exist** anywhere in the server or the client; its only two mentions are the
comments at [`axisChoices.ts:13`](../web-client/src/components/lobby/axisChoices.ts) and
[`useLobbyCommands.ts:12`](../web-client/src/components/lobby/useLobbyCommands.ts) pointing here, which
is the seam it lands on. It is pure server work and does not belong to a client-only PR: the two lobby
models share no interface and no `kind` discriminator, and `lobbyId` ownership is how
`QuickGameLobbyHandler.handleJoin:241` decides which handler a code belongs to — so preserving the id
and the joined players across a conversion means atomically moving a lobby between an in-memory map and
Redis and re-associating every session. It overlaps gap #7.

**The other in-`/` screens still have no URL.** Lobby, draft, deckbuild, standings, game, game over and
spectate remain Zustand views under `*` → `App`. Two things make this harder than the wizard was, and
both are worth knowing before starting:

- They are **store-driven, not navigable**: the state comes from WebSocket messages, so a URL for
  `/game/:id` can only *reflect* what is showing. Entering one cold would need the socket to repopulate
  before the screen means anything.
- **The route table fights it.** `/tournament/:lobbyId` renders `TournamentEntryPage`, so `App` cannot
  `navigate()` there without unmounting itself and dropping the socket — which is exactly why the sync
  at `App.tsx:115–125` writes the address bar with raw `history.replaceState` and leaves React Router's
  location stale. Giving these screens real URLs means restructuring that route so one element owns both
  the bare and the `:lobbyId` form.

### What Phases 0/1/3 actually shipped

- **New files.** `components/lobby/axes.ts`, `components/ui/modePresets.ts`,
  `components/ui/SettingsLabel.tsx`, `components/ui/FullscreenButton.tsx`,
  `components/help/{HelpTip,HelpDrawer,HelpTopicView}.tsx` + `help.module.css`,
  `help/{topics,shortcuts,helpStore}.ts`, `pages/HelpPage.tsx` + module CSS.
- **Verified against the running stack.** All six presets walked from a cold home screen;
  "Multiplayer" lands on `Premade Decks Free-for-All` (the combination the plan calls out as
  supported-but-unreachable), "Variants" on a Momir Basic lobby, "Draft & Sealed" on a sealed lobby.
  The in-game drawer was opened from a real vs-AI game.
- **`ModePreset.launch`** is the seam onto today's two unrelated server lobby kinds. Phase 4/5 close
  it; until then the home screen stays declarative and only one function knows the mapping.
- **e2e specs updated.** The lobby/draft specs were already stale (they typed into a placeholder
  renamed some time ago); they now click the `mode-preset-draft-sealed` test id.
- **Resolved from Part 1's findings:** the phantom number-key comment at `useMultiplayerView.ts:64`
  (no such handler exists — comment corrected, feature not invented). Still open: dead
  `components/spectating/SpectatorView.tsx`, and the two duplicated deckbuilders / replay viewers.

### What Phase 2 actually shipped

- **`axes.ts` gained the half it was missing.** Phase 1 gave it the vocabulary; Phase 2 gave it the
  translation onto the two server lobby kinds — `axesFromLobbySettings`, `axesFromQuickGameLobby`,
  `tableFromGameMode` / `gameModeForTable`, `cardsFromTournamentFormat`, `eventFromGameMode`,
  `eventUnavailableReason`, the `*TopicId` helpers and a shared `LEGALITY_OPTIONS` derived from the
  deckbuilder's `DECK_FORMATS`. One module now knows the mapping, which is what Phase 4's view model
  will be built on.
- **`components/lobby/LobbyAxisSummary.tsx`** — Cards/Table/Event chips in *both* lobby headers,
  each with a `HelpTip` bound to the value in effect. It replaced the one-word `lobbyFormat` chip.
  Notably this is the first time a **non-host** can see what they joined: the settings panel is
  host-only.
- **Tournament lobby rows restructured**, not just relabelled:
  - `Format: Sealed|Draft|Premade` → `Cards: Bring a deck|Sealed|Draft`, with its sub-options
    (deck legality, sealed shape, draft shape) as indented rows directly beneath it. The deck-format
    dropdown moved up from the bottom of the panel to sit under the value it belongs to.
  - `Mode: Tournament|Multiplayer` + `Variant: FFA|2HG|Team` → one flat
    `Table: 1v1|Free-for-All|Two-Headed Giant|Team vs. Team`. The old pair made 1v1 a peer of
    "multiplayer" rather than of the three shapes, and hid the shapes behind a click.
  - New `Event: Single game|Round-robin bracket` row. Derived from Table today, so the unreachable
    value renders **disabled with the reason attached** (`eventUnavailableReason`) — that is the
    Phase 5 hole, made visible instead of hidden. `.settingsButton:disabled` got a style, which the
    already-disabled Winston/Grid/Commander buttons had been silently missing.
  - Draft "Normal" → **"Booster"**, matching `cardsLabel()` and the plan's taxonomy.
- **Quick lobby**: `FormatSelector` → `CardsSelector`; "Format" → "Cards"; the dropdown reads
  "Bring a deck — no restriction"; "or pick a custom format" → "or pick a variant".
- **`Games per matchup` is now gated on `event === ROUND_ROBIN`** (was `!isFfa`), so it stops
  appearing on 2HG and Team vs. Team tables where a single shared game made it a no-op.
- **New topic `axis-limits`** documents every combination that isn't wired up yet, cross-linked from
  `axes`, `ranked` and both event topics.

Verified against the running stack by walking the tournament lobby through Sealed → Bring a deck
(+ Modern) → Free-for-All → 2HG → Draft and the quick lobby through Bring a deck → Pauper → Momir,
asserting the header chips against the control state at each step. One honest bug surfaced and was
fixed doing this: the quick lobby's Cards chip read "Bring a deck" while the deck picker sat on its
Random tab. Random pool is per-player, not a lobby setting, so `axesFromQuickGameLobby` now takes
the viewer's seat and reports `RANDOM` when they have submitted an empty deck (the server's own
"roll me one" signal). A chip contradicting the control under it is precisely the drift this
vocabulary exists to remove.

Two things fixed on the way past, both reported by Vincent:

- **`/help` could not be scrolled.** `HelpPage.module.css` used `min-height: 100vh` with
  `overflow-y: auto` — with `min-height` the box grows to its content, so there is nothing to
  scroll, and `#root { height:100%; overflow:hidden }` just clips it. Now `height: 100vh`, the same
  fix `SetCompletionPage` and `adminUi` already document. The in-game drawer was never affected
  (fixed positioning + `flex:1; overflow-y:auto`).
- **`the-ring` and `speed` topics removed.** They explained MTG mechanics rather than what Argentum
  does with them, which is out of this project's stated scope. Neither had a call site — both were
  `/help`-only. The in-game `title=` tooltips on `TheRingBadge` and `SpeedGauge` are untouched, which
  is the right home for a mechanic explainer. `card-badges` stays: it answers "what is this label the
  client is drawing on my card", which is unanswerable from the card text.
- **Scenario Builder is now dev-only** on the home screen, alongside LLM Tournament — it drives
  `/api/dev/scenarios/*`, which a production server does not expose. The *route* stays open in both
  builds, because a replay's "share as scenario" link is a real `/scenario?s=` deep link.

### What Phase 4 actually shipped

**One screen.** `QuickGameLobbyOverlay` (454) and `LobbyOverlay` (1131) are deleted; every lobby is
[`components/lobby/LobbyScreen.tsx`](../web-client/src/components/lobby/LobbyScreen.tsx). They
already shared a stylesheet — `QuickGameLobbyOverlay`'s header comment said so — but not a line of
structure, and the behaviour had quietly diverged: only one had a fullscreen button, only one showed
a QR code, they disagreed about who could see the settings, and the axes were editable on one of
them. All of that is now one answer.

Three new modules, one job each:

- **`lobbyViewModel.ts`** — `UnifiedLobbyView` plus `fromQuickGameLobby` / `fromTournamentLobby`.
  Pure. Everything the two kinds genuinely disagree about is a *field* (`startModel`,
  `primaryAction`, `invitable`, `canAddAi`, `teams`, `ranked.available`) rather than a branch at
  every call site — which is what lets Phase 5 close the server gaps without touching the screen.
- **`axisChoices.ts`** — for every Cards / Table / Event value on *this* lobby: `DIRECT`,
  `RECREATE` onto the other backing kind, or `BLOCKED` with the reason. This supersedes `axes.ts`'s
  kind-blind `eventUnavailableReason`, which was removed.
- **`useLobbyCommands.ts`** — the write side of the same seam, including the recreate.

**The axes are now the lobby's primary control, on both kinds.** Before, a quick lobby had a
`Cards` dropdown and nothing else; the tournament lobby had all three rows. Now both show all three
rows and all five Cards values.

**Cross-kind switching (4b v1).** Values the current implementation can't express are marked `⇄`
and, on click, confirm before tearing the lobby down and recreating it on the other kind. The
confirm counts what is actually lost — the invite code always, joined humans and AI seats and
submitted decks only when there are any — rather than warning in the abstract. Two combinations
become reachable for the first time:

- **quick → tournament**: turn a vs-Friend game into a draft, a Free-for-All or a bracket without
  backing out to the home screen.
- **tournament → quick**: `Event: Single game` at a 1v1 bring-a-deck table, and `Cards: Momir Basic`
  / `Random pool`. Previously the only route to a 1v1 single game was knowing that the *home screen*
  button, not the lobby, was the way to get one.

Everything else stays visible and disabled with its reason attached, which is where the Phase 5
holes now show up: Momir and random pools are 1v1-single-game only, bracket play is 1v1 only, a
limited pool always runs as a bracket.

**`Random pool` is a real Cards value now, not just a chip.** It is the deck picker's Random tab —
per player, not a lobby setting — so `DeckPicker` grew an optional controlled `tab` / `onTabChange`
pair and the lobby hoists it. Selecting Random on the Cards row moves the picker; the picker moving
updates the row and the header chip. Phase 2 had to infer this from the server echoing back an empty
deck; now there is one source of truth. `RecreateSpec` carries a `deckTab` for the same reason —
without it, "Random pool ⇄" would land you on a lobby reading "Bring a deck".

**Also folded in:**

- `TournamentLobbySettings.tsx` holds what is genuinely tournament-only (sets, boosters, timers,
  commander preset, ban list, teams, attack mode, AI assist), ordered by what it belongs to rather
  than by when it was added.
- **The disabled start button explains itself.** The old reason string fell through to a bare "Need
  at least 2 players" for the exact-count shapes, so a Two-Headed Giant lobby holding three players
  offered a dead button and no explanation. Every branch of the seat rule now has a sentence.
- **Winston's booster cap was about to break.** Winston is a draft everywhere else but counts
  *boosters* capped at 16, not *packs* capped at 6; the extraction nearly folded it in with the
  drafts. Called out in a comment at the one place that cares.
- **The routing either/or is gone.** `GameUI.tsx` has one lobby branch (`quickGameLobbyState ||
  lobbyState`), and `HomeScreen` no longer routes to any overlay.
- Two new help topics' worth of change: `lobby-switching` explains `⇄`, and `axis-limits` was
  rewritten — "Event follows Table" stopped being true the moment 1v1 single game became reachable.
- `DEFAULT_LOBBY_SET_CODE` replaces the `'ECL'` that was hardcoded in `HomeScreen.launchPreset` and
  would have been hardcoded a second time in the recreate path.

**One honest bug fixed on the way past.** The server broadcasts a lobby closure to everyone still
listed in it, *including the host who closed it by leaving* — so a recreate landed you in a working
new lobby underneath a red "Host left the lobby" banner. `onQuickGameLobbyClosed` now ignores a
closure for a lobby the client has already cleared. This was always wrong (pressing Leave did it
too); the recreate just made it impossible to miss.

**Verified against the running stack**, walking: vs Friend → Cards Random↔Bring a deck (picker
follows) → Table Free-for-All (confirm → premade FFA lobby) → Table 1v1 (direct; Ranked and Games
per matchup appear, Attack disappears) → Event Single game (confirm → quick lobby) → Cards Sealed
(confirm → sealed lobby, shape sub-row, set chips, ban list) → Cards Random pool (confirm → quick
lobby *on the Random tab*) → vs AI → Cards Momir Basic → started a real Momir game. Reload mid-lobby
rejoins into the unified screen on both kinds. `npm run typecheck` and `npm run build` clean; no
console errors.

**Deviation from the plan's naming:** the descriptor module is `axisChoices.ts`, not `lobbyAxes.ts`
— sitting next to the existing `axes.ts`, a name differing only by a prefix would have been a
coin-flip every time you went looking for one of them.

### What Phase 7 actually shipped

**Two modules.** [`components/lobby/modeMatrix.ts`](../web-client/src/components/lobby/modeMatrix.ts)
is the cold-start reachability model — the pre-lobby twin of `axisChoices.ts`, with `RECREATE`
replaced by "create the right kind first time". [`components/ui/PlayWizard.tsx`](../web-client/src/components/ui/PlayWizard.tsx)
only renders it. `modePresets.ts` and the six `preset-*` topics are deleted: `ModePreset.launch`'s
six hand-written server mappings became one `resolveLaunch` derivation, so a new Cards or Table value
no longer needs a home-screen change.

**A test asserts the two surfaces agree.** `modeMatrix.test.ts` walks every selection the wizard can
offer — roster × Cards × sub-shape × shape × seat count — resolves each one, and checks the lobby's
own projection (`axesFromLobbySettings` / `axesFromQuickGameLobby`) reads back the triple that was
asked for. It also asserts no selection asks for AI seats where `handleAddAiToLobby` would reject
them. This is the safeguard the plan called for in § 3d, and it is what makes keeping the two
modules separate safe.

**Refinements after seeing it running** (all four reported by Vincent):

- **The answers are a numbered stepper, not chips.** The first cut floated small chips to the right
  of the step title; nothing said they were previous answers, that they were revisitable, or what
  order they came in. Now all three questions are always on screen as `1 WHO WITH · 2 WHAT WITH ·
  3 HOW`, each with its answer underneath — a dashed-underlined button with a pencil when it can be
  changed, plain text when it is the step you are on, and `auto` when it was decided for you.
- **Every option says whether it is one game or an event.** Cards tiles carry `Play right away` or
  `Build a deck first`; shape tiles carry `One game` or `Several rounds · standings` — the
  distinction the old `Multiplayer | Tournament` pair blurred, since both were "multiplayer" and only
  one was multi-game. Once the selection is complete a flow line spells the whole thing out:
  *Open boosters → Build a deck → Everyone plays everyone (8 players) → Standings*. Naming a mode
  cannot tell you how many steps it has; this can.
- **Seats are open by default.** `maxPlayers` is a cap, not a quorum — `startBlockReason` only ever
  counts the players actually present — so the wizard defaults to the maximum, captions it as a
  limit you can start before reaching, and offers the numbers as an optional narrowing. To make
  "changeable in the lobby" true, `TournamentLobbySettings` gained a **Seats** row: the server has
  always accepted `maxPlayers` on `updateLobbySettings` and no client could send it.
- **The landing panel gives space back to the artwork.** `.homeTiers` is 760px → 660px with tighter
  tile padding. An earlier attempt made the glass itself translucent; that washed the panel out
  against a bright background and was reverted — the style is unchanged, only the footprint.

**One latent bug fixed on the way past.** `useLobbyCommands.recreate`'s `setDeckTab(spec.deckTab)`
was a no-op. Leaving nulls the store slice synchronously while the create only sends a message, so
`LobbyScreen` unmounts in between and the promised Random tab never arrived — "Random pool ⇄" landed
on a lobby reading "Bring a deck". Both the recreate and the wizard now hand the tab to the *next*
screen through `pendingDeckTab.ts`, consumed in `LobbyScreen`'s `useState` initialiser.

**Commander limited was capped at two players for no reason.** Reported by Vincent, who asked why. The
client had required exactly two since the formats shipped (`06d8df91b5`, comment: *"multiplayer
commander is a separate project"*), and that conflated sharing a **pool** with sharing a **game** —
an eight-player Commander Draft is a bracket of 1v1 Commander matches, which has always worked. The
server never had the restriction: `LobbyHandler.kt:605-616` caps Winston, Grid, 2HG, Teams and FFA and
puts everything else at 2–8, its start guard only asks for two players, and Commander Draft runs
through the plain draft path. So `cardsSeatCap` now reads 8 for both Commander shapes.

The two things that genuinely *aren't* supported now say so instead, which is the point:

- **Commander at a multiplayer table** — `COMMANDER_LIMITED_NEEDS_A_1V1_TABLE`, blocked on the Table
  axis and on the wizard's shape step. The plumbing accepts it (`FreeForAllHandler.kt:64,84` stamps
  the Commander engine format for a pod) but the rules work is `commander-format.md` Phase 3.
- **Commander with AI seats** — `COMMANDER_LIMITED_HAS_NO_AI`. `buildAiSealedDeck` builds a 40-card
  deck and calls `submitDeck` with no commander, and `TournamentLobby.validateDeck` doesn't check for
  one, so the AI wouldn't be rejected — it would sit down in a Commander game *without a commander*.
  Silently wrong is worse than blocked, so `canAddAi` excludes it and the wizard disables the
  Commander sub-shapes under "Just me".

**This exposed a gap in the skip rule.** Auto-resolving a one-answer step hides the disabled tiles and
with them their reasons — fine for "a solo pod plays a bracket", not fine here, where the reason *is*
the answer to "why can't eight of us play Commander together". The stepper now prints the skipped
step's reasons under the title (`wizardAutoNote`) rather than leaving them in a tooltip on a tile that
is no longer rendered.

**Two capabilities became reachable that were implemented but effectively hidden:**

- **A solo draft pod against up to 7 AI drafters.** `handleAddAiToLobby` caps only on `isFull`, so
  the wizard's `Just me → Draft` fills the pod and plays the bracket out. Same class of finding as
  Part 2's "4-player FFA with my own deck". Verified: a 4-seat pod created 3 AI drafters and offered
  Start Draft.
- **Limited at a multiplayer table.** `SEALED` + `gameMode = FREE_FOR_ALL` is a legal lobby, so
  Phase 4's "a limited pool always runs as a bracket" is only true at a *1v1* table.

**Verified against the running stack.** `Just me → Draft → Booster` (step 3 auto-skipped, pod size 4
→ real lobby with 3 AI seats, Start Draft live) · `A group → Bring a deck → Free-for-All` (seats 3–6)
· `A group → Sealed → Standard → bracket` (Commander Sealed correctly disabled at a group;
`Seats 8 → 4` in the lobby moved the player count to 1/4) · `A friend → Random pool` (step 3 skipped;
quick lobby opens with the Cards chip on **Random pool** and the deck picker on its Random tab) ·
`Play again` chip replays the last selection. `npm run typecheck`, `npm run build` and all 189 unit
tests clean; no console errors.

**e2e.** The three tournament specs moved onto
[`e2e-scenarios/helpers/homeScreen.ts`](../e2e-scenarios/helpers/homeScreen.ts) — `enterName`,
`createLobby(choice)`, `joinLobby` — rather than each hard-coding the landing screen's test ids and
placeholder copy. Two consecutive landing changes had already rewritten them; a third should be one
edit. `draft-tournament.spec.ts` also stopped creating a sealed lobby and switching the format,
which was only ever a workaround for the old "Draft & Sealed" card.

### Polish pass

Closing the loose ends the phases left behind, so the PR lands clean rather than carrying a tail of
"noticed but not touched". No new features; four things.

**The help registry now has an integrity test.** `topicById` returns `undefined` for an unknown id and
`HelpTip` then renders *nothing* — so a typo or a renamed topic silently deletes the `?` button
instead of failing. "One content source, two surfaces" only holds if something enforces it, and
nothing did. [`help/topics.test.ts`](../web-client/src/help/topics.test.ts) asserts unique ids, every
topic in a declared section *and* rendered by some `/help` section, every `related` and `shortcuts`
cross-reference resolving, no self-relation, and — the part that matters for the axes — that all six
`*TopicId` helpers land on a real topic for **every value in their closed domain**. `modeMatrix`
gained `SHAPE_IDS` so the test walks the domain rather than restating it. All 12 assertions pass
today: this is a guard, not a bug fix.

**`GameBoard`'s conditional hook is fixed at source**, which Part 1 flagged twice as worth doing. Its
one early return sat *above* the `manaProgress` `useMemo`, so mounting the board against an empty
store ran one fewer hook on the first render and React aborted the tree with *"Rendered more hooks
than during the previous render"*. Both old call sites dodged it by accident and `ReplayPlayer` had to
gate its mount explicitly. The memo only ever read `manaSelectionState` and `viewingPlayer?.manaPool`,
both resolved well above the guard, so the move is mechanical — and the guard now carries a comment
saying why nothing may go below it.

**The lobby's Cards row stopped orphaning its last value.** Phase 4 put all five Cards values on both
kinds, making that row the widest control in the panel: it wants 441px next to a 46px label inside
508px, and misses by 11. `.settingsRow` was a single unbreakable flex line, so flex *shrank* the group
instead of breaking it — and since `.settingsButtons` is right-aligned, the overflow landed alone on a
second line. A five-value row rendering as 4 + 1 reads as a bug, not a wrap. `flex-wrap: wrap` fixes
it, because flex breaks lines on base sizes *before* it shrinks; it also degrades properly, dropping
the group to its own full-width line if the row ever genuinely outgrows the panel — which it will,
since Cube adds a sixth Cards value.

**151 lines of CSS this branch orphaned, deleted.** `customFormat*` (8 keys) and `formatSelector*` /
`formatOrDivider` / `formatDropdownInactive` went with `QuickGameLobbyOverlay`; `statusBox` with the
old tournament status panel; `wizardRecap` is Phase 7's own leftover from the chips-to-stepper change.
Each was confirmed dead by scanning every `styles.<key>` in the tree *and* confirmed live on `main`,
so only this branch's litter is removed — the ~13 keys that were already dead before it are left
alone. Two `className` references that resolved to `undefined` and rendered a literal `"undefined"`
class went too: `standingsReady` (the cell styles itself inline) and `setPickerMinCardsLabel`. Both
predate the branch; the PR body had listed the first as untouched.

### Part 1 duplication, closed

Both of Part 1's remaining findings are resolved.

- **`components/spectating/SpectatorView.tsx` deleted** — 1025 dead lines, superseded by
  `SpectatorGameBoard.tsx`. Its only mention anywhere was its own `export`.
- **The two replay viewers now share one playback surface**,
  [`components/replay/ReplayPlayer.tsx`](../web-client/src/components/replay/ReplayPlayer.tsx):
  transport, scrubber, share/scenario/snapshot actions, `SpectatorContext` and the board.

  The two *entry points* deliberately stay separate, because they are different things: the route is
  a shareable URL that loads a public replay by id; the overlay is an in-app screen that lists games
  and **must not navigate**, since routing away drops the WebSocket. Everything after "here are the
  frames" is now shared. `ReplayPage` is 494 → 143 lines (route, fetch, loading/error, team
  stamping); `ReplayViewer` is 733 → 354 (list + fetch plumbing), and its dead style keys went with
  it. 1227 lines → 920, with the surface existing once.

  The copies had already drifted, and merging onto the route's better version means **the overlay
  gains** replay metadata, the archived-frames badge, `stateReproducible` gating, multiplayer seat
  labels instead of "Alice vs Bob", and the "Share as scenario" / "Save snapshot" buttons it never
  had. `SpectatorStateUpdate` also moved out of `components/admin/` into
  `replay/reconstructSnapshots.ts` — the public route was importing its core wire type from the
  admin overlay.

  > **Found while doing this:** `GameBoard` calls a **different number of hooks** depending on
  > whether the store holds spectating state, so mounting it before frame 0 lands crashes React with
  > *"Rendered more hooks than during the previous render"*. Both old call sites avoided it by
  > accident, writing frame 0 in the same batch that revealed the board. `ReplayPlayer` now gates the
  > mount explicitly (`primed`), but **the conditional hook in `GameBoard` is still there** and will
  > bite the next thing that mounts it against an empty store. Worth fixing at source.

**Deliberately not done:** merging the two deckbuilders. `/deckbuilder` (constructed) and
`sealed/DeckBuilderOverlay` (limited pool) solve genuinely different problems, and Vincent's call is
that the duplication is not worth paying down.

### Deliberate deviations from the plan above

- `HelpTopic.body` is a small `HelpBlock` union (`p` / `ul` / `shortcuts`) rather than `ReactNode`,
  so `topics.ts` stays a plain data module both surfaces render and a test can walk.
- Mode preset cards are a `div` wrapping a `button`, not one big `button` — `HelpTip` is itself a
  button and nesting interactive elements is invalid HTML.
- The "Variants" preset opens **Momir vs a friend**. Momir vs AI stays reachable via the vs-AI
  lobby's format selector, exactly as before; it is not a second card.

---

## Critical files

**Client**

- `web-client/src/components/ui/GameUI.tsx` — 2992 lines. `ConnectionOverlay` 109–533,
  `LobbyOverlay` 763–1777, `PremadeDeckPickerPanel` 1778–1882, `FreeForAllOverlay` 1883–2069,
  `TournamentOverlay` 2080–2603.
- `web-client/src/components/ui/QuickGameLobbyOverlay.tsx` — 456 lines.
- `web-client/src/components/ui/GameUI.module.css` — 68 KB; the landing + lobby + tournament design
  language, already shared by both lobbies.
- `web-client/src/main.tsx` — route registration.
- `web-client/src/store/slices/quickGameLobbySlice.ts` (90), `lobbySlice.ts` (203), `types.ts:581–593`.
- `web-client/src/store/slices/handlers/quickGameLobbyHandlers.ts` (27), `lobbyHandlers.ts` (299).
- `web-client/src/types/messages.ts` — `LobbySettings` 1259–1297, `QuickGameLobbyStateMessage` 2711–2727.
- Tokens and primitives to build on: `styles/variables.css`, `styles/responsive.css`,
  `components/shared/Button.tsx`.

**Server** (Phase 5 only)

- `game-server/.../lobby/QuickGameLobby.kt`, `TournamentLobby.kt`, `MomirBasicSetup.kt`,
  `QuickGameLobbyRepository.kt`
- `game-server/.../handler/QuickGameLobbyHandler.kt`, `LobbyHandler.kt` (AI guards 1386–1403,
  2080–2088; premade start 1304–1335; settings update 2072–2118), `FreeForAllHandler.kt` (47–111)
- `game-server/.../protocol/ClientMessage.kt` (tournament 152–344, quick 468–575), `ServerMessage.kt`
  (`LobbySettings` 446, `LobbyUpdate` 510, `QuickGameLobbyState` 1092)
- `game-server/.../persistence/LobbyConverter.kt`

---

## Verification

Per `web-client/AGENTS.md`, UI changes need real data — run the stack, don't mock.

1. `GAME_DEV_ENDPOINTS_ENABLED=true ./gradlew :game-server:bootRun --args='--spring.profiles.active=local'`
   (~90 s cold) + `npm run dev`. The dev server falls back to :5174+ if 5173 is taken — read the
   actual port from its log.
2. `npm run typecheck` and `npm run build` in `web-client/` after each phase.
3. **Mode matrix walk** — from a cold home screen, reach each of: vs AI · vs Friend (code and QR) ·
   Sealed 1v1 · Booster Draft 8-player bracket · FFA 4-player with own decks · 2HG · Team vs Team ·
   Momir · Tournament with standings. Confirm the axis labels shown match what was clicked. Record
   which combinations remain blocked by the Phase 5 gaps.
4. **Back-button walk** — home → lobby → game → game over. Document current behaviour as the
   baseline for Phase 6.
5. **Screenshots** (required for UI PRs): Playwright via system Chrome
   (`chromium.launch({ channel: 'chrome' })`), reuse the `playwright` already in `node_modules`
   (CommonJS import), `deviceScaleFactor: 2`, `waitForTimeout(~800)` so webfonts paint. Capture:
   home, unified lobby in three axis states, `/help` index, a `HelpTip` popover open. Host on a
   throwaway flat `<feature>-screenshots` branch per the AGENTS.md git-plumbing recipe — do not
   commit PNGs to the feature branch.
6. **E2E** — `e2e-scenarios/`; the existing lobby and draft specs are the regression net for phases
   0, 2 and 4. Set `E2E_BASE_URL` when running against a worktree dev server.
7. **Server phases** — `just` gates only, never raw `./gradlew` (root `AGENTS.md`); use the `verify`
   skill to pick the suite.

## Out of scope

- Rules teaching for players new to Magic (what a phase is, the stack, mulligans as a concept).
- A first-run guided tour / step-through overlay.
- A persistent nav bar — the glass card stays.
- Promoting hotseat to a real Table value; it stays a debugging affordance.
- Cube — it slots into the Cards axis when [`cube-draft-format.md`](cube-draft-format.md) lands.
- Merging the two deckbuilders. Noted in Part 1 as duplication, but the constructed builder and the
  limited-pool builder solve different problems — decided 2026-07-26 not to pay it down. (The two
  *replay viewers* were merged; see § *Part 1 duplication, closed*.)
