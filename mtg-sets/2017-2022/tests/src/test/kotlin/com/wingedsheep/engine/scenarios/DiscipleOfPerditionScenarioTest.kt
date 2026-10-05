package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Disciple of Perdition (J22 #23) — {1}{B} Creature — Human Warlock, 1/3.
 *
 * "When this creature dies, choose one. If you have exactly 13 life, you may choose both instead.
 *  • You draw a card and you lose 1 life.
 *  • Exile target opponent's graveyard. That player loses 1 life."
 *
 * Covers the conditional mode cap (two modes only at exactly 13 life, one otherwise) and that the
 * second mode exiles the *opponent's* graveyard and drains *that* player, not the controller.
 */
class DiscipleOfPerditionScenarioTest : ScenarioTestBase() {

    /** Alice controls the Disciple and kills it with her own Murder; Bob has two cards in his graveyard. */
    private fun killDisciple(aliceLife: Int): TestGame {
        val game = scenario()
            .withPlayers("Alice", "Bob")
            .withCardOnBattlefield(1, "Disciple of Perdition")
            .withCardInHand(1, "Murder")
            .withLandsOnBattlefield(1, "Swamp", 3)
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withCardInGraveyard(2, "Grizzly Bears")
            .withCardInGraveyard(2, "Forest")
            .withLifeTotal(1, aliceLife)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        game.castSpell(1, "Murder", game.findPermanent("Disciple of Perdition")!!).error shouldBe null
        game.resolveStack()
        game.isInGraveyard(1, "Disciple of Perdition") shouldBe true
        return game
    }

    private fun TestGame.chooseMode(label: String) {
        val decision = getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOfFirst { it.contains(label, ignoreCase = true) }
        check(index >= 0) { "$label not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    private fun TestGame.targetBobIfAsked() {
        if (getPendingDecision() is ChooseTargetsDecision) {
            selectTargets(listOf(player2Id)).error shouldBe null
        }
    }

    init {
        context("Disciple of Perdition") {

            test("at exactly 13 life the controller may choose both modes") {
                val game = killDisciple(aliceLife = 13)
                val handBefore = game.handSize(1)

                game.chooseMode("draw a card")
                withClue("at 13 life a second mode is offered") {
                    game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
                        .options.any { it.contains("Exile target opponent", ignoreCase = true) } shouldBe true
                }
                game.chooseMode("Exile target opponent")
                game.targetBobIfAsked()
                game.resolveStack()

                withClue("mode 1: Alice drew a card and lost 1 life") {
                    game.handSize(1) shouldBe handBefore + 1
                    game.getLifeTotal(1) shouldBe 12
                }
                withClue("mode 2: Bob's graveyard was exiled and Bob lost 1 life") {
                    game.graveyardSize(2) shouldBe 0
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                    game.isInExile(2, "Forest") shouldBe true
                    game.getLifeTotal(2) shouldBe 19
                }
            }

            test("at any other life total exactly one mode is chosen") {
                val game = killDisciple(aliceLife = 20)
                val handBefore = game.handSize(1)

                game.chooseMode("draw a card")
                game.targetBobIfAsked()
                withClue("no second mode question away from 13 life") {
                    (game.getPendingDecision() is ChooseOptionDecision) shouldBe false
                }
                game.resolveStack()

                game.handSize(1) shouldBe handBefore + 1
                game.getLifeTotal(1) shouldBe 19
                withClue("the unchosen exile mode did nothing") {
                    game.graveyardSize(2) shouldBe 2
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("the exile mode exiles the target opponent's graveyard and that player loses 1 life") {
                val game = killDisciple(aliceLife = 20)
                val handBefore = game.handSize(1)

                game.chooseMode("Exile target opponent")
                game.targetBobIfAsked()
                game.resolveStack()

                withClue("Bob's graveyard is exiled, owned by Bob, and Bob loses the life") {
                    game.graveyardSize(2) shouldBe 0
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                    game.isInExile(2, "Forest") shouldBe true
                    game.getLifeTotal(2) shouldBe 19
                }
                withClue("Alice's own graveyard and life are untouched, and she drew nothing") {
                    game.isInGraveyard(1, "Disciple of Perdition") shouldBe true
                    game.isInGraveyard(1, "Murder") shouldBe true
                    game.getLifeTotal(1) shouldBe 20
                    game.handSize(1) shouldBe handBefore
                }
            }
        }
    }
}
