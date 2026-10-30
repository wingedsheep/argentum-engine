package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The Binding of the Titans (THB #166).
 *
 *   II — Exile up to two target cards from graveyards. For each creature card exiled this way,
 *        you gain 1 life.
 *   III — Return target creature or land card from your graveyard to your hand.
 *
 * Chapter II's life gain counts only the creature cards among those actually exiled: one creature
 * and one noncreature card gain exactly 1. Chapter III may only target a creature or land card.
 */
class TheBindingOfTheTitansScenarioTest : ScenarioTestBase() {

    init {
        fun builder() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "The Binding of the Titans")
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .apply { repeat(4) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Forest") } }

        /** Sets the Saga to [lore] counters with every earlier chapter done, then draws into the next one. */
        fun TestGame.advanceSagaFrom(lore: Int) {
            val saga = findPermanent("The Binding of the Titans")!!
            state = state.updateEntity(saga) { container ->
                container.with(CountersComponent().withAdded(CounterType.LORE, lore))
                    .with(SagaComponent(triggeredChapters = (1..lore).toSet()))
            }
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        }

        test("chapter II exiling one creature card and one noncreature card gains exactly 1 life") {
            val game = builder()
                .withCardInGraveyard(2, "Grizzly Bears")
                .withCardInGraveyard(2, "Lightning Bolt")
                .build()
            val bears = game.findCardsInGraveyard(2, "Grizzly Bears").single()
            val bolt = game.findCardsInGraveyard(2, "Lightning Bolt").single()

            game.advanceSagaFrom(1)
            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(bears, bolt)).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Grizzly Bears") shouldBe true
            game.isInExile(2, "Lightning Bolt") shouldBe true
            withClue("only the creature card counts toward the life gain") {
                game.getLifeTotal(1) shouldBe 21
            }
        }

        test("chapter III returns a creature or land card from your graveyard to your hand") {
            val game = builder()
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Forest")
                .withCardInGraveyard(1, "Lightning Bolt")
                .build()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val forest = game.findCardsInGraveyard(1, "Forest").single()

            game.advanceSagaFrom(2)
            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            withClue("a noncreature, nonland card isn't a legal target") {
                decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(bears, forest)
            }
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Forest") shouldBe true
            game.isInGraveyard(1, "Lightning Bolt") shouldBe true
        }
    }
}
