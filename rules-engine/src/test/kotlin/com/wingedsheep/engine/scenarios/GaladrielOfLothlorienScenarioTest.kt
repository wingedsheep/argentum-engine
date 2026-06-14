package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blc.cards.TempleOfMystery
import com.wingedsheep.mtg.sets.definitions.ltr.cards.GaladrielOfLothlorien
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Galadriel of Lothlórien — "Whenever you scry, you may reveal the top card of your library.
 * If a land card is revealed this way, put it onto the battlefield tapped."
 *
 * Triggered by an unrelated scry (Temple of Mystery's ETB scry 1). Exercises the
 * gather→reveal→MoveCollection(filter) pipeline placing the revealed land tapped.
 */
class GaladrielOfLothlorienScenarioTest : FunSpec({

    test("a scry lets Galadriel reveal the top land and put it onto the battlefield tapped") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GaladrielOfLothlorien, TempleOfMystery))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!

        driver.putCreatureOnBattlefield(active, "Galadriel of Lothlórien")
        // Put a land on top so the scry (kept) then Galadriel's reveal find a land.
        driver.putCardOnTopOfLibrary(active, "Forest")

        val temple = driver.putCardInHand(active, "Temple of Mystery")
        driver.playLand(active, temple)

        // Drain: Temple ETB scry 1 (keep top → bottom none, reorder trivially), then Galadriel's
        // "may reveal" — say yes.
        var guard = 0
        while (guard++ < 12) {
            when (val pd = driver.pendingDecision) {
                is SelectCardsDecision -> driver.submitCardSelection(active, emptyList()) // scry: bottom none
                is ReorderLibraryDecision -> driver.submitOrderedResponse(active, pd.cards)
                is YesNoDecision -> driver.submitYesNo(active, true) // Galadriel: yes, reveal
                else -> if (driver.state.stack.isNotEmpty()) driver.bothPass() else break
            }
        }

        // The revealed Forest entered the battlefield tapped.
        val forestOnBf = driver.state.getBattlefield().firstOrNull {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Forest"
        }
        (forestOnBf != null) shouldBe true
        driver.state.getEntity(forestOnBf!!)?.has<TappedComponent>() shouldBe true
    }
})
