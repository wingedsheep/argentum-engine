package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bfz.cards.OracleOfDust
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Oracle of Dust — {2}, Put a card an opponent owns from exile into that player's graveyard:
 * Draw a card, then discard a card. The processor cost is paid with an opponent's exiled card,
 * never your own; the loot draws first, so the drawn card is a legal discard.
 */
class OracleOfDustScenarioTest : FunSpec({

    val abilityId = OracleOfDust.activatedAbilities.first().id

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(OracleOfDust)
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("processing an opponent's exiled card draws a card, then discards a chosen card") {
        val d = driver()
        val alice = d.activePlayer!!
        val bob = d.getOpponent(alice)
        val oracle = d.putPermanentOnBattlefield(alice, "Oracle of Dust")
        val exiled = d.putCardInExile(bob, "Island")
        val drawn = d.putCardOnTopOfLibrary(alice, "Grizzly Bears")
        val handBefore = d.getHandSize(alice)
        val discarded = d.getHand(alice).first()
        d.giveColorlessMana(alice, 2)

        d.submit(
            ActivateAbility(
                playerId = alice,
                sourceId = oracle,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(exiledCards = listOf(exiled)),
            )
        ).error shouldBe null
        d.getGraveyard(bob) shouldContain exiled
        d.bothPass()

        val discard = d.pendingDecision as? SelectCardsDecision
            ?: error("expected a discard selection; got ${d.pendingDecision}")
        discard.options shouldContain drawn
        d.submitCardSelection(alice, listOf(discarded)).error shouldBe null

        d.getHand(alice) shouldContain drawn
        d.getHand(alice) shouldNotContain discarded
        d.getGraveyard(alice) shouldContain discarded
        d.getHandSize(alice) shouldBe handBefore
    }

    test("it can't be activated with only your own cards in exile") {
        val d = driver()
        val alice = d.activePlayer!!
        val oracle = d.putPermanentOnBattlefield(alice, "Oracle of Dust")
        val mine = d.putCardInExile(alice, "Island")
        d.giveColorlessMana(alice, 2)

        d.legalActions(alice).none { (it.action as? ActivateAbility)?.sourceId == oracle && it.affordable } shouldBe true
        d.submit(
            ActivateAbility(
                playerId = alice,
                sourceId = oracle,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(exiledCards = listOf(mine)),
            )
        ).error shouldNotBe null
        d.state.getZone(ZoneKey(alice, Zone.EXILE)) shouldContain mine
    }
})
