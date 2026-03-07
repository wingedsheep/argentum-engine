# Web Client Improvements Backlog

Analysis of the web-client codebase for modernization, modularity, and maintainability.

**Current state summary:** The web-client is well-architected overall — strict TypeScript, Zustand slice pattern,
dumb-terminal design, CSS custom properties for theming, and clean feature-based directory organization. The
improvements below target specific pain points that will compound as the codebase grows.

---

## 1. Break Up God Components

**Priority: High | Impact: Maintainability, testability**

Several components exceed 1,000 LOC and handle too many responsibilities:

| Component | LOC | Issue |
|-----------|-----|-------|
| `DeckBuilderOverlay.tsx` | 1,735 | Deck building logic, card pool filtering, tier editing, waiting state |
| `DecisionUI.tsx` | 1,705 | Routes 7+ decision types with inline logic for each |
| `GameUI.tsx` | 1,500 | Connection, lobby, tournament creation, settings — all in one |
| `GameCard.tsx` | 1,132 | Rendering, interactions, dragging, preview, combat, targeting |
| `App.tsx` | 500+ | Auto-connect, combat auto-entry, page tracking, overlay routing |

**Proposed splits:**

- **DecisionUI** → Extract each decision type into its own component file under `decisions/`. DecisionUI becomes a
  thin router that maps `pendingDecision.type` to the correct component. Each decision component owns its own state
  and submission logic.

- **GameCard** → Extract drag handling into `useCardDrag` hook. Extract interaction logic (already partially in
  `useInteraction`) more completely. Extract the overlay/badge rendering into a `CardBadges` subcomponent.

- **DeckBuilderOverlay** → Split into `CardPool` (filterable card grid), `DeckList` (current deck with curve
  display), and `DeckBuilderControls` (submit, tier editing). Extract deck validation logic into a utility.

- **GameUI** → Split into `ConnectionOverlay`, `LobbyCreator`, `LobbyWaiting`, `TournamentLobby`. Each screen
  is already conceptually distinct behind conditional rendering.

- **App.tsx** → Extract auto-connect logic and combat-mode management into custom hooks (`useAutoConnect`,
  `useCombatModeSync`). App becomes a pure overlay compositor.

---

## 2. Consolidate Styling Approach

**Priority: High | Impact: Consistency, design system integrity**

The codebase has a well-designed CSS variable system (`variables.css`) but underuses it:

- **150+ hard-coded colors** in inline styles (e.g., `'#666'`, `'#888'`, `'rgba(255,255,255,0.5)'`) that
  should reference `var(--text-secondary)` etc.
- **149+ hard-coded font sizes** in TSX (e.g., `fontSize: 22`) instead of CSS variables.
- **Dual responsive systems**: CSS media queries in `responsive.css` AND `responsive.isMobile` checks in
  components. Pick one source of truth.
- **`board/styles.ts`** (979 LOC) is a massive inline styles object — should be a CSS module.

**Action items:**

1. Audit and replace hard-coded colors/sizes with CSS variable references.
2. Migrate `board/styles.ts` to `GameBoard.module.css` (or split per sub-component).
3. Unify responsive approach: use CSS variables + media queries as primary, reserve JS `responsive.isMobile`
   only for structural differences (e.g., showing/hiding entire sections), not for spacing/font tweaks.
4. Define missing CSS variables referenced in modules (`--border-card`, `--text-on-primary`).

---

## 3. Add React Error Boundaries

**Priority: High | Impact: Reliability, user experience**

No error boundaries exist. A rendering error in any component crashes the entire app with a white screen.

- Add a top-level `ErrorBoundary` around the app with a "something went wrong" fallback and a reconnect button.
- Add granular boundaries around: `GameBoard`, `DecisionUI`, `DeckBuilderOverlay`, and draft overlays.
  These are complex components where rendering errors are most likely — isolating them prevents a card
  rendering bug from killing the entire UI.

---

## 4. Modularize Message Handlers

**Priority: Medium | Impact: Maintainability, extensibility**

`handlers.ts` is a 400+ line file implementing a single `createMessageHandlers()` function that handles 40+
server message types. Adding a new message type means editing this monolith.

**Proposed approach:**

- Split handlers by domain: `connectionHandlers.ts`, `gameplayHandlers.ts`, `lobbyHandlers.ts`,
  `draftHandlers.ts`, `spectatingHandlers.ts`.
- Each file exports a partial `MessageHandlers` object for its domain.
- `handlers.ts` becomes a combiner: `{ ...connectionHandlers, ...gameplayHandlers, ... }`.
- This mirrors the existing slice organization.

---

## 5. Extract Custom Hooks from Components

**Priority: Medium | Impact: Reusability, testability**

Several components contain complex stateful logic that could be extracted into hooks:

- **`useCardDrag`** — Drag-and-drop logic duplicated between `GameCard`, `OrderBlockersUI`, and
  `ReorderCardsUI` (long-press detection, move threshold, drag state).
- **`useAnimationQueue`** — Animation lifecycle (add → display → auto-remove after timeout) is repeated
  for draw, damage, reveal, coin flip, and target-reselected animations. A generic hook would reduce
  boilerplate.
- **`useAutoConnect`** — Auto-connection logic in `App.tsx` (check localStorage, connect if stored name
  exists) is a side effect that belongs in a hook.
- **`useCombatModeSync`** — Combat mode auto-entry/exit logic in `App.tsx` reacts to `legalActions` changes
  and should be a self-contained hook.
