package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Halo Forager (MOM #227) — "When this creature enters, you may pay {X}. When you do, you may cast
 * target instant or sorcery card with mana value X from a graveyard without paying its mana cost.
 * If that spell would be put into a graveyard, exile it instead."
 */
class HaloForagerScenarioTest : ScenarioTestBase() {

    init {
        context("Halo Forager") {

            fun board() = scenario()
                .withPlayers()
                .withCardInHand(1, "Halo Forager")
                .withLandsOnBattlefield(1, "Island", 2)
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInGraveyard(2, "Lightning Bolt")   // MV 1, opponent's graveyard
                .withCardInGraveyard(1, "Doom Blade")       // MV 2, your graveyard
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("paying X = 1 targets only mana value 1 cards in any graveyard and casts one free, then exiles it") {
                val game = board()
                val bolt = game.findCardsInGraveyard(2, "Lightning Bolt").single()

                game.castSpell(1, "Halo Forager").error shouldBe null
                game.resolveStack()

                (game.getPendingDecision() is ChooseNumberDecision) shouldBe true
                game.chooseNumber(1).error shouldBe null

                val targets = game.getPendingDecision()
                withClue("reflexive trigger asks for its target: $targets") {
                    (targets is ChooseTargetsDecision) shouldBe true
                }
                targets as ChooseTargetsDecision
                withClue("only the mana value 1 instant/sorcery, from the opponent's graveyard") {
                    targets.legalTargets[0]?.toSet() shouldBe setOf(bolt)
                }
                game.selectTargets(listOf(bolt)).error shouldBe null
                game.resolveStack()

                (game.getPendingDecision() is YesNoDecision) shouldBe true
                game.answerYesNo(true).error shouldBe null
                (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
                withClue("the free spell is exiled instead of going to a graveyard") {
                    game.isInExile(2, "Lightning Bolt") shouldBe true
                    game.isInGraveyard(2, "Lightning Bolt") shouldBe false
                }
                game.isInGraveyard(1, "Doom Blade") shouldBe true
            }

            test("declining the cast leaves the target in its graveyard") {
                val game = board()
                val bolt = game.findCardsInGraveyard(2, "Lightning Bolt").single()

                game.castSpell(1, "Halo Forager").error shouldBe null
                game.resolveStack()
                game.chooseNumber(1).error shouldBe null
                game.selectTargets(listOf(bolt)).error shouldBe null
                game.resolveStack()

                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 20
                game.isInGraveyard(2, "Lightning Bolt") shouldBe true
            }

            test("declining to pay (X = 0) does nothing") {
                val game = board()

                game.castSpell(1, "Halo Forager").error shouldBe null
                game.resolveStack()
                game.chooseNumber(0).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.isOnBattlefield("Halo Forager") shouldBe true
                game.isInGraveyard(2, "Lightning Bolt") shouldBe true
            }
        }
    }
}
