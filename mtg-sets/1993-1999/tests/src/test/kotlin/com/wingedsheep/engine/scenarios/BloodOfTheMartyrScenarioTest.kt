package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Scenario tests for Blood of the Martyr (DRK #2).
 *
 * {W}{W}{W} Instant
 * "Until end of turn, if damage would be dealt to any creature, you may have that damage dealt to
 *  you instead."
 *
 * The shield protects a *class* rather than a list, so the tests check that it covers a creature
 * the caster doesn't control and that it leaves damage aimed at players alone. The "you may" is a
 * real question: the caster is asked once per damage instance, before any of that damage is dealt,
 * and each answer stands on its own.
 */
class BloodOfTheMartyrScenarioTest : ScenarioTestBase() {

    init {
        context("Blood of the Martyr") {

            test("the caster may take a creature's damage — accepting redirects it") {
                val game = scenario()
                    .withPlayers("Martyr", "Burner")
                    .withCardInHand(1, "Blood of the Martyr")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Blood of the Martyr").error shouldBe null
                game.resolveStack()

                // Priority is back with the active player after the instant resolves; hand it to
                // the opponent so they can respond.
                game.passPriority()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(2, "Lightning Bolt", targetId = bears).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision().asYesNo()
                withClue("the caster is asked before the damage is dealt") {
                    decision.playerId shouldBe game.player1Id
                    decision.prompt shouldContain "Grizzly Bears"
                }
                game.answerYesNo(true)
                game.resolveStack()

                withClue("the 3 damage went to the caster, not the creature") {
                    game.getLifeTotal(1) shouldBe 17
                }
                withClue("so the 2/2 survived untouched") {
                    game.findPermanent("Grizzly Bears").shouldNotBeNull()
                }
            }

            test("declining leaves the damage where it was aimed") {
                val game = scenario()
                    .withPlayers("Martyr", "Burner")
                    .withCardInHand(1, "Blood of the Martyr")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Blood of the Martyr").error shouldBe null
                game.resolveStack()

                game.passPriority()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(2, "Lightning Bolt", targetId = bears).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldNotBeNull()
                game.answerYesNo(false)
                game.resolveStack()

                withClue("the caster declined, so the bolt stayed on the creature") {
                    game.getLifeTotal(1) shouldBe 20
                }
                withClue("and the 2/2 died to it") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
            }

            test("damage aimed at a player is untouched — it shields creatures only") {
                val game = scenario()
                    .withPlayers("Martyr", "Burner")
                    .withCardInHand(1, "Blood of the Martyr")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Blood of the Martyr").error shouldBe null
                game.resolveStack()

                game.passPriority()
                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()

                withClue("no question is raised for damage the shield doesn't cover") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("a bolt to the face is still a bolt to the face") {
                    game.getLifeTotal(1) shouldBe 17
                }
            }

            test("combat damage is answered instance by instance") {
                val game = scenario()
                    .withPlayers("Martyr", "Blocker")
                    .withCardInHand(1, "Blood of the Martyr")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Blood of the Martyr").error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null

                // Both halves of the exchange are creature damage, so the shield covers both. Pass
                // priority until the combat damage step stops to ask; never through passUntilPhase,
                // which would answer the questions itself.
                repeat(4) { if (!game.hasPendingDecision()) game.passPriority() }

                // The 3/3 would kill the 2/2 — take that one. The 2 damage coming back at the 3/3
                // is an opponent's creature's problem, so let it through. Both prompts name the
                // Bears (once as the recipient, once as the source), so match on the recipient half.
                repeat(2) {
                    val decision = game.getPendingDecision().asYesNo()
                    decision.playerId shouldBe game.player1Id
                    game.answerYesNo(decision.prompt.contains("damage to Grizzly Bears"))
                }

                withClue("no third question — one per damage instance, and there were two") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("the caster took the Hill Giant's 3 damage") {
                    game.getLifeTotal(1) shouldBe 17
                }
                withClue("so their 2/2 survived the 3/3") {
                    game.findPermanent("Grizzly Bears").shouldNotBeNull()
                }
                withClue("and the declined half still marked 2 damage on the 3/3, which survives it") {
                    game.findPermanent("Hill Giant").shouldNotBeNull()
                }
            }
        }
    }
}

/** The pending decision as the yes/no it must be — a clearer failure than a raw cast. */
private fun com.wingedsheep.engine.core.PendingDecision?.asYesNo(): YesNoDecision =
    this as? YesNoDecision ?: error("Expected a pending yes/no decision, got ${this?.let { it::class.simpleName }}")
