package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for the colored spell-cost tax used by the Invasion "Leech" creatures
 * ([CostModification.IncreaseColored]).
 *
 * Stand-in card: a {R} creature that taxes the controller's red spells by {R}
 * ("Red spells you cast cost {R} more to cast"), mirroring Ruby Leech. Cost is
 * checked directly through [CostCalculator] to avoid targeting/timing noise.
 */
class LeechColoredTaxTest : FunSpec({

    val RedLeech = CardDefinition.creature(
        name = "Red Leech",
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype("Leech")),
        power = 1,
        toughness = 1,
        oracleText = "Red spells you cast cost {R} more to cast.",
        script = CardScript(
            staticAbilities = listOf(
                ModifySpellCost(
                    target = SpellCostTarget.YouCast(GameObjectFilter.Any.withColor(Color.RED)),
                    modification = CostModification.IncreaseColored("{R}"),
                ),
            ),
        ),
    )

    fun redPips(cost: ManaCost): Int =
        cost.symbols.count { it is ManaSymbol.Colored && it.color == Color.RED }

    data class Fixture(val driver: GameTestDriver, val calculator: CostCalculator, val registry: CardRegistry)

    fun setup(): Fixture {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(RedLeech)
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(RedLeech)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20, "Island" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(driver.activePlayer!!, "Red Leech")
        return Fixture(driver, CostCalculator(registry), registry)
    }

    test("red spell you cast costs an extra {R}") {
        val (driver, calculator, registry) = setup()
        val bolt = registry.requireCard("Lightning Bolt")
        // Lightning Bolt base {R} → taxed to {R}{R}.
        val cost = calculator.calculateEffectiveCost(driver.state, bolt, driver.activePlayer!!)
        cost.cmc shouldBe 2
        redPips(cost) shouldBe 2
        cost.genericAmount shouldBe 0
    }

    test("non-red spell you cast is not taxed") {
        val (driver, calculator, registry) = setup()
        val counter = registry.requireCard("Counterspell")
        // Counterspell {U}{U} stays {U}{U}.
        val cost = calculator.calculateEffectiveCost(driver.state, counter, driver.activePlayer!!)
        cost.cmc shouldBe 2
        redPips(cost) shouldBe 0
    }

    test("red spell cast by the opponent is not taxed") {
        val (driver, calculator, registry) = setup()
        val bolt = registry.requireCard("Lightning Bolt")
        // The tax is YouCast-scoped: the opponent pays the base {R} only.
        val cost = calculator.calculateEffectiveCost(driver.state, bolt, driver.player2)
        cost.cmc shouldBe 1
        redPips(cost) shouldBe 1
    }
})
