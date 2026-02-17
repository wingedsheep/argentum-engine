# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
npm run dev          # Dev server at localhost:5173
npm run build        # Type-check + production build
npm run typecheck    # TypeScript type checking only
npm run preview      # Preview production build
```

E2E tests live in `e2e-scenarios/` (sibling directory, not inside web-client).

The Vite dev server proxies `/game` and `/api` to `localhost:8080` (the Spring Boot backend).

## Architecture

### Dumb Terminal Pattern

The client has **no game logic**. The server is the source of truth for all game rules, legal actions, and state. The client's job is:
1. Render the game state sent by the server
2. Capture player intent (clicks, selections)
3. Forward intent to the server as `GameAction` messages

The server sends `legalActions` with every state update. The client checks this array before enabling interactions — it never computes legality itself.

### State Management: Zustand Slices

`src/store/gameStore.ts` combines five domain slices via Zustand with `subscribeWithSelector`:

| Slice | File | Manages |
|-------|------|---------|
| `connectionSlice` | `connectionSlice.ts` | WebSocket status, playerId, sessionId |
| `gameplaySlice` | `gameplaySlice.ts` | `gameState`, `legalActions`, `pendingDecision`, mulligan, game-over |
| `lobbySlice` | `lobbySlice.ts` | Tournament lobbies, spectating |
| `draftSlice` | `draftSlice.ts` | Sealed/draft deck building |
| `uiSlice` | `uiSlice.ts` | Client-only UI state: targeting, combat, animations |

`src/store/selectors.ts` contains derived selectors — prefer these over computing in components.

### WebSocket Communication

`src/network/websocket.ts` — `GameWebSocket` class handles connection lifecycle with exponential backoff reconnection (5 attempts, 1s base delay).

`src/network/messageHandlers.ts` — Exhaustive router dispatching all server message types to store actions. Every server message type maps to a handler here.

### Key Interaction Flows

**Card click → action:**
1. `useInteraction.handleCardClick(cardId)` checks `legalActions`
2. Returns one of: `noAction`, `singleAction` (auto-execute), `multipleActions` (show menu), `requiresTargeting`, `requiresXSelection`, `requiresConvokeSelection`
3. Menu or auto-execution dispatches to store

**Targeting flow:**
1. `uiSlice.startTargeting({ validTargets, minTargets, maxTargets })` — enters targeting mode
2. `TargetingArrows` overlay renders. User clicks valid targets → `addTarget(entityId)`
3. `confirmTargeting()` exits targeting mode and sends action via WebSocket

**Combat flow:**
1. `uiSlice.startCombat()` — enters combat mode
2. Attackers: `toggleAttacker(creatureId)` builds attacker list
3. Blockers: `assignBlocker(blocker, attacker)` maps assignments
4. `confirmCombat()` sends `DeclareAttackers`/`DeclareBlockers` action

**Decisions (scry, discard, etc.):**
- `gameplaySlice.pendingDecision` holds the current `PendingDecision` (union type)
- `DecisionUI` routes to the appropriate modal component
- Submit via `submitDecision()` / `submitYesNoDecision()` etc.

**Animations:**
- Server events trigger `addDrawAnimation()`, `addDamageAnimation()` etc. in `uiSlice`
- Animation components in `src/components/animations/` subscribe and auto-remove after duration

### TypeScript Strictness

Full strict mode is enabled including `noUncheckedIndexedAccess` and `exactOptionalPropertyTypes`. Path alias `@/` maps to `src/`. Key types are in `src/types/` — `ClientGameState`, `ClientCard`, `GameAction`, `ServerMessage`, `PendingDecision` etc.

### Component Structure

- `src/components/game/` — Game board orchestration (`GameBoard.tsx`) and sub-zones (Battlefield, HandZone, StackZone, ZonePiles)
- `src/components/game/card/` — Card rendering (`GameCard`, `CardPreview`, `CardStack`, `CardOverlays`)
- `src/components/decisions/` — Decision modals (scry, discard, target selection, damage distribution)
- `src/components/combat/` — Combat arrows (attacker/blocker visualization)
- `src/components/ui/` — HUD elements (phase strip, mana pool, life counter, action menu)
- `src/components/animations/` — Draw, damage, reveal, coin flip animations
- Spectating, draft, sealed, and tournament have dedicated component directories
