package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.battlefield.chosenCardName
import com.wingedsheep.engine.state.components.battlefield.withCastChoice
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Medomai's Prophecy (THB #53).
 *
 *   II — Choose a card name.
 *   III — When you cast a spell with the chosen name for the first time this turn, draw two cards.
 *   IV — Look at the top card of each player's library.
 *
 * Pins that chapter II's name survives to chapter III's turn, that only the first spell with the
 * name this turn draws, and that chapter IV only looks (nothing moves).
 */
class MedomaisProphecyScenarioTest : ScenarioTestBase() {

    init {
        fun builder() = scenario()
            .withPlayers("Prophet", "Opponent")
            .withCardOnBattlefield(1, "Medomai's Prophecy")
            .withLandsOnBattlefield(1, "Mountain", 3)
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .apply { repeat(8) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }

        /** Put the Saga at [lore] lore counters, so the next one added triggers chapter [lore] + 1. */
        fun TestGame.setLore(lore: Int) {
            val saga = findPermanent("Medomai's Prophecy")!!
            state = state.updateEntity(saga) { container ->
                container.with(CountersComponent().withAdded(CounterType.LORE, lore))
                    .with(SagaComponent(triggeredChapters = (1..lore).toSet()))
            }
        }

        test("chapter II's name carries to chapter III, and only the first such spell draws two") {
            val game = builder()
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Lightning Bolt")
                .build()
            game.setLore(1)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.resolveStack()
            val decision = game.getPendingDecision() as ChooseOptionDecision
            game.submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf("Lightning Bolt")))
                .error shouldBe null
            game.resolveStack()
            val saga = game.findPermanent("Medomai's Prophecy")!!
            game.state.getEntity(saga)?.chosenCardName() shouldBe "Lightning Bolt"

            // Chapter III, as on the next turn: the recorded name is still on the Saga.
            game.state = game.state.updateEntity(saga) { container ->
                container.with(CountersComponent().withAdded(CounterType.LORE, 2))
                    .with(SagaComponent(triggeredChapters = setOf(1, 2)))
            }
            repeat(2) {
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            }
            withClue("back on the Prophet's turn") { game.state.activePlayerId shouldBe game.player1Id }
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.resolveStack()

            val before = game.handSize(1)
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()
            withClue("cast one Bolt (-1), drew two (+2)") { game.handSize(1) shouldBe before + 1 }

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()
            withClue("the second Bolt this turn draws nothing") { game.handSize(1) shouldBe before }
        }

        test("the name is still read if the Saga is destroyed in response to chapter III") {
            val game = builder()
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInHand(1, "Disenchant")
                .withCardInHand(1, "Lightning Bolt")
                .build()
            game.setLore(2)
            val saga = game.findPermanent("Medomai's Prophecy")!!
            game.state = game.state.updateEntity(saga) {
                it.withCastChoice(ChoiceSlot.CARD_NAME, ChoiceValue.TextChoice("Lightning Bolt"))
            }
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            withClue("chapter III is waiting on the stack") { game.state.stack.isEmpty() shouldBe false }
            game.castSpell(1, "Disenchant", saga).error shouldBe null
            game.resolveStack()
            withClue("the Saga is gone before chapter III resolved") {
                game.findPermanent("Medomai's Prophecy") shouldBe null
            }

            val before = game.handSize(1)
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()
            withClue("the Saga's last-known chosen name still counts: -1 Bolt, +2 cards") {
                game.handSize(1) shouldBe before + 1
            }
        }

        test("chapter IV looks at each library's top card without moving anything") {
            val game = builder().build()
            game.setLore(3)
            val library1 = game.librarySize(1)
            val library2 = game.librarySize(2)
            val tops = listOf(game.state.getLibrary(game.player1Id).first(), game.state.getLibrary(game.player2Id).first())
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.resolveStack()

            withClue("the look moved nothing (the starting player skips the turn-1 draw)") {
                game.librarySize(1) shouldBe library1
                game.librarySize(2) shouldBe library2
            }
            withClue("each top card was seen by the Saga's controller only") {
                tops.forEach { game.state.getEntity(it)?.get<RevealedToComponent>()?.playerIds shouldBe setOf(game.player1Id) }
            }
            withClue("sacrificed after IV") { game.findPermanent("Medomai's Prophecy") shouldBe null }
        }
    }
}
