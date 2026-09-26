package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.NezumiGraverobber
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Nezumi Graverobber // Nighteyes the Desecrator (CHK) — a flip card.
 *
 * "{1}{B}: Exile target card from an opponent's graveyard. If no cards are in that graveyard,
 * flip this creature."
 * Nighteyes: "{4}{B}: Put target creature card from a graveyard onto the battlefield under your
 * control."
 *
 * The emptiness check reads the exiled card's owner's graveyard after the exile.
 */
class NezumiGraverobberScenarioTest : FunSpec({

    val robAbility = NezumiGraverobber.activatedAbilities.single().id
    val nighteyesAbility = NezumiGraverobber.flipSide!!.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + NezumiGraverobber)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.rob(robber: EntityId, card: EntityId) {
        giveMana(player1, Color.BLACK, 2)
        submitSuccess(
            ActivateAbility(
                player1, robber, robAbility,
                targets = listOf(ChosenTarget.Card(card, player2, Zone.GRAVEYARD)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        bothPass()
    }

    fun GameTestDriver.name(id: EntityId) = state.getEntity(id)!!.get<CardComponent>()!!.name

    test("exiling a card while others remain in that graveyard leaves it unflipped") {
        val d = driver()
        val robber = d.putCreatureOnBattlefield(d.player1, "Nezumi Graverobber")
        val first = d.putCardInGraveyard(d.player2, "Centaur Courser")
        d.putCardInGraveyard(d.player2, "Centaur Courser")

        d.rob(robber, first)

        d.getExile(d.player2) shouldContain first
        d.getGraveyard(d.player2).size shouldBe 1
        d.name(robber) shouldBe "Nezumi Graverobber"
    }

    test("exiling the last card flips it into Nighteyes, which reanimates from any graveyard") {
        val d = driver()
        val robber = d.putCreatureOnBattlefield(d.player1, "Nezumi Graverobber")
        val last = d.putCardInGraveyard(d.player2, "Centaur Courser")
        // A card in the robber's own graveyard doesn't count — "that graveyard" is the opponent's.
        val mine = d.putCardInGraveyard(d.player1, "Centaur Courser")

        d.rob(robber, last)

        d.getGraveyard(d.player2).size shouldBe 0
        d.name(robber) shouldBe "Nighteyes the Desecrator"
        d.state.projectedState.getPower(robber) shouldBe 4
        d.state.projectedState.getToughness(robber) shouldBe 2
        d.state.projectedState.isLegendary(robber) shouldBe true

        d.giveMana(d.player1, Color.BLACK, 5)
        d.submitSuccess(
            ActivateAbility(
                d.player1, robber, nighteyesAbility,
                targets = listOf(ChosenTarget.Card(mine, d.player1, Zone.GRAVEYARD)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        d.bothPass()

        d.state.getBattlefield(d.player1) shouldContain mine
        d.getController(mine) shouldBe d.player1
    }

    test("Nighteyes steals a creature card from an opponent's graveyard") {
        val d = driver()
        val robber = d.putCreatureOnBattlefield(d.player1, "Nezumi Graverobber")
        val last = d.putCardInGraveyard(d.player2, "Centaur Courser")
        d.rob(robber, last)
        d.name(robber) shouldBe "Nighteyes the Desecrator"

        val theirs = d.putCardInGraveyard(d.player2, "Centaur Courser")
        d.giveMana(d.player1, Color.BLACK, 5)
        d.submitSuccess(
            ActivateAbility(
                d.player1, robber, nighteyesAbility,
                targets = listOf(ChosenTarget.Card(theirs, d.player2, Zone.GRAVEYARD)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        d.bothPass()

        d.state.getBattlefield(d.player1) shouldContain theirs
        d.getController(theirs) shouldBe d.player1
    }
})
