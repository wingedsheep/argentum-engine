# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
npm test                        # Run all E2E tests (auto-starts server + client)
npm run test:portal             # Portal set tests only
npm run test:onslaught          # Onslaught set tests only
npm run test:general            # General mechanics tests only
npm run test:ui                 # Playwright UI mode (debug/interactive)
npm run test:headed             # Run with visible browser
npx playwright test tests/onslaught/sparksmith  # Run a specific test file
```

**Prerequisites:** The `webServer` config in `playwright.config.ts` auto-starts both the game server and web client. To skip auto-start (e.g., when using manually running instances), set `SKIP_WEB_SERVER=true`.

The server requires `GAME_DEV_ENDPOINTS_ENABLED=true` to expose the `/api/dev/scenarios` endpoint used by tests.

## Architecture

Tests use a **scenario fixture** pattern: instead of playing through turns, each test calls `createGame()` to POST a `ScenarioRequest` to the server's dev API, which creates a game with a pre-configured board state. The fixture opens two isolated browser contexts (one per player) with auth tokens pre-injected into localStorage, then returns `{ player1, player2 }` each with a `gamePage` (page object) for UI interaction.

```
createGame(ScenarioRequest)
  → POST /api/dev/scenarios          # Server creates deterministic game state
  → Two browser contexts with auth   # player1.gamePage, player2.gamePage
  → Test interacts via GamePage helpers
  → Assertions via expect* methods
```

**Key files:**
- `fixtures/scenarioFixture.ts` — Custom Playwright fixture providing `createGame()`
- `helpers/gamePage.ts` — Page object with all UI interaction and assertion methods
- `helpers/scenarioApi.ts` — `ScenarioRequest` type and API client
- `helpers/selectors.ts` — CSS/ARIA selectors for game zones and elements

Tests are organized by card set: `tests/portal/`, `tests/onslaught/`, `tests/general/`.

## ScenarioRequest Config

```typescript
await createGame({
  player1Name: 'Player1',
  player1: {
    hand: ['Lightning Bolt'],
    battlefield: [{ name: 'Mountain', tapped: false, summoningSickness: false }],
    graveyard: ['Grizzly Bears'],
    library: ['Mountain'],       // Always include at least one card to prevent draw-loss
    lifeTotal: 20,
  },
  player2: { ... },
  phase: 'PRECOMBAT_MAIN',       // 'BEGINNING' | 'PRECOMBAT_MAIN' | 'COMBAT' | 'POSTCOMBAT_MAIN' | 'ENDING'
  step: 'UPKEEP',                // Optional specific step
  activePlayer: 1,               // 1 or 2
  priorityPlayer: 1,             // Defaults to activePlayer
  player1StopAtSteps: ['UPKEEP'], // Prevent auto-pass at specific steps
})
```

## Priority and Auto-Pass Behavior

- When a player has no legal responses, they **auto-pass priority**
- After P1 puts a spell/ability on the stack, P1 auto-passes → P2 gets priority
- P2 must explicitly resolve stack items: `await p2.resolveStack('Spell Name')` or `await p2.pass()`
- Use `player1StopAtSteps` / `player2StopAtSteps` to prevent auto-passing through steps like `UPKEEP`

## Common Test Patterns

**Cast a spell with targets:**
```typescript
await p1.clickCard('Lightning Bolt')
await p1.selectAction('Cast Lightning Bolt')
await p1.selectTarget('Glory Seeker')
await p1.confirmTargets()
await p2.resolveStack('Lightning Bolt')
await p1.expectNotOnBattlefield('Glory Seeker')
```

**Triggered ability with MayEffect + target:**
Flow: `resolveStack()` → `answerYes()` → `selectTarget()` → `confirmTargets()` → `resolveStack()`

**Triggered ability with target only (no MayEffect):**
Flow: triggered ability goes on stack → `resolveStack()` opens targeting modal → `selectTarget()` → `confirmTargets()`

**Combat:**
```typescript
await p1.pass()         // Pass through main phase to declare attackers
await p1.attackAll()    // Click "Attack All" button
await p2.noBlocks()     // or: await p2.declareBlocker('Blocker', 'Attacker')
await p2.confirmBlockers()
```

**Library/graveyard overlay (e.g., Gravedigger ETB):**
```typescript
await p1.selectCardInZoneOverlay('Grizzly Bears')
await p1.confirmTargets()
```

## Gotchas

- **Face-down creatures:** Alt text is `"Card back"`, not the card name. Use `clickCard('Card back')`.
- **Activated ability buttons:** Show the full ability text, not "Activate". Use partial match: `selectAction('damage to target')`.
- **Aura / sacrifice-as-cost targeting:** Uses the ChooseTargets modal — always needs `selectTarget()` + `confirmTargets()`.
- **DamageDistributionModal** (e.g., Forked Lightning): Use `increaseDamageAllocation(name, times)` + `castSpellFromDistribution()`. Button is "Cast Spell", not "Confirm".
- **Stack items always stop the opponent:** Opponent always gets priority to respond. Use `p2.resolveStack('Name')` to let them pass.
- **OrderBlockers button:** May be outside viewport — use `evaluate(el => el.click())` for JS-level click if needed.
