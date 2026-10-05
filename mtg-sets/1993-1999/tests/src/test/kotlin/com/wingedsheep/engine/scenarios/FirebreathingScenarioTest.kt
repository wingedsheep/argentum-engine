package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Firebreathing
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class FirebreathingScenarioTest : FunSpec({
    test("the Aura controller pumps an opponent's creature until end of turn") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Firebreathing)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val owner = driver.activePlayer!!
        val opponent = driver.getOpponent(owner)
        val creature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val aura = driver.putPermanentOnBattlefield(owner, "Firebreathing")
        driver.addComponent(aura, AttachedToComponent(creature))
        driver.addComponent(creature, AttachmentsComponent(listOf(aura)))
        StateProjector().project(driver.state).getPower(creature) shouldBe 2
        StateProjector().project(driver.state).getToughness(creature) shouldBe 2

        driver.giveMana(owner, Color.RED, 2)
        repeat(2) {
            driver.submit(ActivateAbility(owner, aura, Firebreathing.activatedAbilities.single().id,
                paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
            driver.bothPass()
        }
        StateProjector().project(driver.state).getPower(creature) shouldBe 4
        StateProjector().project(driver.state).getToughness(creature) shouldBe 2

        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        StateProjector().project(driver.state).getPower(creature) shouldBe 2
        StateProjector().project(driver.state).getToughness(creature) shouldBe 2
    }

    test("activation follows the Aura at resolution and its bonus survives the Aura leaving") {
        val moveAura = card("Firebreathing Test Move Aura") {
            manaCost = "{0}"
            typeLine = "Instant"
            spell {
                effect = Effects.AttachToChosenHost(target(TargetFilter.Enchantment))
            }
        }
        val destroyAura = card("Firebreathing Test Destroy Aura") {
            manaCost = "{0}"
            typeLine = "Instant"
            spell {
                effect = Effects.Destroy(target(TargetFilter.Enchantment))
            }
        }
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Firebreathing)
        driver.registerCard(moveAura)
        driver.registerCard(destroyAura)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val owner = driver.activePlayer!!
        val originalHost = driver.putCreatureOnBattlefield(owner, "Grizzly Bears")
        val newHost = driver.putCreatureOnBattlefield(driver.getOpponent(owner), "Grizzly Bears")
        val aura = driver.putPermanentOnBattlefield(owner, "Firebreathing")
        driver.addComponent(aura, AttachedToComponent(originalHost))
        driver.addComponent(originalHost, AttachmentsComponent(listOf(aura)))

        driver.giveMana(owner, Color.RED)
        driver.submit(ActivateAbility(owner, aura, Firebreathing.activatedAbilities.single().id,
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        val move = driver.putCardInHand(owner, moveAura.name)
        driver.castSpell(owner, move, listOf(aura)).error shouldBe null
        driver.bothPass()
        driver.submitTargetSelection(owner, listOf(newHost)).error shouldBe null
        driver.state.getEntity(aura)!!.get<AttachedToComponent>()!!.targetId shouldBe newHost
        driver.state.stack.size shouldBe 1
        StateProjector().project(driver.state).getPower(newHost) shouldBe 2

        driver.bothPass()
        StateProjector().project(driver.state).getPower(originalHost) shouldBe 2
        StateProjector().project(driver.state).getPower(newHost) shouldBe 3

        val removal = driver.putCardInHand(owner, destroyAura.name)
        driver.castSpell(owner, removal, listOf(aura)).error shouldBe null
        driver.bothPass()
        (aura in driver.getGraveyard(owner)) shouldBe true
        StateProjector().project(driver.state).getPower(originalHost) shouldBe 2
        StateProjector().project(driver.state).getPower(newHost) shouldBe 3

        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        StateProjector().project(driver.state).getPower(newHost) shouldBe 2
    }

    test("the enchanted creature's controller cannot activate the opponent's Aura") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Firebreathing)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val creatureController = driver.activePlayer!!
        val auraController = driver.getOpponent(creatureController)
        val creature = driver.putCreatureOnBattlefield(creatureController, "Grizzly Bears")
        val aura = driver.putPermanentOnBattlefield(auraController, "Firebreathing")
        driver.addComponent(aura, AttachedToComponent(creature))
        driver.addComponent(creature, AttachmentsComponent(listOf(aura)))
        driver.giveMana(creatureController, Color.RED)

        val result = driver.submit(ActivateAbility(creatureController, aura, Firebreathing.activatedAbilities.single().id,
            paymentStrategy = PaymentStrategy.FromPool))
        (result.error != null) shouldBe true
    }
})
