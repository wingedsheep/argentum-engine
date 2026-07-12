package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.LoseAtEndStepComponent
import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.effects.DamageCantBePreventedThisTurnEffect
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Alchemist's Gambit (VOW #140).
 *
 * {1}{R}{R} Sorcery — Cleave {4}{U}{U}{R}
 * "Take an extra turn after this one. During that turn, damage can't be prevented. [At the
 *  beginning of that turn's end step, you lose the game.]
 *  Exile Alchemist's Gambit."
 *
 * Cleave (CR 702.148) removes the bracketed words when its alternative cost is paid. The single
 * bracket span is the drawback — "[At the beginning of that turn's end step, you lose the game.]" —
 * so paying the (much steeper) cleave cost buys the extra turn *without* the downside. Everything
 * else (the extra turn, "during that turn damage can't be prevented", and "Exile Alchemist's
 * Gambit") is outside the brackets and applies to both modes.
 *
 * The three moving parts are asserted separately:
 *  - **Extra turn** — in a 2-player game an extra turn is modelled by the opponent skipping their
 *    next turn ([SkipNextTurnComponent]).
 *  - **Lose clause** — [LoseAtEndStepComponent] is present on the printed cast and *absent* on the
 *    cleaved cast (the whole point of the bracket removal).
 *  - **Damage can't be prevented** — scoped to the extra turn, so it's scheduled as a delayed
 *    trigger ([DamageCantBePreventedThisTurnEffect], timing NEXT_TURN); a behavioural test crosses
 *    into the extra turn's upkeep and confirms the flag is set once the trigger fires.
 *  - **Self-exile** — Alchemist's Gambit goes to exile, not the graveyard, on resolution.
 */
class AlchemistsGambitScenarioTest : ScenarioTestBase() {

    init {
        context("Alchemist's Gambit — printed cast (brackets present)") {

            test("grants an extra turn, schedules the lose-the-game clause, and self-exiles") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Alchemist's Gambit")
                    .withLandsOnBattlefield(1, "Mountain", 3) // {1}{R}{R}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Alchemist's Gambit")
                withClue("Casting Alchemist's Gambit should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Player 1 takes the extra turn, so Player 2 skips their next turn") {
                    game.state.getEntity(game.player2Id)?.has<SkipNextTurnComponent>() shouldBe true
                }
                withClue("The printed cast keeps the drawback: the caster loses at the extra turn's end step") {
                    game.state.getEntity(game.player1Id)?.has<LoseAtEndStepComponent>() shouldBe true
                }
                withClue("A delayed trigger is scheduled to make damage unpreventable on the extra turn") {
                    game.state.delayedTriggers.any {
                        it.effect == DamageCantBePreventedThisTurnEffect
                    } shouldBe true
                }
                withClue("Alchemist's Gambit exiles itself instead of going to the graveyard") {
                    game.isInExile(1, "Alchemist's Gambit") shouldBe true
                    game.isInGraveyard(1, "Alchemist's Gambit") shouldBe false
                }
            }
        }

        context("Alchemist's Gambit — cleaved cast (brackets removed)") {

            test("grants the extra turn WITHOUT the lose-the-game clause, and still self-exiles") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Alchemist's Gambit")
                    .withLandsOnBattlefield(1, "Island", 4) // Cleave {4}{U}{U}{R}
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpellWithCleave(1, "Alchemist's Gambit")
                withClue("Casting Alchemist's Gambit for its cleave cost should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Player 1 still takes the extra turn (that clause is outside the brackets)") {
                    game.state.getEntity(game.player2Id)?.has<SkipNextTurnComponent>() shouldBe true
                }
                withClue("The cleaved cast drops the drawback: NO lose-at-end-step is scheduled") {
                    game.state.getEntity(game.player1Id)?.has<LoseAtEndStepComponent>() shouldBe false
                }
                withClue("The damage-can't-be-prevented clause survives (it's outside the brackets)") {
                    game.state.delayedTriggers.any {
                        it.effect == DamageCantBePreventedThisTurnEffect
                    } shouldBe true
                }
                withClue("Alchemist's Gambit still exiles itself in the cleaved mode") {
                    game.isInExile(1, "Alchemist's Gambit") shouldBe true
                    game.isInGraveyard(1, "Alchemist's Gambit") shouldBe false
                }
            }

            test("damage can't be prevented once the extra turn begins") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Alchemist's Gambit")
                    .withLandsOnBattlefield(1, "Island", 4) // Cleave {4}{U}{U}{R} — no lose clause
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellWithCleave(1, "Alchemist's Gambit").error shouldBe null
                game.resolveStack()

                withClue("Prevention is not yet disabled on the turn the spell resolves") {
                    game.state.damageCantBePreventedThisTurn shouldBe false
                }

                // Cross into the extra turn: end this turn, Player 2's turn is skipped, so Player 1's
                // upkeep is the extra turn. The delayed trigger fires at that upkeep.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("Player 1 is active again — the extra turn (Player 2 was skipped)") {
                    game.state.activePlayerId shouldBe game.player1Id
                }
                withClue("The delayed trigger fired: damage can't be prevented during the extra turn") {
                    game.state.damageCantBePreventedThisTurn shouldBe true
                }
            }
        }
    }
}
