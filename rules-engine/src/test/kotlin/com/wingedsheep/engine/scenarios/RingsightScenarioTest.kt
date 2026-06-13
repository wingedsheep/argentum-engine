package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.TheRingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ringsight — "The Ring tempts you. Search your library for a card that shares a color with a
 * legendary creature you control, reveal it, put it into your hand, then shuffle." Exercises the
 * new `SharesColorWithPermanentYouControl` filter: only cards sharing a color with the controlled
 * legendary (mono-blue Naban) are eligible.
 */
class RingsightScenarioTest : ScenarioTestBase() {

    init {
        test("only fetches a card sharing a color with a legendary creature you control") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Naban, Dean of Iteration") // mono-blue legendary
                .withCardInHand(1, "Ringsight")
                .withLandsOnBattlefield(1, "Island", 2)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardInLibrary(1, "Glorious Gale") // mono-blue → eligible
                .withCardInLibrary(1, "Grizzly Bears") // mono-green → not eligible
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Ringsight").error shouldBe null
            game.resolveStack()

            // First decision: the Ring temptation picks a Ring-bearer (the only creature is Naban).
            val ringDec = game.getPendingDecision()
            ringDec.shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(listOf(ringDec.options.first()))

            // Second decision: the filtered library search.
            val searchDec = game.getPendingDecision()
            searchDec.shouldBeInstanceOf<SelectCardsDecision>()
            val optionNames = searchDec.options.map { game.state.getEntity(it)?.get<CardComponent>()?.name }
            optionNames shouldContain "Glorious Gale"
            optionNames shouldNotContain "Grizzly Bears"

            val gale = searchDec.options.first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Glorious Gale"
            }
            game.selectCards(listOf(gale))
            game.resolveStack()

            game.state.getHand(game.player1Id).count {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Glorious Gale"
            } shouldBe 1
            game.state.getEntity(game.player1Id)?.get<TheRingComponent>()?.temptCount shouldBe 1
        }
    }
}
