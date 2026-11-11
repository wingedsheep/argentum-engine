package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bfz.cards.CrypticCruiser
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Cryptic Cruiser — {2}{U}, Put a card an opponent owns from exile into that player's graveyard:
 * Tap target creature. The processor cost is paid with an opponent's exiled card, never your own.
 */
class CrypticCruiserScenarioTest : FunSpec({

    val abilityId = CrypticCruiser.activatedAbilities.first().id

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(CrypticCruiser)
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("processing an opponent's exiled card taps target creature") {
        val d = driver()
        val alice = d.activePlayer!!
        val bob = d.getOpponent(alice)
        val cruiser = d.putPermanentOnBattlefield(alice, "Cryptic Cruiser")
        val bears = d.putPermanentOnBattlefield(bob, "Grizzly Bears")
        val exiled = d.putCardInExile(bob, "Island")
        d.giveMana(alice, Color.BLUE, 1)
        d.giveColorlessMana(alice, 2)

        d.submit(
            ActivateAbility(
                playerId = alice,
                sourceId = cruiser,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(bears)),
                costPayment = AdditionalCostPayment(exiledCards = listOf(exiled)),
            )
        ).error shouldBe null
        d.getGraveyard(bob) shouldContain exiled
        d.bothPass()
        d.isTapped(bears) shouldBe true
    }

    test("it can't be activated with only your own cards in exile") {
        val d = driver()
        val alice = d.activePlayer!!
        val bob = d.getOpponent(alice)
        val cruiser = d.putPermanentOnBattlefield(alice, "Cryptic Cruiser")
        val bears = d.putPermanentOnBattlefield(bob, "Grizzly Bears")
        val mine = d.putCardInExile(alice, "Island")
        d.giveMana(alice, Color.BLUE, 1)
        d.giveColorlessMana(alice, 2)

        d.legalActions(alice).none { (it.action as? ActivateAbility)?.sourceId == cruiser && it.affordable } shouldBe true
        d.submit(
            ActivateAbility(
                playerId = alice,
                sourceId = cruiser,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(bears)),
                costPayment = AdditionalCostPayment(exiledCards = listOf(mine)),
            )
        ).error shouldNotBe null
        d.state.getZone(ZoneKey(alice, Zone.EXILE)) shouldContain mine
    }
})
