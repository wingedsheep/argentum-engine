package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceSpellCostByFilter
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Mistform Warchief.
 *
 * Mistform Warchief: {2}{U}
 * Creature — Illusion
 * 1/3
 * Creature spells you cast that share a creature type with this creature cost {1} less to cast.
 * {T}: This creature becomes the creature type of your choice until end of turn.
 */
class MistformWarchiefTest : FunSpec({

    val MistformWarchief = card("Mistform Warchief") {
        manaCost = "{2}{U}"
        typeLine = "Creature — Illusion"
        power = 1
        toughness = 3

        staticAbility {
            ability = ReduceSpellCostByFilter(
                filter = GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.IsCreature,
                        CardPredicate.SharesCreatureTypeWithSource
                    )
                ),
                amount = 1
            )
        }

        activatedAbility {
            cost = Costs.Tap
            effect = BecomeCreatureTypeEffect(
                target = EffectTarget.Self
            )
        }
    }

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(MistformWarchief)
        return driver
    }

    test("Illusion creature spells cost 1 less with Mistform Warchief") {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(MistformWarchief)

        val calculator = CostCalculator(registry, projector)

        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Mistform Warchief")

        // Phantom Warrior is Illusion Warrior, costs {1}{U}{U} = 1 generic
        // With Mistform Warchief (Illusion), should reduce to {U}{U}
        val phantomWarrior = registry.requireCard("Phantom Warrior")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, phantomWarrior, activePlayer)
        effectiveCost.genericAmount shouldBe 0
        effectiveCost.cmc shouldBe 2 // Just {U}{U}
    }

    test("non-sharing creature spells are not reduced") {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(MistformWarchief)

        val calculator = CostCalculator(registry, projector)

        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 10, "Forest" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Mistform Warchief")

        // Grizzly Bears is Bear, doesn't share type with Illusion
        val bears = registry.requireCard("Grizzly Bears")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, bears, activePlayer)
        effectiveCost.genericAmount shouldBe 1
        effectiveCost.cmc shouldBe 2 // Still {1}{G}
    }

    test("non-creature spells are not reduced") {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(MistformWarchief)

        val calculator = CostCalculator(registry, projector)

        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 10, "Mountain" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Mistform Warchief")

        // Lightning Bolt is an instant, not a creature — should not be reduced
        val bolt = registry.requireCard("Lightning Bolt")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, bolt, activePlayer)
        effectiveCost.cmc shouldBe 1 // Still {R}
    }
})
