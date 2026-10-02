package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh3.cards.TempestHarvester
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tempest Harvester (MH3) — "When this creature enters, you get {E}{E}. /
 * {T}, Pay {E}: Draw a card, then discard a card."
 */
class TempestHarvesterScenarioTest : ScenarioTestBase() {

    private val lootAbility = TempestHarvester.activatedAbilities.single().id

    private fun TestGame.energy(): Int =
        state.getEntity(player1Id)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    private fun TestGame.setEnergy(n: Int) {
        state = state.updateEntity(player1Id) { it.with(CountersComponent(mapOf(CounterType.ENERGY to n))) }
    }

    init {
        context("Tempest Harvester") {

            test("entering gets you two energy counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Tempest Harvester")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Tempest Harvester").error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Tempest Harvester") shouldBe true
                game.energy() shouldBe 2
            }

            test("tap and pay one energy to draw a card, then discard a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tempest Harvester")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.setEnergy(2)

                val harvester = game.findPermanent("Tempest Harvester")!!
                game.execute(ActivateAbility(game.player1Id, harvester, lootAbility)).error shouldBe null
                game.energy() shouldBe 1
                game.resolveStack()

                game.handSize(1) shouldBe 2
                val discard = game.getPendingDecision()
                discard.shouldBeInstanceOf<SelectCardsDecision>()
                val bears = game.findCardsInHand(1, "Grizzly Bears").single()
                game.selectCards(listOf(bears)).error shouldBe null

                game.handSize(1) shouldBe 1
                game.isInHand(1, "Forest") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true

                // Tapped now — can't loot again this turn even with energy left.
                game.execute(ActivateAbility(game.player1Id, harvester, lootAbility)).error shouldNotBe null
            }

            test("cannot activate without energy") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tempest Harvester")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val harvester = game.findPermanent("Tempest Harvester")!!
                game.execute(ActivateAbility(game.player1Id, harvester, lootAbility)).error shouldNotBe null
                game.handSize(1) shouldBe 0
            }
        }
    }
}
