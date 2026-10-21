package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Tolarian Contempt (MAT #8): the enters trigger marks each opposing creature with a rejection
 * counter; the end-step trigger may only target marked opposing creatures, and the target's owner
 * picks top or bottom of their library.
 */
class TolarianContemptScenarioTest : ScenarioTestBase() {

    private fun rejection(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.REJECTION) ?: 0

    /**
     * Walk to Player1's end step and through the trigger. Returns the offered targets; [choose]
     * picks from them, [position] is the owner's top (0) / bottom (1) answer.
     */
    private fun TestGame.walkEndStep(choose: (List<EntityId>) -> List<EntityId>, position: Int = 1): List<EntityId>? {
        passUntilPhase(Phase.ENDING, Step.END)
        var offered: List<EntityId>? = null
        var guard = 0
        while (guard++ < 40) {
            when (val decision = state.pendingDecision) {
                is ChooseTargetsDecision -> {
                    offered = decision.legalTargets[0] ?: emptyList()
                    selectTargets(choose(offered))
                }
                is ChooseOptionDecision -> {
                    decision.playerId shouldBe player2Id
                    submitDecision(OptionChosenResponse(decision.id, position))
                }
                null -> {
                    if (state.stack.isEmpty()) return offered
                    resolveStack()
                }
                else -> error("unexpected decision: $decision")
            }
        }
        error("decision loop did not settle")
    }

    init {
        test("enters: a rejection counter on each opposing creature; end step: owner puts the target on the bottom") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Tolarian Contempt")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val wurm = game.findPermanent("Craw Wurm")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Tolarian Contempt").error shouldBe null
            var guard = 0
            while (guard++ < 20 && (game.state.stack.isNotEmpty() || game.state.pendingDecision != null)) {
                if (game.state.pendingDecision is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                else game.resolveStack()
            }

            withClue("only the opponent's creatures are marked") {
                rejection(game, wurm) shouldBe 1
                rejection(game, giant) shouldBe 1
                rejection(game, bears) shouldBe 0
            }

            val offered = game.walkEndStep(choose = { listOf(wurm) }, position = 1)

            offered!! shouldContainExactlyInAnyOrder listOf(wurm, giant)
            game.isOnBattlefield("Craw Wurm") shouldBe false
            game.state.getLibrary(game.player2Id).last() shouldBe wurm
            game.isOnBattlefield("Hill Giant") shouldBe true
        }

        test("only creatures with a rejection counter can be targeted, and up to one means none is fine") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Tolarian Contempt")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val wurm = game.findPermanent("Craw Wurm")!!
            game.state = game.state.updateEntity(wurm) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.REJECTION, 1))
            }

            val offered = game.walkEndStep(choose = { emptyList() })

            offered!! shouldContainExactlyInAnyOrder listOf(wurm)
            game.isOnBattlefield("Craw Wurm") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
        }
    }
}
