package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Wrench Mind (MRD) — "Target player discards two cards unless they discard an artifact card."
 *
 * "Unless" is a choice the *discarding* player makes on resolution, and the whole card lives or
 * dies on the two branches being genuinely available to them: pitch two cards of their choosing, or
 * a single artifact card. These tests pin both branches, that the decision belongs to the targeted
 * player rather than the caster, and the small-hand boundary where the choice collapses.
 */
class WrenchMindScenarioTest : ScenarioTestBase() {

    init {
        test("the targeted player may discard a single artifact card instead of two") {
            val game = scenario()
                .withPlayers("Caster", "Victim")
                .withCardInHand(1, "Wrench Mind")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInHand(2, "Bonesplitter")
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Wrench Mind", 2).error shouldBe null
            game.resolveStack()

            // The victim chooses from their own hand, not the caster.
            val decision = game.getPendingDecision().shouldNotBeNull()
            decision.playerId shouldBe game.player2Id

            val bonesplitter = game.findCardsInHand(2, "Bonesplitter").single()
            game.selectCards(listOf(bonesplitter)).error shouldBe null

            game.isInGraveyard(2, "Bonesplitter") shouldBe true
            game.handSize(2) shouldBe 2 // only the one artifact left
        }

        test("without an artifact in the selection, two cards go") {
            val game = scenario()
                .withPlayers("Caster", "Victim")
                .withCardInHand(1, "Wrench Mind")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInHand(2, "Bonesplitter")
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Wrench Mind", 2).error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldNotBeNull()

            val bears = game.findCardsInHand(2, "Grizzly Bears").single()
            val courser = game.findCardsInHand(2, "Centaur Courser").single()
            game.selectCards(listOf(bears, courser)).error shouldBe null

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Centaur Courser") shouldBe true
            game.isInGraveyard(2, "Bonesplitter") shouldBe false
            game.handSize(2) shouldBe 1
        }

        test("a hand with no artifact loses two cards") {
            val game = scenario()
                .withPlayers("Caster", "Victim")
                .withCardInHand(1, "Wrench Mind")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Centaur Courser")
                .withCardInHand(2, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Wrench Mind", 2).error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldNotBeNull()

            val bears = game.findCardsInHand(2, "Grizzly Bears").single()
            val bolt = game.findCardsInHand(2, "Lightning Bolt").single()
            game.selectCards(listOf(bears, bolt)).error shouldBe null

            game.handSize(2) shouldBe 1
        }

        test("an empty hand makes it a legal but inert cast") {
            val game = scenario()
                .withPlayers("Caster", "Victim")
                .withCardInHand(1, "Wrench Mind")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Wrench Mind", 2).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.handSize(2) shouldBe 0
            game.isInGraveyard(1, "Wrench Mind") shouldBe true
        }
    }
}
