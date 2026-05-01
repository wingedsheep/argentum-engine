---
name: generate-scenario
description: Generate a test scenario JSON for the DevScenarioController. Takes a description of what you want to test and produces a valid scenario file.
argument-hint: <description of what you want to test>
---

# Generate Test Scenario

Generate a test scenario JSON file based on the user's request: `$ARGUMENTS`

## Step 1: Understand the Request

Parse what the user wants to test. Common scenarios:
- Testing a specific card's abilities (combat tricks, removal, ETB triggers, etc.)
- Testing a specific game state (low life, empty hand, crowded board, etc.)
- Testing specific interactions between cards
- Reproducing a bug scenario

## Step 2: Find Available Cards

The scenario API only accepts cards registered in the card registry. To find available cards:

1. **List all card definition files** to see what's available:
   ```
   ls mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/*/cards/
   ```

2. **Search for specific card types** if needed (e.g., creatures with flying, red instants):
   - Read set files (`PortalSet.kt`, `OnslaughtSet.kt`, `ScourgeSet.kt`, `LegionsSet.kt`, `KhansOfTarkirSet.kt`) to see card lists
   - Grep card definition files for specific keywords, types, or mechanics

3. **Verify card names exactly match** — the API uses exact name matching. Read the card definition file to get the exact `cardDef("Name")` string.

**Available sets:** Portal, Onslaught, Scourge, Legions, Khans of Tarkir

## Step 2b: Audit Card Abilities for Testability

If the scenario is about testing a specific card, read the card definition file and enumerate **every** ability the card has:

- **Spell effects** (what happens on resolution)
- **Triggered abilities** (ETB, dies, on attack, on upkeep, on cycle, etc.)
- **Activated abilities** (tap abilities, sacrifice costs, mana costs)
- **Static abilities** (lord effects, keywords, continuous effects)
- **Alternative costs / special actions** (morph, cycling, kicker, etc.)
- **Keywords** (flying, trample, haste — relevant for combat scenarios)

For each ability, verify the scenario enables testing it:

| Ability type | Scenario requirement |
|---|---|
| Spell effect / ETB trigger | Card in hand + enough mana to cast it |
| Activated ability (tap cost) | Card on battlefield without summoning sickness (or with haste) |
| Activated ability (sacrifice cost) | Card on battlefield + valid sacrifice targets |
| Activated ability (mana cost) | Card on battlefield + enough untapped lands |
| Triggered ability (on attack) | Card on battlefield ready to attack + combat phase accessible |
| Triggered ability (on damage) | Card on battlefield + combat or damage source available |
| Triggered ability (on death/dies) | Card on battlefield + a way to destroy it (opponent has removal, or combat) |
| Triggered ability (on upkeep/end step) | Use `stopAtSteps: ["UPKEEP"]` or `["END"]` and set appropriate phase |
| Triggered ability (on cycle) | Card with cycling in hand + mana for cycling cost |
| Static/lord ability | Card on battlefield + other creatures it affects |
| Keyword (flying, trample, etc.) | Creatures on both sides for meaningful combat |
| Morph / face-down | Card in hand + 3 generic mana available; morph-up cost mana on battlefield |

If the scenario cannot cover an ability, adjust the board state (add lands, creatures, move card to correct zone, change phase, add stop-at-steps). If a single scenario genuinely cannot test all abilities (e.g., both "ETB" and "dies" triggers require different starting positions), note this to the user and suggest generating a second scenario, or set up the board so the card can be cast and then destroyed in the same game sequence.

## Step 3: Build the Scenario JSON

Create a valid `ScenarioRequest` JSON matching this schema:

