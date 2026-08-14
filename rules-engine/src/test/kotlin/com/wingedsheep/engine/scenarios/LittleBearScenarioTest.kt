package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Little Bear (HOB #128) — {2}{G} Creature — Bear 3/2.
 *
 * "Flash
 *  When this creature enters, untap another target creature you control. If that creature is a
 *  Bear, put a +1/+1 counter on it."
 *
 * Covers the untap, the Bear-only counter rider, and that the target requirement excludes
 * Little Bear itself.
 */
class LittleBearScenarioTest : ScenarioTestBase() {

    private fun isTapped(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.get<TappedComponent>() != null

    private fun game(otherCreature: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Little Bear")
        .withCardOnBattlefield(1, otherCreature, tapped = true)
        .withLandsOnBattlefield(1, "Forest", 3)
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Little Bear") {

            test("a tapped Bear is untapped and gets a +1/+1 counter") {
                val g = game("Grizzly Bears")
                val bears = g.findPermanent("Grizzly Bears")!!
                isTapped(g, bears) shouldBe true

                g.castSpell(1, "Little Bear").error shouldBe null
                g.resolveStack()
                g.selectTargets(listOf(bears)).error shouldBe null
                g.resolveStack()

                withClue("the target was untapped") {
                    isTapped(g, bears) shouldBe false
                }
                withClue("Grizzly Bears is a Bear, so it also grew to 3/3") {
                    g.state.projectedState.getPower(bears) shouldBe 3
                    g.state.projectedState.getToughness(bears) shouldBe 3
                }
            }

            test("a tapped non-Bear is untapped but gets no counter") {
                val g = game("Old Thrush")
                val thrush = g.findPermanent("Old Thrush")!!
                isTapped(g, thrush) shouldBe true

                g.castSpell(1, "Little Bear").error shouldBe null
                g.resolveStack()
                g.selectTargets(listOf(thrush)).error shouldBe null
                g.resolveStack()

                withClue("the target was untapped regardless of its type") {
                    isTapped(g, thrush) shouldBe false
                }
                withClue("a Bird is not a Bear, so its printed 1/2 is unchanged") {
                    g.state.projectedState.getPower(thrush) shouldBe 1
                    g.state.projectedState.getToughness(thrush) shouldBe 2
                }
            }

            test("Little Bear cannot target itself") {
                val g = game("Grizzly Bears")

                g.castSpell(1, "Little Bear").error shouldBe null
                g.resolveStack()

                val decision = g.getPendingDecision()
                val legal = when (decision) {
                    is ChooseTargetsDecision -> decision.legalTargets.values.flatten()
                    else -> error("expected a target choice, got $decision")
                }
                withClue("\"another target creature you control\" excludes the source") {
                    legal shouldNotContain g.findPermanent("Little Bear")!!
                }
            }
        }
    }
}
