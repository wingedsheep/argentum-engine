package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.TheRingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * In the Darkness Bind Them (LTC #58) — chapter IV's one-creature-per-opponent steal on a Saga
 * chapter trigger, followed by the Ring tempting you (also when no target was chosen).
 */
class InTheDarknessBindThemScenarioTest : ScenarioTestBase() {

    private val saga = "In the Darkness Bind Them"

    /** Saga with three lore counters, moving into the main phase so chapter IV triggers. */
    private fun atChapterFour(extra: ScenarioBuilder.() -> Unit): TestGame {
        val game = scenario()
            .withPlayers("Alice", "Bob")
            .withCardOnBattlefield(1, saga)
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .withTurnNumber(3)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .apply(extra)
            .build()
        val sagaId = game.findPermanent(saga)!!
        game.state = game.state.updateEntity(sagaId) { c ->
            c.with(CountersComponent().withAdded(CounterType.LORE, 3))
                .with(SagaComponent(triggeredChapters = setOf(1, 2, 3)))
        }
        game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        return game
    }

    private fun temptCount(game: TestGame): Int =
        game.state.getEntity(game.player1Id)?.get<TheRingComponent>()?.temptCount ?: 0

    init {
        test("chapter I creates a 3/3 black Wraith with menace and the Ring tempts you") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, saga)
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, saga).error shouldBe null
            game.resolveStack()
            game.resolveStack()

            // The Wraith just created can be chosen as Ring-bearer (ruling).
            val ring = game.getPendingDecision()
            ring.shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(listOf(ring.options.single()))
            game.resolveStack()

            val wraith = game.state.projectedState.let { p ->
                game.state.getBattlefield().single { p.hasSubtype(it, "Wraith") }
            }
            game.state.projectedState.getPower(wraith) shouldBe 3
            game.state.projectedState.getToughness(wraith) shouldBe 3
            game.state.projectedState.hasKeyword(wraith, Keyword.MENACE) shouldBe true
            temptCount(game) shouldBe 1
        }

        test("chapter IV steals the opponent's creature until end of turn, untaps it, gives haste, then tempts") {
            val game = atChapterFour {
                withCardOnBattlefield(2, "Hill Giant", tapped = true)
            }
            val giant = game.findPermanent("Hill Giant")!!

            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()

            // The Ring tempts you; the stolen giant is now a creature you control.
            val ring = game.getPendingDecision()
            ring.shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(listOf(giant))
            game.resolveStack()

            game.state.projectedState.getController(giant) shouldBe game.player1Id
            game.state.getEntity(giant)!!.has<TappedComponent>() shouldBe false
            game.state.projectedState.hasKeyword(giant, Keyword.HASTE) shouldBe true
            temptCount(game) shouldBe 1

            // Control returns at end of turn.
            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.projectedState.getController(giant) shouldBe game.player2Id
        }

        test("chapter IV with no targets chosen still tempts you") {
            val game = atChapterFour {
                withCardOnBattlefield(2, "Hill Giant")
            }
            val giant = game.findPermanent("Hill Giant")!!

            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.skipTargets().error shouldBe null
            game.resolveStack()

            game.getPendingDecision() shouldBe null
            game.state.projectedState.getController(giant) shouldBe game.player2Id
            temptCount(game) shouldBe 1
        }
    }
}
