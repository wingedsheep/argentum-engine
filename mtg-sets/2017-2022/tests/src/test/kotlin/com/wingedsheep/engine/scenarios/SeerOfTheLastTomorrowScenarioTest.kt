package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hou.cards.SeerOfTheLastTomorrow
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe

/**
 * Seer of the Last Tomorrow (HOU) — {U}, {T}, Discard a card: Target player mills three cards.
 */
class SeerOfTheLastTomorrowScenarioTest : ScenarioTestBase() {
    init {
        test("{U}, {T}, discard: target opponent mills three") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Seer of the Last Tomorrow")
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardInHand(1, "Grizzly Bears")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val seer = game.findPermanent("Seer of the Last Tomorrow")!!
            val discard = game.findCardsInHand(1, "Grizzly Bears").single()

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = seer,
                    abilityId = SeerOfTheLastTomorrow.activatedAbilities.single().id,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                    costPayment = AdditionalCostPayment(discardedCards = listOf(discard))
                )
            ).error shouldBe null
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.state.getEntity(seer)?.has<TappedComponent>() shouldBe true

            game.resolveStack()
            game.graveyardSize(2) shouldBe 3
            game.librarySize(2) shouldBe 1
        }
    }
}
