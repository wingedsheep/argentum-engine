package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Grishnákh, Brash Instigator (LTR #134) — {2}{R} Legendary Creature.
 *
 * "When Grishnákh enters, amass Orcs 2. When you do, until end of turn, gain control of target
 *  nonlegendary creature an opponent controls with power less than or equal to the amassed Army's
 *  power. Untap that creature. It gains haste until end of turn."
 *
 * Exercises the new pipeline-target-filter primitive: the reflexive target requirement filters by
 * "power <= the amassed Army's power", a value only known at resolution time. After "amass Orcs 2"
 * produces a 2/2 Army, a power-2 Grizzly Bears is a legal target while a power-3 Hill Giant is not.
 * Then verifies the steal: control flips until end of turn, the stolen creature untaps and gains
 * haste, and control reverts to its owner during the cleanup step.
 */
class GrishnakhBrashInstigatorScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Grishnákh, Brash Instigator") {

            test("amasses Orcs 2, then can only steal a creature with power <= the Army's power") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Grishnákh, Brash Instigator")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    // Opponent's board: a 2/2 (legal) and a 3/3 (illegal under power <= 2).
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Grishnákh, Brash Instigator")
                withClue("Casting Grishnákh should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                // Grishnákh resolves and enters; its ETB amass resolves first, then the reflexive
                // trigger lands on the stack and pauses for target selection.
                game.resolveStack()

                // Amass Orcs 2 made a 2/2 Orc Army.
                val army = game.findPermanent("Orc Army") ?: error("Amass should create an Orc Army token")
                withClue("Amass Orcs 2 → 2/2 Army") {
                    projector.project(game.state).getPower(army) shouldBe 2
                }

                // The reflexive trigger asks for a target — its legal targets must include the
                // power-2 Grizzly Bears and exclude the power-3 Hill Giant.
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                val bear = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val legal = decision.legalTargets[0] ?: emptyList()
                withClue("Power-2 Grizzly Bears is a legal target (power <= 2)") {
                    legal shouldContain bear
                }
                withClue("Power-3 Hill Giant is NOT a legal target (power > 2)") {
                    legal shouldNotContain giant
                }

                // Steal the bear.
                game.selectTargets(listOf(bear))
                game.resolveStack()

                // Control flips to player 1, the stolen creature untaps, and it has haste.
                val projected = projector.project(game.state)
                withClue("Control of the bear flips to the caster until end of turn") {
                    projected.getController(bear) shouldBe game.player1Id
                }
                withClue("Stolen creature is untapped") {
                    game.state.getEntity(bear)?.has<TappedComponent>() shouldBe false
                }
                withClue("Stolen creature gains haste until end of turn") {
                    projected.hasKeyword(bear, Keyword.HASTE) shouldBe true
                }

                // Advance to the cleanup step: the until-end-of-turn control effect wears off.
                game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
                withClue("Control reverts to the original owner at end of turn") {
                    projector.project(game.state).getController(bear) shouldBe game.player2Id
                }
            }
        }
    }
}
