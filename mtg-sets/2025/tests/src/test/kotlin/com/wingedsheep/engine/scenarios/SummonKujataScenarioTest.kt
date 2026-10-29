package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Summon: Kujata (FIC #61), chapter III — "Discard a card, then draw two cards. When you discard a
 * card this way, this creature deals damage equal to that card's mana value to each opponent."
 *
 * What rides on the engine rather than the script: the reflexive trigger must carry the discarded
 * card's mana value across the stack round-trip, the two cards are still drawn, and the damage is
 * dealt even though the Saga has been sacrificed (chapter III is its last) by the time the
 * reflexive trigger resolves.
 */
class SummonKujataScenarioTest : ScenarioTestBase() {

    init {
        fun builder() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Summon: Kujata")
            .withCardInHand(1, "Craw Wurm")
            .withCardInHand(1, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .apply { repeat(6) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }

        fun TestGame.reachChapterThreeDiscard() {
            val saga = findPermanent("Summon: Kujata")!!
            state = state.updateEntity(saga) { container ->
                container.with(CountersComponent().withAdded(CounterType.LORE, 2))
                    .with(SagaComponent(triggeredChapters = setOf(1, 2)))
            }
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            resolveStack()
            getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
        }

        fun TestGame.handCard(name: String) = state.getHand(player1Id).first { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == name
        }

        test("discarding a mana value 6 card deals 6 to each opponent, and you still draw two") {
            val game = builder().build()
            game.reachChapterThreeDiscard()
            val handBefore = game.handSize(1)

            game.selectCards(listOf(game.handCard("Craw Wurm"))).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Craw Wurm") shouldBe true
            withClue("one discarded, two drawn") {
                game.handSize(1) shouldBe handBefore + 1
            }
            withClue("the reflexive trigger reads the discarded card's mana value") {
                game.getLifeTotal(2) shouldBe 14
            }
            withClue("the Saga is sacrificed after chapter III") {
                game.findPermanent("Summon: Kujata") shouldBe null
            }
        }

        test("discarding a land deals no damage") {
            val game = builder().build()
            game.reachChapterThreeDiscard()
            val handBefore = game.handSize(1)

            game.selectCards(listOf(game.handCard("Island"))).error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe handBefore + 1
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
