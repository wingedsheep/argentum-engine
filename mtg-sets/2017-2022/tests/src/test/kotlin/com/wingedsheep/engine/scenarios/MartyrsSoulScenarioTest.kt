package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Martyr's Soul (MH1 #19) — {2}{W} Creature — Spirit Soldier, 3/2.
 *
 *   Convoke
 *   When this creature enters, if you control no tapped lands, put two +1/+1 counters on it.
 *
 * Paying {2}{W} with lands taps them, so the intervening-if fails; convoking with creatures for the
 * whole cost leaves every land untapped and the Soul gets its two counters. A land that was already
 * tapped before casting also stops the counters.
 */
class MartyrsSoulScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun fullConvoke(game: TestGame): AlternativePaymentChoice {
        val (firstBears, secondBears) = game.findPermanents("Grizzly Bears")
        val lions = game.findPermanent("Savannah Lions")!!
        return AlternativePaymentChoice(
            convokedCreatures = mapOf(
                firstBears to ConvokePayment(color = null),
                secondBears to ConvokePayment(color = null),
                lions to ConvokePayment(color = Color.WHITE)
            )
        )
    }

    init {
        context("Martyr's Soul enters-with-counters intervening-if") {

            test("convoked for the whole cost with only untapped lands: gets two +1/+1 counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Martyr's Soul")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Martyr's Soul").single(),
                        alternativePayment = fullConvoke(game)
                    )
                )
                withClue("two Bears pay {2}, Savannah Lions pays {W}: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val soul = game.findPermanent("Martyr's Soul")!!
                withClue("no lands were tapped, so the trigger puts two +1/+1 counters on it") {
                    plusOneCounters(game, soul) shouldBe 2
                    game.state.projectedState.getPower(soul) shouldBe 5
                    game.state.projectedState.getToughness(soul) shouldBe 4
                }
            }

            test("hard-cast by tapping lands for mana: no counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Martyr's Soul")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Martyr's Soul").single()
                    )
                )
                withClue("three Plains pay {2}{W}: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val soul = game.findPermanent("Martyr's Soul")!!
                withClue("the Plains tapped for its cost are still tapped, so no counters") {
                    plusOneCounters(game, soul) shouldBe 0
                    game.state.projectedState.getPower(soul) shouldBe 3
                    game.state.projectedState.getToughness(soul) shouldBe 2
                }
            }

            test("fully convoked but a land was already tapped: no counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Martyr's Soul")
                    .withCardOnBattlefield(1, "Plains", tapped = true)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Martyr's Soul").single(),
                        alternativePayment = fullConvoke(game)
                    )
                )
                withClue("convoke pays the whole cost: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val soul = game.findPermanent("Martyr's Soul")!!
                withClue("a tapped land on entry means the ability doesn't trigger") {
                    plusOneCounters(game, soul) shouldBe 0
                    game.state.projectedState.getPower(soul) shouldBe 3
                }
            }
        }
    }
}
