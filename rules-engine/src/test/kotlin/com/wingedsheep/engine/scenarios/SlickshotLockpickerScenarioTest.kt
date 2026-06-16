package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.SlickshotLockpicker
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Slickshot Lockpicker (OTJ #67) — {2}{U} 2/3 Creature — Human Rogue.
 *
 * "When this creature enters, target instant or sorcery card in your graveyard gains flashback
 *  until end of turn. The flashback cost is equal to its mana cost.
 *  Plot {2}{U}"
 */
class SlickshotLockpickerScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(SlickshotLockpicker)
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("ETB grants flashback equal to the card's mana cost, then exiles it on cast") {
        val driver = newDriver()
        val player = driver.player1
        val opponent = driver.player2

        val bolt = driver.putCardInGraveyard(player, "Lightning Bolt") // {R}

        // Cast Slickshot Lockpicker; its ETB trigger targets the graveyard instant/sorcery.
        val pick = driver.putCardInHand(player, "Slickshot Lockpicker")
        driver.giveMana(player, Color.BLUE, 1)
        driver.giveColorlessMana(player, 2)
        driver.submitSuccess(CastSpell(player, pick, paymentStrategy = PaymentStrategy.FromPool))
        driver.bothPass() // resolve the creature -> ETB trigger goes on the stack
        driver.submitTargetSelection(player, listOf(bolt))
        driver.bothPass() // resolve the flashback grant

        driver.state.grantedKeywordAbilities.any { it.entityId == bolt } shouldBe true

        // Flashback cost equals the card's mana cost: {R}.
        driver.giveMana(player, Color.RED, 1)
        driver.submit(
            CastSpell(
                player, bolt,
                targets = listOf(ChosenTarget.Player(opponent)),
                useAlternativeCost = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // The spell was exiled after the flashback cast.
        driver.state.getZone(ZoneKey(player, Zone.EXILE)).contains(bolt) shouldBe true
        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)).contains(bolt) shouldBe false
    }
})
