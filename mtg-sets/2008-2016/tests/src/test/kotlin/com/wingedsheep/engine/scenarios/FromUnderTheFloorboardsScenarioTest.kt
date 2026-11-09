package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * From Under the Floorboards (SOI #111) — {3}{B}{B} Sorcery. "Create three tapped 2/2 black
 * Zombie creature tokens and you gain 3 life. If this spell's madness cost was paid, instead
 * create X of those tokens and you gain X life." Madness {X}{B}{B}.
 */
class FromUnderTheFloorboardsScenarioTest : FunSpec({
    val selfDiscard = card("Self Discard") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell { effect = Effects.Discard(1) }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(selfDiscard))
        initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.zombies(player: EntityId) =
        getCreatures(player).filter { state.projectedState.hasSubtype(it, "Zombie") }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && state.pendingDecision == null && guard++ < 20) bothPass()
    }

    test("cast from hand, it makes three tapped Zombies and gains 3 life") {
        val d = driver()
        val p = d.activePlayer!!
        val spell = d.putCardInHand(p, "From Under the Floorboards")
        val life = d.getLifeTotal(p)
        d.giveMana(p, Color.BLACK, 5)
        d.castSpell(p, spell).error shouldBe null
        d.resolveStack()

        val zombies = d.zombies(p)
        zombies.size shouldBe 3
        zombies.all { d.isTapped(it) } shouldBe true
        d.getLifeTotal(p) shouldBe life + 3
    }

    test("cast for its madness cost with X = 4, it makes four tapped Zombies and gains 4 life") {
        val d = driver()
        val p = d.activePlayer!!
        val floorboards = d.putCardInHand(p, "From Under the Floorboards")
        val life = d.getLifeTotal(p)

        val discard = d.putCardInHand(p, "Self Discard")
        d.giveColorlessMana(p, 1)
        d.submitSuccess(CastSpell(p, discard, paymentStrategy = PaymentStrategy.FromPool))
        d.bothPass()
        d.submitCardSelection(p, listOf(floorboards))
        d.resolveStack()

        d.giveMana(p, Color.BLACK, 6) // {X}{B}{B} with X = 4
        d.submitYesNo(p, true)
        val ask = d.pendingDecision.shouldBeInstanceOf<ChooseNumberDecision>()
        d.submitDecision(p, NumberChosenResponse(ask.id, 4)).error shouldBe null
        d.resolveStack()

        val zombies = d.zombies(p)
        zombies.size shouldBe 4
        zombies.all { d.isTapped(it) } shouldBe true
        d.getLifeTotal(p) shouldBe life + 4
    }
})
