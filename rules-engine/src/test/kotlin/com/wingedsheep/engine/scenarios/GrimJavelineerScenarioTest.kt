package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Grim Javelineer (DFT #89) — {2}{B} Creature — Human Warrior, 3/2.
 *
 *   "Whenever you attack, target attacking creature gets +1/+0 until end of turn. When that
 *    creature dies this turn, surveil 1."
 *
 * Exercises the attack trigger's targeting (only attacking creatures are legal), the pump, and the
 * watched-entity delayed trigger that surveils when the pumped creature dies later that turn.
 */
class GrimJavelineerScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Grim Javelineer attack trigger") {

            test("pumps a chosen attacking creature and offers only attackers as targets") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grim Javelineer", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val javelineer = game.findPermanent("Grim Javelineer")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                // Hill Giant stays home, so it must not be a legal target.
                game.declareAttackers(mapOf("Grim Javelineer" to 2, "Grizzly Bears" to 2))
                    .error shouldBe null

                val decision = game.state.pendingDecision as? ChooseTargetsDecision
                withClue("The attack trigger pauses to choose an attacking creature") {
                    decision shouldNotBe null
                }
                withClue("Only the two attacking creatures are legal targets") {
                    (decision!!.legalTargets[0] ?: emptyList()) shouldContainExactlyInAnyOrder
                        listOf(javelineer, bears)
                }

                game.selectTargets(listOf(bears))
                game.resolveStack()

                withClue("Grizzly Bears is 2/2 pumped to 3/2 — power only") {
                    projector.getProjectedPower(game.state, bears) shouldBe 3
                    projector.getProjectedToughness(game.state, bears) shouldBe 2
                }
                withClue("Grim Javelineer itself is untouched at 3/2") {
                    projector.getProjectedPower(game.state, javelineer) shouldBe 3
                }
            }

            test("surveils 1 when the pumped creature dies later that turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grim Javelineer", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Sol Ring")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val javelineer = game.findPermanent("Grim Javelineer")!!

                game.declareAttackers(mapOf("Grim Javelineer" to 2)).error shouldBe null
                (game.state.pendingDecision as? ChooseTargetsDecision) shouldNotBe null
                game.selectTargets(listOf(javelineer))
                game.resolveStack()

                withClue("Grim Javelineer is now 4/2") {
                    projector.getProjectedPower(game.state, javelineer) shouldBe 4
                    projector.getProjectedToughness(game.state, javelineer) shouldBe 2
                }

                // A 2/2 blocker trades with the 4/2 attacker: the Javelineer dies to 2 damage.
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Grim Javelineer")))
                    .error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.state.pendingDecision != null &&
                    game.state.pendingDecision !is SelectCardsDecision
                ) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }

                withClue("Grim Javelineer died in combat") {
                    game.isInGraveyard(1, "Grim Javelineer") shouldBe true
                }

                val surveil = game.state.pendingDecision as? SelectCardsDecision
                withClue("The delayed trigger fired: surveil 1 pauses for the keep/bin choice") {
                    surveil shouldNotBe null
                    surveil!!.options.size shouldBe 1
                }

                // Bin the looked-at card.
                game.selectCards(game.findCardsInLibrary(1, "Sol Ring"))
                withClue("Surveil put the card into the graveyard") {
                    game.isInGraveyard(1, "Sol Ring") shouldBe true
                }
            }

            test("no surveil when the pumped creature survives the turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grim Javelineer", summoningSickness = false)
                    .withCardInLibrary(1, "Sol Ring")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val javelineer = game.findPermanent("Grim Javelineer")!!

                game.declareAttackers(mapOf("Grim Javelineer" to 2)).error shouldBe null
                game.selectTargets(listOf(javelineer))
                game.resolveStack()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("The attacker lived, so nothing surveils and the library is untouched") {
                    game.isOnBattlefield("Grim Javelineer") shouldBe true
                    game.librarySize(1) shouldBe 1
                    game.isInGraveyard(1, "Sol Ring") shouldBe false
                }
            }
        }
    }
}