```json
{
  "player1Name": "Alice",
  "player2Name": "Bob",
  "player1": {
    "lifeTotal": 20,
    "hand": ["Card Name", "Card Name"],
    "battlefield": [
      {"name": "Card Name"},
      {"name": "Card Name", "tapped": true},
      {"name": "Card Name", "summoningSickness": true},
      {"name": "Card Name", "counters": {"PLUS_ONE_PLUS_ONE": 2}},
      {"name": "Aura Name", "attachedTo": "Host Creature Name"}
    ],
    "graveyard": ["Card Name"],
    "library": ["Card Name", "Card Name"]
  },
  "player2": {
    "lifeTotal": 20,
    "hand": [],
    "battlefield": [
      {"name": "Card Name"}
    ],
    "graveyard": [],
    "library": ["Card Name", "Card Name"]
  },
  "phase": "PRECOMBAT_MAIN",
  "activePlayer": 1,
  "priorityPlayer": 1,
  "player1StopAtSteps": [],
  "player2StopAtSteps": [],
  "player1OpponentStopAtSteps": [],
  "player2OpponentStopAtSteps": []
}
```

### Field reference

- **phase**: `BEGINNING`, `PRECOMBAT_MAIN`, `COMBAT`, `POSTCOMBAT_MAIN`, `ENDING`
- **step** (optional, defaults based on phase): `UNTAP`, `UPKEEP`, `DRAW`, `PRECOMBAT_MAIN`, `BEGIN_COMBAT`, `DECLARE_ATTACKERS`, `DECLARE_BLOCKERS`, `COMBAT_DAMAGE`, `END_COMBAT`, `POSTCOMBAT_MAIN`, `END`, `CLEANUP`
- **activePlayer** / **priorityPlayer**: `1` or `2`
- **battlefield cards**: Can specify `tapped`, `summoningSickness`, `counters`, `attachedTo`
- **counters**: Map of counter type to count. Types: `PLUS_ONE_PLUS_ONE`, `MINUS_ONE_MINUS_ONE`, `CHARGE`, `LOYALTY`, etc.
- **stopAtSteps**: Steps where auto-pass is disabled (useful for testing upkeep triggers, combat phases, etc.)
- **library**: IMPORTANT — always include at least 1-2 cards per player to prevent draw-from-empty game loss

### Design guidelines

- Include enough mana sources (lands) for the cards you want to cast
- Keep libraries small but non-empty (2-3 cards minimum)
- Set up the board state so the interesting interaction is immediately available
- Use `tapped: false` (default) for lands the player should be able to use
- Use `summoningSickness: false` (default) for creatures that should be able to attack
- Set appropriate life totals for the scenario
- Use `player1StopAtSteps` / `player2StopAtSteps` if the scenario needs to stop at specific steps (e.g., `["UPKEEP"]` for upkeep trigger testing)

## Step 4: Save the Scenario

1. Choose a descriptive filename based on the scenario (kebab-case, `.json` extension)
2. Write the file to `manual-scenarios/<filename>.json` (use the `cards/<first-letter>/` subdir for single-card scenarios; `mechanics/`, `bugs/`, or `ui/` for non-card scenarios)
3. Present the scenario to the user with:
   - The file path
   - A brief description of the board state
   - How to use it: `curl -X POST http://localhost:8080/api/dev/scenarios -H "Content-Type: application/json" -d @manual-scenarios/<path>/<filename>.json`
   - Or remind them they can paste the JSON into the Swagger UI at `http://localhost:8080/swagger-ui.html`

## Step 5: Verify Card Names

Before saving, verify that every card name used in the scenario exists in the card registry by checking the card definition files. Card names must match exactly (case-sensitive).

## Important Rules

1. **Only use cards that exist in the registry** — check card definition files
2. **Always include library cards** — at least 1-2 per player to prevent draw losses
3. **Include enough lands** — players need mana to cast their spells
4. **Card names must be exact** — match the `cardDef("...")` string precisely
5. **Keep scenarios focused** — set up the minimal board state to test the interaction
6. **Don't omit optional fields** — include `lifeTotal`, `phase`, `activePlayer` for clarity
