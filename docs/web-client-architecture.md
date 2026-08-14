# Web Client Architecture

## Overview

The Argentum Engine web client is an MTG Arena-style 3D browser application built with React and React-Three-Fiber. It follows a **"dumb terminal" architecture** - the client contains no game rules logic, only renders server state and captures player intent.

## Technology Stack

| Technology | Purpose |
|------------|---------|
| React 18+ | UI framework |
| React-Three-Fiber (R3F) | Three.js declarative wrapper |
| @react-three/drei | R3F helpers (Text, OrbitControls, etc.) |
| Zustand | State management |
| Framer Motion 3D | Animations |
| TypeScript 5 | Strict typing |
| Vite | Build tool |

## Architecture Principles

### 1. Dumb Terminal Pattern

The client is purely presentational:
- **No game rules** - Server validates all actions
- **No state computation** - Server sends complete game state
- **Intent capture only** - Client sends player clicks/selections to server

### 2. Server Authority

All game logic lives on the server:
- Client requests actions → Server validates → Server sends new state
- Legal actions list comes from server, not computed locally
- Animation events come from server event stream

### 3. Optimistic UI (Future)

For responsiveness, we may later add:
- Immediate visual feedback on clicks
- Rollback if server rejects action

## Data Flow

```
┌─────────────┐     WebSocket      ┌─────────────┐
│   Server    │◄──────────────────►│   Client    │
│             │                    │             │
│ GameState   │────stateUpdate────►│ Zustand     │
│ Events      │────events─────────►│ Store       │
│ LegalActions│────legalActions───►│             │
│             │                    │             │
│             │◄───submitAction────│ User Click  │
└─────────────┘                    └─────────────┘
```

## WebSocket Protocol

### Connection Flow

1. **Connect** → `{ type: "connect", playerName: "Alice" }`
2. **Connected** ← `{ type: "connected", playerId: "p1" }`
3. **Create/Join** → `{ type: "createGame", deckList: {...} }`
4. **Game Started** ← `{ type: "gameStarted", players: [{ playerId, name, seatIndex, isYou, isAi }, …] }`
   (N-player seat roster from this recipient's perspective; "the opponent" is the non-`isYou` seat
   in a 2-player game)
5. **Mulligan Phase** ↔ `mulliganDecision` / `keepHand` / `mulligan`
6. **Game Loop** ← `stateUpdate` with state, events, legalActions

### Server Messages (ServerMessage)

| Type | Description |
|------|-------------|
| `connected` | Connection confirmed with player ID |
| `gameCreated` | Game created, waiting for opponent |
| `gameStarted` | Both players connected, game beginning |
| `stateUpdate` | Game state + events + legal actions |
| `mulliganDecision` | Player must keep/mulligan |
| `chooseBottomCards` | Player must choose cards for bottom |
| `mulliganComplete` | Mulligan phase finished |
| `gameOver` | Game ended with winner/reason |
| `error` | Error with code and message |

### Client Messages (ClientMessage)

| Type | Description |
|------|-------------|
| `connect` | Connect with player name |
| `createGame` | Create game with deck list |
| `joinGame` | Join game with session ID + deck |
| `submitAction` | Submit a GameAction |
| `keepHand` | Keep current hand |
| `mulligan` | Take a mulligan |
| `chooseBottomCards` | Select cards for library bottom |
| `concede` | Concede the game |

## State Management (Zustand)

### Store Structure

```typescript
interface GameStore {
  // Connection state
  connectionStatus: 'disconnected' | 'connecting' | 'connected';
  playerId: string | null;
  sessionId: string | null;

  // Game state (from server)
  gameState: ClientGameState | null;
  legalActions: LegalActionInfo[];

  // Mulligan state
  mulliganState: MulliganState | null;

  // UI state (local only)
  selectedCardId: EntityId | null;
  targetingMode: TargetingState | null;

  // Animation queue
  pendingEvents: ClientEvent[];

  // Actions
  connect: (playerName: string) => void;
  createGame: (deckList: Record<string, number>) => void;
  joinGame: (sessionId: string, deckList: Record<string, number>) => void;
  submitAction: (action: GameAction) => void;
  selectCard: (cardId: EntityId | null) => void;
}
```

