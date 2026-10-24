package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Promising Duskmage (STX #85) — "When this creature dies, if it had a +1/+1 counter on it, draw a
 * card."
 *
 * The intervening-if reads last-known information: by the time the trigger is checked the creature
 * is in the graveyard and its counters are gone, so the condition must read the +1/+1 count the
 * dies event carried.
 */
class PromisingDuskmageScenarioTest : ScenarioTestBase() {

    private val testExecution = card("Duskmage Test Execution") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val victim = target(TargetFilter.Creature)
            effect = Effects.Destroy(victim)
        }
    }

    init {
        cardRegistry.register(testExecution)

        fun board(withCounter: Boolean): TestGame {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Promising Duskmage")
                .withCardInHand(1, "Duskmage Test Execution")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            if (withCounter) {
                val duskmage = game.findPermanent("Promising Duskmage")!!
                game.state = game.state.updateEntity(duskmage) { c ->
                    c.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
                }
            }
            return game
        }

        context("Promising Duskmage") {

            test("dying with a +1/+1 counter draws a card") {
                val game = board(withCounter = true)
                val duskmage = game.findPermanent("Promising Duskmage")!!

                game.castSpell(1, "Duskmage Test Execution", targetId = duskmage).error shouldBe null
                game.resolveStack()

                withClue("Duskmage died") { game.isInGraveyard(1, "Promising Duskmage") shouldBe true }
                withClue("the last-known +1/+1 counter satisfies the intervening-if") {
                    game.handSize(1) shouldBe 1
                }
            }

            test("dying without a +1/+1 counter draws nothing") {
                val game = board(withCounter = false)
                val duskmage = game.findPermanent("Promising Duskmage")!!

                game.castSpell(1, "Duskmage Test Execution", targetId = duskmage).error shouldBe null
                game.resolveStack()

                withClue("Duskmage died") { game.isInGraveyard(1, "Promising Duskmage") shouldBe true }
                withClue("no counter, so the ability never triggers") { game.handSize(1) shouldBe 0 }
            }
        }
    }
}
