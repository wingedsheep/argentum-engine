package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dripping-Tongue Zubera (CHK #206) — "When this creature dies, create a 1/1 colorless Spirit
 * creature token for each Zubera that died this turn."
 */
class DrippingTongueZuberaScenarioTest : ScenarioTestBase() {

    /** Resolve everything, answering each prompt: [pick] chooses trigger targets, trigger order is kept, discards take the first cards. */
    private fun TestGame.settle(pick: (ChooseTargetsDecision) -> List<EntityId> = { listOf(it.legalTargets.getValue(0).first()) }) {
        repeat(20) {
            resolveStack()
            when (val d = state.pendingDecision) {
                null -> return
                is ChooseTargetsDecision -> selectTargets(pick(d)).error shouldBe null
                is OrderObjectsDecision -> submitDecision(OrderedResponse(d.id, d.objects)).error shouldBe null
                is SelectCardsDecision -> selectCards(d.options.take(d.minSelections)).error shouldBe null
                else -> error("unexpected decision $d")
            }
        }
    }

    init {
        fun base() = scenario().withPlayers("Player1", "Player2")
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        context("Dripping-Tongue Zubera") {
            test("dying alone creates one 1/1 Spirit") {
                val game = base().withCardOnBattlefield(1, "Dripping-Tongue Zubera")
                    .withCardInHand(1, "Shock").withLandsOnBattlefield(1, "Mountain", 1).build()

                game.castSpell(1, "Shock", game.findPermanent("Dripping-Tongue Zubera")!!).error shouldBe null
                game.settle()

                val spirits = game.findPermanents("Spirit Token")
                spirits.size shouldBe 1
                game.state.projectedState.getPower(spirits.single()) shouldBe 1
                game.state.projectedState.getToughness(spirits.single()) shouldBe 1
            }

            test("a Zubera that died earlier this turn adds a token") {
                val game = base().withCardOnBattlefield(1, "Dripping-Tongue Zubera")
                    .withCardOnBattlefield(1, "Silent-Chant Zubera")
                    .withCardsInHand(1, "Shock", 2).withLandsOnBattlefield(1, "Mountain", 2).build()

                game.castSpell(1, "Shock", game.findPermanent("Silent-Chant Zubera")!!).error shouldBe null
                game.settle()
                game.castSpell(1, "Shock", game.findPermanent("Dripping-Tongue Zubera")!!).error shouldBe null
                game.settle()

                game.findPermanents("Spirit Token").size shouldBe 2
            }
        }
    }
}
