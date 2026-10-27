package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Fall of the Impostor (KHM #208).
 *
 *   III — Exile a creature with the greatest power among creatures target opponent controls.
 *
 * Only the opponent is targeted; the greatest power is measured among that player's creatures
 * alone (a bigger creature of the Saga's controller doesn't count), and a tie is the Saga
 * controller's choice.
 */
class FallOfTheImpostorScenarioTest : ScenarioTestBase() {

    init {
        fun builder() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Fall of the Impostor")
            .withCardOnBattlefield(1, "Craw Wurm")
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .apply { repeat(4) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }

        fun TestGame.playChapterThree() {
            val saga = findPermanent("Fall of the Impostor")!!
            state = state.updateEntity(saga) { container ->
                container.with(CountersComponent().withAdded(CounterType.LORE, 2))
                    .with(SagaComponent(triggeredChapters = setOf(1, 2)))
            }
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            if (getPendingDecision() is ChooseTargetsDecision) {
                selectTargets(listOf(player2Id)).error shouldBe null
            }
            resolveStack()
        }

        test("exiles the opponent's creature with the greatest power, ignoring the controller's own") {
            val game = builder()
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()

            game.playChapterThree()

            withClue("a single greatest-power creature needs no choice") {
                game.getPendingDecision() shouldBe null
            }
            game.isInExile(2, "Hill Giant") shouldBe true
            game.findPermanent("Grizzly Bears") shouldNotBe null
            game.findPermanent("Craw Wurm") shouldNotBe null
            withClue("the Saga is sacrificed after chapter III") {
                game.findPermanent("Fall of the Impostor") shouldBe null
            }
        }

        test("a tie for greatest power is the Saga controller's choice") {
            val game = builder()
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
            val giants = game.findAllPermanents("Hill Giant")

            game.playChapterThree()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            decision.playerId shouldBe game.player1Id
            decision.options shouldContainExactlyInAnyOrder giants
            game.selectCards(listOf(giants.first())).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Hill Giant") shouldBe true
            game.findAllPermanents("Hill Giant") shouldBe listOf(giants.last())
            game.findAllPermanents("Grizzly Bears").size shouldBe 1
        }
    }
}
