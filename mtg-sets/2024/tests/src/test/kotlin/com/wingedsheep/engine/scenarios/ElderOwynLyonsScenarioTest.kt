package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Elder Owyn Lyons (PIP #103) — {2}{W}{U} Legendary Creature — Human Knight 3/3.
 *
 *   Artifacts you control have ward {1}.
 *   When Elder Owyn Lyons enters or dies, return target artifact card from your graveyard to your hand.
 */
class ElderOwynLyonsScenarioTest : ScenarioTestBase() {

    init {
        test("entering returns the targeted artifact card from your graveyard to your hand") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Elder Owyn Lyons")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInGraveyard(1, "Ornithopter")
                .withCardInGraveyard(1, "Memnite")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Elder Owyn Lyons").error shouldBe null
            game.resolveStack()
            withClue("the enters trigger asks for a target") { game.hasPendingDecision() shouldBe true }

            val ornithopter = game.findCardsInGraveyard(1, "Ornithopter").single()
            game.selectTargets(listOf(ornithopter)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Ornithopter") shouldBe true
            game.isInGraveyard(1, "Memnite") shouldBe true
        }

        test("dying returns the targeted artifact card from your graveyard to your hand") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Elder Owyn Lyons")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInGraveyard(1, "Ornithopter")
                .withCardInGraveyard(1, "Memnite")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val owyn = game.findPermanent("Elder Owyn Lyons")!!

            game.castSpell(1, "Lightning Bolt", owyn).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Elder Owyn Lyons") shouldBe true
            withClue("the dies trigger asks for a target") { game.hasPendingDecision() shouldBe true }

            val memnite = game.findCardsInGraveyard(1, "Memnite").single()
            game.selectTargets(listOf(memnite)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Memnite") shouldBe true
            game.isInGraveyard(1, "Ornithopter") shouldBe true
        }

        test("an artifact you control has ward {1}: an opponent who can't pay has their spell countered") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Elder Owyn Lyons")
                .withCardOnBattlefield(1, "Ornithopter")
                .withCardInHand(2, "Shatter")
                // Exactly {1}{R} for Shatter: nothing left over for ward {1}.
                .withLandsOnBattlefield(2, "Mountain", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val ornithopter = game.findPermanent("Ornithopter")!!

            game.castSpell(2, "Shatter", ornithopter).error shouldBe null
            game.resolveStack()

            withClue("ward countered Shatter, so Ornithopter survives") {
                game.isOnBattlefield("Ornithopter") shouldBe true
                game.isInGraveyard(2, "Shatter") shouldBe true
            }
        }

        test("without Owyn, the same Shatter destroys the artifact") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Ornithopter")
                .withCardInHand(2, "Shatter")
                .withLandsOnBattlefield(2, "Mountain", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val ornithopter = game.findPermanent("Ornithopter")!!

            game.castSpell(2, "Shatter", ornithopter).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Ornithopter") shouldBe true
        }
    }
}
