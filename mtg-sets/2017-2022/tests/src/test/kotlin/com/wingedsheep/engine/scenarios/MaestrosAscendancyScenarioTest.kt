package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Maestros Ascendancy — "Once during each of your turns, you may cast an instant or sorcery spell
 * from your graveyard by sacrificing a creature in addition to paying its other costs. If a spell
 * cast this way would be put into your graveyard, exile it instead."
 *
 * First graveyard-cast grant whose additional cost is a sacrifice: the cast is only offered with
 * a creature to sacrifice, the creature is sacrificed on cast, the spell is exiled after resolving,
 * and the grant is spent for the turn.
 */
class MaestrosAscendancyScenarioTest : ScenarioTestBase() {

    private fun game(withCreature: Boolean = true) = scenario()
        .withPlayers("P1", "P2")
        .withCardOnBattlefield(1, "Maestros Ascendancy")
        .let { if (withCreature) it.withCardOnBattlefield(1, "Grizzly Bears") else it }
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withCardInGraveyard(1, "Lightning Bolt")
        .withCardInGraveyard(1, "Shock")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.graveyardCasts(cardId: EntityId) =
        getLegalActions(1).filter { (it.action as? CastSpell)?.cardId == cardId }

    init {
        test("an instant is cast from the graveyard by sacrificing a creature, then exiled") {
            val game = game()
            val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
            val bears = game.findPermanent("Grizzly Bears")!!

            val offered = game.graveyardCasts(bolt).single()
            offered.additionalCostInfo shouldNotBe null

            game.execute(
                CastSpell(
                    game.player1Id, bolt,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                    additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears)),
                    graveyardCastRider = (offered.action as CastSpell).graveyardCastRider
                )
            ).error shouldBe null
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            game.isInExile(1, "Lightning Bolt") shouldBe true
            game.isInGraveyard(1, "Lightning Bolt") shouldBe false
        }

        test("only once each turn") {
            val game = game()
            val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
            val shock = game.findCardsInGraveyard(1, "Shock").single()
            val bears = game.findPermanent("Grizzly Bears")!!
            val offered = game.graveyardCasts(bolt).single()

            game.execute(
                CastSpell(
                    game.player1Id, bolt,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                    additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears)),
                    graveyardCastRider = (offered.action as CastSpell).graveyardCastRider
                )
            ).error shouldBe null
            game.resolveStack()

            game.graveyardCasts(shock).size shouldBe 0
        }

        test("with no creature to sacrifice there is no graveyard cast") {
            val game = game(withCreature = false)
            val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()

            game.graveyardCasts(bolt).size shouldBe 0
        }
    }
}
