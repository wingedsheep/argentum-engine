package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.Demilich
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Demilich (AFR #53) — {U}{U}{U}{U} 4/3 Creature — Skeleton Wizard.
 *
 * "This spell costs {U} less to cast for each instant and sorcery spell you've cast this turn. /
 *  Whenever this creature attacks, exile up to one target instant or sorcery card from your
 *  graveyard. Copy it. You may cast the copy. / You may cast this card from your graveyard by
 *  exiling four instant and/or sorcery cards from your graveyard in addition to paying its other
 *  costs."
 *
 * Covers the self cost reduction — a `SelfCast` `ReduceColoredPerUnit` counted by a dynamic
 * spells-cast-this-turn amount — and the exile-four graveyard cast.
 */
class DemilichScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(Demilich))
        initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.boltOpponent() {
        val bolt = putCardInHand(player1, "Lightning Bolt")
        giveMana(player1, Color.RED, 1)
        castSpell(player1, bolt, targets = listOf(player2)).error shouldBe null
        bothPass()
    }

    test("with no instants or sorceries cast this turn, {U}{U} isn't enough") {
        val d = newDriver()
        val demilich = d.putCardInHand(d.player1, "Demilich")
        d.giveMana(d.player1, Color.BLUE, 2)
        d.castSpell(d.player1, demilich).error shouldNotBe null
    }

    test("two instants cast this turn reduce the cost to {U}{U}") {
        val d = newDriver()
        d.boltOpponent()
        d.boltOpponent()

        val demilich = d.putCardInHand(d.player1, "Demilich")
        d.giveMana(d.player1, Color.BLUE, 2)
        d.castSpell(d.player1, demilich).error shouldBe null
        d.bothPass()

        d.findPermanent(d.player1, "Demilich") shouldNotBe null
    }

    test("castable from the graveyard by exiling four instant and sorcery cards") {
        val d = newDriver()
        val you = d.player1
        val demilich = d.putCardInGraveyard(you, "Demilich")
        val fodder = List(4) { d.putCardInGraveyard(you, "Lightning Bolt") }
        d.giveMana(you, Color.BLUE, 4)

        val result = d.submit(
            CastSpell(
                playerId = you,
                cardId = demilich,
                paymentStrategy = PaymentStrategy.FromPool,
                additionalCostPayment = AdditionalCostPayment(exiledCards = fodder),
            )
        )
        result.outcome shouldBe Outcome.Done
        d.bothPass()

        d.findPermanent(you, "Demilich") shouldNotBe null
        fodder.all { d.state.getZone(you, Zone.EXILE).contains(it) } shouldBe true
    }
})