### Selectors

Memoized selectors extract derived state:

```typescript
// Get cards in a specific zone
const selectZoneCards = (zoneId: ZoneId) => (state: GameStore) => ...

// Get legal actions for a card
const selectCardLegalActions = (cardId: EntityId) => (state: GameStore) => ...

// Check if it's the player's turn
const selectIsMyTurn = (state: GameStore) => ...
```

## Component Hierarchy

```
App
├── GameScene (R3F Canvas)
│   ├── Camera
│   ├── Lighting
│   ├── Table
│   ├── OpponentArea
│   │   ├── Hand (face-down)
│   │   ├── Library
│   │   └── Graveyard
│   ├── Battlefield
│   │   ├── OpponentLands
│   │   ├── OpponentCreatures
│   │   ├── PlayerLands
│   │   └── PlayerCreatures
│   ├── Stack
│   ├── PlayerArea
│   │   ├── Hand
│   │   ├── Library
│   │   └── Graveyard
│   └── TargetArrow
├── GameUI (2D overlay)
│   ├── PhaseIndicator
│   ├── LifeCounters
│   ├── ManaPool
│   ├── ActionMenu
│   └── MulliganUI
└── EventEffects
    ├── DamageEffect
    └── DeathEffect
```

## In-game deck tracker

Clicking your own Deck pile (or pressing `D`) opens `board/DeckBrowser.tsx`, a two-tab overlay:

- **Deck list** — your decklist with a `remaining/copies` count per card, fully-drawn rows dimmed,
  next-draw odds, and the deckbuilder's mana curve and colour pips. It renders
  `ClientGameState.deck` through `components/deck/GameDeckView.tsx`'s `DeckCardBody`, the same
  component the profile and admin deck viewers use — a `DeckViewCard` is just a recorded
  `GameDeckCard` plus an optional `remaining`, and supplying it is what switches rows into tracker
  mode.
- **Library order** — the pre-existing library view (top to bottom, card backs for anything not
  revealed to you).

Everything shown comes from the server: `deck` is populated only for `viewingPlayerId` and is empty
for spectators, so an opponent's Deck pile has no deck-list tab at all and the client never has to
decide what to hide. See `data-contracts.md` §B3 for the payload and its two masking rules — in
particular that `remaining` means "copies you haven't seen", which is not always the same as
"copies in your library".

### Face-up top card

