package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Destructive Digger (M20 #134) — "{3}, {T}, Sacrifice an artifact or land: Draw a card."
 */
class DestructiveDiggerScenarioTest : ScenarioTestBase() {

    init {
        val abilityId = cardRegistry.getCard("Destructive Digger")!!.activatedAbilities.single().id

        test("sacrificing an artifact draws a card and taps the Digger") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Destructive Digger")
                .withCardOnBattlefield(1, "Ornithopter")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val digger = game.findPermanent("Destructive Digger")!!
            val thopter = game.findPermanent("Ornithopter")!!
            val handBefore = game.handSize(1)

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = digger,
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(thopter)),
                )
            )
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.isOnBattlefield("Ornithopter") shouldBe false
            game.isInGraveyard(1, "Ornithopter") shouldBe true
            game.state.getEntity(digger)!!.has<TappedComponent>() shouldBe true
            game.handSize(1) shouldBe handBefore + 1
        }

        test("a creature that is neither artifact nor land can't be sacrificed for the cost") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Destructive Digger")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val digger = game.findPermanent("Destructive Digger")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = digger,
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears)),
                )
            ).error shouldNotBe null
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
