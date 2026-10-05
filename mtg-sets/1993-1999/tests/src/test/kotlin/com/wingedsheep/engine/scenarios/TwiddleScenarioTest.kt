package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Twiddle
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TwiddleScenarioTest : FunSpec({
    listOf(0, 1).forEach { mode ->
        test("tap or untap choice $mode occurs after the opponent taps the target in response") {
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all)
            driver.registerCard(Twiddle)
            driver.initMirrorMatch(deck = Deck.of("Island" to 40))
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val caster = driver.activePlayer!!
            val opponent = driver.getOpponent(caster)
            val land = driver.putLandOnBattlefield(opponent, "Forest")
            val spell = driver.putCardInHand(caster, "Twiddle")
            driver.giveMana(caster, Color.BLUE)
            driver.castSpell(caster, spell, listOf(land)).error shouldBe null
            driver.pendingDecision shouldBe null
            driver.isTapped(land) shouldBe false

            driver.passPriority(caster)
            val manaAbility = driver.cardRegistry.requireCard("Forest").activatedAbilities.single()
            driver.submit(ActivateAbility(opponent, land, manaAbility.id)).error shouldBe null
            driver.isTapped(land) shouldBe true
            driver.bothPass()
            (driver.pendingDecision is YesNoDecision) shouldBe true
            driver.submitYesNo(caster, true).error shouldBe null
            val choice = driver.pendingDecision as ChooseOptionDecision
            driver.submitDecision(caster, OptionChosenResponse(choice.id, mode)).error shouldBe null
            driver.isTapped(land) shouldBe (mode == 0)
        }
    }

    test("declining leaves an untapped creature untouched") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Twiddle)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val caster = driver.activePlayer!!
        val creature = driver.putCreatureOnBattlefield(driver.getOpponent(caster), "Grizzly Bears")
        val spell = driver.putCardInHand(caster, "Twiddle")
        driver.giveMana(caster, Color.BLUE)
        driver.castSpell(caster, spell, listOf(creature)).error shouldBe null
        driver.pendingDecision shouldBe null
        driver.bothPass()
        (driver.pendingDecision is YesNoDecision) shouldBe true
        driver.submitYesNo(caster, false).error shouldBe null
        driver.isTapped(creature) shouldBe false
        driver.pendingDecision shouldBe null
    }

    test("a plain enchantment is not a legal target") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Twiddle)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val caster = driver.activePlayer!!
        val enchantment = driver.putPermanentOnBattlefield(caster, "Crusade")
        val spell = driver.putCardInHand(caster, "Twiddle")
        driver.giveMana(caster, Color.BLUE)
        (driver.castSpell(caster, spell, listOf(enchantment)).error != null) shouldBe true
    }
})
