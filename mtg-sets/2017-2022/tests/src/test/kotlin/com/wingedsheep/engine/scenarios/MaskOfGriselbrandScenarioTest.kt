package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Mask of Griselbrand (MID #111) — {1}{B}{B} Legendary Artifact — Equipment.
 *
 * "Equipped creature has flying and lifelink.
 *  Whenever equipped creature dies, you may pay X life, where X is its power. If you do, draw X cards."
 *
 * Proves X is the creature's last-known power (a Giant Growth pump counts — 2021-09-24 ruling),
 * that the same X drives both the payment and the draw, that declining costs nothing, and that a
 * player with less than X life is never offered the payment (exactly X or nothing — CR 119.4).
 */
class MaskOfGriselbrandScenarioTest : ScenarioTestBase() {

    private fun setup(life: Int = 20) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardAttachedTo(1, "Mask of Griselbrand", "Grizzly Bears")
        .withCardInHand(1, "Giant Growth")
        .withCardInHand(1, "Doom Blade")
        .withLandsOnBattlefield(1, "Forest", 1)
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withLifeTotal(1, life)
        .apply { repeat(7) { withCardInLibrary(1, "Island") } }
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun pumpAndKill(game: TestGame) {
        val bears = game.findPermanent("Grizzly Bears")!!
        game.castSpell(1, "Giant Growth", bears).error shouldBe null
        game.resolveStack()
        game.state.projectedState.getPower(bears) shouldBe 5
        game.castSpell(1, "Doom Blade", bears).error shouldBe null
        game.resolveStack()
    }

    init {
        context("Mask of Griselbrand") {

            test("equipped creature has flying and lifelink") {
                val game = setup()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
                game.state.projectedState.hasKeyword(bears, Keyword.LIFELINK) shouldBe true
            }

            test("paying X life draws X cards, X being the last-known power") {
                val game = setup()
                pumpAndKill(game)

                withClue("the dies trigger offers the payment") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe true
                }
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                withClue("X = 5 (2 base + Giant Growth): pay 5 life, draw 5 cards") {
                    game.getLifeTotal(1) shouldBe 15
                    game.handSize(1) shouldBe 5
                    game.librarySize(1) shouldBe 2
                }
            }

            test("declining pays no life and draws nothing") {
                val game = setup()
                pumpAndKill(game)

                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 20
                game.handSize(1) shouldBe 0
                game.librarySize(1) shouldBe 7
            }

            test("with less than X life the payment is not offered") {
                val game = setup(life = 4)
                pumpAndKill(game)

                withClue("4 life can't pay exactly 5, and paying less isn't allowed") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe false
                }
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 4
                game.handSize(1) shouldBe 0
                game.librarySize(1) shouldBe 7
            }
        }
    }
}
