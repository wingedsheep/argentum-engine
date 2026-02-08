package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Festering Goblin.
 *
 * Card reference:
 * - Festering Goblin ({B}): Creature — Zombie Goblin, 1/1
 *   "When Festering Goblin dies, target creature gets -1/-1 until end of turn."
 */
class FesteringGoblinScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Festering Goblin death trigger via sacrifice continuation") {
            test("death trigger fires when sacrificed via player choice") {
                // Accursed Centaur ETB: "sacrifice a creature"
                // Player has Festering Goblin + Accursed Centaur on battlefield,
                // so a sacrifice decision is presented (continuation path).
                // Opponent has Glory Seeker as the target for -1/-1.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Festering Goblin")
                    .withCardInHand(1, "Accursed Centaur")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2 target
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Accursed Centaur
                val castResult = game.castSpell(1, "Accursed Centaur")
                withClue("Accursed Centaur should be cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve: Centaur enters, ETB trigger fires, requires sacrifice
                game.resolveStack()

                // Player should have a sacrifice decision (Festering Goblin or Accursed Centaur)
                withClue("Player should have pending sacrifice decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Choose to sacrifice Festering Goblin
                val festeringGoblin = game.findPermanent("Festering Goblin")!!
                game.selectCards(listOf(festeringGoblin))

                // Festering Goblin's death trigger should fire, requiring target selection
                withClue("Festering Goblin death trigger should create target decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Target opponent's Glory Seeker with -1/-1
                val glorySeeker = game.findPermanent("Glory Seeker")!!
                game.selectTargets(listOf(glorySeeker))

                // Resolve the triggered ability
                game.resolveStack()

                // Verify Festering Goblin is in graveyard
                withClue("Festering Goblin should be in graveyard") {
                    game.isInGraveyard(1, "Festering Goblin") shouldBe true
                }

                // Verify Glory Seeker got -1/-1 (should be 1/1 instead of 2/2)
                val projected = stateProjector.project(game.state)
                val glorySeekerAfter = game.findPermanent("Glory Seeker")!!
                withClue("Glory Seeker should have power reduced by 1") {
                    projected.getPower(glorySeekerAfter) shouldBe 1
                }
                withClue("Glory Seeker should have toughness reduced by 1") {
                    projected.getToughness(glorySeekerAfter) shouldBe 1
                }
            }
        }
    }
}
