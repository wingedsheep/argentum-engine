package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sift Through Sands (CHK #84) — "Draw two cards, then discard a card. If you've cast a spell named
 * Peer Through Depths and a spell named Reach Through Mists this turn, you may search your library
 * for a card named The Unspeakable, put it onto the battlefield, then shuffle."
 *
 * Pins the loot, both halves of the cast-history gate (each named spell is required, and it still
 * counts after it resolved), and the "you may" decline.
 */
class SiftThroughSandsScenarioTest : ScenarioTestBase() {

    /**
     * Twelve Islands then The Unspeakable last: Reach draws one, Peer bottoms five, Sift draws two —
     * none of which reaches The Unspeakable, so it is still in the library to be searched for.
     */
    private fun setup(withPeer: Boolean): TestGame {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Sift Through Sands")
            .withCardInHand(1, "Reach Through Mists")
            .withLandsOnBattlefield(1, "Island", 6)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (withPeer) builder = builder.withCardInHand(1, "Peer Through Depths")
        repeat(12) { builder = builder.withCardInLibrary(1, "Island") }
        builder = builder.withCardInLibrary(1, "The Unspeakable")
        repeat(3) { builder = builder.withCardInLibrary(2, "Island") }
        return builder.build()
    }

    private fun TestGame.castAndResolve(name: String) {
        castSpell(1, name).error shouldBe null
        resolveStack()
    }

    /** Peer Through Depths: take nothing, keep the order of what goes to the bottom. */
    private fun TestGame.finishPeer() {
        while (hasPendingDecision()) {
            when (getPendingDecision()) {
                is ReorderLibraryDecision -> keepLibraryOrder()
                else -> skipSelection()
            }
        }
    }

    /** Resolve Sift Through Sands up to (and through) its discard. */
    private fun TestGame.castSiftAndDiscard() {
        val handBefore = handSize(1)
        castAndResolve("Sift Through Sands")
        withClue("drew two before the discard prompt (spell left the hand)") {
            handSize(1) shouldBe handBefore - 1 + 2
        }
        getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
        val island = findCardsInHand(1, "Island").first()
        selectCards(listOf(island)).error shouldBe null
        withClue("then discarded one") { handSize(1) shouldBe handBefore }
    }

    init {
        context("Sift Through Sands") {

            test("after both named spells, it may fetch The Unspeakable onto the battlefield") {
                val game = setup(withPeer = true)
                game.castAndResolve("Reach Through Mists")
                game.castSpell(1, "Peer Through Depths").error shouldBe null
                game.resolveStack()
                game.finishPeer()

                game.castSiftAndDiscard()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null
                val search = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val unspeakable = game.findCardsInLibrary(1, "The Unspeakable").single()
                withClue("only The Unspeakable is searchable") { search.options shouldBe listOf(unspeakable) }
                game.selectCards(listOf(unspeakable)).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("The Unspeakable") shouldBe true
                game.isInGraveyard(1, "Sift Through Sands") shouldBe true
            }

            test("the controller may decline the search") {
                val game = setup(withPeer = true)
                game.castAndResolve("Reach Through Mists")
                game.castSpell(1, "Peer Through Depths").error shouldBe null
                game.resolveStack()
                game.finishPeer()

                game.castSiftAndDiscard()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.isOnBattlefield("The Unspeakable") shouldBe false
                game.findCardsInLibrary(1, "The Unspeakable").size shouldBe 1
            }

            test("with only Reach Through Mists cast, it just loots") {
                val game = setup(withPeer = false)
                game.castAndResolve("Reach Through Mists")

                game.castSiftAndDiscard()
                game.resolveStack()

                withClue("Peer Through Depths was never cast, so no search is offered") {
                    game.hasPendingDecision() shouldBe false
                }
                game.isOnBattlefield("The Unspeakable") shouldBe false
                game.findCardsInLibrary(1, "The Unspeakable").size shouldBe 1
            }
        }
    }
}
