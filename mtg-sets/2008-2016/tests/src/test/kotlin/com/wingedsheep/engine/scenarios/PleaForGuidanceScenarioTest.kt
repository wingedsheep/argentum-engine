package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Plea for Guidance (BNG #24) — "Search your library for up to two enchantment cards, reveal them,
 * put them into your hand, then shuffle."
 */
class PleaForGuidanceScenarioTest : ScenarioTestBase() {
    init {
        test("finds up to two enchantment cards and only enchantments are offered") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Plea for Guidance")
                .withLandsOnBattlefield(1, "Plains", 6)
                .withCardInLibrary(1, "Glorious Anthem")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Courser of Kruphix")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val enchantments = game.findCardsInLibrary(1, "Glorious Anthem") +
                game.findCardsInLibrary(1, "Courser of Kruphix")

            game.castSpell(1, "Plea for Guidance").error shouldBe null
            game.resolveStack()

            val search = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("only the two enchantment cards are offered") {
                search.options shouldContainExactlyInAnyOrder enchantments
            }
            game.selectCards(enchantments).error shouldBe null
            game.resolveStack()

            withClue("both enchantments are in hand, the creature stays in the library") {
                game.isInHand(1, "Glorious Anthem") shouldBe true
                game.isInHand(1, "Courser of Kruphix") shouldBe true
                game.librarySize(1) shouldBe 1
            }
        }
    }
}
