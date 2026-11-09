package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.tor.cards.GhostlyWings
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe

/**
 * Ghostly Wings (TOR #38) — {1}{U} Enchantment — Aura.
 *
 *   Enchant creature
 *   Enchanted creature gets +1/+1 and has flying.
 *   Discard a card: Return enchanted creature to its owner's hand.
 */
class GhostlyWingsScenarioTest : ScenarioTestBase() {

    private val abilityId = GhostlyWings.activatedAbilities.first().id

    private fun build() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardAttachedTo(1, "Ghostly Wings", "Grizzly Bears")
        .withCardInHand(1, "Lightning Bolt")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("the enchanted creature gets +1/+1 and flying") {
            val game = build()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.getToughness(bears) shouldBe 3
            game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
        }

        test("discarding a card returns the enchanted creature to its owner's hand") {
            val game = build()
            val wings = game.findPermanent("Ghostly Wings")!!
            val bolt = game.findCardsInHand(1, "Lightning Bolt").single()

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = wings,
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(discardedCards = listOf(bolt)),
                )
            ).error shouldBe null
            game.isInGraveyard(1, "Lightning Bolt") shouldBe true
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.isInGraveyard(1, "Ghostly Wings") shouldBe true
        }
    }
}
