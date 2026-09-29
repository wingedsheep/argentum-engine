package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Traumatic Revelation (MOM #127) — "Target opponent reveals their hand. You may choose a creature
 * or battle card from it. If you do, that player discards that card. If you don't, incubate 3."
 */
class TraumaticRevelationScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun setup() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Traumatic Revelation")
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withCardInHand(2, "Grizzly Bears")
        .withCardInHand(2, "Lightning Bolt")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Traumatic Revelation") {

            test("choosing a creature card makes the opponent discard it and does not incubate") {
                val game = setup()
                val bears = game.findCardsInHand(2, "Grizzly Bears").single()

                game.castSpellTargetingPlayer(1, "Traumatic Revelation", 2).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the caster chooses") { decision.playerId shouldBe game.player1Id }
                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears discarded") { game.isInGraveyard(2, "Grizzly Bears") shouldBe true }
                withClue("Lightning Bolt stays in hand") { game.isInHand(2, "Lightning Bolt") shouldBe true }
                withClue("no Incubator") { game.findPermanent("Incubator") shouldBe null }
            }

            test("declining to choose incubates 3 and discards nothing") {
                val game = setup()

                game.castSpellTargetingPlayer(1, "Traumatic Revelation", 2).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                withClue("hand untouched") {
                    game.isInHand(2, "Grizzly Bears") shouldBe true
                    game.isInHand(2, "Lightning Bolt") shouldBe true
                }
                val incubator = game.findPermanent("Incubator")
                withClue("an Incubator with three +1/+1 counters") {
                    incubator shouldNotBe null
                    game.plusOneCounters(incubator!!) shouldBe 3
                }
            }

            test("with no creature or battle card in hand, incubates 3") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Traumatic Revelation")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInHand(2, "Lightning Bolt")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Traumatic Revelation", 2).error shouldBe null
                game.resolveStack()
                if (game.getPendingDecision() is SelectCardsDecision) {
                    game.selectCards(emptyList())
                    game.resolveStack()
                }

                withClue("Lightning Bolt can't be chosen") { game.isInHand(2, "Lightning Bolt") shouldBe true }
                val incubator = game.findPermanent("Incubator")
                withClue("an Incubator with three +1/+1 counters") {
                    incubator shouldNotBe null
                    game.plusOneCounters(incubator!!) shouldBe 3
                }
            }
        }
    }
}
