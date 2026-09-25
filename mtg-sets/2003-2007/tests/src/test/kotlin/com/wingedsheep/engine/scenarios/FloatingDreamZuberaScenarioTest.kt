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
 * Floating-Dream Zubera (CHK #61) — "When this creature dies, draw a card for each Zubera that
 * died this turn."
 *
 * The count is game-wide, includes the dying Zubera itself, counts Zubera that died earlier in the
 * turn, and ignores non-Zubera deaths.
 */
class FloatingDreamZuberaScenarioTest : ScenarioTestBase() {

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
            .withCardInLibrary(1, "Island").withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island").withCardInLibrary(1, "Island")

        context("Floating-Dream Zubera") {
            test("dying alone draws one card — it counts itself") {
                val game = base().withCardOnBattlefield(1, "Floating-Dream Zubera")
                    .withCardInHand(1, "Shock").withLandsOnBattlefield(1, "Mountain", 1).build()

                game.castSpell(1, "Shock", game.findPermanent("Floating-Dream Zubera")!!).error shouldBe null
                game.settle()

                withClue("Shock left hand, one card drawn") { game.handSize(1) shouldBe 1 }
            }

            test("a Zubera that died earlier this turn — even an opponent's — adds to the count") {
                val game = base().withCardOnBattlefield(1, "Floating-Dream Zubera")
                    .withCardOnBattlefield(2, "Silent-Chant Zubera")
                    .withCardsInHand(1, "Shock", 2).withLandsOnBattlefield(1, "Mountain", 2).build()

                game.castSpell(1, "Shock", game.findPermanent("Silent-Chant Zubera")!!).error shouldBe null
                game.settle()
                // The engine hands priority to the resolved trigger's controller (Player2) rather
                // than the active player (CR 117.3b); pass it back before casting again.
                if (game.state.priorityPlayerId == game.player2Id) game.passPriority()
                game.castSpell(1, "Shock", game.findPermanent("Floating-Dream Zubera")!!).error shouldBe null
                game.settle()

                withClue("two Zubera died this turn, so two cards drawn") { game.handSize(1) shouldBe 2 }
            }

            test("simultaneous deaths all count, but a non-Zubera creature does not") {
                val game = base().withCardOnBattlefield(1, "Floating-Dream Zubera")
                    .withCardOnBattlefield(1, "Silent-Chant Zubera")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Pyroclasm").withLandsOnBattlefield(1, "Mountain", 2).build()

                game.castSpell(1, "Pyroclasm").error shouldBe null
                game.settle()

                withClue("Bears died too") { game.isInGraveyard(1, "Grizzly Bears") shouldBe true }
                withClue("two Zubera died, the Bears aren't one") { game.handSize(1) shouldBe 2 }
            }
        }
    }
}
