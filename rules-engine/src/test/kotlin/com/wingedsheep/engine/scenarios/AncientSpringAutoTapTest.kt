package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.inv.cards.AncientSpring
import com.wingedsheep.mtg.sets.definitions.inv.cards.MetathranZombie
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ancient Spring has a sacrifice-free mana ability *and* a sacrifice one, and the sacrifice
 * ability adds two mana on a single tap:
 *   {T}: Add {U}
 *   {T}, Sacrifice this land: Add {W}{B}
 *
 * Both abilities cost {T}, so the land can only ever be used once: it makes either one {U}, or
 * {W}{B} while dying. Two things went wrong:
 *
 *  1. The extra `{B}` leaf of the sacrifice ability was folded into the source's generic
 *     "bonus mana per tap" channel (the one auras like Fertile Ground use), which carries no
 *     sacrifice provenance. Auto-pay therefore saw "tap for {U}, plus a free floating {B}" and
 *     happily cast Metathran Zombie ({1}{U}) off Ancient Spring alone, without ever sacrificing.
 *  2. Affordability counted the land twice — once as a regular {U} source and once via the
 *     sacrifice-ability bonus helper — reporting three available mana from a land that can
 *     produce at most two.
 */
class AncientSpringAutoTapTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AncientSpring, MetathranZombie))
        return driver
    }

    test("Ancient Spring alone can't auto-pay Metathran Zombie ({1}{U})") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val spring = driver.putPermanentOnBattlefield(activePlayer, "Ancient Spring")
        val zombie = driver.putCardInHand(activePlayer, "Metathran Zombie")

        // Both of Ancient Spring's abilities tap it, so it makes at most {U} *or* {W}{B} — never
        // {U} plus something else. {1}{U} is unpayable off this land alone.
        val result = driver.castSpell(activePlayer, zombie)

        result.isSuccess shouldBe false
        driver.findPermanent(activePlayer, "Ancient Spring") shouldBe spring
        driver.isTapped(spring) shouldBe false
        driver.getGraveyardCardNames(activePlayer).contains("Ancient Spring") shouldBe false
    }

    test("canPay reports Metathran Zombie ({1}{U}) unaffordable off Ancient Spring alone") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Ancient Spring")

        val solver = ManaSolver(driver.cardRegistry)
        // {W}{B} is payable — that's exactly what the sacrifice ability makes.
        solver.canPay(driver.state, activePlayer, ManaCost.parse("{W}{B}")) shouldBe true
        // A lone {U} is payable via the sacrifice-free ability.
        solver.canPay(driver.state, activePlayer, ManaCost.parse("{U}")) shouldBe true
        // But {1}{U} needs the land to be tapped twice.
        solver.canPay(driver.state, activePlayer, ManaCost.parse("{1}{U}")) shouldBe false
    }

    test("Ancient Spring counts as two available mana, not three") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Ancient Spring")

        val solver = ManaSolver(driver.cardRegistry)
        // Best case is sacrificing for {W}{B}; the {U} ability shares the same {T} cost.
        solver.getAvailableManaCount(driver.state, activePlayer) shouldBe 2
    }

    test("Ancient Spring's sacrifice-free {U} ability still auto-taps alongside another land") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val spring = driver.putPermanentOnBattlefield(activePlayer, "Ancient Spring")
        val mountain = driver.putPermanentOnBattlefield(activePlayer, "Mountain")
        val zombie = driver.putCardInHand(activePlayer, "Metathran Zombie")

        // Mountain pays the generic {1}, Ancient Spring taps for {U} without being sacrificed.
        val result = driver.castSpell(activePlayer, zombie)

        result.isSuccess shouldBe true
        driver.isTapped(spring) shouldBe true
        driver.isTapped(mountain) shouldBe true
        driver.findPermanent(activePlayer, "Ancient Spring") shouldBe spring
        driver.getGraveyardCardNames(activePlayer).contains("Ancient Spring") shouldBe false
    }
})
