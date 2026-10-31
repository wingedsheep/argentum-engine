package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Sea-Dasher Octopus (IKO #66) — {1}{U}{U} Creature — Octopus 2/2.
 *
 *   Mutate {1}{U}
 *   Flash
 *   Whenever this creature deals combat damage to a player, draw a card.
 *
 * The first mutate card with flash: Flash must unlock the mutate cast at instant speed (CR 702.140a
 * makes mutate an alternative cost of casting the creature spell, so the spell's own timing applies).
 * Mutated under an attacker-to-be, the Bears keep their 2/2 body and gain the card-draw trigger.
 */
class SeaDasherOctopusScenarioTest : ScenarioTestBase() {

    private fun scenarioAtBeginCombat() = scenario()
        .withPlayers("P1", "P2")
        .withCardInHand(1, "Sea-Dasher Octopus")
        .withLandsOnBattlefield(1, "Island", 3)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
        .build()

    init {
        context("Sea-Dasher Octopus") {

            test("flash lets it mutate at instant speed, and the merged creature draws on combat damage") {
                val game = scenarioAtBeginCombat()
                val bears = game.findPermanent("Grizzly Bears")!!

                val offer = game.getLegalActions(1).singleOrNull {
                    (it.action as? CastSpell)?.alternativeCostType == AlternativeCostType.MUTATE
                }
                withClue("flash offers the mutate cast outside a main phase") {
                    (offer != null) shouldBe true
                }
                game.execute((offer!!.action as CastSpell).copy(targets = listOf(ChosenTarget.Permanent(bears))))
                    .error shouldBe null
                game.resolveStack()
                game.answerYesNo(false) // under: the Bears stay on top
                game.resolveStack()

                withClue("a 2/2 Grizzly Bears with the Octopus's flash") {
                    val projected = game.state.projectedState
                    game.findPermanent("Sea-Dasher Octopus") shouldBe null
                    projected.getPower(bears) shouldBe 2
                    projected.hasKeyword(bears, Keyword.FLASH) shouldBe true
                }

                val handBefore = game.handSize(1)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers().error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                withClue("2 combat damage dealt and a card drawn") {
                    game.getLifeTotal(2) shouldBe 18
                    game.handSize(1) shouldBe handBefore + 1
                }
            }

            test("cast normally with flash it enters on its own outside a main phase") {
                val game = scenarioAtBeginCombat()
                game.castSpell(1, "Sea-Dasher Octopus").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Sea-Dasher Octopus") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
