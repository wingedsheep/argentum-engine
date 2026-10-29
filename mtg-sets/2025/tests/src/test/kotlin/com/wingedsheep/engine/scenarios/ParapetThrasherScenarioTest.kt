package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tdc.cards.ParapetThrasher
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Parapet Thrasher (TDC #36) — {2}{R}{R} Creature — Dragon, 4/3.
 *
 *   Flying
 *   Whenever one or more Dragons you control deal combat damage to an opponent, choose one that
 *   hasn't been chosen this turn —
 *   • Destroy target artifact that opponent controls.
 *   • This creature deals 4 damage to each other opponent.
 *   • Exile the top card of your library. You may play it this turn.
 */
class ParapetThrasherScenarioTest : FunSpec({

    fun driverWith(players: Int): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(ParapetThrasher))
        val ids = d.initMultiplayer(List(players) { Deck.of("Grizzly Bears" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe ids[0]
        return d to ids
    }

    /**
     * Walks combat to the postcombat main phase, picking the mode whose text contains [mode] and
     * recording the targets each target prompt offered before picking the first.
     */
    fun GameTestDriver.finishCombat(mode: String): List<List<EntityId>> {
        val offered = mutableListOf<List<EntityId>>()
        var guard = 0
        while (state.step != Step.POSTCOMBAT_MAIN && guard++ < 60) {
            when (val decision = pendingDecision) {
                is ChooseOptionDecision -> {
                    val index = decision.options.indexOfFirst { it.contains(mode) }
                    withClue("mode offered: ${decision.options}") { (index >= 0) shouldBe true }
                    submitDecision(decision.playerId, OptionChosenResponse(decision.id, index)).error shouldBe null
                }
                is ChooseTargetsDecision -> {
                    val legal = decision.legalTargets[0].orEmpty()
                    offered += legal
                    submitDecision(decision.playerId, TargetsResponse(decision.id, mapOf(0 to legal.take(1))))
                        .error shouldBe null
                }
                null -> bothPass()
                else -> autoResolveDecision()
            }
        }
        return offered
    }

    test("deals 4 damage to each opponent other than the one it hit") {
        val (d, p) = driverWith(4)
        val thrasher = d.putCreatureOnBattlefield(p[0], "Parapet Thrasher").also { d.removeSummoningSickness(it) }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p[0], mapOf(thrasher to p[1])).error shouldBe null
        d.finishCombat("each other opponent")

        withClue("the damaged opponent takes only the combat damage") { d.getLifeTotal(p[1]) shouldBe 16 }
        withClue("each other opponent takes 4") {
            d.getLifeTotal(p[2]) shouldBe 16
            d.getLifeTotal(p[3]) shouldBe 16
        }
        withClue("you are not an opponent") { d.getLifeTotal(p[0]) shouldBe 20 }
    }

    test("destroys an artifact only the damaged opponent controls") {
        val (d, p) = driverWith(3)
        val thrasher = d.putCreatureOnBattlefield(p[0], "Parapet Thrasher").also { d.removeSummoningSickness(it) }
        val hitArtifact = d.putCreatureOnBattlefield(p[1], "Artifact Creature")
        val otherArtifact = d.putCreatureOnBattlefield(p[2], "Artifact Creature")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p[0], mapOf(thrasher to p[1])).error shouldBe null
        val offered = d.finishCombat("Destroy target artifact")

        offered.single() shouldContainExactly listOf(hitArtifact)
        d.findPermanent(p[1], "Artifact Creature") shouldBe null
        d.findPermanent(p[2], "Artifact Creature") shouldBe otherArtifact
    }
})
