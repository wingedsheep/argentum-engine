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
 * Consuming Fervor (AKH #126) — {R} Enchantment — Aura.
 *
 *   Enchant creature
 *   Enchanted creature gets +3/+3 and has "At the beginning of your upkeep, put a -1/-1 counter on
 *   this creature."
 *
 * The upkeep trigger is granted to the creature, so it fires on the *creature controller's* upkeep
 * and puts the counter on the creature — even when someone else controls the Aura.
 */
class ConsumingFervorScenarioTest : ScenarioTestBase() {

    private fun TestGame.minusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.MINUS_ONE_MINUS_ONE) ?: 0

    init {
        context("Consuming Fervor") {

            test("enchanted creature gets +3/+3") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Consuming Fervor", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.state.projectedState.getPower(bears) shouldBe 5
                game.state.projectedState.getToughness(bears) shouldBe 5
            }

            test("the creature's controller's upkeep puts a -1/-1 counter on the creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    // The Aura's controller is the opponent: the granted trigger still belongs to
                    // the creature, so it reads Player1's upkeep.
                    .withCardAttachedTo(2, "Consuming Fervor", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("one -1/-1 counter on the enchanted creature") { game.minusOnes(bears) shouldBe 1 }
                withClue("2/2 +3/+3 with a -1/-1 counter is 4/4") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 4
                }
            }
        }
    }
}
