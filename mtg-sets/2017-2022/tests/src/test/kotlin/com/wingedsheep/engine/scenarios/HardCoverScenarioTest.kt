package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Hard Cover (M20 #63) — "Enchant creature. Enchanted creature gets +0/+2 and has
 * '{T}: Draw a card, then discard a card.'"
 */
class HardCoverScenarioTest : ScenarioTestBase() {

    init {
        val grantedId = cardRegistry.getCard("Hard Cover")!!.staticAbilities
            .filterIsInstance<GrantActivatedAbility>().single().ability.id

        test("enchanted creature gets +0/+2 and taps to loot") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Hard Cover", "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getPower(bears) shouldBe 2
            game.state.projectedState.getToughness(bears) shouldBe 4

            val result = game.execute(ActivateAbility(game.player1Id, bears, grantedId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            withClue("drew the Island, now chooses a card to discard") {
                game.handSize(1) shouldBe 2
                game.getPendingDecision().shouldNotBeNull()
            }
            val bolt = game.findCardsInHand(1, "Lightning Bolt").single()
            game.selectCards(listOf(bolt)).error shouldBe null
            game.resolveStack()

            game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
            game.handSize(1) shouldBe 1
            game.findCardsInHand(1, "Island").size shouldBe 1
            game.isInGraveyard(1, "Lightning Bolt") shouldBe true
        }
    }
}
