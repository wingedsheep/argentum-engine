package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Cacophony Scamp (ONE #124) — {R} 1/1 Creature — Phyrexian Goblin Warrior.
 *
 * "Whenever this creature deals combat damage to a player, you may sacrifice it. If you do,
 *  proliferate. When this creature dies, it deals damage equal to its power to any target."
 */
class CacophonyScampScenarioTest : ScenarioTestBase() {

    private fun TestGame.counters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.seed(id: EntityId, amount: Int) {
        state = state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, amount))
        }
    }

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Cacophony Scamp")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Resolve everything, answering each decision kind as the test dictates. */
    private fun TestGame.drain(
        sacrifice: Boolean,
        proliferateChoice: List<EntityId>,
        damageTarget: EntityId
    ): List<String> {
        val seen = mutableListOf<String>()
        var guard = 0
        while ((state.stack.isNotEmpty() || hasPendingDecision()) && guard++ < 30) {
            when (val d = state.pendingDecision) {
                null -> resolveStack()
                is YesNoDecision -> { seen += "yesno"; answerYesNo(sacrifice).error shouldBe null }
                is ChooseTargetsDecision -> {
                    seen += "target"
                    submitDecision(TargetsResponse(d.id, mapOf(0 to listOf(damageTarget)))).error shouldBe null
                }
                else -> { seen += "proliferate"; selectCards(proliferateChoice).error shouldBe null }
            }
        }
        return seen
    }

    private fun TestGame.attackUnblocked() {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Cacophony Scamp" to 2)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
    }

    init {
        test("sacrificing it after combat damage proliferates, then its death deals damage equal to its power") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.seed(bears, 1)

            game.attackUnblocked()
            withClue("the Scamp hit the opponent for 1") { game.getLifeTotal(2) shouldBe 19 }

            val seen = game.drain(sacrifice = true, proliferateChoice = listOf(bears), damageTarget = game.player2Id)

            withClue("decisions: $seen") {
                seen.contains("yesno") shouldBe true
                seen.contains("proliferate") shouldBe true
                seen.contains("target") shouldBe true
            }
            game.isInGraveyard(1, "Cacophony Scamp") shouldBe true
            withClue("proliferate added a +1/+1 counter to the Bears") { game.counters(bears) shouldBe 2 }
            withClue("the dies trigger dealt 1 (its last-known power) to the opponent") {
                game.getLifeTotal(2) shouldBe 18
            }
        }

        test("declining the sacrifice keeps the Scamp and does not proliferate") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.seed(bears, 1)

            game.attackUnblocked()
            val seen = game.drain(sacrifice = false, proliferateChoice = listOf(bears), damageTarget = game.player2Id)

            withClue("decisions: $seen") { seen shouldBe listOf("yesno") }
            game.isOnBattlefield("Cacophony Scamp") shouldBe true
            game.counters(bears) shouldBe 1
            game.getLifeTotal(2) shouldBe 19
        }

        test("its dies trigger uses last-known power, including counters") {
            val game = board()
            val scamp = game.findPermanent("Cacophony Scamp")!!
            game.seed(scamp, 2)

            game.attackUnblocked()
            withClue("a 3/3 Scamp hit for 3") { game.getLifeTotal(2) shouldBe 17 }
            game.drain(sacrifice = true, proliferateChoice = emptyList(), damageTarget = game.player2Id)

            game.isInGraveyard(1, "Cacophony Scamp") shouldBe true
            withClue("dies trigger dealt 3 more") { game.getLifeTotal(2) shouldBe 14 }
        }
    }
}
