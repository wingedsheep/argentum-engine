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
 * Ashen-Skin Zubera (CHK #101) — "When this creature dies, target opponent discards a card for each
 * Zubera that died this turn."
 */
class AshenSkinZuberaScenarioTest : ScenarioTestBase() {

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
            .withCardsInHand(2, "Island", 4)

        context("Ashen-Skin Zubera") {
            test("dying alone makes the opponent discard one card") {
                val game = base().withCardOnBattlefield(1, "Ashen-Skin Zubera")
                    .withCardInHand(1, "Shock").withLandsOnBattlefield(1, "Mountain", 1).build()

                game.castSpell(1, "Shock", game.findPermanent("Ashen-Skin Zubera")!!).error shouldBe null
                game.settle { listOf(game.player2Id) }

                game.handSize(2) shouldBe 3
            }

            test("discards one card per Zubera that died this turn") {
                val game = base().withCardOnBattlefield(1, "Ashen-Skin Zubera")
                    .withCardOnBattlefield(1, "Silent-Chant Zubera")
                    .withCardInHand(1, "Pyroclasm").withLandsOnBattlefield(1, "Mountain", 2).build()

                game.castSpell(1, "Pyroclasm").error shouldBe null
                game.settle { listOf(game.player2Id) }

                withClue("two Zubera died: discard two") { game.handSize(2) shouldBe 2 }
            }
        }
    }
}
