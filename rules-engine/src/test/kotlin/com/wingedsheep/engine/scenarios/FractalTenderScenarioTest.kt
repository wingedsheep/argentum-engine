package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Fractal Tender (Secrets of Strixhaven #190).
 *
 * Fractal Tender ({3}{G}{U}, 3/3, Elf Wizard):
 *   Ward {2}
 *   Increment (Whenever you cast a spell, if the amount of mana you spent is greater than this
 *     creature's power or toughness, put a +1/+1 counter on this creature.)
 *   At the beginning of each end step, if you put a counter on this creature this turn, create a
 *     0/0 green and blue Fractal creature token and put three +1/+1 counters on it.
 *
 * Exercises the new Increment keyword (cast-spell intervening-if on mana spent vs power/toughness)
 * and the SourceReceivedCounterThisTurn end-step condition.
 */
class FractalTenderScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Fractal Tender — Increment + end-step Fractal token") {

            test("casting a spell with mana spent > P/T increments, then end step makes a Fractal") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Fractal Tender")
                    .withCardInHand(1, "Stoke the Flames") // {2}{R}{R} = 4 mana spent
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tender = game.findPermanent("Fractal Tender")!!

                // Cast Stoke the Flames paying 4 mana (no convoke), targeting the opponent.
                val cast = game.castSpellTargetingPlayer(1, "Stoke the Flames", 2)
                withClue("Casting Stoke the Flames should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack() // Increment trigger resolves: 4 > 3 -> +1/+1 counter
                game.resolveStack() // Stoke the Flames resolves

                withClue("Increment should have placed one +1/+1 counter (4 mana > 3 toughness)") {
                    plusOneCounters(game, tender) shouldBe 1
                }

                // Advance to the end step; the intervening-if (counter placed this turn) is true.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                val fractal = game.findPermanent("Fractal Token")
                withClue("A Fractal token should have been created at the end step") {
                    (fractal != null) shouldBe true
                }
                withClue("The Fractal token enters with three +1/+1 counters (0/0 base)") {
                    plusOneCounters(game, fractal!!) shouldBe 3
                }
            }

            test("casting a spell with mana spent not greater than P/T does not increment") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Fractal Tender")
                    .withCardInHand(1, "Lightning Bolt") // {R} = 1 mana spent
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tender = game.findPermanent("Fractal Tender")!!

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
                game.resolveStack()

                withClue("1 mana is not greater than power 3 or toughness 3 — no counter") {
                    plusOneCounters(game, tender) shouldBe 0
                }

                // No counter placed this turn -> no Fractal token at end step.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                withClue("No Fractal token without a counter placed this turn") {
                    (game.findPermanent("Fractal Token") == null) shouldBe true
                }
            }
        }
    }
}
