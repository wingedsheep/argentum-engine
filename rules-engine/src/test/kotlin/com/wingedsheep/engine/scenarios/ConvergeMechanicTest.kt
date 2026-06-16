package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.sos.cards.ArcaneOmens
import com.wingedsheep.mtg.sets.definitions.sos.cards.RancorousArchaic
import com.wingedsheep.mtg.sets.definitions.sos.cards.SunderingArchaic
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.values.EntityReference
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Converge (ability word, CR 207.2c) — scales an effect by the number of distinct *colors* of
 * mana spent to cast the spell. Exercises the two new SDK primitives end-to-end:
 *  - [com.wingedsheep.sdk.scripting.values.DynamicAmount.DistinctColorsManaSpent] — counter / spell
 *    payoff (Rancorous Archaic enters-with-counters; Arcane Omens "discard X").
 *  - [com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueAtMostColorsSpent] — the
 *    exile-by-colour-count target gate (Sundering Archaic).
 *
 * The CR clauses pinned here: colours are counted, not pips (5 mana of 2 colours → 2); colourless
 * is not a colour (CR 105.1) and never counts; mana paid on generic costs still has its colour
 * counted; a permanent put onto the battlefield without being cast spent 0 mana → 0 colours.
 */
class ConvergeMechanicTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(RancorousArchaic, ArcaneOmens, SunderingArchaic))
        return driver
    }

    fun startTurn(driver: GameTestDriver) {
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun plusCounters(driver: GameTestDriver, id: com.wingedsheep.sdk.model.EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    // ── DistinctColorsManaSpent via EntersWithDynamicCounters (Rancorous Archaic {5}) ──

    test("Rancorous Archaic: all colorless mana spent → 0 colors → no counters (2/2)") {
        val driver = createDriver()
        startTurn(driver)
        val p = driver.activePlayer!!
        val spell = driver.putCardInHand(p, "Rancorous Archaic")
        driver.giveColorlessMana(p, 5)

        driver.castSpell(p, spell).isSuccess shouldBe true
        driver.bothPass()

        plusCounters(driver, spell) shouldBe 0
    }

    test("Rancorous Archaic: one color among the generic payment → 1 counter") {
        val driver = createDriver()
        startTurn(driver)
        val p = driver.activePlayer!!
        val spell = driver.putCardInHand(p, "Rancorous Archaic")
        driver.giveColorlessMana(p, 4)
        driver.giveMana(p, Color.RED, 1)

        driver.castSpell(p, spell).isSuccess shouldBe true
        driver.bothPass()

        plusCounters(driver, spell) shouldBe 1
    }

    test("Rancorous Archaic: counts colors, not pips — 3 white + 2 blue (5 mana, 2 colors) → 2 counters") {
        val driver = createDriver()
        startTurn(driver)
        val p = driver.activePlayer!!
        val spell = driver.putCardInHand(p, "Rancorous Archaic")
        driver.giveMana(p, Color.WHITE, 3)
        driver.giveMana(p, Color.BLUE, 2)

        driver.castSpell(p, spell).isSuccess shouldBe true
        driver.bothPass()

        // Five mana spent, but only two distinct colors → two counters (a 4/4), not five.
        plusCounters(driver, spell) shouldBe 2
    }

    test("Rancorous Archaic: five different colors → 5 counters (7/7)") {
        val driver = createDriver()
        startTurn(driver)
        val p = driver.activePlayer!!
        val spell = driver.putCardInHand(p, "Rancorous Archaic")
        driver.giveMana(p, Color.WHITE, 1)
        driver.giveMana(p, Color.BLUE, 1)
        driver.giveMana(p, Color.BLACK, 1)
        driver.giveMana(p, Color.RED, 1)
        driver.giveMana(p, Color.GREEN, 1)

        driver.castSpell(p, spell).isSuccess shouldBe true
        driver.bothPass()

        plusCounters(driver, spell) shouldBe 5
    }

    // ── DistinctColorsManaSpent in a resolution effect (Arcane Omens {4}{B} "discard X") ──

    test("Arcane Omens: one color spent → target player discards exactly 1") {
        val driver = createDriver()
        startTurn(driver)
        val p = driver.activePlayer!!
        val opp = driver.getOpponent(p)
        repeat(3) { driver.putCardInHand(opp, "Savannah Lions") }

        val spell = driver.putCardInHand(p, "Arcane Omens")
        driver.giveMana(p, Color.BLACK, 5) // {4}{B} all black → 1 color

        driver.castSpell(p, spell, targets = listOf(opp)).isSuccess shouldBe true
        driver.bothPass()

        val discard = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        discard.minSelections shouldBe 1
        discard.maxSelections shouldBe 1
    }

    test("Arcane Omens: five colors spent → target player discards exactly 5") {
        val driver = createDriver()
        startTurn(driver)
        val p = driver.activePlayer!!
        val opp = driver.getOpponent(p)
        repeat(6) { driver.putCardInHand(opp, "Savannah Lions") }

        val spell = driver.putCardInHand(p, "Arcane Omens")
        // {4}{B}: B pays {B}; W/U/R/G pay the {4} generic → five distinct colors.
        driver.giveMana(p, Color.WHITE, 1)
        driver.giveMana(p, Color.BLUE, 1)
        driver.giveMana(p, Color.RED, 1)
        driver.giveMana(p, Color.GREEN, 1)
        driver.giveMana(p, Color.BLACK, 1)

        driver.castSpell(p, spell, targets = listOf(opp)).isSuccess shouldBe true
        driver.bothPass()

        val discard = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        discard.minSelections shouldBe 5
        discard.maxSelections shouldBe 5
    }

    // ── ManaValueAtMostColorsSpent predicate (Sundering Archaic exile gate) ──

    test("Sundering Archaic: exile target gated by colors spent — MV ≤ colors is legal, MV > colors is not") {
        val driver = createDriver()
        startTurn(driver)
        val p = driver.activePlayer!!
        val opp = driver.getOpponent(p)
        val mv1 = driver.putCreatureOnBattlefield(opp, "Goblin Guide")      // {R} → mana value 1
        val mv3 = driver.putCreatureOnBattlefield(opp, "Centaur Courser")   // {2}{G} → mana value 3

        val spell = driver.putCardInHand(p, "Sundering Archaic")
        driver.giveColorlessMana(p, 4)
        driver.giveMana(p, Color.RED, 1)
        driver.giveMana(p, Color.BLUE, 1) // {6} paid with 4 colorless + R + U → 2 colors

        driver.castSpell(p, spell).isSuccess shouldBe true
        driver.bothPass() // creature resolves; CastRecordComponent stamped (2 colors)

        val predicateEvaluator = PredicateEvaluator()
        val filter = GameObjectFilter.NonlandPermanent
            .opponentControls()
            .manaValueAtMostColorsSpent(EntityReference.Source)
        val context = PredicateContext(controllerId = p, sourceId = spell)

        // 2 colors of mana spent: MV1 is a legal target, MV3 is not.
        predicateEvaluator.matches(driver.state, driver.state.projectedState, mv1, filter, context) shouldBe true
        predicateEvaluator.matches(driver.state, driver.state.projectedState, mv3, filter, context) shouldBe false
    }

    test("Sundering Archaic: all-colorless cast → 0 colors → even a mana-value-1 permanent is illegal") {
        val driver = createDriver()
        startTurn(driver)
        val p = driver.activePlayer!!
        val opp = driver.getOpponent(p)
        val mv1 = driver.putCreatureOnBattlefield(opp, "Goblin Guide") // mana value 1

        val spell = driver.putCardInHand(p, "Sundering Archaic")
        driver.giveColorlessMana(p, 6) // 0 colors spent

        driver.castSpell(p, spell).isSuccess shouldBe true
        driver.bothPass()

        val predicateEvaluator = PredicateEvaluator()
        val filter = GameObjectFilter.NonlandPermanent
            .opponentControls()
            .manaValueAtMostColorsSpent(EntityReference.Source)
        val context = PredicateContext(controllerId = p, sourceId = spell)

        // 0 colors → 1 > 0 → not a legal target (colorless is not a color).
        predicateEvaluator.matches(driver.state, driver.state.projectedState, mv1, filter, context) shouldBe false
    }
})
