package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Well Rested (PIP #88) — {1}{G} Enchantment — Aura.
 *
 *   Enchant creature
 *   Enchanted creature has "Whenever this creature becomes untapped, put two +1/+1 counters on
 *   it, then you gain 2 life and draw a card. This ability triggers only once each turn."
 */
class WellRestedScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.isTapped(id: EntityId): Boolean =
        state.getEntity(id)!!.has<TappedComponent>()

    init {
        test("untapping during its controller's untap step adds two counters, gains 2 life and draws") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                .withCardAttachedTo(1, "Well Rested", "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                // Start on the opponent's turn so the next untap step is ours.
                .withActivePlayer(2)
                .withPriorityPlayer(2)
                .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val life = game.getLifeTotal(1)

            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.resolveStack()

            game.isTapped(bears) shouldBe false
            game.plusOnes(bears) shouldBe 2
            game.getLifeTotal(1) shouldBe life + 2
            // One card from the draw step, one from Well Rested.
            game.handSize(1) shouldBe 2
        }

        test("an untap effect triggers it, but only once each turn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                .withCardAttachedTo(1, "Well Rested", "Grizzly Bears")
                .withCardsInHand(1, "Inspirit", 2)
                .withLandsOnBattlefield(1, "Plains", 6)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val life = game.getLifeTotal(1)

            game.castSpell(1, "Inspirit", bears).error shouldBe null
            game.resolveStack()

            game.isTapped(bears) shouldBe false
            game.plusOnes(bears) shouldBe 2
            game.getLifeTotal(1) shouldBe life + 2
            game.handSize(1) shouldBe 2 // one Inspirit left + the drawn card

            // Tap it again and untap it a second time this turn: no second trigger.
            game.state = game.state.updateEntity(bears) { it.with(TappedComponent) }
            game.castSpell(1, "Inspirit", bears).error shouldBe null
            game.resolveStack()

            game.isTapped(bears) shouldBe false
            game.plusOnes(bears) shouldBe 2
            game.getLifeTotal(1) shouldBe life + 2
            game.handSize(1) shouldBe 1
        }
    }
}
