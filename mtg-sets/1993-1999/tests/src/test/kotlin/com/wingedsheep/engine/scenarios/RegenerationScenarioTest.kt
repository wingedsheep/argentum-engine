package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Regeneration
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RegenerationScenarioTest : FunSpec({
    test("the Aura controller creates a shield for an opponent's creature, consumed by lethal damage") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Regeneration)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val owner = driver.activePlayer!!
        val opponent = driver.getOpponent(owner)
        val creature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val aura = driver.putPermanentOnBattlefield(owner, "Regeneration")
        driver.addComponent(aura, AttachedToComponent(creature))
        driver.addComponent(creature, AttachmentsComponent(listOf(aura)))
        driver.giveMana(owner, Color.GREEN)
        driver.submit(ActivateAbility(owner, aura, Regeneration.activatedAbilities.single().id,
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        driver.bothPass()
        driver.isTapped(creature) shouldBe false

        repeat(2) { damageIndex ->
            val bolt = driver.putCardInHand(owner, "Lightning Bolt")
            driver.giveMana(owner, Color.RED)
            driver.castSpell(owner, bolt, listOf(creature)).error shouldBe null
            driver.bothPass()
            (creature in driver.getGraveyard(opponent)) shouldBe (damageIndex == 1)
            if (damageIndex == 0) driver.isTapped(creature) shouldBe true
        }
    }
})
