package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Izzet Charm (DDJ, reprinted in RTR) — modal: counter noncreature spell unless {2} / 2 damage to
 * target creature / draw two, then discard two.
 */
class IzzetCharmScenarioTest : ScenarioTestBase() {
    init {
        fun setup() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Izzet Charm")
            .withLandsOnBattlefield(1, "Island", 1)
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("second mode deals 2 damage to a creature, killing a 2/2") {
            val game = setup()
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            val result = game.castSpellWithMode(1, "Izzet Charm", 1, bears)
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            withClue("the Bears died to 2 damage") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }
        }

        test("third mode draws two then discards two chosen cards") {
            val game = setup()
                .withCardInHand(1, "Hill Giant")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Forest")
                .build()

            val result = game.castSpellWithMode(1, "Izzet Charm", 2)
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            withClue("after drawing two the caster must choose discards") {
                game.handSize(1) shouldBe 3
                game.librarySize(1) shouldBe 1
                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            }
            val keep = game.state.getHand(game.player1Id).first { it !in game.findCardsInHand(1, "Hill Giant") }
            val discards = game.state.getHand(game.player1Id).filter { it != keep }
            game.selectCards(discards).error shouldBe null
            game.resolveStack()

            withClue("one card left in hand, the two chosen ones discarded") {
                game.state.getHand(game.player1Id) shouldBe listOf(keep)
                game.isInGraveyard(1, "Hill Giant") shouldBe true
                game.graveyardSize(1) shouldBe 3 // two discards + Izzet Charm
            }
        }
    }
}
