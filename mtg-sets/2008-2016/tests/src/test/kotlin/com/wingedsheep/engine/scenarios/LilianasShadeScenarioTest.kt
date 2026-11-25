package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Liliana's Shade (M13 #98) — {2}{B}{B} 1/1 Creature — Shade.
 *   When this creature enters, you may search your library for a Swamp card, reveal it, put it
 *   into your hand, then shuffle.
 *   {B}: This creature gets +1/+1 until end of turn.
 */
class LilianasShadeScenarioTest : ScenarioTestBase() {
    init {
        test("entering lets you search for a Swamp card — and only a Swamp card") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Liliana's Shade")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
            val swamp = game.findCardsInLibrary(1, "Swamp").single()

            val result = game.castSpell(1, "Liliana's Shade")
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true)
            val search = game.getPendingDecision()
            search.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("a non-Swamp isn't offered") { search.options shouldNotContain bears }
            game.selectCards(listOf(swamp)).error shouldBe null

            game.isInHand(1, "Swamp") shouldBe true
            game.isInHand(1, "Grizzly Bears") shouldBe false
        }

        test("{B} pumps it +1/+1") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Liliana's Shade")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val shade = game.findPermanent("Liliana's Shade")!!
            val pumpId = cardRegistry.getCard("Liliana's Shade")!!.script.activatedAbilities.single().id

            val result = game.execute(ActivateAbility(game.player1Id, shade, pumpId))
            withClue("pump should activate: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.projectedState.getPower(shade) shouldBe 2
            game.state.projectedState.getToughness(shade) shouldBe 2
        }
    }
}
