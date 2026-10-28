package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mox Diamond — "If this artifact would enter, you may discard a land card instead. If you do, put
 * this artifact onto the battlefield. If you don't, put it into its owner's graveyard. {T}: Add
 * one mana of any color." The replacement itself is pinned by `EntersOnlyIfPaidScenarioTest`; this
 * covers the card end to end: discard a land, then spend its mana.
 */
class MoxDiamondScenarioTest : ScenarioTestBase() {
    init {
        test("discarding a land puts it onto the battlefield, and it taps for any color") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Mox Diamond")
                .withCardInHand(1, "Swamp")
                .withCardInHand(1, "Llanowar Elves")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Mox Diamond").error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(game.findCardsInHand(1, "Swamp")).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Mox Diamond") shouldBe true
            game.isInGraveyard(1, "Swamp") shouldBe true

            // A Swamp was discarded, yet the Mox still pays {G}.
            game.castSpell(1, "Llanowar Elves").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Llanowar Elves") shouldBe true
        }

        test("keeping the land puts it into the graveyard") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Mox Diamond")
                .withCardInHand(1, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Mox Diamond").error shouldBe null
            game.resolveStack()
            game.selectCards(emptyList()).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Mox Diamond") shouldBe true
            game.isInHand(1, "Swamp") shouldBe true
        }
    }
}
