package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Mirkwood Nurturer (HOB #160) — {2}{G/U} Creature — Elf Ranger 3/2
 * "When this creature enters, return up to one other target permanent you control to its owner's
 * hand. If you do, put a +1/+1 counter on this creature."
 *
 * The "if you do" rider is gated on a permanent actually reaching hand, so declining the "up to
 * one" target must skip the counter too.
 */
class MirkwoodNurturerScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun game() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Mirkwood Nurturer")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Forest", 3)
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("choosing a permanent returns it and puts a +1/+1 counter on the Nurturer") {
            val g = game()
            val bears = g.findPermanent("Grizzly Bears")!!

            g.castSpell(1, "Mirkwood Nurturer").error shouldBe null
            g.resolveStack()

            (g.getPendingDecision() is ChooseTargetsDecision) shouldBe true
            g.selectTargets(listOf(bears)).error shouldBe null
            g.resolveStack()

            val nurturer = g.findPermanent("Mirkwood Nurturer")!!
            withClue("the chosen permanent went back to its owner's hand") {
                g.isOnBattlefield("Grizzly Bears") shouldBe false
                g.isInHand(1, "Grizzly Bears") shouldBe true
            }
            withClue("the rider fired because a permanent was returned") {
                plusOneCounters(g, nurturer) shouldBe 1
            }
        }

        test("choosing no target returns nothing and gives no counter") {
            val g = game()

            g.castSpell(1, "Mirkwood Nurturer").error shouldBe null
            g.resolveStack()

            (g.getPendingDecision() is ChooseTargetsDecision) shouldBe true
            g.selectTargets(emptyList()).error shouldBe null
            g.resolveStack()

            val nurturer = g.findPermanent("Mirkwood Nurturer")!!
            withClue("nothing was bounced") {
                g.isOnBattlefield("Grizzly Bears") shouldBe true
            }
            withClue("no permanent returned means no counter") {
                plusOneCounters(g, nurturer) shouldBe 0
            }
        }

        test("the Nurturer itself is not a legal target") {
            val g = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Mirkwood Nurturer")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            g.castSpell(1, "Mirkwood Nurturer").error shouldBe null
            g.resolveStack()

            val nurturer = g.findPermanent("Mirkwood Nurturer")!!
            val decision = g.getPendingDecision()
            if (decision is ChooseTargetsDecision) {
                withClue("'other' excludes the source, so only the three Forests are offered") {
                    decision.legalTargets.values.flatten().contains(nurturer) shouldBe false
                }
                g.selectTargets(emptyList()).error shouldBe null
                g.resolveStack()
            }

            plusOneCounters(g, nurturer) shouldBe 0
        }
    }
}
