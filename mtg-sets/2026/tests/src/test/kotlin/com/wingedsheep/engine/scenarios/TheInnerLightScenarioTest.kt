package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The Inner Light (TRK #241).
 *
 *   II — Choose target creature. Whenever it deals combat damage to a player this turn, you draw
 *        that many cards.
 *
 * Chapter II is the Saga's own delayed trigger watching the target: the draw counts the damage
 * dealt, goes to the Saga's controller, and the watcher is gone once the turn ends — so an
 * opponent's creature targeted on your turn draws you nothing when it attacks on theirs.
 */
class TheInnerLightScenarioTest : ScenarioTestBase() {

    init {
        fun builder() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "The Inner Light")
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .apply { repeat(10) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Forest") } }

        /** Set the Saga at lore 1 and let chapter II trigger in precombat main, targeting [target]. */
        fun TestGame.playChapterTwo(target: EntityId) {
            val saga = findPermanent("The Inner Light")!!
            state = state.updateEntity(saga) { container ->
                container.with(CountersComponent().withAdded(CounterType.LORE, 1))
                    .with(SagaComponent(triggeredChapters = setOf(1)))
            }
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            selectTargets(listOf(target)).error shouldBe null
            resolveStack()
        }

        test("your targeted creature's combat damage to a player draws you that many cards") {
            val game = builder()
                .withCardOnBattlefield(1, "Hill Giant")
                .build()
            val giant = game.findPermanent("Hill Giant")!!

            game.playChapterTwo(giant)
            withClue("chapter II installed one watcher") {
                game.state.delayedTriggers.size shouldBe 1
            }

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
            val handBefore = game.handSize(1)
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.resolveStack()

            withClue("3 combat damage dealt, so three cards drawn") {
                game.getLifeTotal(2) shouldBe 17
                game.handSize(1) shouldBe handBefore + 3
            }
        }

        test("an opponent's creature targeted on your turn draws nothing on their turn — the watcher expired") {
            val game = builder()
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.playChapterTwo(bears)
            game.state.delayedTriggers.size shouldBe 1

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            withClue("\"this turn\": the watcher is removed when the Saga controller's turn ends") {
                game.state.delayedTriggers.size shouldBe 0
            }

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            val handBefore = game.handSize(1)
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.resolveStack()

            withClue("the Bears hit the Saga's controller, but nobody draws") {
                game.getLifeTotal(1) shouldBe 18
                game.handSize(1) shouldBe handBefore
            }
        }
    }
}
