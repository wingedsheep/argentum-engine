package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.BruseTarlRovingRancher
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Bruse Tarl, Roving Rancher (OTJ) — {2}{R}{W} Legendary Creature — Human Warrior 4/3
 *
 * "Oxen you control have double strike.
 *  Whenever Bruse Tarl enters or attacks, exile the top card of your library. If it's a
 *  land card, create a 2/2 white Ox creature token. Otherwise, you may cast it until the
 *  end of your next turn."
 *
 * Composes existing primitives (no new SDK surface): a [GrantKeyword] lord scoped to Oxen,
 * two triggered abilities (enters / attacks) sharing a gather → exile → split body, a
 * conditional Ox token on the land leg, and a may-play window on the non-land leg.
 */
class BruseTarlRovingRancherScenarioTest : FunSpec({

    val projector = StateProjector()

    // A vanilla Ox to check the double-strike lord.
    val testOx = card("Test Ox") {
        manaCost = "{2}"
        typeLine = "Creature — Ox"
        power = 2
        toughness = 2
        oracleText = ""
    }

    fun newDriver(deck: Deck): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(BruseTarlRovingRancher)
        driver.registerCard(testOx)
        driver.initMirrorMatch(deck, skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("Oxen you control have double strike") {
        val driver = newDriver(Deck.of("Mountain" to 40))
        val me = driver.player1

        val ox = driver.putCreatureOnBattlefield(me, "Test Ox")
        // No double strike before Bruse Tarl is in play.
        projector.project(driver.state).hasKeyword(ox, Keyword.DOUBLE_STRIKE) shouldBe false

        driver.putCreatureOnBattlefield(me, "Bruse Tarl, Roving Rancher")
        projector.project(driver.state).hasKeyword(ox, Keyword.DOUBLE_STRIKE) shouldBe true
    }

    test("enters: land on top creates a 2/2 white Ox token") {
        val driver = newDriver(Deck.of("Mountain" to 40))
        val me = driver.player1

        driver.putCardOnTopOfLibrary(me, "Mountain")
        val oxBefore = driver.getPermanents(me).count { driver.getCardName(it) == "Ox Token" }
        val exileBefore = driver.getExile(me).size

        // Cast Bruse Tarl so the enters trigger fires.
        val bruse = driver.putCardInHand(me, "Bruse Tarl, Roving Rancher")
        driver.giveMana(me, Color.RED, 1)
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveColorlessMana(me, 2)
        driver.castSpell(me, bruse)
        driver.bothPass() // resolve Bruse -> enters trigger on stack
        driver.bothPass() // resolve enters trigger

        // The land was exiled (and stays there) and a 2/2 white Ox token was created.
        driver.getExile(me).size shouldBe exileBefore + 1
        val oxen = driver.getPermanents(me).filter { driver.getCardName(it) == "Ox Token" }
        oxen.size shouldBe oxBefore + 1
        driver.state.projectedState.getPower(oxen.single()) shouldBe 2
        driver.state.projectedState.getToughness(oxen.single()) shouldBe 2
    }

    test("attacks: non-land on top is exiled and may be cast") {
        val driver = newDriver(Deck.of("Centaur Courser" to 40))
        val me = driver.player1

        val bruse = driver.putCreatureOnBattlefield(me, "Bruse Tarl, Roving Rancher")
        driver.removeSummoningSickness(bruse)

        driver.putCardOnTopOfLibrary(me, "Centaur Courser")
        val exileBefore = driver.getExile(me).size

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(bruse), driver.player2)
        driver.bothPass() // resolve attack trigger

        driver.getExile(me).size shouldBe exileBefore + 1
        val exiled = driver.getExile(me).last()
        driver.state.mayPlayPermissions.any { exiled in it.cardIds } shouldBe true
    }
})
