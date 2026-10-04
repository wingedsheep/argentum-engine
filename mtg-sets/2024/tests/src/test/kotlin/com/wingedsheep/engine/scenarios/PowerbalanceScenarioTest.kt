package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.Powerbalance
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Powerbalance ({R}{R}, Enchantment):
 * "Whenever an opponent casts a spell, you may reveal the top card of your library. If you do,
 *  you may cast that card without paying its mana cost if the two spells have the same mana value."
 */
class PowerbalanceScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Powerbalance))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30, "Forest" to 10))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.creatureNames(player: com.wingedsheep.sdk.model.EntityId) =
        getCreatures(player).mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }

    /** Opponent (the active player) casts Centaur Courser, MV 3; returns (caster, me). */
    fun GameTestDriver.opponentCastsCourser(): Pair<com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        val caster = activePlayer!!
        val me = getOpponent(caster)
        putPermanentOnBattlefield(me, "Powerbalance")
        val courser = putCardInHand(caster, "Centaur Courser")
        repeat(3) { putLandOnBattlefield(caster, "Forest") }
        submit(CastSpell(playerId = caster, cardId = courser, paymentStrategy = PaymentStrategy.AutoPay))
        return caster to me
    }

    test("same mana value: reveal and cast the top card for free") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val me = driver.getOpponent(caster)
        val warrior = driver.putCardOnTopOfLibrary(me, "Phantom Warrior") // {1}{U}{U}, MV 3
        driver.opponentCastsCourser()

        driver.bothPass()
        driver.submitYesNo(me, true)
        driver.submitCardSelection(me, listOf(warrior))
        repeat(3) { driver.bothPass() }

        driver.creatureNames(me).contains("Phantom Warrior") shouldBe true
        driver.creatureNames(caster).contains("Centaur Courser") shouldBe true
    }

    test("different mana value: the revealed card can't be cast and stays on top") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val me = driver.getOpponent(caster)
        val force = driver.putCardOnTopOfLibrary(me, "Force of Nature") // MV 5
        driver.opponentCastsCourser()

        driver.bothPass()
        driver.submitYesNo(me, true)
        val pending = driver.state.pendingDecision
        if (pending is SelectCardsDecision && pending.playerId == me) {
            (force in pending.options) shouldBe false
            driver.submitCardSelection(me, emptyList())
        }
        repeat(3) { driver.bothPass() }

        driver.creatureNames(me).contains("Force of Nature") shouldBe false
        driver.state.getZone(ZoneKey(me, Zone.LIBRARY)).first() shouldBe force
    }

    test("declining the reveal does nothing") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val me = driver.getOpponent(caster)
        val warrior = driver.putCardOnTopOfLibrary(me, "Phantom Warrior")
        driver.opponentCastsCourser()

        driver.bothPass()
        driver.submitYesNo(me, false)
        repeat(3) { driver.bothPass() }

        driver.creatureNames(me).contains("Phantom Warrior") shouldBe false
        driver.state.getZone(ZoneKey(me, Zone.LIBRARY)).first() shouldBe warrior
    }

    test("your own spells don't trigger it") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Powerbalance")
        val warrior = driver.putCardOnTopOfLibrary(me, "Phantom Warrior")
        val courser = driver.putCardInHand(me, "Centaur Courser")
        repeat(3) { driver.putLandOnBattlefield(me, "Forest") }
        driver.submit(CastSpell(playerId = me, cardId = courser, paymentStrategy = PaymentStrategy.AutoPay))
        driver.bothPass()

        driver.state.pendingDecision shouldBe null
        driver.state.getZone(ZoneKey(me, Zone.LIBRARY)).first() shouldBe warrior
    }
})
