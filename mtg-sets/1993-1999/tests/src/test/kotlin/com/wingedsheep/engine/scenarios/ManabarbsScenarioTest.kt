package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.AlphaForest294
import com.wingedsheep.mtg.sets.definitions.lea.cards.AlphaMountain292
import com.wingedsheep.mtg.sets.definitions.lea.cards.Manabarbs
import com.wingedsheep.mtg.sets.definitions.lea.cards.MoxRuby
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Manabarbs (LEA) — {3}{R} Enchantment.
 *
 * "Whenever a player taps a land for mana, this enchantment deals 1 damage to that player."
 *
 * First card on the any-player form of `tapsLandForMana`: pins that the damage goes to whoever tapped
 * (not Manabarbs's controller), that it uses the stack, that each land tapped while paying for a spell
 * triggers separately, and that a non-land mana source doesn't trigger it.
 */
class ManabarbsScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all.filter { it.name != "Mountain" && it.name != "Forest" })
        driver.registerCard(AlphaMountain292)
        driver.registerCard(AlphaForest294)
        driver.registerCard(Manabarbs)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    val mountainAbility = AlphaMountain292.activatedAbilities.single().id

    test("tapping your own land for mana damages you, via the stack") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putPermanentOnBattlefield(you, "Manabarbs")
        val mountain = driver.putPermanentOnBattlefield(you, "Mountain")

        driver.submit(ActivateAbility(you, mountain, mountainAbility)).error shouldBe null

        driver.stackSize shouldBe 1
        driver.getLifeTotal(you) shouldBe 20
        driver.bothPass()
        driver.getLifeTotal(you) shouldBe 19
    }

    test("an opponent tapping their land is damaged, not Manabarbs's controller") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.putPermanentOnBattlefield(you, "Manabarbs")
        val theirMountain = driver.putPermanentOnBattlefield(opponent, "Mountain")

        driver.passPriority(you)
        driver.submit(ActivateAbility(opponent, theirMountain, mountainAbility)).error shouldBe null

        driver.stackSize shouldBe 1
        driver.bothPass()
        driver.getLifeTotal(opponent) shouldBe 19
        driver.getLifeTotal(you) shouldBe 20
    }

    test("each land tapped to pay for a spell triggers separately and resolves before the spell") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putPermanentOnBattlefield(you, "Manabarbs")
        driver.putPermanentOnBattlefield(you, "Forest")
        driver.putPermanentOnBattlefield(you, "Forest")

        val bears = driver.putCardInHand(you, "Grizzly Bears")
        driver.submit(CastSpell(you, bears, paymentStrategy = PaymentStrategy.AutoPay)).error shouldBe null

        // Grizzly Bears plus two Manabarbs triggers on top of it.
        driver.stackSize shouldBe 3
        driver.bothPass()
        driver.bothPass()
        driver.getLifeTotal(you) shouldBe 18
        driver.stackSize shouldBe 1
    }

    test("a non-land mana source doesn't trigger it") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putPermanentOnBattlefield(you, "Manabarbs")
        val mox = driver.putPermanentOnBattlefield(you, "Mox Ruby")

        driver.submit(ActivateAbility(you, mox, MoxRuby.activatedAbilities.single().id)).error shouldBe null

        driver.stackSize shouldBe 0
        driver.getLifeTotal(you) shouldBe 20
    }
})
