package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Grim Servant (MH3) — "When this creature enters, search your library for a card with mana value
 * less than or equal to your devotion to black, reveal it, put it into your hand, then shuffle. You
 * lose 3 life."
 */
class GrimServantScenarioTest : ScenarioTestBase() {

    private fun TestGame.castServantAndReachSearch(): SelectCardsDecision {
        castSpell(1, "Grim Servant").error shouldBe null
        var guard = 0
        while (guard++ < 20) {
            when (val d = getPendingDecision()) {
                is SelectManaSourcesDecision -> submitManaSourcesAutoPay().error shouldBe null
                is SelectCardsDecision -> return d
                null -> resolveStack()
                else -> error("unexpected decision $d")
            }
        }
        error("search never offered")
    }

    private fun builder() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Grim Servant")
        .withLandsOnBattlefield(1, "Swamp", 4)
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(1, "Dark Ritual")
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Hypnotic Specter")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("devotion 1 from Grim Servant alone caps the search at mana value 1, then you lose 3 life") {
            val game = builder().build()
            val ritual = game.findCardsInLibrary(1, "Dark Ritual").single()
            val swamp = game.findCardsInLibrary(1, "Swamp").single()

            val decision = game.castServantAndReachSearch()
            decision.options shouldContainExactlyInAnyOrder listOf(ritual, swamp)

            game.selectCards(listOf(ritual)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Dark Ritual") shouldBe true
            game.getLifeTotal(1) shouldBe 17
        }

        test("other black permanents raise devotion and the cap") {
            val game = builder()
                .withCardOnBattlefield(1, "Hypnotic Specter")
                .build()
            val specterInLibrary = game.findCardsInLibrary(1, "Hypnotic Specter").single()

            // Grim Servant {B} + Hypnotic Specter {B}{B} = devotion 3: every card is findable.
            val decision = game.castServantAndReachSearch()
            decision.options.size shouldBe 4

            game.selectCards(listOf(specterInLibrary)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Hypnotic Specter") shouldBe true
            game.getLifeTotal(1) shouldBe 17
        }

        test("failing to find still loses 3 life") {
            val game = builder().build()
            game.castServantAndReachSearch()
            game.skipSelection().error shouldBe null
            game.resolveStack()

            game.state.pendingDecision shouldBe null
            game.getLifeTotal(1) shouldBe 17
        }
    }
}
