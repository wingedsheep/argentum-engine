package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Triton Tactics (THS #71) — "Up to two target creatures each get +0/+3 until end of turn. Untap
 * those creatures. At this turn's next end of combat, tap each creature that was blocked by one of
 * those creatures this turn and it doesn't untap during its controller's next untap step."
 *
 * Player 2 attacks with a vigilant Woodland Patrol and Grizzly Bears; player 1's Wall of Stone blocks
 * the Patrol and Wall of Wood blocks the Bears. Only the creature a *targeted* creature blocked is
 * tapped at end of combat and locked through its controller's next untap step — whether the block
 * came before or after Triton Tactics resolved.
 */
class TritonTacticsScenarioTest : ScenarioTestBase() {

    init {
        context("Triton Tactics") {

            fun setup(): TestGame {
                val builder = scenario()
                    .withPlayers("Triton Player", "Attacker")
                    .withCardOnBattlefield(1, "Wall of Stone")
                    .withCardOnBattlefield(1, "Wall of Wood")
                    .withCardOnBattlefield(1, "Hill Giant", tapped = true)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInHand(1, "Triton Tactics")
                    .withCardOnBattlefield(2, "Woodland Patrol")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(10) {
                    builder.withCardInLibrary(1, "Island")
                    builder.withCardInLibrary(2, "Plains")
                }
                return builder.build()
            }

            fun TestGame.tapped(name: String) = state.getEntity(findPermanent(name)!!)?.get<TappedComponent>() != null

            fun TestGame.castTactics(vararg targetNames: String) {
                // The attacking player holds priority first; hand it to player 1.
                if (state.priorityPlayerId == player2Id) passPriority()
                val cardId = state.getHand(player1Id).first {
                    state.getEntity(it)?.get<CardComponent>()?.name == "Triton Tactics"
                }
                execute(
                    CastSpell(player1Id, cardId, targetNames.map { ChosenTarget.Permanent(findPermanent(it)!!) })
                ).error shouldBe null
                resolveStack()
            }

            fun TestGame.attack() {
                passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                declareAttackers(mapOf("Woodland Patrol" to 1, "Grizzly Bears" to 1)).error shouldBe null
            }

            fun TestGame.block() {
                passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                declareBlockers(
                    mapOf("Wall of Stone" to listOf("Woodland Patrol"), "Wall of Wood" to listOf("Grizzly Bears"))
                ).error shouldBe null
            }

            fun TestGame.throughEndOfCombat() {
                passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                resolveStack()
                passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            }

            /** From player 2's turn, through player 1's turn, to player 2's next upkeep. */
            fun TestGame.toAttackersNextUpkeep() {
                passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                state.activePlayerId shouldBe player2Id
            }

            test("pumps and untaps its targets, then locks down what a target blocked earlier this turn") {
                val game = setup()
                game.attack()
                game.block()
                game.castTactics("Wall of Stone", "Hill Giant")

                withClue("each target gets +0/+3 and is untapped") {
                    game.state.projectedState.getToughness(game.findPermanent("Wall of Stone")!!) shouldBe 11
                    game.state.projectedState.getToughness(game.findPermanent("Hill Giant")!!) shouldBe 6
                    game.tapped("Hill Giant") shouldBe false
                }
                withClue("nothing is tapped before end of combat (Woodland Patrol has vigilance)") {
                    game.tapped("Woodland Patrol") shouldBe false
                }

                game.throughEndOfCombat()
                withClue("the creature the targeted Wall blocked is tapped at end of combat") {
                    game.tapped("Woodland Patrol") shouldBe true
                }

                game.toAttackersNextUpkeep()
                withClue("it doesn't untap during its controller's next untap step") {
                    game.tapped("Woodland Patrol") shouldBe true
                }
                withClue("the creature an untargeted Wall blocked untaps normally") {
                    game.tapped("Grizzly Bears") shouldBe false
                }
            }

            test("cast before blockers, the creature a target blocks afterwards is tapped and locked") {
                val game = setup()
                game.attack()
                game.castTactics("Wall of Stone")
                game.block()
                game.throughEndOfCombat()

                game.tapped("Woodland Patrol") shouldBe true
                game.toAttackersNextUpkeep()
                game.tapped("Woodland Patrol") shouldBe true
                game.tapped("Grizzly Bears") shouldBe false
            }

            test("cast after the turn's last combat, there is no next end of combat") {
                val game = setup()
                game.attack()
                game.block()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.castTactics("Wall of Stone")

                game.tapped("Woodland Patrol") shouldBe false
                game.toAttackersNextUpkeep()
                game.tapped("Woodland Patrol") shouldBe false
            }
        }
    }
}
