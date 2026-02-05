# Reference: Existing Effects, Keywords, and Test Helpers

## Existing Effects (in Effect.kt)

- `DealDamageEffect` - damage (supports fixed `Int` and `DynamicAmount` including `XValue`)
- `DrawCardsEffect` - card draw
- `DiscardCardsEffect`, `DiscardRandomEffect` - discard
- `GainLifeEffect`, `LoseLifeEffect` - life changes
- `MoveToZoneEffect` - destroy, exile, bounce, shuffle-into-library, put-on-top (use `Effects.Destroy()`, `Effects.Exile()`, `Effects.ReturnToHand()`, etc.)
- `TapUntapEffect` - tap/untap
- `ModifyStatsEffect` - +X/+Y until end of turn
- `AddCountersEffect`, `RemoveCountersEffect` - counter manipulation
- `CreateTokenEffect` - token creation
- `SearchLibraryEffect` - tutoring
- `ScryEffect`, `SurveilEffect` - library manipulation
- `GrantKeywordUntilEndOfTurnEffect` - temporary keyword grant
- `CounterSpellEffect` - counter target spell

## Existing Keywords (in Keyword.kt)

- FLYING, MENACE, FEAR, SHADOW, HORSEMANSHIP
- FIRST_STRIKE, DOUBLE_STRIKE, TRAMPLE, DEATHTOUCH, LIFELINK
- VIGILANCE, REACH, DEFENDER, INDESTRUCTIBLE
- HASTE, FLASH, HEXPROOF, SHROUD
- SWAMPWALK, FORESTWALK, ISLANDWALK, MOUNTAINWALK, PLAINSWALK
- CHANGELING, PROWESS, CONVOKE, DELVE

## Key File Paths

| File | Purpose |
|------|---------|
| `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/scripting/Effect.kt` | Effect type definitions |
| `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/core/Keyword.kt` | Keyword enum |
| `rules-engine/src/main/kotlin/com/wingedsheep/engine/handlers/effects/` | Effect executors |
| `mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/{set}/cards/` | Card definitions |
| `mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/{set}/{Set}Set.kt` | Set card lists |
| `game-server/src/test/kotlin/com/wingedsheep/gameserver/scenarios/` | Scenario tests |

## Test Helper Methods

### Scenario Builder

- `scenario().withPlayers(name1, name2)` - Create 2-player game
- `.withCardInHand(playerNum, cardName)` - Add card to hand
- `.withCardOnBattlefield(playerNum, cardName, tapped?, summoningSickness?)` - Add permanent
- `.withLandsOnBattlefield(playerNum, landName, count)` - Add lands
- `.withCardInGraveyard(playerNum, cardName)` - Add to graveyard
- `.withCardInLibrary(playerNum, cardName)` - Add to library
- `.withLifeTotal(playerNum, life)` - Set life total
- `.inPhase(phase, step)` - Set game phase
- `.withActivePlayer(playerNum)` - Set active player
- `.build()` - Create TestGame

### Game Actions

- `game.castSpell(playerNum, spellName, targetId?)` - Cast spell
- `game.castSpellTargetingPlayer(playerNum, spellName, targetPlayerNum)` - Cast targeting player
- `game.castXSpell(playerNum, spellName, xValue, targetId?)` - Cast X spell
- `game.resolveStack()` - Resolve stack (pass priority)
- `game.passPriority()` - Pass priority once

### Game Queries

- `game.findPermanent(name)` - Find permanent by name
- `game.getLifeTotal(playerNum)` - Get life total
- `game.handSize(playerNum)` - Get hand size
- `game.graveyardSize(playerNum)` - Get graveyard size
- `game.isOnBattlefield(cardName)` - Check if on battlefield
- `game.isInGraveyard(playerNum, cardName)` - Check if in graveyard

### Decision Handling

- `game.hasPendingDecision()` - Check for pending decision
- `game.selectTargets(entityIds)` - Submit target selection
- `game.skipTargets()` - Skip optional targets
- `game.answerYesNo(choice)` - Submit yes/no response
- `game.selectCards(cardIds)` - Submit card selection
- `game.submitDistribution(map)` - Submit distribution (divided damage)
