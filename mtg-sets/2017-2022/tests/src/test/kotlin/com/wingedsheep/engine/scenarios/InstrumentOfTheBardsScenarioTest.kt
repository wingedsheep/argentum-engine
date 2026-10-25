package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.afr.cards.InstrumentOfTheBards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Instrument of the Bards (AFR #190) — {G} Legendary Artifact.
 *
 *   At the beginning of your upkeep, you may put a harmony counter on Instrument of the Bards.
 *   {3}{G}, {T}: Search your library for a creature card with mana value equal to the number of
 *   harmony counters on Instrument of the Bards, reveal it, and put it into your hand. If that
 *   card is legendary, create a Treasure token. Then shuffle.
 */
class InstrumentOfTheBardsScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.harmonyCounters(): Int =
        state.getEntity(findPermanent("Instrument of the Bards")!!)
            ?.get<CountersComponent>()?.getCount(CounterType.HARMONY) ?: 0

    private fun ScenarioTestBase.TestGame.setHarmony(count: Int) {
        val instrument = findPermanent("Instrument of the Bards")!!
        state = state.updateEntity(instrument) {
            it.with(CountersComponent().withAdded(CounterType.HARMONY, count))
        }
    }

    private fun ScenarioTestBase.TestGame.activateInstrument() {
        execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = findPermanent("Instrument of the Bards")!!,
                abilityId = InstrumentOfTheBards.activatedAbilities.single().id
            )
        ).error shouldBe null
        resolveStack()
    }

    private fun builder() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Instrument of the Bards")
        .withLandsOnBattlefield(1, "Forest", 4)
        .withCardInLibrary(1, "Isamaru, Hound of Konda")
        .withCardInLibrary(1, "Llanowar Elves")
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("the upkeep trigger may add a harmony counter") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Instrument of the Bards")
                .withActivePlayer(2)
                .inPhase(Phase.ENDING, Step.END)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()
            game.answerYesNo(true)
            game.resolveStack()

            game.harmonyCounters() shouldBe 1
        }

        test("declining the upkeep trigger adds no counter") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Instrument of the Bards")
                .withActivePlayer(2)
                .inPhase(Phase.ENDING, Step.END)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()
            game.answerYesNo(false)
            game.resolveStack()

            game.harmonyCounters() shouldBe 0
        }

        test("it finds only creatures whose mana value equals the harmony count, and a nonlegendary one makes no Treasure") {
            val game = builder().build()
            game.setHarmony(2)

            game.activateInstrument()

            withClue("only the mana value 2 creature is offered") {
                game.getPendingDecision()!!.let { decision ->
                    (decision as SelectCardsDecision).options shouldBe
                        game.findCardsInLibrary(1, "Grizzly Bears")
                }
            }
            game.selectCards(game.findCardsInLibrary(1, "Grizzly Bears"))
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.findPermanents("Treasure").size shouldBe 0
        }

        test("finding a legendary creature creates a Treasure") {
            val game = builder().build()
            game.setHarmony(1)

            game.activateInstrument()
            game.selectCards(game.findCardsInLibrary(1, "Isamaru, Hound of Konda"))
            game.resolveStack()

            game.isInHand(1, "Isamaru, Hound of Konda") shouldBe true
            game.findPermanents("Treasure").size shouldBe 1
        }

        test("finding nothing makes no Treasure") {
            val game = builder().build()
            game.setHarmony(1)

            game.activateInstrument()
            game.skipSelection()
            game.resolveStack()

            game.isInHand(1, "Isamaru, Hound of Konda") shouldBe false
            game.findPermanents("Treasure").size shouldBe 0
        }
    }
}
