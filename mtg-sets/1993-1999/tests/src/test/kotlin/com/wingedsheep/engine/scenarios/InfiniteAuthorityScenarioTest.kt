package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Infinite Authority — "Whenever enchanted creature blocks or becomes blocked by
 * a creature with toughness 3 or less, destroy the other creature at end of combat. At the beginning
 * of the next end step, if that creature was destroyed this way, put a +1/+1 counter on the first
 * creature."
 *
 * A delayed trigger that creates a delayed trigger: the cases pin that the partner dies at end of
 * combat (after damage), that the counter waits for the end step, that it lands only when the
 * destroy actually destroyed something (an indestructible partner gives nothing), that the
 * toughness filter excludes bigger partners, and that "the first creature" stays the creature that
 * blocked even after the Aura is gone.
 */
class InfiniteAuthorityScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(name: String): Int =
        state.getEntity(findPermanent(name)!!)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Player 2 attacks with [attacker] into Player 1's enchanted Wall of Granite, which blocks it. */
    private fun blockWithEnchantedWall(attacker: String): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Wall of Granite")
            .withCardAttachedTo(1, "Infinite Authority", "Wall of Granite")
            .withCardOnBattlefield(2, attacker)
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf(attacker to 1)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareBlockers(mapOf("Wall of Granite" to listOf(attacker))).error shouldBe null
        return game
    }

    init {
        context("Infinite Authority") {

            test("the blocked attacker dies at end of combat and the Wall gets a counter at the end step") {
                val game = blockWithEnchantedWall("Centaur Courser")

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                withClue("the 3/3 survived combat damage, then the end-of-combat trigger destroyed it") {
                    game.isInGraveyard(2, "Centaur Courser") shouldBe true
                }
                withClue("the counter waits for the end step") { game.plusOneCounters("Wall of Granite") shouldBe 0 }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.plusOneCounters("Wall of Granite") shouldBe 1
            }

            test("a partner with toughness 4 or more does not trigger it") {
                val game = blockWithEnchantedWall("Craw Wurm")

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.isOnBattlefield("Craw Wurm") shouldBe true
                game.plusOneCounters("Wall of Granite") shouldBe 0
            }

            test("an indestructible partner isn't destroyed this way, so no counter") {
                val game = blockWithEnchantedWall("Darksteel Myr")

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.isOnBattlefield("Darksteel Myr") shouldBe true
                withClue("the end-step half is conditional on the destroy") {
                    game.plusOneCounters("Wall of Granite") shouldBe 0
                }
            }

            test("the first creature still gets the counter after the Aura has left") {
                val game = blockWithEnchantedWall("Centaur Courser")

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.isInGraveyard(2, "Centaur Courser") shouldBe true
                val aura = game.findPermanent("Infinite Authority")!!
                game.state = game.zones.moveToZone(game.state, aura, Zone.GRAVEYARD).state

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                withClue("'the first creature' was fixed when the block triggered, not re-read off the Aura") {
                    game.plusOneCounters("Wall of Granite") shouldBe 1
                }
            }
        }
    }
}
