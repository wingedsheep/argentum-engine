package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hob.cards.PatientInstructor
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Patient Instructor (HOB #162) — {2}{W/U} Creature — Human Citizen 2/2.
 *
 * "Vigilance" + "When this creature enters, recruit."
 */
class PatientInstructorScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(PatientInstructor)

        context("Patient Instructor") {

            test("entering recruits; a nonland discard mints a Soldier") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Patient Instructor")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Patient Instructor").error shouldBe null
                game.resolveStack()

                withClue("the enters trigger should pause for recruit's discard choice") {
                    game.hasPendingDecision() shouldBe true
                }
                val bears = game.findCardsInHand(1, "Grizzly Bears").single()
                game.selectCards(listOf(bears))
                game.resolveStack()

                withClue("recruit drew the Forest") {
                    game.isInHand(1, "Forest") shouldBe true
                }
                withClue("the nonland discard mints one Human Soldier token") {
                    game.findAllPermanents("Human Soldier Token").size shouldBe 1
                }
            }

            test("has vigilance") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Patient Instructor")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val instructor = game.findPermanent("Patient Instructor")!!
                withClue("printed vigilance should be visible in projected state") {
                    game.state.projectedState.hasKeyword(instructor, Keyword.VIGILANCE) shouldBe true
                }
            }
        }
    }
}
