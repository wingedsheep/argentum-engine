package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Deathgorge Scavenger (XLN #184) — {2}{G} Creature — Dinosaur, 3/2.
 *
 *   Whenever this creature enters or attacks, you may exile target card from a graveyard. If a
 *   creature card is exiled this way, you gain 2 life. If a noncreature card is exiled this way,
 *   this creature gets +1/+1 until end of turn.
 *
 * Covers the three outcomes of the "may": a creature card exiled (life, no pump), a noncreature
 * card exiled on attack (pump, no life), and declining (nothing exiled, neither payoff).
 */
class DeathgorgeScavengerScenarioTest : ScenarioTestBase() {

    init {
        context("Deathgorge Scavenger") {

            test("entering and exiling a creature card gains 2 life and no pump") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Deathgorge Scavenger")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInGraveyard(2, "Hill Giant")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Deathgorge Scavenger").error shouldBe null
                game.resolveStack()

                val giant = game.findCardsInGraveyard(2, "Hill Giant").first()
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()
                game.answerYesNo(true).error shouldBe null

                val scavenger = game.findPermanent("Deathgorge Scavenger")!!
                withClue("the creature card is exiled") {
                    game.isInExile(2, "Hill Giant") shouldBe true
                }
                withClue("creature exiled -> gain 2 life") {
                    game.getLifeTotal(1) shouldBe 22
                }
                withClue("creature exiled -> no +1/+1") {
                    game.state.projectedState.getPower(scavenger) shouldBe 3
                    game.state.projectedState.getToughness(scavenger) shouldBe 2
                }
            }

            test("attacking and exiling a noncreature card pumps +1/+1 and gains no life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Deathgorge Scavenger")
                    .withCardInGraveyard(2, "Mountain")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Deathgorge Scavenger" to 2)).error shouldBe null

                val mountain = game.findCardsInGraveyard(2, "Mountain").first()
                game.selectTargets(listOf(mountain)).error shouldBe null
                game.resolveStack()
                game.answerYesNo(true).error shouldBe null

                val scavenger = game.findPermanent("Deathgorge Scavenger")!!
                withClue("the land card is exiled") {
                    game.isInExile(2, "Mountain") shouldBe true
                }
                withClue("noncreature exiled -> +1/+1 until end of turn") {
                    game.state.projectedState.getPower(scavenger) shouldBe 4
                    game.state.projectedState.getToughness(scavenger) shouldBe 3
                }
                withClue("noncreature exiled -> no life gain") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("declining the may exiles nothing and grants neither payoff") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Deathgorge Scavenger")
                    .withCardInGraveyard(2, "Mountain")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Deathgorge Scavenger" to 2)).error shouldBe null

                val mountain = game.findCardsInGraveyard(2, "Mountain").first()
                game.selectTargets(listOf(mountain)).error shouldBe null
                game.resolveStack()
                game.answerYesNo(false).error shouldBe null

                val scavenger = game.findPermanent("Deathgorge Scavenger")!!
                withClue("the card stays in the graveyard") {
                    game.isInGraveyard(2, "Mountain") shouldBe true
                }
                withClue("no pump and no life gain") {
                    game.state.projectedState.getPower(scavenger) shouldBe 3
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
