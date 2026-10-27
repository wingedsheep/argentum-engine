package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
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
 * The Bears of Littjara (KHM #205).
 *
 *   II — Any number of target Shapeshifter creatures you control have base power and toughness 4/4.
 *   III — Choose up to one target creature or planeswalker. Each creature with power 4 or greater
 *         you control deals damage equal to its power to that permanent.
 *
 * Pins that chapter II reaches every chosen Shapeshifter and lasts past end of turn, and that
 * chapter III sums damage only from your power-4+ creatures, with "up to one" allowing no target.
 */
class TheBearsOfLittjaraScenarioTest : ScenarioTestBase() {

    init {
        fun builder() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "The Bears of Littjara")
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .apply { repeat(6) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Forest") } }

        /** Set the Saga one counter short of [chapter] and advance to the main phase so it triggers. */
        fun TestGame.triggerChapter(chapter: Int) {
            val saga = findPermanent("The Bears of Littjara")!!
            state = state.updateEntity(saga) { container ->
                container.with(CountersComponent().withAdded(CounterType.LORE, chapter - 1))
                    .with(SagaComponent(triggeredChapters = (1 until chapter).toSet()))
            }
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
        }

        fun TestGame.damageOn(id: EntityId) = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

        context("chapter II") {
            test("every chosen Shapeshifter becomes a 4/4, and it lasts beyond the turn") {
                val game = builder()
                    .withCardOnBattlefield(1, "Woodland Changeling")
                    .withCardOnBattlefield(1, "Woodland Changeling")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .build()
                val changelings = game.findAllPermanents("Woodland Changeling")

                game.triggerChapter(2)
                game.selectTargets(changelings).error shouldBe null
                game.resolveStack()

                changelings.forEach { id ->
                    game.state.projectedState.getPower(id) shouldBe 4
                    game.state.projectedState.getToughness(id) shouldBe 4
                }
                withClue("a non-Shapeshifter is untouched") {
                    game.state.projectedState.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 2
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id
                withClue("the base P/T setting doesn't expire at end of turn") {
                    changelings.forEach { id -> game.state.projectedState.getPower(id) shouldBe 4 }
                }
            }

            test("a non-Shapeshifter creature you control is not a legal target") {
                val game = builder()
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Woodland Changeling")
                    .build()

                game.triggerChapter(2)
                withClue("Grizzly Bears isn't a Shapeshifter") {
                    (game.selectTargets(listOf(game.findPermanent("Grizzly Bears")!!)).error != null) shouldBe true
                }
            }
        }

        context("chapter III") {
            test("only your creatures with power 4 or greater deal damage, each equal to its power") {
                val game = builder()
                    .withCardOnBattlefield(1, "Serra Angel")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withCardOnBattlefield(2, "Colossal Dreadmaw")
                    .build()
                val dreadmaw = game.findPermanent("Colossal Dreadmaw")!!

                game.triggerChapter(3)
                game.selectTargets(listOf(dreadmaw)).error shouldBe null
                game.resolveStack()

                withClue("Serra Angel's 4; the 3-power Hill Giant and the opponent's Craw Wurm deal nothing") {
                    game.damageOn(dreadmaw) shouldBe 4
                }
                game.findPermanent("The Bears of Littjara") shouldBe null
            }

            test("several qualifying creatures each deal their power") {
                val game = builder()
                    .withCardOnBattlefield(1, "Serra Angel")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Colossal Dreadmaw")
                    .build()
                val dreadmaw = game.findPermanent("Colossal Dreadmaw")!!

                game.triggerChapter(3)
                game.selectTargets(listOf(dreadmaw)).error shouldBe null
                game.resolveStack()

                withClue("4 + 6 damage destroys the 6/6") { game.findPermanent("Colossal Dreadmaw") shouldBe null }
            }

            test("choosing no target does nothing and the Saga is still sacrificed") {
                val game = builder()
                    .withCardOnBattlefield(1, "Serra Angel")
                    .withCardOnBattlefield(2, "Colossal Dreadmaw")
                    .build()

                game.triggerChapter(3)
                game.skipTargets().error shouldBe null
                game.resolveStack()

                game.damageOn(game.findPermanent("Colossal Dreadmaw")!!) shouldBe 0
                game.damageOn(game.findPermanent("Serra Angel")!!) shouldBe 0
                game.findPermanent("The Bears of Littjara") shouldBe null
            }
        }
    }
}
