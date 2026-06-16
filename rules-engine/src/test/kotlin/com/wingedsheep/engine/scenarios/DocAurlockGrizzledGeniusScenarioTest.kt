package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.PlotCostReducer
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.DocAurlockGrizzledGenius
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Doc Aurlock, Grizzled Genius — {G}{U} Legendary Creature — Bear Druid 2/3.
 *
 * - "Spells you cast from your graveyard or from exile cost {2} less to cast." →
 *   [com.wingedsheep.sdk.scripting.SpellCostTarget.YouCastFromZones] checked by [CostCalculator].
 * - "Plotting cards from your hand costs {2} less." → [com.wingedsheep.sdk.scripting.ModifyPlotCost]
 *   checked by [PlotCostReducer].
 */
class DocAurlockGrizzledGeniusScenarioTest : FunSpec({

    // A {4}{R}{R} sorcery used to probe the spell-cast half.
    val BigSpell = card("Doc Aurlock Test Spell") {
        manaCost = "{4}{R}{R}"
        typeLine = "Sorcery"
    }

    // A {3}{G} creature with plot {5}{G} used to probe the plot half.
    val PlottableCreature = card("Doc Aurlock Plot Creature") {
        manaCost = "{3}{G}"
        typeLine = "Creature — Bear"
        power = 3
        toughness = 3
        keywordAbility(com.wingedsheep.sdk.scripting.KeywordAbility.plot("{5}{G}"))
    }

    fun registry(): CardRegistry {
        val r = CardRegistry()
        r.register(TestCards.all)
        r.register(DocAurlockGrizzledGenius)
        r.register(BigSpell)
        r.register(PlottableCreature)
        return r
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(DocAurlockGrizzledGenius)
        d.registerCard(BigSpell)
        d.registerCard(PlottableCreature)
        return d
    }

    fun setup(): Pair<GameTestDriver, com.wingedsheep.sdk.model.EntityId> {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 20), startingLife = 20)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(active, "Doc Aurlock, Grizzled Genius")
        return d to active
    }

    test("spell cast from graveyard costs {2} less") {
        val reg = registry()
        val calc = CostCalculator(reg)
        val (d, active) = setup()
        val spell = reg.requireCard("Doc Aurlock Test Spell")
        // {4}{R}{R} → −{2} generic → {2}{R}{R}
        val cost = calc.calculateEffectiveCost(d.state, spell, active, fromZone = Zone.GRAVEYARD)
        cost.genericAmount shouldBe 2
        cost.cmc shouldBe 4
    }

    test("spell cast from exile costs {2} less") {
        val reg = registry()
        val calc = CostCalculator(reg)
        val (d, active) = setup()
        val spell = reg.requireCard("Doc Aurlock Test Spell")
        val cost = calc.calculateEffectiveCost(d.state, spell, active, fromZone = Zone.EXILE)
        cost.genericAmount shouldBe 2
    }

    test("spell cast from hand is NOT reduced") {
        val reg = registry()
        val calc = CostCalculator(reg)
        val (d, active) = setup()
        val spell = reg.requireCard("Doc Aurlock Test Spell")
        val cost = calc.calculateEffectiveCost(d.state, spell, active, fromZone = Zone.HAND)
        cost.genericAmount shouldBe 4
    }

    test("spell cast with no fromZone is NOT reduced") {
        val reg = registry()
        val calc = CostCalculator(reg)
        val (d, active) = setup()
        val spell = reg.requireCard("Doc Aurlock Test Spell")
        val cost = calc.calculateEffectiveCost(d.state, spell, active)
        cost.genericAmount shouldBe 4
    }

    test("opponent's graveyard cast is NOT reduced by my Doc Aurlock") {
        val reg = registry()
        val calc = CostCalculator(reg)
        val (d, active) = setup()
        val opponent = d.state.turnOrder.first { it != active }
        val spell = reg.requireCard("Doc Aurlock Test Spell")
        val cost = calc.calculateEffectiveCost(d.state, spell, opponent, fromZone = Zone.GRAVEYARD)
        cost.genericAmount shouldBe 4
    }

    test("plotting a card from hand costs {2} less") {
        val reg = registry()
        val reducer = PlotCostReducer(reg)
        val (d, active) = setup()
        // base plot cost {5}{G} → −{2} generic → {3}{G}
        val effective = reducer.effectivePlotCostFromHand(d.state, active, ManaCost.parse("{5}{G}"))
        effective.genericAmount shouldBe 3
        effective.cmc shouldBe 4
    }

    test("plot reduction floors generic at 0 and never reduces colored pips") {
        val reg = registry()
        val reducer = PlotCostReducer(reg)
        val (d, active) = setup()
        // {1}{G} → −{2} generic → {G} (generic floored at 0, colored kept)
        val effective = reducer.effectivePlotCostFromHand(d.state, active, ManaCost.parse("{1}{G}"))
        effective.genericAmount shouldBe 0
        effective.cmc shouldBe 1
    }

    test("plot reduction does not apply to an opponent without Doc Aurlock") {
        val reg = registry()
        val reducer = PlotCostReducer(reg)
        val (d, active) = setup()
        val opponent = d.state.turnOrder.first { it != active }
        val effective = reducer.effectivePlotCostFromHand(d.state, opponent, ManaCost.parse("{5}{G}"))
        effective.genericAmount shouldBe 5
    }
})
