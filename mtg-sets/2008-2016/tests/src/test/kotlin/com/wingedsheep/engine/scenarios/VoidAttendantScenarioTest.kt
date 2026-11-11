package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bfz.cards.VoidAttendant
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Void Attendant — {1}{G}, Put a card an opponent owns from exile into that player's graveyard:
 * Create a 1/1 colorless Eldrazi Scion creature token. The processor cost is paid with an
 * opponent's exiled card, never your own.
 */
class VoidAttendantScenarioTest : FunSpec({

    val abilityId = VoidAttendant.activatedAbilities.first().id

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(VoidAttendant)
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.scions(you: EntityId): List<EntityId> =
        getCreatures(you).filter { state.projectedState.hasSubtype(it, "Scion") }

    test("processing an opponent's exiled card creates a 1/1 colorless Eldrazi Scion") {
        val d = driver()
        val alice = d.activePlayer!!
        val bob = d.getOpponent(alice)
        val attendant = d.putPermanentOnBattlefield(alice, "Void Attendant")
        val exiled = d.putCardInExile(bob, "Forest")
        d.giveMana(alice, Color.GREEN, 1)
        d.giveColorlessMana(alice, 1)

        d.submit(
            ActivateAbility(
                playerId = alice,
                sourceId = attendant,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(exiledCards = listOf(exiled)),
            )
        ).error shouldBe null
        d.getGraveyard(bob) shouldContain exiled
        d.bothPass()

        val scions = d.scions(alice)
        scions shouldHaveSize 1
        val scion = scions.single()
        val projected = d.state.projectedState
        projected.hasSubtype(scion, "Eldrazi") shouldBe true
        projected.getPower(scion) shouldBe 1
        projected.getToughness(scion) shouldBe 1
        projected.getColors(scion) shouldBe emptySet()
    }

    test("it can't be activated with only your own cards in exile") {
        val d = driver()
        val alice = d.activePlayer!!
        val attendant = d.putPermanentOnBattlefield(alice, "Void Attendant")
        val mine = d.putCardInExile(alice, "Forest")
        d.giveMana(alice, Color.GREEN, 1)
        d.giveColorlessMana(alice, 1)

        d.legalActions(alice).none { (it.action as? ActivateAbility)?.sourceId == attendant && it.affordable } shouldBe true
        d.submit(
            ActivateAbility(
                playerId = alice,
                sourceId = attendant,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(exiledCards = listOf(mine)),
            )
        ).error shouldNotBe null
        d.state.getZone(ZoneKey(alice, Zone.EXILE)) shouldContain mine
        d.scions(alice) shouldHaveSize 0
    }
})
