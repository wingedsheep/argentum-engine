package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Mesmerizing Dose (ONE #62) — {1}{U}{U} Enchantment — Aura.
 *
 *   Enchant creature
 *   When this Aura enters, tap enchanted creature, then proliferate.
 *   Enchanted creature doesn't untap during its controller's untap step.
 *
 * Pins the enters trigger (tap, then a proliferate choice) and the untap lock.
 */
class MesmerizingDoseScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, type: CounterType, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, amount))
        }
    }

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    init {
        test("taps the enchanted creature, proliferates, and keeps the creature tapped") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Mesmerizing Dose")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            seed(game, giant, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(1, "Mesmerizing Dose", bears).error shouldBe null
            game.resolveStack()

            withClue("the Aura attached and its enters trigger tapped the creature before proliferating") {
                game.isOnBattlefield("Mesmerizing Dose") shouldBe true
                game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
            }

            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            withClue("proliferate added another +1/+1 counter to the chosen permanent") {
                count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            }

            // Into Bob's turn, past his untap step.
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("it's Bob's turn") { game.state.activePlayerId shouldBe game.player2Id }
            withClue("the creature didn't untap during its controller's untap step") {
                game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
            }
        }
    }
}
