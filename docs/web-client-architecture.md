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
  `data-life-display`) are carried by the orb for the viewed opponent and by the rail chip
  for everyone else — never both, so arrows, damage floats, and player-target clicks
  resolve unambiguously.
- **Board switching**: rail-chip click (pins; re-click unpins), keyboard `1`/`2`/`3`,
  horizontal swipe. Follow-the-action (`useMultiplayerView` + the `boardView` slice:
  `viewedOpponentId`, `viewPinned`, `followAction`) slides automatically on coarse
  boundaries — an opponent's turn starting, the attacker's board when you're attacked, the
  priority seat in hotseat — and is refused inside `followViewTo` while any input is
  pending (the camera never moves under an in-progress selection).
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
  selected attacker has an explicit defender. Arrows against the viewed defender render
  per-creature in the defender's seat color; attacks on slid-away boards bundle into one
  arrow to the defender's rail chip with a creature-count badge (`CombatArrows`), and any
  card anchor on a slid-away board remaps to its controller's chip (also in
  `TargetingArrows`). While you declare blocks, attackers aimed at other defenders render
  dimmed (CR 509.1b — `CombatState.actingSeat` scopes `attackingCreatures` to attacks on
  you).
- **Spectator/replay** reuse the same layout anchored to a chosen bottom seat
  (`spectatorBottomSeatId`, cycled from the spectator header); replays render through the
  same `GameBoard spectatorMode` path.

Dev loop: the scenario builder (`POST /api/scenarios`) accepts an N-player `players` seat
list (3-4 seats ⇒ hotseat) — see `ScenarioSeat` in `ScenarioDtos.kt`.

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
