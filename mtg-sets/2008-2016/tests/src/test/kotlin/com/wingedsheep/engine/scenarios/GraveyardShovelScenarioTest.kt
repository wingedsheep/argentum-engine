package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Graveyard Shovel (ISD #225) — {2} Artifact.
 *
 * "{2}, {T}: Target player exiles a card from their graveyard. If it's a creature card, you gain 2 life."
 *
 * Pins that the targeted player picks the card, and that the life gain reads the card they picked
 * and goes to the Shovel's controller: a creature gains 2, a non-creature gains nothing.
 */
class GraveyardShovelScenarioTest : ScenarioTestBase() {

    private fun board(): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Graveyard Shovel")
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withCardInGraveyard(2, "Grizzly Bears")
        .withCardInGraveyard(2, "Lightning Bolt")
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.activateShovel() = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent("Graveyard Shovel")!!,
            abilityId = cardRegistry.getCard("Graveyard Shovel")!!.activatedAbilities.single().id,
            targets = listOf(ChosenTarget.Player(player2Id)),
            paymentStrategy = PaymentStrategy.AutoPay,
        )
    )

    init {
        test("the targeted player exiles a creature card and the Shovel's controller gains 2 life") {
            val game = board()
            game.activateShovel().error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the targeted player chooses, not the Shovel's controller") {
                decision.playerId shouldBe game.player2Id
            }
            game.selectCards(listOf(game.findCardsInGraveyard(2, "Grizzly Bears").single())).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Lightning Bolt") shouldBe true
            game.getLifeTotal(1) shouldBe 22
            game.getLifeTotal(2) shouldBe 20
        }

        test("exiling a non-creature card gains no life") {
            val game = board()
            game.activateShovel().error shouldBe null
            game.resolveStack()

            game.selectCards(listOf(game.findCardsInGraveyard(2, "Lightning Bolt").single())).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Lightning Bolt") shouldBe true
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
