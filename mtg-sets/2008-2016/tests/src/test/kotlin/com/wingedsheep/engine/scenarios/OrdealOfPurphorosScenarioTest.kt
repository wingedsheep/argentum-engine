package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ordeal of Purphoros (DDL #23, THS #131, J22 #577) — {1}{R} Enchantment — Aura.
 *
 *   Enchant creature
 *   Whenever enchanted creature attacks, put a +1/+1 counter on it. Then if it has three or more
 *   +1/+1 counters on it, sacrifice this Aura.
 *   When you sacrifice this Aura, it deals 3 damage to any target.
 *
 * Pins the chain: the attack trigger's own sacrifice fires the *targeted* sacrifice trigger
 * from the graveyard, and below three counters the Aura stays put.
 */
class OrdealOfPurphorosScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun setup(startingCounters: Int): TestGame {
        val game = scenario()
            .withPlayers("Alice", "Bob")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardAttachedTo(1, "Ordeal of Purphoros", "Grizzly Bears")
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(2, "Mountain")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        if (startingCounters > 0) {
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(bears) {
                it.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, startingCounters))
            }
        }
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        return game
    }

    init {
        context("Ordeal of Purphoros") {

            test("the third counter sacrifices the Ordeal, which deals 3 damage to any target") {
                val game = setup(startingCounters = 2)
                val bears = game.findPermanent("Grizzly Bears")!!
                val bobLife = game.getLifeTotal(2)

                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.resolveStack()

                withClue("the attack trigger put the third counter on, then sacrificed the Aura") {
                    game.plusOneCounters(bears) shouldBe 3
                    game.isInGraveyard(1, "Ordeal of Purphoros") shouldBe true
                }
                withClue("the sacrifice trigger asks for its target") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe bobLife - 3
            }

            test("below three counters the Ordeal stays on the creature") {
                val game = setup(startingCounters = 0)
                val bears = game.findPermanent("Grizzly Bears")!!

                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.resolveStack()

                game.plusOneCounters(bears) shouldBe 1
                game.findPermanent("Ordeal of Purphoros") shouldNotBe null
                game.hasPendingDecision() shouldBe false
            }
        }
    }
}
