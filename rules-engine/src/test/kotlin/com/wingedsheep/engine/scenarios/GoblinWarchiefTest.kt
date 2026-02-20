package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.ReduceSpellCostBySubtype
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Goblin Warchief.
 *
 * Goblin Warchief: {1}{R}{R}
 * Creature — Goblin Warrior
 * 2/2
 * Goblin spells you cast cost {1} less to cast.
 * Goblins you control have haste.
 */
class GoblinWarchiefTest : FunSpec({

    val GoblinWarchief = card("Goblin Warchief") {
        manaCost = "{1}{R}{R}"
        typeLine = "Creature — Goblin Warrior"
        power = 2
        toughness = 2

        staticAbility {
            ability = ReduceSpellCostBySubtype(subtype = "Goblin", amount = 1)
        }

        staticAbility {
            ability = GrantKeywordToCreatureGroup(
                keyword = Keyword.HASTE,
                filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Goblin"))
            )
        }
    }

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(GoblinWarchief)
        return driver
    }

    test("Goblins you control have haste") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Goblin Warchief")
        val goblin = driver.putCreatureOnBattlefield(activePlayer, "Goblin Guide")

        val projected = projector.project(driver.state)
        projected.hasKeyword(goblin, Keyword.HASTE) shouldBe true
    }

    test("non-Goblin creatures do not gain haste") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Goblin Warchief")
        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        val projected = projector.project(driver.state)
        projected.hasKeyword(bears, Keyword.HASTE) shouldBe false
    }

    test("Goblin Warchief itself has haste from its own ability") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val warchief = driver.putCreatureOnBattlefield(activePlayer, "Goblin Warchief")

        val projected = projector.project(driver.state)
        projected.hasKeyword(warchief, Keyword.HASTE) shouldBe true
    }

    test("Goblin spells cost 1 less to cast") {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(GoblinWarchief)

        val calculator = CostCalculator(registry)

        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Goblin Warchief")

        // Goblin Guide costs {R} — only 1 colored mana, 0 generic
        // With Warchief, the reduction of 1 can't go below 0 generic, so still costs {R}
        val goblinGuide = registry.requireCard("Goblin Guide")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, goblinGuide, activePlayer)
        effectiveCost.cmc shouldBe 1  // Just {R}, generic was already 0
    }

    test("cost reduction reduces generic mana for Goblin spells") {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(GoblinWarchief)

        val calculator = CostCalculator(registry)

        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Goblin Warchief")

        // Goblin Warchief costs {1}{R}{R} = 3 CMC, 1 generic
        // With one Warchief on the battlefield, another Goblin Warchief would cost {R}{R}
        val warchiefDef = registry.requireCard("Goblin Warchief")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, warchiefDef, activePlayer)
        effectiveCost.genericAmount shouldBe 0
        effectiveCost.cmc shouldBe 2  // Just {R}{R}
    }

    test("cost reduction does not apply to non-Goblin spells") {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(GoblinWarchief)

        val calculator = CostCalculator(registry)

        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Goblin Warchief")

        // Grizzly Bears costs {1}{G} — should NOT be reduced
        val bears = registry.requireCard("Grizzly Bears")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, bears, activePlayer)
        effectiveCost.genericAmount shouldBe 1
        effectiveCost.cmc shouldBe 2  // Still {1}{G}
    }

    test("multiple Warchiefs stack cost reduction") {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(GoblinWarchief)

        val calculator = CostCalculator(registry)

        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Goblin Warchief")
        driver.putCreatureOnBattlefield(activePlayer, "Goblin Warchief")

        // Goblin Warchief costs {1}{R}{R} = 1 generic
        // With two Warchiefs, reduction is 2 but generic is only 1, so it becomes {R}{R}
        val warchiefDef = registry.requireCard("Goblin Warchief")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, warchiefDef, activePlayer)
        effectiveCost.genericAmount shouldBe 0
        effectiveCost.cmc shouldBe 2  // Just {R}{R}
    }

    test("casting a Goblin with cost reduction and sufficient mana succeeds") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Goblin Warchief")

        // Goblin Guide costs {R}, Warchief doesn't reduce it further (0 generic)
        val goblinInHand = driver.putCardInHand(activePlayer, "Goblin Guide")
        driver.giveMana(activePlayer, Color.RED, 1)

        val result = driver.castSpell(activePlayer, goblinInHand)
        result.isSuccess shouldBe true
    }
})
