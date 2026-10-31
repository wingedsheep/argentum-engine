package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.war.cards.JacesProjection
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Jace's Projection (WAR #272) — {2}{U}{U} Creature — Wizard Illusion 2/2.
 *
 *   Whenever you draw a card, put a +1/+1 counter on this creature.
 *   {3}{U}: Put a loyalty counter on target Jace planeswalker.
 */
class JacesProjectionScenarioTest : ScenarioTestBase() {

    private val loyaltyAbility = JacesProjection.activatedAbilities[0].id

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    // Jace, Arcane Strategist pumps on your second draw, so the draw test keeps him off the board.
    private fun board(withPlaneswalkers: Boolean = true) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Jace's Projection")
        .let { if (withPlaneswalkers) it.withCardOnBattlefield(1, "Jace, Arcane Strategist").withCardOnBattlefield(1, "Chandra, Acolyte of Flame") else it }
        .withCardInHand(1, "Divination")
        .withLandsOnBattlefield(1, "Island", 4)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("each card you draw puts a +1/+1 counter on it") {
            val game = board(withPlaneswalkers = false)
            val projection = game.findPermanent("Jace's Projection")!!

            game.castSpell(1, "Divination").error shouldBe null
            game.resolveStack()

            count(game, projection, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            game.state.projectedState.getPower(projection) shouldBe 4
        }

        test("the ability adds a loyalty counter to a Jace but cannot target another planeswalker") {
            val game = board()
            val projection = game.findPermanent("Jace's Projection")!!
            val jace = game.findPermanent("Jace, Arcane Strategist")!!
            val chandra = game.findPermanent("Chandra, Acolyte of Flame")!!
            seedLoyalty(game, jace, 4)
            seedLoyalty(game, chandra, 4)

            game.execute(
                ActivateAbility(game.player1Id, projection, loyaltyAbility, targets = listOf(ChosenTarget.Permanent(chandra)))
            ).error shouldNotBe null

            game.execute(
                ActivateAbility(game.player1Id, projection, loyaltyAbility, targets = listOf(ChosenTarget.Permanent(jace)))
            ).error shouldBe null
            game.resolveStack()

            count(game, jace, CounterType.LOYALTY) shouldBe 5
            count(game, chandra, CounterType.LOYALTY) shouldBe 4
        }
    }
}
