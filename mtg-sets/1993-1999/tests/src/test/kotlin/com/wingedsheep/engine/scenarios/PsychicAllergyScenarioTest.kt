package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Psychic Allergy.
 *
 * Two things are easy to get wrong independently: *whose* permanents are counted, and in *which*
 * colour. So the Allergy is cast for real (which is what raises the as-it-enters colour choice), the
 * opponent's board is stocked with green permanents, and mine with a red one that must not count. A
 * count taken against the Allergy's controller — or against colours generally — lands on a different
 * number than the two the opponent actually has.
 */
class PsychicAllergyScenarioTest : ScenarioTestBase() {

    init {
        context("Psychic Allergy") {

            test("burns the upkeep player for their nontoken permanents of the chosen colour") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Psychic Allergy")
                    .withLandsOnBattlefield(1, "Island", 5)
                    // Mine, red — the wrong colour, and the wrong side of the table.
                    .withCardOnBattlefield(1, "Goblin Balloon Brigade")
                    // Theirs, green: exactly two nontoken permanents of the chosen colour.
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Psychic Allergy").error shouldBe null
                game.resolveStack()

                val colorDecision = game.getPendingDecision()
                withClue("as it enters, it asks for a colour (CR 614.12a)") {
                    (colorDecision is ChooseColorDecision) shouldBe true
                }
                game.submitDecision(ColorChosenResponse(colorDecision!!.id, Color.GREEN))
                game.resolveStack()

                // Round to the opponent's upkeep.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("two green nontoken permanents of theirs -> 2 damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
                withClue("my red permanent never counted, and the trigger isn't on my upkeep") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("counts what the upkeep player controls, not what they own") {
                // The printed clause is "permanents ... they control". The battlefield zone map is
                // keyed by owner, so a count that reads the zone directly gets this backwards on
                // both halves: it misses the creature they stole from me and would credit me with
                // a creature of mine they now control. Control Magic makes exactly that board.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Psychic Allergy")
                    .withLandsOnBattlefield(1, "Island", 5)
                    // Mine by ownership, theirs by control — this is the one an owner-keyed
                    // count drops.
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(2, "Control Magic", "Grizzly Bears")
                    // And one green permanent that is theirs outright.
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Psychic Allergy").error shouldBe null
                game.resolveStack()

                val colorDecision = game.getPendingDecision()
                game.submitDecision(ColorChosenResponse(colorDecision!!.id, Color.GREEN))
                game.resolveStack()

                withClue("the stolen Bears is controlled by Player2, whatever the zone map says") {
                    val bears = game.findPermanent("Grizzly Bears")!!
                    game.state.projectedState.getController(bears) shouldBe game.player2Id
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("the Wurm they own plus the Bears they stole -> 2 damage, not 1") {
                    game.getLifeTotal(2) shouldBe 18
                }
                withClue("Control Magic is blue, so the Aura itself never counts") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
