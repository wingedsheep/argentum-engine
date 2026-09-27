package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.AuraOfDominion
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Aura of Dominion (CHK #51) — "Enchant creature. {1}, Tap an untapped creature you control:
 * Untap enchanted creature."
 *
 * The tap is a cost of the Aura's ability, not a {T} symbol, so a freshly cast creature can pay it
 * and the {1} is required on top.
 */
class AuraOfDominionScenarioTest : FunSpec({

    val untapAbility = AuraOfDominion.activatedAbilities.single().id

    fun GameTestDriver.attachAura(auraId: EntityId, hostId: EntityId) {
        replaceState(state.updateEntity(auraId) { it.with(AttachedToComponent(hostId)) })
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(AuraOfDominion))
        initMirrorMatch(Deck.of("Island" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.activate(player: EntityId, aura: EntityId, tapping: EntityId) =
        submit(
            ActivateAbility(
                playerId = player,
                sourceId = aura,
                abilityId = untapAbility,
                costPayment = AdditionalCostPayment(tappedPermanents = listOf(tapping)),
            )
        )

    test("paying {1} and tapping another creature untaps the enchanted creature") {
        val d = driver()
        val me = d.activePlayer!!
        val host = d.putCreatureOnBattlefield(me, "Centaur Courser")
        val helper = d.putCreatureOnBattlefield(me, "Savannah Lions")
        val aura = d.putPermanentOnBattlefield(me, "Aura of Dominion")
        d.attachAura(aura, host)
        d.tapPermanent(host)

        d.giveMana(me, Color.BLUE, 1)
        d.activate(me, aura, helper).error shouldBe null
        withClue("the helper creature paid the cost") { d.isTapped(helper) shouldBe true }
        d.bothPass()

        withClue("the enchanted creature is untapped on resolution") { d.isTapped(host) shouldBe false }
    }

    test("the ability cannot be activated without the {1}") {
        val d = driver()
        val me = d.activePlayer!!
        val host = d.putCreatureOnBattlefield(me, "Centaur Courser")
        val helper = d.putCreatureOnBattlefield(me, "Savannah Lions")
        val aura = d.putPermanentOnBattlefield(me, "Aura of Dominion")
        d.attachAura(aura, host)
        d.tapPermanent(host)

        d.activate(me, aura, helper).error shouldNotBe null
        d.isTapped(helper) shouldBe false
        d.isTapped(host) shouldBe true
    }

    test("an already-tapped creature cannot pay the tap cost") {
        val d = driver()
        val me = d.activePlayer!!
        val host = d.putCreatureOnBattlefield(me, "Centaur Courser")
        val helper = d.putCreatureOnBattlefield(me, "Savannah Lions")
        val aura = d.putPermanentOnBattlefield(me, "Aura of Dominion")
        d.attachAura(aura, host)
        d.tapPermanent(host)
        d.tapPermanent(helper)

        d.giveMana(me, Color.BLUE, 1)
        d.activate(me, aura, helper).error shouldNotBe null
        d.isTapped(host) shouldBe true
    }
})
