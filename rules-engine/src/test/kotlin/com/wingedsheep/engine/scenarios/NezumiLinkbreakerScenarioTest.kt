package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.NezumiLinkbreaker
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Nezumi Linkbreaker — {B} 1/1 Creature — Rat Warlock
 *
 * "When this creature dies, create a 1/1 red Mercenary creature token with
 *  "{T}: Target creature you control gets +1/+0 until end of turn. Activate only as a sorcery.""
 */
class NezumiLinkbreakerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(NezumiLinkbreaker)
        return driver
    }

    test("creates a 1/1 red Mercenary token when it dies") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val nezumi = driver.putCreatureOnBattlefield(player, "Nezumi Linkbreaker")
        driver.findPermanent(player, "Nezumi Linkbreaker") shouldNotBe null

        // Kill it with Doom Blade (black creatures can't be targeted by Doom Blade, so use a
        // non-black destroy). Nezumi is black, so use Lightning Bolt for lethal damage instead.
        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        driver.giveMana(player, Color.RED, 1)
        driver.castSpell(player, bolt, targets = listOf(nezumi))
        driver.bothPass() // resolve Bolt -> Nezumi dies -> dies trigger on stack
        driver.bothPass() // resolve the dies trigger -> Mercenary token created

        driver.findPermanent(player, "Nezumi Linkbreaker") shouldBe null
        driver.getGraveyardCardNames(player).contains("Nezumi Linkbreaker") shouldBe true
        driver.findPermanent(player, "Mercenary Token") shouldNotBe null
    }
})
