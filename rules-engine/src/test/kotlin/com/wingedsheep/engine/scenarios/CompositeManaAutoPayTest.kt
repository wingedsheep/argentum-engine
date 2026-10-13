package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CounterCondition
import com.wingedsheep.sdk.scripting.effects.CounterEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A single mana ability that adds several fixed colors at once ("{T}: Add {W}{U}{B}{R}{G}") must be
 * modelled by auto-pay as every one of those colors, not as its first color plus copies of the
 * second. The solver used to fold every extra leaf into one bonus color, so a lone five-color rock
 * read as {W}{U}{U}{U}{U} and a {W}{U}{B}{R}{G} spell it pays for came up unaffordable.
 */
class CompositeManaAutoPayTest : FunSpec({

    val prismRelic = card("Prism Relic") {
        manaCost = "{5}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddMana(Color.WHITE) then
                Effects.AddMana(Color.BLUE) then
                Effects.AddMana(Color.BLACK) then
                Effects.AddMana(Color.RED) then
                Effects.AddMana(Color.GREEN)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
    }

    fun creature(name: String, cost: String) = CardDefinition.creature(
        name = name, manaCost = ManaCost.parse(cost), subtypes = emptySet(), power = 1, toughness = 1
    )

    val fiveColor = creature("Five Color Probe", "{W}{U}{B}{R}{G}")
    val blackGreen = creature("Black Green Probe", "{B}{G}")
    val taxingCounter = card("Taxing Counter") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            target(TargetFilter.SpellOnStack)
            effect = CounterEffect(condition = CounterCondition.UnlessPaysMana(ManaCost.parse("{3}")))
        }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(prismRelic)
        registerCard(fiveColor)
        registerCard(blackGreen)
        registerCard(taxingCounter)
        initMirrorMatch(deck = Deck.of("Mountain" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.pool(): List<Int> {
        val pool = state.getEntity(player1)!!.get<ManaPoolComponent>()!!
        return listOf(pool.white, pool.blue, pool.black, pool.red, pool.green, pool.colorless)
    }

    test("a lone five-color composite source pays a five-color cost") {
        val d = driver()
        val relic = d.putPermanentOnBattlefield(d.player1, "Prism Relic")
        val spell = d.putCardInHand(d.player1, "Five Color Probe")

        d.submit(CastSpell(d.player1, spell, paymentStrategy = PaymentStrategy.AutoPay))
            .outcome shouldBe Outcome.Done

        d.isTapped(relic) shouldBe true
        d.pool() shouldBe listOf(0, 0, 0, 0, 0, 0)
    }

    test("colors the cost doesn't use float in the pool as the colors the tap produced") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Prism Relic")
        val spell = d.putCardInHand(d.player1, "Black Green Probe")

        d.submit(CastSpell(d.player1, spell, paymentStrategy = PaymentStrategy.AutoPay))
            .outcome shouldBe Outcome.Done

        d.pool() shouldBe listOf(1, 1, 0, 1, 0, 0)
    }

    test("manually selecting the composite source in a mana window credits every color it adds") {
        val d = driver()
        val relic = d.putPermanentOnBattlefield(d.player1, "Prism Relic")
        d.giveMana(d.player1, Color.BLACK, 1)
        d.giveMana(d.player1, Color.GREEN, 1)
        val spell = d.putCardInHand(d.player1, "Black Green Probe")
        d.castSpell(d.player1, spell).outcome shouldBe Outcome.Done

        d.passPriority(d.player1)
        d.giveMana(d.player2, Color.BLUE, 1)
        val counter = d.putCardInHand(d.player2, "Taxing Counter")
        d.castSpellWithTargets(d.player2, counter, listOf(ChosenTarget.Spell(spell))).outcome shouldBe Outcome.Done
        d.bothPass()

        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, true)
        val window = d.pendingDecision
        window.shouldBeInstanceOf<SelectManaSourcesDecision>()
        d.submitDecision(
            d.player1,
            ManaSourcesSelectedResponse(decisionId = window.id, selectedSources = listOf(relic), autoPay = false)
        ).outcome shouldBe Outcome.Done

        // One tap made five mana; {3} of it paid the tax, so the spell survives and two float.
        d.isTapped(relic) shouldBe true
        d.state.stack.contains(spell) shouldBe true
        d.pool().sum() shouldBe 2
    }
})
