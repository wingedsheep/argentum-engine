package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.MayCastWithoutPayingCostUsedThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for the two Secrets of Strixhaven cards that needed new engine primitives:
 *
 *  - **Ennis, Debate Moderator** — the `CARDS_PUT_INTO_EXILE` turn tracker + its end-step
 *    intervening-if counter, plus the ETB blink (exile up to one other creature you control,
 *    return at the next end step).
 *  - **Zaffai and the Tempests** — `MayCastWithoutPayingManaCost(oncePerTurn = true)`: one free
 *    instant/sorcery cast from hand per your turn, consumed after a single use.
 */
class EnnisAndZaffaiScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(entity: EntityId): Int =
        state.getEntity(entity)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.freeCast(playerNumber: Int, spellName: String): com.wingedsheep.engine.core.ExecutionResult {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        val cardId = state.getHand(playerId).find {
            state.getEntity(it)?.get<CardComponent>()?.name == spellName
        } ?: error("Card '$spellName' not in player $playerNumber's hand")
        return execute(CastSpell(playerId, cardId, useWithoutPayingManaCost = true))
    }

    init {
        context("Ennis, Debate Moderator") {

            test("ETB blinks a creature you control and returns it at the next end step; Ennis gains a counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Ennis, Debate Moderator")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Ennis, Debate Moderator").error shouldBe null
                game.resolveStack()

                // ETB trigger ("up to one other target creature you control") pauses for a target.
                game.state.pendingDecision shouldNotBe null
                game.selectTargets(listOf(bears))
                game.resolveStack()

                withClue("the targeted creature is exiled") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }

                // Advance to Ennis's end step: the delayed return and the counter trigger resolve.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("the exiled creature returns to the battlefield") {
                    game.findPermanent("Grizzly Bears") shouldNotBe null
                }
                val ennis = game.findPermanent("Ennis, Debate Moderator")!!
                withClue("a card was put into exile this turn, so Ennis gets a +1/+1 counter") {
                    game.plusOneCounters(ennis) shouldBe 1
                }
            }

            test("no counter at end step when no card was put into exile this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ennis, Debate Moderator", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                val ennis = game.findPermanent("Ennis, Debate Moderator")!!
                withClue("nothing was exiled this turn → the intervening-if is false, no counter") {
                    game.plusOneCounters(ennis) shouldBe 0
                }
            }
        }

        context("Zaffai and the Tempests") {

            test("free-casts one instant/sorcery from hand per turn, then the permission is spent") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Zaffai and the Tempests", summoningSickness = false)
                    .withCardInHand(1, "Divination")
                    .withCardInHand(1, "Divination")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                // Library fodder so Divination's draws don't deck the player.
                repeat(6) { builder = builder.withCardInLibrary(1, "Forest") }
                val game = builder.build()

                val zaffai = game.findPermanent("Zaffai and the Tempests")!!

                // No lands in play — the cast is only possible because it's free.
                withClue("first free cast of the turn succeeds") {
                    game.freeCast(1, "Divination").error shouldBe null
                }
                withClue("the source is marked as having used its once-per-turn free cast") {
                    game.state.getEntity(zaffai)
                        ?.get<MayCastWithoutPayingCostUsedThisTurnComponent>() shouldNotBe null
                }
                game.resolveStack()

                withClue("a second free cast this turn is refused — the permission was consumed") {
                    game.freeCast(1, "Divination").error shouldNotBe null
                }
            }
        }
    }
}
