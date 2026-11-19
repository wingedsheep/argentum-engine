package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Oracle's Insight (BNG #47) — "Enchanted creature has '{T}: Scry 1, then draw a card.'"
 */
class OraclesInsightScenarioTest : ScenarioTestBase() {
    init {
        val grantedId = cardRegistry.getCard("Oracle's Insight")!!.staticAbilities
            .filterIsInstance<GrantActivatedAbility>().single().ability.id

        test("tapping the enchanted creature scries 1, then draws") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Oracle's Insight", "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val top = game.state.getLibrary(game.player1Id).first()
            val second = game.state.getLibrary(game.player1Id)[1]

            val result = game.execute(ActivateAbility(game.player1Id, bears, grantedId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            val scry = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("scry 1 looks at the top card") { scry.options shouldBe listOf(top) }
            game.selectCards(listOf(top)).error shouldBe null
            game.resolveStack()

            game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
            withClue("the scried card went to the bottom, and the next card was drawn") {
                game.state.getHand(game.player1Id) shouldBe listOf(second)
                game.state.getLibrary(game.player1Id) shouldBe listOf(top)
            }
        }
    }
}
