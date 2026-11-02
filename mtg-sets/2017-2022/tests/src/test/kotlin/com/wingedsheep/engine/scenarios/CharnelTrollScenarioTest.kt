package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Charnel Troll (GRN #160): the upkeep trigger exiles a creature card from your graveyard and
 * grows the Troll, or — with no creature card to exile — sacrifices it. A noncreature card in the
 * graveyard doesn't keep it alive.
 */
class CharnelTrollScenarioTest : ScenarioTestBase() {

    init {
        // Opponent is active in their main phase; the next upkeep is the Troll controller's.
        fun TestGame.toTrollUpkeepAndResolve() {
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            state.activePlayerId shouldBe player1Id
            resolveStack()
            if (getPendingDecision() is SelectCardsDecision) {
                selectCards(listOf(findCardsInGraveyard(1, "Grizzly Bears").single())).error shouldBe null
                resolveStack()
            }
        }

        context("Charnel Troll upkeep trigger") {

            test("exiles a creature card and puts a +1/+1 counter on the Troll") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Charnel Troll")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Giant Growth")
                    .apply { repeat(5) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Forest") } }
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.toTrollUpkeepAndResolve()

                val troll = game.findPermanent("Charnel Troll")
                withClue("the Troll survives with a +1/+1 counter") {
                    (troll != null) shouldBe true
                    game.state.getEntity(troll!!)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }
                withClue("only the creature card is exiled") {
                    game.isInExile(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Giant Growth") shouldBe true
                }
            }

            test("with no creature card in the graveyard the Troll is sacrificed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Charnel Troll")
                    .withCardInGraveyard(1, "Giant Growth")
                    .apply { repeat(5) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Forest") } }
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.toTrollUpkeepAndResolve()

                withClue("the Troll is sacrificed and the noncreature card stays put") {
                    game.findPermanent("Charnel Troll") shouldBe null
                    game.isInGraveyard(1, "Charnel Troll") shouldBe true
                    game.isInGraveyard(1, "Giant Growth") shouldBe true
                }
            }
        }
    }
}
