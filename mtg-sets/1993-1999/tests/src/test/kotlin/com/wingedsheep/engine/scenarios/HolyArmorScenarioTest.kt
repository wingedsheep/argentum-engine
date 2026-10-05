package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.HolyArmor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class HolyArmorScenarioTest : FunSpec({
    test("the Aura controller pumps an opponent's creature until end of turn") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(HolyArmor)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val owner = driver.activePlayer!!
        val opponent = driver.getOpponent(owner)
        val creature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val aura = driver.putPermanentOnBattlefield(owner, "Holy Armor")
        driver.addComponent(aura, AttachedToComponent(creature))
        driver.addComponent(creature, AttachmentsComponent(listOf(aura)))
        StateProjector().project(driver.state).getPower(creature) shouldBe 2
        StateProjector().project(driver.state).getToughness(creature) shouldBe 4

        driver.giveMana(owner, Color.WHITE, 2)
        repeat(2) {
            driver.submit(ActivateAbility(owner, aura, HolyArmor.activatedAbilities.single().id,
                paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
            driver.bothPass()
        }
        StateProjector().project(driver.state).getPower(creature) shouldBe 2
        StateProjector().project(driver.state).getToughness(creature) shouldBe 6

        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        StateProjector().project(driver.state).getPower(creature) shouldBe 2
        StateProjector().project(driver.state).getToughness(creature) shouldBe 4
    }

    test("the enchanted creature's controller cannot activate the opponent's Aura") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(HolyArmor)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val creatureController = driver.activePlayer!!
        val auraController = driver.getOpponent(creatureController)
        val creature = driver.putCreatureOnBattlefield(creatureController, "Grizzly Bears")
        val aura = driver.putPermanentOnBattlefield(auraController, "Holy Armor")
        driver.addComponent(aura, AttachedToComponent(creature))
        driver.addComponent(creature, AttachmentsComponent(listOf(aura)))
        driver.giveMana(creatureController, Color.WHITE)

        val result = driver.submit(ActivateAbility(creatureController, aura, HolyArmor.activatedAbilities.single().id,
            paymentStrategy = PaymentStrategy.FromPool))
        (result.error != null) shouldBe true
    }
})
