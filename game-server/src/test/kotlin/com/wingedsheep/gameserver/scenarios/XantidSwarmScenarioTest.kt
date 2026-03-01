package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.player.CantCastSpellsComponent
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Xantid Swarm.
 *
 * Xantid Swarm: {G} 0/1 Creature — Insect
 * Flying
 * Whenever Xantid Swarm attacks, defending player can't cast spells this turn.
 */
class XantidSwarmScenarioTest : ScenarioTestBase() {

    init {
        context("Xantid Swarm attack trigger") {

            test("attacking with Xantid Swarm adds CantCastSpellsComponent to opponent") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Xantid Swarm")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                // Declare Xantid Swarm as attacker
                game.declareAttackers(mapOf("Xantid Swarm" to 2))

                // Resolve the triggered ability
                game.resolveStack()

                // Verify opponent has CantCastSpellsComponent
                val cantCast = game.state.getEntity(game.player2Id)?.get<CantCastSpellsComponent>()
                cantCast shouldNotBe null
                cantCast?.removeOn shouldBe PlayerEffectRemoval.EndOfTurn
            }

            test("opponent cannot cast spells after Xantid Swarm trigger resolves") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Xantid Swarm")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                // Attack with Xantid Swarm
                game.declareAttackers(mapOf("Xantid Swarm" to 2))
                game.resolveStack()

                // Verify opponent has the restriction
                game.state.getEntity(game.player2Id)?.has<CantCastSpellsComponent>() shouldBe true

                // Try to cast Shock as opponent — should fail
                val castResult = game.castSpellTargetingPlayer(2, "Shock", 1)
                withClue("Opponent should not be able to cast spells after Xantid Swarm trigger") {
                    castResult.error shouldNotBe null
                }
            }

            test("CantCastSpellsComponent is removed at end of turn") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Xantid Swarm")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                // Attack with Xantid Swarm, resolve trigger
                game.declareAttackers(mapOf("Xantid Swarm" to 2))
                game.resolveStack()

                // Verify restriction is present
                game.state.getEntity(game.player2Id)?.has<CantCastSpellsComponent>() shouldBe true

                // Advance to end of turn and to next turn
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passPriority()
                game.passPriority()

                // Now on opponent's turn, the component should be gone
                withClue("CantCastSpellsComponent should be removed at end of turn") {
                    game.state.getEntity(game.player2Id)?.has<CantCastSpellsComponent>() shouldBe false
                }
            }

            test("defending player can still cast spells before trigger resolves") {
                // Per Scryfall ruling: "The defending player may cast spells before
                // Xantid Swarm's triggered ability resolves."
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Xantid Swarm")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                // Attack with Xantid Swarm — trigger goes on stack
                game.declareAttackers(mapOf("Xantid Swarm" to 2))

                // Before resolving trigger, opponent should NOT have the component yet
                game.state.getEntity(game.player2Id)?.has<CantCastSpellsComponent>() shouldBe false

                // Active player passes priority so opponent can respond
                game.passPriority()

                // Opponent should be able to cast in response to the trigger
                val castResult = game.castSpellTargetingPlayer(2, "Shock", 1)
                withClue("Opponent should be able to cast before trigger resolves: ${castResult.error}") {
                    castResult.error shouldBe null
                }
            }
        }
    }
}
