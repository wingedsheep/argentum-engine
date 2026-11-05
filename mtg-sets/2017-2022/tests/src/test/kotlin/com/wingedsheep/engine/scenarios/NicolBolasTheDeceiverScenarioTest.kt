package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hou.cards.NicolBolasTheDeceiver
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Nicol Bolas, the Deceiver (HOU #205).
 *
 * +3: "Each opponent loses 3 life unless that player sacrifices a nonland permanent of their choice
 * or discards a card." The opponent — not Bolas's controller — chooses; an option they can't afford
 * (lands aren't nonland permanents, an empty hand can't discard) isn't offered, and with neither
 * they simply lose 3 life.
 */
class NicolBolasTheDeceiverScenarioTest : ScenarioTestBase() {

    private val plusThree = NicolBolasTheDeceiver.activatedAbilities[0].id

    private fun TestGame.setLoyalty(id: EntityId, loyalty: Int) {
        state = state.updateEntity(id) { c -> c.with(CountersComponent().withAdded(CounterType.LOYALTY, loyalty)) }
    }

    private fun TestGame.activatePlusThree() {
        val bolas = findPermanent("Nicol Bolas, the Deceiver")!!
        setLoyalty(bolas, 5)
        execute(ActivateAbility(player1Id, bolas, plusThree)).error shouldBe null
        resolveStack()
    }

    private fun boardWithBearsAndCardInHand() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Nicol Bolas, the Deceiver")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withLandsOnBattlefield(2, "Swamp", 1)
        .withCardInHand(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Nicol Bolas, the Deceiver +3") {

            test("the opponent may sacrifice a nonland permanent instead of losing life") {
                val game = boardWithBearsAndCardInHand()
                game.activatePlusThree()

                val choice = game.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
                withClue("the opponent makes the choice") { choice.playerId shouldBe game.player2Id }
                withClue("sacrifice, discard, and lose life are all offered") { choice.options.size shouldBe 3 }
                game.submitDecision(OptionChosenResponse(choice.id, 0))

                val pick = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("only the nonland permanent can be sacrificed") {
                    pick.options shouldBe listOf(game.findPermanent("Grizzly Bears")!!)
                }
                game.selectCards(pick.options)

                game.state.pendingDecision shouldBe null
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.handSize(2) shouldBe 1
                game.getLifeTotal(2) shouldBe 20
            }

            test("the opponent may discard a card instead of losing life") {
                val game = boardWithBearsAndCardInHand()
                game.activatePlusThree()

                val choice = game.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
                game.submitDecision(OptionChosenResponse(choice.id, 1))
                (game.state.pendingDecision as? SelectCardsDecision)?.let { game.selectCards(it.options) }

                game.state.pendingDecision shouldBe null
                game.handSize(2) shouldBe 0
                game.isInGraveyard(2, "Swamp") shouldBe true
                game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                game.getLifeTotal(2) shouldBe 20
            }

            test("the opponent may choose to lose 3 life even with options to pay") {
                val game = boardWithBearsAndCardInHand()
                game.activatePlusThree()

                val choice = game.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
                game.submitDecision(OptionChosenResponse(choice.id, choice.options.lastIndex))

                game.state.pendingDecision shouldBe null
                game.getLifeTotal(2) shouldBe 17
                game.handSize(2) shouldBe 1
                game.isInGraveyard(2, "Grizzly Bears") shouldBe false
            }

            test("an opponent with only lands and an empty hand loses 3 life without a choice") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nicol Bolas, the Deceiver")
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.activatePlusThree()

                game.state.pendingDecision shouldBe null
                game.getLifeTotal(2) shouldBe 17
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
