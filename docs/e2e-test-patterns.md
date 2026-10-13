# E2E Test Patterns (Playwright)

E2E tests run against the full stack (server + client) in a real browser. They live in `e2e-scenarios/` and use the
`createGame` fixture to set up board state via the dev scenario API, then interact with the UI through the `GamePage`
page object.

**Prerequisites:** Server running (`just server`), client running (`just client`).

## Running

Prefer the `just` recipes — they wrap the CLI flags an agent typically wants and stay out of MCP territory (CLI is
markedly more token-efficient for browser automation than the Playwright MCP).

| Recipe | When to use |
|--------|-------------|
| `just e2e` | Run the full suite |
| `just e2e-test <pattern>` | Run a specific test by path or `--grep` substring (e.g. `just e2e-test sparksmith`) |
| `just e2e-loop <test-path> <client-port> <server-port>` | Verify a loop's own worktree in isolated headless Chromium; retain screenshots |
| `just e2e-headed` | Watch the run in a visible browser |
| `just e2e-ui` | Playwright UI mode — best for triaging failures interactively |
| `just e2e-debug <pattern>` | Step through a single test under the Playwright Inspector |
| `just e2e-codegen [URL]` | Record interactions against the running client to discover selectors / generate scaffolding |
| `just e2e-trace <path>` | Open a trace zip after a failed CI / report run |
| `just e2e-report` | Run + open the HTML report (screenshots, videos, traces) |
| `just e2e-install-browsers` | One-time `playwright install chromium` (after `just e2e-install`) |

Direct CLI for anything not covered:

```bash
cd e2e-scenarios && npx playwright test tests/onslaught/sparksmith   # specific path
cd e2e-scenarios && npx playwright test --grep "Lightning Bolt"      # grep test names
SKIP_WEB_SERVER=true npx playwright test ...                         # skip auto-starting server/client (when already running)
```

## CLI-first iteration loop (for agents)

1. **Write the test** based on a similar existing test in `tests/{set}/`.
2. **Run only that test** via `just e2e-test <name>` — fast feedback, no full-suite cost.
3. **If selectors are unclear**, `just e2e-codegen` against `http://localhost:5173` while the server + a manually
   created game session are running. Copy the generated locators back into the test.
4. **If a test fails mysteriously**, rerun under `just e2e-debug <pattern>` to step through, or `just e2e-report` to
   inspect screenshots/videos/traces.
5. **If a CI trace is provided**, `just e2e-trace <path>` opens it locally.

Avoid `just e2e` (full suite) during iteration — it's slow and most failures are unrelated to the test under work.

## Unattended loop verification

When a loop adds or changes a player-visible flow, including an engine/server feature rendered by existing
components, verify that flow in dedicated Playwright Chromium. Card batches that reuse established UI do
not need another browser gate. Desktop browser permissions (for example Brave access) are not a dependency;
leave denied apps alone and use the isolated test browser, which has no personal profile or signed-in tabs.

From the unit's worktree, install dependencies and Chromium if missing (`just e2e-install`, then
`just e2e-install-browsers`). Choose two unused ports and run a specific test file:

```bash
just e2e-loop tests/general/gift-promised-at-cast-time.spec.ts 15173 18080
```

The loop config starts this worktree's game server with dev endpoints and its Vite client, routes both the
scenario API and client proxy to that server, and stops both on exit. It always starts its own servers,
ignores `SKIP_WEB_SERVER` and URL overrides, rejects occupied ports, and makes Vite fail rather than shift
to another port. Concurrent loops must choose different port pairs; never kill another loop's server.
Server startup goes through `just e2e-server` and the global Gradle semaphore. No desktop browser is opened.

Reuse a relevant test or add a focused test using the existing fixtures. Assert that the player can complete
the affected interaction; a page-load screenshot alone does not verify a decision flow. Capture the important
prompt/state with `await gamePage.screenshot('Describe the state')` or `test.info().attach(...)` so passing
tests preserve it too. Results and PNG attachments are in `e2e-scenarios/.loop-artifacts/test-results/`, with
an HTML report in `.loop-artifacts/report/`. These worktree-local outputs are gitignored and replaced by the
next run; preserve any evidence needed for the PR before rerunning or removing the worktree.

Inspect the relevant PNGs with the image-viewing tool before claiming visual verification. Record the command,
result, screenshot paths and inspection status in the ledger and PR body. State browser startup or image-viewing
limitations precisely, and distinguish automated E2E from a manual playthrough. Shell/browser execution still
follows the task's sandbox permissions; this command does not override a denied app or execution request.
If Chromium fails with a sandbox permission error (on macOS, `bootstrap_check_in` /
`MachPortRendezvousServer`), request scoped execution approval for the same `just e2e-loop` command through
the harness. Do not disable global approval review. If approval is rejected, record the rejection and reason.

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
- `players`: N-player seat list `[{ name?, config? }, …]` (3-4 player pods, turn order; overrides the
  two-seat fields). Pods of more than two seats start as `SELF` (single-client hotseat).

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
