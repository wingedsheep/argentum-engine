package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Epiphany Storm (BNG #91) — "Enchanted creature has '{R}, {T}, Discard a card: Draw a card.'"
 */
class EpiphanyStormScenarioTest : ScenarioTestBase() {
    init {
        val grantedId = cardRegistry.getCard("Epiphany Storm")!!.staticAbilities
            .filterIsInstance<GrantActivatedAbility>().single().ability.id

        test("the enchanted creature discards a card to draw a card") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Epiphany Storm", "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Forest")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val forest = game.findCardsInHand(1, "Forest").single()

            val result = game.execute(
                ActivateAbility(
                    game.player1Id, bears, grantedId,
                    costPayment = AdditionalCostPayment(discardedCards = listOf(forest)),
                )
            )
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.isInGraveyard(1, "Forest") shouldBe true
            game.isInHand(1, "Island") shouldBe true
            game.handSize(1) shouldBe 1
        }
    }
}
