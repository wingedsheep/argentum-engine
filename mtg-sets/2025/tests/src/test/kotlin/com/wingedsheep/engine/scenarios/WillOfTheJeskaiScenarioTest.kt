package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tdc.cards.WillOfTheJeskai
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Will of the Jeskai (TDC #40) — the flashback mode: "Each instant and sorcery card in your
 * graveyard gains flashback until end of turn. The flashback cost is equal to its mana cost."
 *
 * Proves the per-card grant composition (gather the graveyard's instants and sorceries, then
 * `GrantFlashback` each one): every instant and sorcery in *your* graveyard gains flashback for its
 * own mana cost, nothing else does, and a card cast that way is exiled.
 */
class WillOfTheJeskaiScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(WillOfTheJeskai)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("flashback mode grants flashback to each instant and sorcery in your graveyard only") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2

        val bolt = driver.putCardInGraveyard(me, "Lightning Bolt")
        val study = driver.putCardInGraveyard(me, "Careful Study")
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")
        val oppBolt = driver.putCardInGraveyard(opp, "Lightning Bolt")

        driver.giveMana(me, Color.RED, 4)
        val will = driver.putCardInHand(me, "Will of the Jeskai")
        driver.submit(
            CastSpell(me, will, chosenModes = listOf(1), paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        driver.bothPass()

        val granted = driver.state.grantedKeywordAbilities.map { it.entityId }.toSet()
        (bolt in granted) shouldBe true
        (study in granted) shouldBe true
        (bears in granted) shouldBe false
        (oppBolt in granted) shouldBe false

        // Flashback cost equals Lightning Bolt's mana cost, {R}; it is exiled as it resolves.
        driver.giveMana(me, Color.RED, 1)
        driver.submit(
            CastSpell(
                me, bolt,
                targets = listOf(ChosenTarget.Player(opp)),
                useAlternativeCost = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.getLifeTotal(opp) shouldBe 17
        driver.state.getZone(ZoneKey(me, Zone.EXILE)).contains(bolt) shouldBe true
    }

    test("the grant ends at end of turn") {
        val driver = newDriver()
        val me = driver.player1

        val bolt = driver.putCardInGraveyard(me, "Lightning Bolt")

        driver.giveMana(me, Color.RED, 4)
        val will = driver.putCardInHand(me, "Will of the Jeskai")
        driver.submit(
            CastSpell(me, will, chosenModes = listOf(1), paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        driver.bothPass()

        driver.state.grantedKeywordAbilities.any { it.entityId == bolt } shouldBe true
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 300)
        driver.state.grantedKeywordAbilities.any { it.entityId == bolt } shouldBe false
    }
})
