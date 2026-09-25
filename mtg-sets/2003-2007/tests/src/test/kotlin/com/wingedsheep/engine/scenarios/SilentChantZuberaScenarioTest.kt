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
 * Silent-Chant Zubera (CHK #45) — "When this creature dies, you gain 2 life for each Zubera that
 * died this turn."
 */
class SilentChantZuberaScenarioTest : ScenarioTestBase() {

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

        context("Silent-Chant Zubera") {
            test("dying alone gains 2 life") {
                val game = base().withCardOnBattlefield(1, "Silent-Chant Zubera")
                    .withCardInHand(1, "Shock").withLandsOnBattlefield(1, "Mountain", 1).build()

                game.castSpell(1, "Shock", game.findPermanent("Silent-Chant Zubera")!!).error shouldBe null
                game.settle()

                game.getLifeTotal(1) shouldBe 22
            }

            test("gains 2 per Zubera that died this turn, dying alongside another") {
                val game = base().withCardOnBattlefield(1, "Silent-Chant Zubera")
                    .withCardOnBattlefield(2, "Floating-Dream Zubera")
                    .withCardInLibrary(2, "Island").withCardInLibrary(2, "Island")
                    .withCardInHand(1, "Pyroclasm").withLandsOnBattlefield(1, "Mountain", 2).build()

                game.castSpell(1, "Pyroclasm").error shouldBe null
                game.settle()

                withClue("two Zubera died: 2 × 2 life") { game.getLifeTotal(1) shouldBe 24 }
            }
        }
    }
}
