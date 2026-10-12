package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bro.cards.TawnosTheToymaker
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tawnos, the Toymaker — "Whenever you cast a Beast or Bird creature spell, you may copy it, except
 * the copy is an artifact in addition to its other types."
 *
 * The artifact type rides on the spell copy as a copy exception, so it has to survive the copy
 * resolving into a token (CR 707.10f).
 */
class TawnosTheToymakerScenarioTest : FunSpec({

    val beast = card("Tawnos Test Beast") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Beast"
        power = 2
        toughness = 2
    }

    val bear = card("Tawnos Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TawnosTheToymaker))
        driver.registerCard(beast)
        driver.registerCard(bear)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        return driver
    }

    fun named(driver: GameTestDriver, player: EntityId, name: String) =
        driver.getPermanents(player).filter {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == name
        }

    fun castAndResolve(d: GameTestDriver, me: EntityId, cardName: String, accept: Boolean): Boolean {
        val spell = d.putCardInHand(me, cardName)
        d.submit(CastSpell(playerId = me, cardId = spell, paymentStrategy = PaymentStrategy.AutoPay)).error shouldBe null
        var asked = false
        var guard = 0
        while (d.state.stack.isNotEmpty() && guard++ < 40) {
            if (d.pendingDecision is YesNoDecision) {
                d.submitYesNo(me, accept)
                asked = true
            } else {
                d.bothPass()
            }
        }
        return asked
    }

    test("copying a Beast spell makes an artifact token copy") {
        val d = createDriver()
        val me = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(me, "Tawnos, the Toymaker")
        repeat(2) { d.putLandOnBattlefield(me, "Forest") }

        castAndResolve(d, me, "Tawnos Test Beast", accept = true) shouldBe true

        val beasts = named(d, me, "Tawnos Test Beast")
        beasts.size shouldBe 2
        val token = beasts.single { d.state.getEntity(it)?.has<TokenComponent>() == true }
        val original = beasts.single { it != token }
        d.state.projectedState.hasType(token, "ARTIFACT") shouldBe true
        d.state.projectedState.isCreature(token) shouldBe true
        d.state.projectedState.hasType(original, "ARTIFACT") shouldBe false
    }

    test("declining the copy leaves only the original") {
        val d = createDriver()
        val me = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(me, "Tawnos, the Toymaker")
        repeat(2) { d.putLandOnBattlefield(me, "Forest") }

        castAndResolve(d, me, "Tawnos Test Beast", accept = false) shouldBe true
        named(d, me, "Tawnos Test Beast").size shouldBe 1
    }

    test("a creature spell that is neither Beast nor Bird does not trigger") {
        val d = createDriver()
        val me = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(me, "Tawnos, the Toymaker")
        repeat(2) { d.putLandOnBattlefield(me, "Forest") }

        castAndResolve(d, me, "Tawnos Test Bear", accept = true) shouldBe false
        named(d, me, "Tawnos Test Bear").size shouldBe 1
    }
})