- **`useDecisionState`** — Selection state management (selected cards, min/max validation, submit) is
  duplicated across `SelectCardsUI`, `LibrarySearchUI`, and `ZoneSelectionUI`.

---

## 6. Improve Shared Component Library

**Priority: Medium | Impact: Consistency, development speed**

`components/shared/` contains only `Button.tsx` (49 LOC). Many UI patterns are duplicated:

- **Modal/Overlay** — Every decision, draft, and sealed component implements its own full-screen overlay
  backdrop with close handling. Extract a `Modal` component with backdrop, close-on-escape, focus trapping.
- **CardGrid** — Card grid layout (responsive columns, gap calculation) is reimplemented in DecisionUI,
  DeckBuilderOverlay, DraftPickOverlay, and LibrarySearchUI. Extract a `CardGrid` component.
- **Scrollable card list** — Horizontal scrolling card rows with overflow handling appear in multiple places.
- **Confirmation dialog** — Yes/No prompts and "are you sure" dialogs could share a component.

---

## 7. Lazy Load Feature Routes

**Priority: Medium | Impact: Bundle size, initial load time**

All feature components (admin, replay, tournament, draft, deck builder, spectator) are eagerly imported
in `App.tsx` and `main.tsx`. These are heavy components that most users won't need on initial load.

- Use `React.lazy()` + `Suspense` for route-level code splitting: `AdminPage`, `ReplayPage`,
  `TournamentEntryPage`.
- Lazy-load overlay components that appear conditionally: `DeckBuilderOverlay`, `DraftPickOverlay`,
  `WinstonDraftOverlay`, `GridDraftOverlay`, `SpectatorGameBoard`.
- This reduces the initial bundle and improves time-to-interactive for the primary game flow.

---

## 8. Add Accessibility Fundamentals

**Priority: Medium | Impact: Usability, inclusiveness**

Current accessibility is minimal — no ARIA labels, limited keyboard navigation, color-only indicators.

**Minimum viable improvements:**

- Add `aria-label` to interactive elements (cards, action buttons, mana symbols).
- Add `role="dialog"` and `aria-modal="true"` to decision modals and overlays.
- Implement focus trapping in modals (currently focus can escape to background elements).
- Add keyboard shortcuts for common actions: `Space` = pass priority, `Enter` = confirm, `Escape` = cancel.
- Add `aria-live="polite"` region for game events (damage dealt, spells cast) so screen readers
  announce game-state changes.

---

## 9. Type-Safe Message Handling

**Priority: Low | Impact: Safety, developer experience**

The `MessageHandlers` interface provides exhaustive coverage at the TypeScript level, but the runtime
`handleServerMessage()` router uses a string-based dispatch that could silently drop unknown message types.

- Add a discriminated union for `ServerMessage` with a `type` discriminant field.
- Use exhaustive `switch` with `never` default to get compile-time guarantees that all message types
  are handled.
- This catches missing handlers at build time rather than at runtime.

---

## 10. Improve Delta Applicator Robustness

**Priority: Low | Impact: Correctness**

`deltaApplicator.ts` uses `JSON.stringify` for zone matching when applying state deltas. This is fragile:

```typescript
// Current: string comparison
JSON.stringify({ zoneType: z.zoneType, ownerId: z.ownerId })
```

- Replace with proper zone ID comparison using `zoneIdEquals()` from `types/entities.ts` (already exists
  but isn't used here).
- Add unit tests for the delta applicator — it's a critical data path with no test coverage.

---

## 11. Standardize Store Access Patterns

**Priority: Low | Impact: Consistency, performance**

Components access the store inconsistently:

- Some use individual selectors: `useGameStore((s) => s.gameState)` repeated 15+ times in `GameCard`.
- Some use custom selector hooks: `useViewingPlayer()`, `useBattlefieldCards()`.
- Some access multiple fields that could be batched into a single selector to reduce subscriptions.

**Proposed convention:**

- For single primitive values, direct selector is fine: `useGameStore((s) => s.connectionStatus)`.
- For derived data, always use or create a named selector in `selectors.ts`.
- For components needing 5+ store fields, create a component-specific selector that returns an object,
  using `useShallow` from Zustand to prevent unnecessary re-renders.

---

## 12. Testing Infrastructure

**Priority: Low | Impact: Confidence, regression prevention**

The web-client has E2E tests (Playwright) but no unit or integration tests for:

- Store slices and their state transitions
- Selectors and their memoization behavior
- Delta applicator correctness
- Hook behavior (useInteraction, useTargeting, useLegalActions)
- Component rendering with various game states

Consider adding Vitest (already compatible with Vite) for:
- Store slice unit tests (create store, dispatch action, assert state)
- Selector tests (given game state, assert derived values)
- Delta applicator tests (given base state + delta, assert merged state)

These are high-value, low-effort tests that validate critical data paths without needing a browser.

---

## Summary: Recommended Order of Execution

| Phase | Items | Rationale |
|-------|-------|-----------|
| **Phase 1** | #1 (God components), #3 (Error boundaries) | Unblock further work; immediate reliability win |
| **Phase 2** | #2 (Styling), #4 (Message handlers), #5 (Hooks) | Reduce duplication; improve modularity |
| **Phase 3** | #6 (Shared components), #7 (Lazy loading) | Polish; performance |
| **Phase 4** | #8-#12 (Accessibility, types, tests) | Hardening; long-term quality |
