package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Temur Battlecrier and the two SDK primitives it introduces:
 *
 *  - `CostReductionSource.PermanentsYouControlMatching(filter)` — the filtered "you control" count
 *    (projected power, so counters/lords count).
 *  - `CostGating.OnlyIf(condition)` — gates the whole modification on a cast-time condition
 *    (here `IsYourTurn`, for "During your turn, ...").
 *
 * Temur Battlecrier: {G}{U}{R}, Creature — Orc Ranger, 4/3.
 * "During your turn, spells you cast cost {1} less to cast for each creature you control with
 *  power 4 or greater."
 */
class TemurBattlecrierTest : FunSpec({

    val Battlecrier = card("Temur Battlecrier") {
        manaCost = "{G}{U}{R}"
        typeLine = "Creature — Orc Ranger"
        power = 4
        toughness = 3
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.Any),
                modification = CostModification.ReduceGenericBy(
                    CostReductionSource.PermanentsYouControlMatching(
                        GameObjectFilter.Creature.powerAtLeast(4),
                    ),
                ),
                gating = CostGating.OnlyIf(IsYourTurn),
            )
        }
    }

    // A power-4 creature (counts toward the discount).
    val Beefy = card("Beefy Brute") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Bear"
        power = 4
        toughness = 4
    }

    // A power-3 creature (below the threshold).
    val Runt = card("Scrawny Runt") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 3
        toughness = 3
    }

    // Anthem granting +1/+1 to creatures you control — proves the count reads projected power.
    val Anthem = card("Herd Anthem") {
        manaCost = "{1}{G}"
        typeLine = "Enchantment"
        staticAbility {
            ability = ModifyStats(1, 1, GroupFilter.AllCreaturesYouControl)
        }
    }

    // The spell whose cost we measure: {4}{R} → 4 generic, CMC 5.
    val TestSpell = card("Test Bolt") {
        manaCost = "{4}{R}"
        typeLine = "Sorcery"
    }

    // SelfCast + OnlyIf(IsYourTurn) — the migrated Mental Modulation shape.
    val SelfDuringTurn = card("Self During Turn") {
        manaCost = "{4}{U}"
        typeLine = "Instant"
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.SelfCast,
                modification = CostModification.ReduceGeneric(1),
                gating = CostGating.OnlyIf(IsYourTurn),
            )
        }
    }

    // SelfCast + OnlyIf(Compare) — the migrated Lashwhip Predator shape.
    val SelfIfOpponentCreatures = card("Self If Opp Creatures") {
        manaCost = "{4}{G}"
        typeLine = "Creature — Beast"
        power = 5
        toughness = 7
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.SelfCast,
                modification = CostModification.ReduceGeneric(2),
                gating = CostGating.OnlyIf(
                    Compare(
                        DynamicAmount.AggregateBattlefield(Player.EachOpponent, GameObjectFilter.Creature),
                        ComparisonOperator.GTE,
                        DynamicAmount.Fixed(3),
                    ),
                ),
            )
        }
    }

    val allCards = listOf(Battlecrier, Beefy, Runt, Anthem, TestSpell, SelfDuringTurn, SelfIfOpponentCreatures)

    fun createRegistry(): CardRegistry =
        CardRegistry().apply {
            register(TestCards.all)
            register(allCards)
        }

    fun createDriver(): GameTestDriver =
        GameTestDriver().apply {
            registerCards(TestCards.all)
            allCards.forEach { registerCard(it) }
            initMirrorMatch(deck = Deck.of("Forest" to 20), startingLife = 20)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }

    test("counts the Battlecrier itself (a 4-power creature) — {4}{R} → {3}{R}") {
        val registry = createRegistry()
        val calculator = CostCalculator(registry)
        val driver = createDriver()
        val me = driver.activePlayer!!

        driver.putCreatureOnBattlefield(me, "Temur Battlecrier")

        val cost = calculator.calculateEffectiveCost(driver.state, registry.requireCard("Test Bolt"), me)
        cost.genericAmount shouldBe 3
        cost.cmc shouldBe 4
    }

    test("power-3 creatures do not count, power-4 creatures do") {
        val registry = createRegistry()
        val calculator = CostCalculator(registry)
        val driver = createDriver()
        val me = driver.activePlayer!!

        driver.putCreatureOnBattlefield(me, "Temur Battlecrier") // power 4 → counts
        driver.putCreatureOnBattlefield(me, "Scrawny Runt")      // power 3 → does NOT count
        driver.putCreatureOnBattlefield(me, "Beefy Brute")       // power 4 → counts

        // Two qualifying creatures → reduce by {2}: {4}{R} → {2}{R}.
        val cost = calculator.calculateEffectiveCost(driver.state, registry.requireCard("Test Bolt"), me)
        cost.genericAmount shouldBe 2
        cost.cmc shouldBe 3
    }

    test("reduction reads projected power — an anthem lifts a 3-power creature to 4") {
        val registry = createRegistry()
        val calculator = CostCalculator(registry)
        val driver = createDriver()
        val me = driver.activePlayer!!

        driver.putCreatureOnBattlefield(me, "Temur Battlecrier") // base 4 → 5 with anthem, counts
        driver.putCreatureOnBattlefield(me, "Scrawny Runt")      // base 3 → 4 with anthem, now counts

        val before = calculator.calculateEffectiveCost(driver.state, registry.requireCard("Test Bolt"), me)
        before.genericAmount shouldBe 3 // only the Battlecrier qualifies (count 1)

        driver.putPermanentOnBattlefield(me, "Herd Anthem")

        val after = calculator.calculateEffectiveCost(driver.state, registry.requireCard("Test Bolt"), me)
        after.genericAmount shouldBe 2 // Runt now power 4 too → count 2
    }

    test("reduction never reduces colored pips below the cost (floors generic at 0)") {
        val registry = createRegistry()
        val calculator = CostCalculator(registry)
        val driver = createDriver()
        val me = driver.activePlayer!!

        // 5 power-4 creatures on a {4}{R} spell: generic floors at 0, {R} preserved.
        driver.putCreatureOnBattlefield(me, "Temur Battlecrier")
        repeat(4) { driver.putCreatureOnBattlefield(me, "Beefy Brute") }

        val cost = calculator.calculateEffectiveCost(driver.state, registry.requireCard("Test Bolt"), me)
        cost.genericAmount shouldBe 0
        cost.cmc shouldBe 1 // just {R}
    }

    test("'During your turn' — no reduction on an opponent's turn") {
        val registry = createRegistry()
        val calculator = CostCalculator(registry)
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        driver.putCreatureOnBattlefield(me, "Temur Battlecrier")

        // My turn → discount applies.
        calculator.calculateEffectiveCost(driver.state, registry.requireCard("Test Bolt"), me)
            .genericAmount shouldBe 3

        // Flip the active player: now it's the opponent's turn. The gate (IsYourTurn) is false for
        // the Battlecrier's controller, so my instant-speed casts get no discount.
        driver.replaceState(driver.state.copy(activePlayerId = opponent))
        calculator.calculateEffectiveCost(driver.state, registry.requireCard("Test Bolt"), me)
            .genericAmount shouldBe 4
    }

    test("reduction is controller-scoped — opponents get no discount from my Battlecrier") {
        val registry = createRegistry()
        val calculator = CostCalculator(registry)
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        driver.putCreatureOnBattlefield(me, "Temur Battlecrier")

        // The opponent casting the same spell sees full cost (YouCast matches only my casts).
        calculator.calculateEffectiveCost(driver.state, registry.requireCard("Test Bolt"), opponent)
            .genericAmount shouldBe 4
    }

    test("migrated SelfCast OnlyIf(IsYourTurn): {4}{U} → {3}{U} on your turn, full otherwise") {
        val registry = createRegistry()
        val calculator = CostCalculator(registry)
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        calculator.calculateEffectiveCost(driver.state, registry.requireCard("Self During Turn"), me)
            .genericAmount shouldBe 3

        driver.replaceState(driver.state.copy(activePlayerId = opponent))
        calculator.calculateEffectiveCost(driver.state, registry.requireCard("Self During Turn"), me)
            .genericAmount shouldBe 4
    }

    test("migrated SelfCast OnlyIf(Compare): discount only when opponents control 3+ creatures") {
        val registry = createRegistry()
        val calculator = CostCalculator(registry)
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        // Opponent controls 2 creatures → no discount: {4}{G} stays 4 generic.
        repeat(2) { driver.putCreatureOnBattlefield(opponent, "Beefy Brute") }
        calculator.calculateEffectiveCost(driver.state, registry.requireCard("Self If Opp Creatures"), me)
            .genericAmount shouldBe 4

        // A third opponent creature trips the gate → reduce by {2}.
        driver.putCreatureOnBattlefield(opponent, "Beefy Brute")
        calculator.calculateEffectiveCost(driver.state, registry.requireCard("Self If Opp Creatures"), me)
            .genericAmount shouldBe 2
    }
})
