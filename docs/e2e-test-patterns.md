# E2E Test Patterns (Playwright)

E2E tests run against the full stack (server + client) in a real browser. They live in `e2e-scenarios/` and use the
`createGame` fixture to set up board state via the dev scenario API, then interact with the UI through the `GamePage`
page object.

**Prerequisites:** Server running (`just server`), client running (`just client`).

## Running

```bash
cd e2e-scenarios && npx playwright test                              # All E2E tests
cd e2e-scenarios && npx playwright test tests/onslaught/sparksmith   # Specific test
```

## Key Files

| File | Purpose |
|------|---------|
| `e2e-scenarios/fixtures/scenarioFixture.ts` | Test fixture — creates game via API, opens browser pages for both players |
| `e2e-scenarios/helpers/gamePage.ts` | `GamePage` page object — all UI interaction and assertion methods |
| `e2e-scenarios/helpers/scenarioApi.ts` | `ScenarioRequest` interface — board state configuration sent to server |
| `e2e-scenarios/helpers/selectors.ts` | CSS selectors for game board elements (`HAND`, `BATTLEFIELD`, `cardByName`) |
| `e2e-scenarios/tests/{set}/` | Test files organized by card set |

## Test Structure

```typescript
import { test, expect } from '../../fixtures/scenarioFixture'

test('card does X', async ({ createGame }) => {
  const { player1, player2 } = await createGame({
    player1Name: 'Player1',
    player2Name: 'Opponent',
    player1: {
      hand: ['Lightning Bolt'],
      battlefield: [{ name: 'Mountain', tapped: false }],
      library: ['Mountain'],
    },
    player2: {
      battlefield: [{ name: 'Glory Seeker' }],
      library: ['Mountain'],
    },
    phase: 'PRECOMBAT_MAIN',
    activePlayer: 1,
  })

  const p1 = player1.gamePage
  await p1.clickCard('Lightning Bolt')    // Click card to open action menu
  await p1.selectAction('Cast')           // Click action button
  await p1.selectTarget('Glory Seeker')   // Select target
  await p1.confirmTargets()               // Confirm targeting
  // Auto-resolves if opponent has no responses
  await p1.expectNotOnBattlefield('Glory Seeker')
})
```

## ScenarioRequest Config

- `player1` / `player2`: `{ hand, battlefield, graveyard, library, lifeTotal }` — battlefield items can specify
  `{ name, tapped?, summoningSickness? }`
- `phase`: `'BEGINNING'` | `'PRECOMBAT_MAIN'` | `'COMBAT'` | `'POSTCOMBAT_MAIN'` | `'ENDING'`
- `step`: Step name (e.g., `'UPKEEP'`, `'DECLARE_ATTACKERS'`)
- `activePlayer` / `priorityPlayer`: `1` or `2`
- `player1StopAtSteps` / `player2StopAtSteps`: Step names where auto-pass is disabled (e.g., `['UPKEEP']`)

## GamePage Helpers

| Category | Methods |
|----------|---------|
| **Card interaction** | `clickCard(name)`, `selectCardInHand(name)`, `selectAction(label)` |
| **Targeting** | `selectTarget(name)`, `confirmTargets()`, `skipTargets()` |
| **Priority** | `pass()`, `resolveStack(stackItemText)` |
| **Decisions** | `answerYes()`, `answerNo()`, `selectNumber(n)`, `selectOption(text)`, `selectXValue(x)` |
| **Combat** | `attackAll()`, `attackWith(name)`, `declareAttacker(name)`, `declareBlocker(blocker, attacker)`, `confirmBlockers()`, `noBlocks()` |
| **Overlays** | `selectCardInZoneOverlay(name)`, `selectCardInDecision(name)`, `confirmSelection()`, `failToFind()` |
| **Assertions** | `expectOnBattlefield(name)`, `expectNotOnBattlefield(name)`, `expectInHand(name)`, `expectNotInHand(name)`, `expectHandSize(n)`, `expectLifeTotal(id, n)`, `expectGraveyardSize(id, n)`, `expectStats(name, "3/3")`, `expectTapped(name)` |
| **Morph** | `castFaceDown(name)`, `turnFaceUp(name)` |
| **Ghost cards** | `expectGhostCardInHand(name)`, `expectNoGhostCardInHand(name)` |
| **Damage distribution** | `increaseDamageAllocation(name, times)`, `castSpellFromDistribution()`, `allocateDamage(name, amount)`, `confirmDamage()` |

## Important Patterns

- **Auto-pass:** When a player has no legal responses, they auto-pass. No explicit `p2.pass()` needed unless P2
  actually has instant-speed responses.
- **Library cards:** Always give both players at least one card in their library to prevent draw-from-empty losses.
- **Face-down creatures:** Alt text is `"Card back"`, not the card name. Use `clickCard('Card back')`.
- **Activated ability buttons:** Show full ability description text, not "Activate". Use partial text match like
  `selectAction('damage to target')` or `selectAction('Sacrifice a creature')`.
- **Turn face-up button:** Label is `"Turn Face-Up"` (hyphenated).
- **Aura/sacrifice targeting:** Uses the ChooseTargets modal — need `confirmTargets()` after `selectTarget()`.
- **Sacrifice-as-cost:** Also uses targeting modal — need `selectTarget()` + `confirmTargets()`.
- **P1 vs P2 priority:** After P1 puts spell/ability on stack, P1 auto-passes. P2 always gets priority to see what's
  on the stack — use `p2.pass()` or `p2.resolveStack('Card Name')` to resolve. P2 only auto-passes on their own turn's
  non-critical steps.
- **Stop at steps:** Use `player1StopAtSteps: ['UPKEEP']` to prevent auto-passing through steps like upkeep.
- **DamageDistributionModal** (e.g., Forked Lightning): Uses +/- buttons per target, "Cast Spell" button (not "Confirm
  Damage"). Each target starts at `minPerTarget=1`. Use `increaseDamageAllocation(name, times)` +
  `castSpellFromDistribution()`.
- **OrderBlockers button:** May be outside viewport — use `evaluate(el => el.click())` for JS-level click.
- **Stack items always stop opponent:** Opponent always gets to see and manually resolve stack items (spells,
  abilities, triggers). Use `p2.pass()` or `p2.resolveStack('Name')`.
