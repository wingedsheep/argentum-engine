package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Diregraf Scavenger (VOW #105) — {3}{B} Creature — Zombie Bear, 2/3,
 * Deathtouch.
 *
 *   When this creature enters, exile up to one target card from a graveyard. If a creature card
 *   was exiled this way, each opponent loses 2 life and you gain 2 life.
 *
 * Exercises both branches of the conditional drain: exiling a creature card triggers the life
 * swing, exiling a noncreature card does not.
 */
class DiregrafScavengerScenarioTest : ScenarioTestBase() {

    init {
        context("Diregraf Scavenger ETB") {

            test("exiling a creature card from a graveyard drains each opponent for 2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Diregraf Scavenger")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInGraveyard(2, "Hill Giant") // creature
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Diregraf Scavenger has deathtouch") {
                    cardRegistry.getCard("Diregraf Scavenger")!!.keywords.contains(Keyword.DEATHTOUCH) shouldBe true
                }

                game.castSpell(1, "Diregraf Scavenger").error shouldBe null
                game.resolveStack() // creature enters -> ETB trigger asks for a target

                val creatureCard = game.findCardsInGraveyard(2, "Hill Giant").first()
                game.selectTargets(listOf(creatureCard)).error shouldBe null
                game.resolveStack()

                withClue("the creature card is exiled") {
                    game.isInExile(2, "Hill Giant") shouldBe true
                }
                withClue("a creature was exiled -> opponent loses 2, you gain 2") {
                    game.getLifeTotal(2) shouldBe 18
                    game.getLifeTotal(1) shouldBe 22
                }
            }

            test("exiling a noncreature card does not drain") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Diregraf Scavenger")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInGraveyard(2, "Mountain") // noncreature
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Diregraf Scavenger").error shouldBe null
                game.resolveStack()

                val landCard = game.findCardsInGraveyard(2, "Mountain").first()
                game.selectTargets(listOf(landCard)).error shouldBe null
                game.resolveStack()

                withClue("the land card is exiled") {
                    game.isInExile(2, "Mountain") shouldBe true
                }
                withClue("no creature exiled -> no life change") {
                    game.getLifeTotal(2) shouldBe 20
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