The Deck pile itself renders its top card face up — with an amber ring, an 👁 badge and the normal
hover preview — whenever the server sent details for entry **0** of that library's `cardIds`. The
library zone is always transmitted in full (opaque ids for unknown cards), so position is all the
client needs; the decision about *which* cards carry details is entirely the server's, and it makes
it for a public reveal (Future Sight, Goblin Spy), a private peek ("you may look at the top card of
your library any time"), and a scry/surveil the viewer just performed alike. `ZonePiles.tsx` never
asks why. `TopOfLibraryClientViewTest` pins both halves of the contract: position 0 is the top, and
the private peek stays out of an opponent's view.

## What a card costs (one card, several prices)

Three surfaces answer "what does this cost?" and they all read the same list, built by
`utils/actionOptions.ts`'s `buildActionOptions(card, legalActions)`:

- **`ActionMenu`** — one clickable button per option. The full ladder, since this is where the player
  commits.
- **`CardPreview`** — the same ladder, read-only, under the hovered card ("Ways to play"), plus the
  cost badge on the image. Unaffordable rows stay listed and dimmed.
- **`GameCard`** — the badge on the card in hand. Card-sized, so it shows only the two ends of the
  span via `playCostRange`.

Sharing the builder is load-bearing. A card usually has several prices — each face of a split or
adventure card is its own `CastSpell` action, kicker/morph/impending are their own action types, and
convoke, delve, waterbend, harmonize and emerge all sit above a floor the server sends separately
(`minimumManaCostString`, or `additionalCostInfo.costAfterSacrifice` for emerge). Each surface used to
pick a "normal" cast out of `legalActions` with its own hand-maintained list of `actionType` strings to
exclude, so the three disagreed, and a card whose only cast was an adventure face or a kicker showed
its printed cost with no sign that the printed cost wasn't the price.

Two rules for `playCostRange`: the **low** end applies each option's reduction floor, the **high** end
deliberately doesn't (the top of the range is what a cast *asks* for before you spend anything on it);
and only options that actually put the card into play count. Cycling, plotting and suspending are
things you do *instead of* playing the card, so folding their costs in would make a {1}{G} creature
with cycling {W} read as a "{W}-to-{1}{G}" spell — they still get their own ladder row.

## "Won't untap" cue

Three server signals mean one thing to a player reading the board — `DOESNT_UNTAP` and
`CANT_BECOME_UNTAPPED` in `ClientCard.abilityFlags` (both ride the projected keyword set, the same
one the untap step gates on) and `ClientCard.isExerted` (CR 701.43a). They collapse into a single
cue: `components/game/card/untapRestriction.ts` picks the strongest, `GameCard` pins a frost padlock
badge, and a tapped-and-restricted permanent additionally gets `styles.untapLockedOverlay` — an
inset rime rim that separates "frozen" from the ordinary darkening every tapped permanent wears.

Two things keep it honest. The restriction shows on *untapped* permanents too, because "tapping this
is one-way" is the read you need before crewing or attacking with it. And the frost is deliberately
pale, never `TARGET_COLOR`'s saturated cyan, and inset where targeting glows outward — otherwise a
locked permanent looks like a legal target. Stun counters stay out of the ladder: their own counter
badge already carries a count, which says more than a padlock would.

A restriction implemented *outside* the layer system (read directly by a manager, as the block
restrictions are) would stop the untap while leaving the card visually identical to one that untaps
normally. `UntapRestrictionVisibilityTest` pins the projected-keyword contract this depends on.

## Battlefield card grouping (token quantity aggregation)

Identical permanents on one player's board collapse into a single visual **stack**
instead of one card each — the display-layer half of "token quantity aggregation"
(`backlog/number-explosion-safety.md`, Option B). The engine stays strictly *one
entity per permanent* (the crash/overflow ceiling is enforced separately by
`GameLimits`, Option A); aggregation is purely a rendering concern and lives in
the client, where the divergence axes (counters, P/T, tap, damage, combat,
chosen mode, class level, badges, …) are already client state.

- **`store/cardGrouping.ts`** — pure, store-free module. `computeCardGroupKey(card)`
  produces a key that two cards share *only* when their entire projected status is
  identical; the instant one is buffed, tapped, attacks, gains a counter or an
  attachment, its key changes and it splits back into its own group.
  `groupCards(cards)` returns one `GroupedCard` per key — **however large** —
  carrying `count`, every member `cardIds` (for action handling), and the member
  `cards`. (Re-exported from `store/selectors.ts` for existing call sites.)
- **Bounded render depth** — `CardStack` paints at most `MAX_VISUAL_STACK_DEPTH`
  (4) overlapping layers and shows a `×N` count badge on the front card when
  members are hidden behind the cap. So a horde of 10,000 identical tokens renders
  ~4 DOM nodes plus a badge instead of 10,000 — what previously made huge boards
  freeze the client (groups used to be *split* into `ceil(N/4)` stacks, all
  rendered). `Battlefield.tsx`'s slot-sizing footprint math counts the capped depth
  (`visibleStackDepth`), not the raw count, so a horde can't drive cards to the
  absolute-minimum size.
- **Interactivity is preserved** — every member still has a server-sent legal
  action and lives in `GroupedCard.cardIds`; only the *rendering* is capped. The
  members hidden behind the cap are identical, so targeting/sacrificing "one of
  them" via a rendered layer is equivalent.
- **Targets split out** — `groupCards(cards, splitOutIds)` forces a permanent that
  is a chosen target / triggering source of a stack object (or a mid-cast selected
  target — `useSplitOutTargetIds`) to render on its own card, so its
  `data-card-id` anchor exists for `TargetingArrows`. Without this, a targeted token
  hidden behind the cap would silently drop its arrow. This mirrors why attackers /
  blockers already split out of a group (they too drive distinct arrows). Eligible-
  but-unchosen targets stay collapsed — identical tokens are interchangeable, so
  clicking the representative picks one.

Deliberate non-goals (see the backlog): the wire still carries one DTO per entity
(`StateDelta` already sends only changed cards, so steady-state traffic is fine),
and no `quantity` field is added to the `ClientCard` contract — that would couple a
presentation concern to the engine and add delta churn. Aggregation belongs in the
layer that renders.

## Multiplayer (3-4 player) board

A game with more than two seats turns on the multiplayer chrome; a 2-player game renders
exactly the classic layout (no rail, no strip, no seat colors — the multiplayer code paths
are gated on `players.length > 2`).

- **One viewed opponent + opponent rail.** The opponent half shows exactly one board at
  full 2-player scale; the other opponents' boards live in a horizontally sliding strip
  (`OpponentBoardArea`, one cell per living opponent, ordered by turn order after you) and
  slide into view when selected. The always-visible `OpponentRail` (fixed at the top; its
  height is added to the board's top offset) carries one chip per opponent: seat color,
  name, life (also the floating ±delta anchor via `data-life-display`), hand count, poison,
  commander-damage warning, active-turn ring, priority dot, deciding spinner
  (`opponentDecisionStatus.playerId`), attention pulses, and a tombstone once a player has
  left the game. The *viewed* opponent additionally keeps a full-size life orb in the
  center HUD (seat-tinted to match their chip) — the familiar, biggest click target for
  targeting and defender assignment. Anchors (`data-player-id` / `data-life-id` /
  `data-life-display`) are carried by exactly one element per player: the orb for the
  viewed opponent, the cell's name plate for every other board visible in a shared-strip
  view (the plate is also a defender-assignment / player-target click target), and the
  rail chip only while the board is off-screen — never more than one, so arrows, damage
  floats, and player-target clicks resolve unambiguously.
- **Board switching**: rail-chip click (pins; re-click unpins), keyboard `1`/`2`/`3`,
  horizontal swipe. Follow-the-action (`useMultiplayerView` + the `boardView` slice:
  `viewedOpponentId`, `viewPinned`, `followAction`) slides automatically on coarse
  boundaries — an opponent's turn starting, the attacker's board when you're attacked, the
  priority seat in hotseat — and is refused inside `followViewTo` while any input is
  pending (the camera never moves under an in-progress selection).
- **Table overview** (`boardView.overviewMode`, rail toggle or key `0`; desktop/tablet
  only — phones keep the focused camera). **Every 3+ player game opens on it** — the
  one-board camera hides most of a pod — and `GameBoard` re-defaults once per game
  (keyed on the session id), so focusing a single board sticks for the rest of that game
  but the next one starts on the overview again. Every living opponent's board shares the strip
  side-by-side instead of the one-board camera — cells split the width evenly (padded
  clear of the fixed rail via `railReservedWidth` and of the Fullscreen/Concede row),
  hand fans hide (chips carry the counts), and the per-slot card sizer shrinks cards to
  fit. Each visible cell gets a seat-colored **name plate** (`BoardNamePlate` — the
  board's "face": name + life), and the **active player's** cell a subtle seat-colored inset
  ring (`activeTurnRingColor` — on the top row, the bottom row, and your own cell): with every
  board on screen at once, whose turn it is is the thing worth highlighting.
  Hidden boards stay mounted after the visible cells at full width, overflowing
  off-screen right, so their card anchors keep remapping to rail chips. Selecting a
  single board (chip click / `1`-`9`) exits back to the focused camera. Each cell can
  also be individually folded to a narrow seat-colored tab (MTGO-style: the "−" button
  next to the plate; the tab re-expands on click — `boardView.collapsedSeats`): the
  remaining boards split the freed width, and a collapsed seat's full board moves to
  the off-screen group so its anchors bundle back to the rail chip. Collapse state is
  overview-only — the focused camera and the combat split ignore it — and persists
  across overview toggles until the game resets. In a 3+ player game the overview is
  **two rows**, not one strip: `GameBoard` partitions the living seats into a top row (the
  opponent strip) and a bottom row (grid rows 4-5) around an *anchor* seat — a team game
  splits by team (your team at the bottom), a free-for-all balances evenly with the anchor
  at the bottom and the odd seat on top. The bottom row renders as a second strip of cells
  (`bottomHalf` — lands toward the bottom edge, per-board collapse) whenever it holds more
  than the anchor; a lone anchor keeps the classic single bottom board.
- **Combat defender-focus split** (`useCombatDefenderFocus`): when the server's confirmed
  combat is between two *other* players, the attacker's and defenders' boards share the
  strip so the fight renders as real arrows between real boards instead of a bundled
  arrow onto a rail chip. Entering the split respects the camera guards (follow on,
  unpinned, no pending input — `hasPendingInputSelection`); once active it holds for the
  whole combat so boards don't shift mid-fight.
- **Eliminated spectator** (`isViewerEliminated`, in the `boardView` slice): the layout is
  **derived from the roster** — the local seat is `hasLost` while two or more seats are still
  standing, in a non-hotseat 3+ player game — not from any message or click, so it holds
  however the seat died (conceding, damage, decking out, poison) and survives a reconnect.
  Alongside it the server sends a personal `PlayerEliminatedMessage` (from
  `GamePlayHandler.notifyEliminatedSeats`, once per seat, for *every* loss reason) which marks
  the defeat overlay `GameOverState.eliminated` and adds a "Keep Watching" button;
  `boardView.eliminatedSpectating` records only that the player took it, dismissing the overlay.
  The layout hides all action UI (hand/pass/undo/concede) behind a "spectating" banner + Leave
  Game button. An eliminated player is just an observer without a board of their own
  (`viewerIsObserver` = spectating ‖ eliminated), so they get the same two-row overview a
  spectator gets: the survivors face each other across the table, with a survivor's board
  holding their now-vacant bottom half — read-only board + face-down hand, that seat's
  life on the right center-HUD orb (which takes over their anchors from the rail chip),
  and their cell out of the opponent strip. `useEliminatedBottomSeatId` resolves which
  survivor that is: the banner's **bottom-seat picker**
  (`boardView.eliminatedBottomSeatId`), else a living teammate (team game), else the next
  living seat in turn order — so it self-heals when that seat dies. Only when no survivor
  is left do grid rows 4-5 collapse to 0 and the freed height flow to the opponent strip.
- **Seat identity**: `styles/seatColors.ts` (Okabe-Ito, by seat index = turn-order index in
  `gameState.players`) colors rail chips, combat arrows and chevrons, stack item borders
  (caster), and log entry names.
- **Targeting across seats**: a chip gets a halo when the in-progress selection has valid
  targets on that opponent's board, and a crosshair badge when the player themself is a
  valid target (badge click targets; chip body click only switches the view — a view
  change never cancels a selection).
- **Combat**: with >1 possible defender, the first attacker selection pops a defender pick;
  assignment is sticky (`CombatState.stickyDefenderId`) and per-creature reassignable via
  rail-chip clicks, the viewed opponent's life orb, or the chip's planeswalker flyout. Confirm is disabled until every
  selected attacker has an explicit defender. When exactly one player is a legal attack
  target (attack left/right — CR 803.1, last opponent standing) the sticky defender is
  pre-assigned so the popup never asks; a restriction banner names who can legally be
  attacked (phrased with the lobby's `attackMode` when known) and rail chips of
  unattackable living seats dim with a 🚫 marker. Arrows against the viewed defender render
  per-creature in the defender's seat color; attacks on boards visible in a shared-strip
  view end on the defender's name plate; attacks on off-screen boards bundle
  into one arrow to the defender's rail chip with a creature-count badge (`CombatArrows`),
  and any card anchor on an off-screen board remaps to its controller's chip (also in
  `TargetingArrows`). While you declare blocks, attackers aimed at other defenders render
  dimmed (CR 509.1b — `CombatState.actingSeat` scopes `attackingCreatures` to attacks on
  you).
- **The transform trap — overlays inside the strip portal to `<body>`**: the strip
  track's `translateX` (and any other transformed ancestor, e.g. a tapped card's
  rotation) turns a `position: fixed` descendant's containing block into that ancestor —
  the "fixed" element then renders relative to the (clipped, possibly off-screen) cell
  instead of the viewport. **Checklist: anything rendered under `OpponentBoardArea`
  (hand `CardRow`, `CommandZone`, `Battlefield`, `ZonePile`, and everything inside
  `GameCard`) that is full-screen or positioned in viewport coordinates must go through
  `createPortal(..., document.body)`.** Current portals: the
  graveyard/exile/plotted/paradigm browsers (`ZonePiles.tsx` — titles carry the
  owner's name, "Carol's Graveyard", since "Opponent's" is ambiguous at a multiplayer
  table) and the deck browser (`DeckBrowser.tsx`), the attachments browser
  (`Battlefield.tsx`), the copy-of hover preview and the
  active-effect badge tooltip (`GameCard.tsx` / `CardOverlays.tsx`). Overlays rendered
  from `GameBoard` itself (stack, action menu, decision modals, yield menu) sit outside
  the strip and don't need it.
- **Spectator/replay** reuse the same layout anchored to a chosen bottom seat
  (`spectatorBottomSeatId`, cycled from the spectator header); replays render through the
  same `GameBoard spectatorMode` path.

Dev loop: the scenario builder (`POST /api/scenarios`) accepts an N-player `players` seat
list (3-4 seats ⇒ hotseat) — see `ScenarioSeat` in `ScenarioDtos.kt`.

### Shared card browser (`components/deckbuilder/browser/`)

The deckbuilder's browsing experience is a reusable module, not a page-private one: `SearchBar`
(Scryfall-style query + sort + syntax help), `FilterSection` (set combobox, colour/type/subtype/
rarity/keyword chips, numeric ranges — all round-tripped through the raw query string),
`CardGrid` (lazy images, count badges, draggable tiles), `HoverFollowPreview`, `useCardCatalog`
(`/api/cards` + `/api/sets`), `useSetPrintingOverride` (reprint art for an active `s:` filter),
and the `cardDrag` payload every card surface speaks. `CardBrowser` composes them for callers
that just need "find me a card" in a side pane; `DeckbuilderPage` composes the same pieces into
its own three-column layout. All of them share `deckbuilder.module.css`.

### Scenario builder (`components/scenario/`)

Split pane: `CardBrowser` on the left, an editable board on the right, divider position
persisted in localStorage. `builderState.ts` holds the editing model — an ordered seat list, so
3-4 player pods edit exactly like duels — plus the pure mutations and the `toSpec`/`fromSpec`
conversions to the wire `ScenarioSpec`; `builderHistory.ts` wraps it in an undo/redo stack
(⌘Z / ⇧⌘Z). `ScenarioBoard.tsx` renders each seat's zones as drop targets holding real card
images (tapped permanents lie sideways, counters and attachments show as badges, pile zones
collapse duplicates to ×N), and `CardEditorModal.tsx` edits one battlefield permanent —
tapped / summoning sickness, counters of any type, `attachedTo`, and the pre-set "as this
enters, choose …" values. Cards arrive by drag from the browser, by drag between zones, or by
click into the currently targeted zone (with an ×N multiplier for filling libraries).

## 3D Layout

### Coordinate System

- **X-axis**: Left (-) to Right (+)
- **Y-axis**: Down (-) to Up (+) (height)
- **Z-axis**: Opponent (-) to Player (+)

### Zone Positions

| Zone | Position | Orientation |
|------|----------|-------------|
| Player Hand | (0, 0.5, 4) | Fan layout facing camera |
| Player Lands | (0, 0, 2.5) | Grid layout |
| Player Creatures | (0, 0, 1.5) | Grid layout |
| Stack | (3.5, 0, 0) | Vertical pile |
| Opponent Creatures | (0, 0, -1.5) | Grid layout, rotated 180° |
| Opponent Lands | (0, 0, -2.5) | Grid layout, rotated 180° |
| Opponent Hand | (0, 0.5, -4) | Face-down, card backs |
| Libraries | (±4, 0, ±3) | Stacked pile |
| Graveyards | (±4, 0, ±2) | Spread pile |

### Card Dimensions

- **Standard card**: 2.5" × 3.5" ratio → 0.63 × 0.88 units
- **Scaling**: ~0.8 for hand, ~0.7 for battlefield

## Animation System

### Event Queue Processing

1. Events arrive with state update
2. Events queue in `pendingEvents`
3. `EventProcessor` plays events sequentially
4. Each event type has animation mapping
5. State renders final positions after animations

### Animation Types

| Event | Animation |
|-------|-----------|
| `cardDrawn` | Card slides from library to hand |
| `permanentEntered` | Card moves from hand/stack to battlefield |
| `damageDealt` | Red number popup |
| `creatureDied` | Fade + fall animation |
| `spellCast` | Card moves to stack with glow |
| `permanentTapped` | 90° rotation |

## Interaction System

### Click Handling

1. Raycaster detects card click
2. Check if card has legal actions
3. If single action → execute immediately
4. If multiple actions → show action menu
5. If action needs target → enter targeting mode

### Targeting Mode

1. Action requires target(s)
2. Filter valid targets from state
3. Highlight valid targets with glow
4. User clicks target → add to selection
5. When enough targets → submit action

Spells with multiple target requirements (e.g. "2 damage to any target and 1 damage to any
other target") walk the requirements one step at a time inside a single targeting phase.
Confirming a step snapshots the outgoing `TargetingState` onto
`TargetingState.previousRequirementStates`; a **Back** button (`goBackTargeting`) pops that
stack so the player can revise an already-confirmed target before the action is submitted —
the restored step keeps its confirmed picks selected, and re-confirming recomputes later
steps' valid-target pools against the revised selection. The resolution-time
`ChooseTargetsDecision` flow (`ChooseTargetsUI`) implements the same back navigation over its
requirement index. Cancel still aborts the whole action.

`ChooseTargetsUI` also decides, **per requirement**, which UI collects that slot: a requirement
whose legal targets all live in a graveyard or exile pile goes to `GraveyardTargetingUI` (a pile
isn't clickable card-by-card on the board), everything else to `BattlefieldTargetingUI`, which
highlights board objects through `decisionSelectionState`. The two can mix inside one decision —
The Spot, Living Portal exiles "up to one target nonland permanent **and** up to one target
nonland permanent card from a graveyard" — so the choice must not be made once for the whole
decision. Both collectors take the restored picks as `initialSelection`, so Back keeps its
confirmed selection whichever UI owns the slot.

A **single** requirement can also span both zones (Taskmaster, Mercenary Mimic: "up to one target
creature on the battlefield **or** creature card in a graveyard"). Then the board banner stays up
so permanents remain clickable *and* offers a button that opens the pile picker, whose "View
Battlefield" hands control back; the picks travel with the player in both directions, so either
half fills the same slot. That routing — `board` / `pile` / `mixed` — is `routeTargetsByZone` in
`utils/targeting.ts`, shared with the cast-time path (`TargetingOverlay` +
`ZoneCardTargetingOverlay`) precisely because the two drifted once and left a trigger's graveyard
targets unreachable. The decision path's walk itself (requirement index, collected picks, which
collector owns the screen) is the pure reducer in `decisions/chooseTargetsWalk.ts`, which is also
where its unit tests live — the web-client has no DOM test environment.

The wording on a pile slot ("Exile" vs "Put onto Battlefield" vs "Shuffle into Library") comes
from `derivePileAction` in `utils/targeting.ts`, which sniffs the decision's `effectHint` prose.
That hint describes the effect as a whole rather than the individual requirement, so a composite
whose slots take different verbs would mislabel one of them; the durable fix is a per-requirement
action hint on `TargetRequirementInfo`.

## Type Mapping

### Backend → Frontend

| Kotlin Type | TypeScript Type |
|-------------|-----------------|
| `EntityId` | `string` (branded) |
| `ZoneId` | `{ type: ZoneType, ownerId?: string }` |
| `Phase` | `enum Phase` |
| `Step` | `enum Step` |
| `Color` | `enum Color` |
| `Keyword` | `enum Keyword` |
| `CounterType` | `enum CounterType` |
| `ClientGameState` | `interface ClientGameState` |
| `ClientCard` | `interface ClientCard` |
| `ServerMessage` | `type ServerMessage = Connected | StateUpdate | ...` |
| `ClientMessage` | `type ClientMessage = Connect | SubmitAction | ...` |
| `GameAction` | `type GameAction = PlayLand | CastSpell | ...` |

## File Structure

```
web-client/
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── src/
    ├── main.tsx
    ├── App.tsx
    ├── types/
    │   ├── index.ts
    │   ├── enums.ts
    │   ├── entities.ts
    │   ├── gameState.ts
    │   ├── messages.ts
    │   ├── events.ts
    │   └── actions.ts
    ├── network/
    │   ├── websocket.ts
    │   └── messageHandlers.ts
    ├── store/
    │   ├── gameStore.ts
    │   ├── animationStore.ts
    │   └── selectors.ts
    ├── components/
    │   ├── scene/
    │   │   ├── GameScene.tsx
    │   │   ├── Camera.tsx
    │   │   ├── Lighting.tsx
    │   │   └── Table.tsx
    │   ├── zones/
    │   │   ├── ZoneLayout.tsx
    │   │   ├── Battlefield.tsx
    │   │   ├── Hand.tsx
    │   │   ├── Library.tsx
    │   │   ├── Graveyard.tsx
    │   │   └── Stack.tsx
    │   ├── card/
    │   │   ├── Card3D.tsx
    │   │   ├── CardHighlight.tsx
    │   │   ├── PowerToughnessDisplay.tsx
    │   │   └── CounterDisplay.tsx
    │   ├── targeting/
    │   │   ├── TargetArrow.tsx
    │   │   └── TargetingOverlay.tsx
    │   ├── effects/
    │   │   ├── DamageEffect.tsx
    │   │   └── DeathEffect.tsx
    │   ├── interaction/
    │   │   └── ClickHandler.tsx
    │   ├── ui/
    │   │   ├── GameUI.tsx
    │   │   ├── LifeCounter.tsx
    │   │   ├── ManaPool.tsx
    │   │   ├── PhaseIndicator.tsx
    │   │   └── ActionMenu.tsx
    │   └── mulligan/
    │       └── MulliganUI.tsx
    ├── animation/
    │   ├── AnimatedCard.tsx
    │   ├── EventProcessor.tsx
    │   ├── eventAnimations.ts
    │   └── useCardAnimation.ts
    └── hooks/
        ├── useCardTexture.ts
        ├── useInteraction.ts
        ├── useLegalActions.ts
        └── useTargeting.ts
```

## Development Workflow

### Local Development

```bash
# Start Vite dev server
cd web-client
npm run dev
# Opens http://localhost:5173

# Start game server (separate terminal)
cd game-server
./gradlew bootRun
# WebSocket at ws://localhost:8080/game
```

### Testing

```bash
# Type checking
npm run typecheck

# Build for production
npm run build

# Preview production build
npm run preview
```

## Future Considerations

### Performance Optimization

- Texture atlasing for card images
- Instanced rendering for many cards
- Level-of-detail for distant cards
- WebWorker for animation calculations

### Features to Add

- Card zoom on hover
- Deck builder UI
- Game history replay
- Spectator mode
- Sound effects
- Mobile touch support
