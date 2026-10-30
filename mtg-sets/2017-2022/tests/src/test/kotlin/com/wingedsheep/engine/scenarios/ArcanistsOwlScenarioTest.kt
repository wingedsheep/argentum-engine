package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Arcanist's Owl (ELD #206) — {W/U}{W/U}{W/U}{W/U} Artifact Creature — Bird 3/3.
 *
 * "Flying. When this creature enters, look at the top four cards of your library. You may reveal
 *  an artifact or enchantment card from among them and put it into your hand. Put the rest on the
 *  bottom of your library in a random order."
 */
class ArcanistsOwlScenarioTest : ScenarioTestBase() {

    private fun game() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Arcanist's Owl")
        .withLandsOnBattlefield(1, "Island", 4)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Gingerbrute")
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Arcanist's Owl") {

            test("ETB takes an artifact card from the top four into hand; the rest go to the bottom") {
                val game = game()
                game.castSpell(1, "Arcanist's Owl").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val gingerbrute = game.findCardsInLibrary(1, "Gingerbrute").single()
                val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
                decision.options shouldContain gingerbrute
                withClue("a creature that is neither artifact nor enchantment can't be taken") {
                    (bears !in decision.options || bears in decision.nonSelectableOptions) shouldBe true
                }
                game.selectCards(listOf(gingerbrute)).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Gingerbrute") shouldBe true
                game.librarySize(1) shouldBe 3
                game.isOnBattlefield("Arcanist's Owl") shouldBe true
            }

            test("the reveal is optional: choosing nothing puts all four on the bottom") {
                val game = game()
                game.castSpell(1, "Arcanist's Owl").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.skipSelection().error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe 0
                game.librarySize(1) shouldBe 4
            }
        }
    }
}
