package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Huntmaster Liger (IKO #16) — {3}{W} Creature — Cat 3/4.
 *
 *   Mutate {2}{W}
 *   Whenever this creature mutates, other creatures you control get +X/+X until end of turn,
 *   where X is the number of times this creature has mutated.
 *
 * The first mutation pumps the other creatures +1/+1; a second Liger mutating onto the same
 * permanent triggers both copies of the ability, each reading X = 2. The merged permanent itself
 * and the opponent's creatures aren't pumped. Cast normally it doesn't mutate and pumps nothing.
 */
class HuntmasterLigerScenarioTest : ScenarioTestBase() {

    private fun TestGame.mutateOnto(host: EntityId) {
        val offer = getLegalActions(1).first { (it.action as? CastSpell)?.alternativeCostType == AlternativeCostType.MUTATE }
        execute((offer.action as CastSpell).copy(targets = listOf(ChosenTarget.Permanent(host)))).error shouldBe null
        resolveStack()
        answerYesNo(false)
        resolveStack()
    }

    private fun build() = scenario()
        .withPlayers("P1", "P2")
        .withCardInHand(1, "Huntmaster Liger")
        .withCardInHand(1, "Huntmaster Liger")
        .withLandsOnBattlefield(1, "Plains", 8)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Memnite")
        .withCardOnBattlefield(2, "Ornithopter")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Huntmaster Liger") {

            test("the first mutation gives other creatures you control +1/+1") {
                val game = build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val memnite = game.findPermanent("Memnite")!!
                game.mutateOnto(bears)

                val projected = game.state.projectedState
                withClue("Memnite gets +1/+1") {
                    projected.getPower(memnite) shouldBe 2
                    projected.getToughness(memnite) shouldBe 2
                }
                withClue("the mutated Bears is not an 'other' creature") {
                    projected.getPower(bears) shouldBe 2
                }
                withClue("the opponent's Ornithopter is untouched") {
                    projected.getPower(game.findPermanent("Ornithopter")!!) shouldBe 0
                }
            }

            test("a second mutation triggers both Ligers, each reading X = 2") {
                val game = build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val memnite = game.findPermanent("Memnite")!!
                game.mutateOnto(bears)
                game.mutateOnto(bears)

                withClue("+1/+1 from the first mutation, then +2/+2 twice from the second") {
                    game.state.projectedState.getPower(memnite) shouldBe 6
                    game.state.projectedState.getToughness(memnite) shouldBe 6
                }
            }

            test("cast normally it doesn't mutate and pumps nothing") {
                val game = build()
                game.castSpell(1, "Huntmaster Liger").error shouldBe null
                game.resolveStack()
                game.state.projectedState.getPower(game.findPermanent("Memnite")!!) shouldBe 1
            }
        }
    }
}
