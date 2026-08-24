package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blb.cards.CorpseberryCultivator
import com.wingedsheep.mtg.sets.definitions.blb.cards.FeedTheCycle
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Corpseberry Cultivator (BLB).
 *
 * Oracle: "At the beginning of combat on your turn, you may forage. (Exile three cards from your
 * graveyard or sacrifice a Food.) / Whenever you forage, put a +1/+1 counter on this creature."
 *
 * Two printed abilities, and the second is a real `Triggers.WheneverYouForage` trigger rather than
 * a counter folded into the card's own forage. So the load-bearing case here is the *cross-source*
 * one: a forage taken by something else on the board still grows the Cultivator.
 */
class CorpseberryCultivatorScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(CorpseberryCultivator, FeedTheCycle, PredefinedTokens.Food)
        )
        driver.initMirrorMatch(Deck.of("Swamp" to 20, "Forest" to 20), startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    // ---------------------------------------------------------------------------------------------
    // Its own forage feeds its own trigger.
    // ---------------------------------------------------------------------------------------------

    test("its own begin-combat forage (sacrifice a Food) grows it to 3/4") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val cultivator = driver.putCreatureOnBattlefield(active, "Corpseberry Cultivator")
        // Only the sacrifice mode is feasible (graveyard is empty), so the forage
        // ChooseActionEffect auto-executes it — no mode decision to answer.
        val food = driver.putPermanentOnBattlefield(active, "Food")

        driver.state.projectedState.getPower(cultivator) shouldBe 2
        driver.state.projectedState.getToughness(cultivator) shouldBe 3

        // The begin-combat trigger goes on the stack as the step is entered.
        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.bothPass()

        // "you may forage" — take it.
        driver.submitYesNo(active, true)

        // The forage emitted the foraged event, which put the second ability on the stack.
        driver.bothPass()

        driver.plusOneCounters(cultivator) shouldBe 1
        driver.state.projectedState.getPower(cultivator) shouldBe 3
        driver.state.projectedState.getToughness(cultivator) shouldBe 4
        driver.state.getBattlefield(active) shouldNotContain food
        driver.state.getGraveyard(active) shouldContain food
    }

    test("its own begin-combat forage (exile three chosen graveyard cards) grows it to 3/4") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val cultivator = driver.putCreatureOnBattlefield(active, "Corpseberry Cultivator")
        // No Food, so only the exile mode is feasible and it auto-executes; five candidates
        // means the player still picks which three.
        val grave = (1..5).map { driver.putCardInGraveyard(active, "Swamp") }

        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.bothPass()
        driver.submitYesNo(active, true)

        val chosen = listOf(grave[0], grave[2], grave[4])
        driver.submitCardSelection(active, chosen)

        driver.bothPass()

        driver.plusOneCounters(cultivator) shouldBe 1
        driver.state.projectedState.getPower(cultivator) shouldBe 3
        driver.state.projectedState.getToughness(cultivator) shouldBe 4
        driver.state.getExile(active) shouldContainAll chosen
        driver.state.getGraveyard(active) shouldContain grave[1]
        driver.state.getGraveyard(active) shouldContain grave[3]
    }

    // ---------------------------------------------------------------------------------------------
    // The regression this change exists for: a forage from ANOTHER source grows it.
    // ---------------------------------------------------------------------------------------------

    test("a forage paid as another spell's additional cost grows it even though it did not forage") {
        // Feed the Cycle: "As an additional cost to cast this spell, forage or pay {B}." A
        // *cost*-shaped forage, so this covers ForageCostResolver's emission path rather than the
        // ChooseActionEffect one.
        //
        // Camellia, the Seedmiser was the other cost-shaped candidate but it is unusable here: its
        // ability puts a +1/+1 counter on each *other Squirrel* you control, and the Cultivator is a
        // Squirrel Warlock — the counter under test would be indistinguishable from the ability's own.
        // Thornvault Forager's forage ability is a mana ability whose "two mana in any combination of
        // colors" prompts pip-by-pip, which only adds noise.
        val driver = newDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        val cultivator = driver.putCreatureOnBattlefield(active, "Corpseberry Cultivator")
        val food = driver.putPermanentOnBattlefield(active, "Food")
        val victim = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val feed = driver.putCardInHand(active, "Feed the Cycle")
        driver.giveMana(active, Color.BLACK, 2)

        val result = driver.submit(
            CastSpell(
                playerId = active,
                cardId = feed,
                targets = listOf(ChosenTarget.Permanent(victim)),
                chosenModes = listOf(1), // mode index 1 = the forage mode
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(victim))),
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(food))
            )
        )
        result.isSuccess shouldBe true

        // The Food went away as the cost was paid, and the Cultivator's forage trigger is now on the
        // stack above Feed the Cycle.
        driver.state.getBattlefield(active) shouldNotContain food

        driver.bothPass() // resolve the forage trigger
        driver.plusOneCounters(cultivator) shouldBe 1

        driver.bothPass() // resolve Feed the Cycle itself
        driver.state.projectedState.getPower(cultivator) shouldBe 3
        driver.state.projectedState.getToughness(cultivator) shouldBe 4
        driver.state.getGraveyard(opponent) shouldContain victim
    }

    // ---------------------------------------------------------------------------------------------
    // Declining, and having nothing to forage with.
    // ---------------------------------------------------------------------------------------------

    test("declining the may does nothing — no counter, nothing sacrificed or exiled") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val cultivator = driver.putCreatureOnBattlefield(active, "Corpseberry Cultivator")
        val food = driver.putPermanentOnBattlefield(active, "Food")
        val grave = (1..5).map { driver.putCardInGraveyard(active, "Swamp") }

        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.bothPass()

        driver.submitYesNo(active, false)

        driver.plusOneCounters(cultivator) shouldBe 0
        driver.state.projectedState.getPower(cultivator) shouldBe 2
        driver.state.projectedState.getToughness(cultivator) shouldBe 3
        driver.state.getBattlefield(active) shouldContain food
        driver.state.getGraveyard(active) shouldContainAll grave
        driver.state.getExile(active).size shouldBe 0
    }

    test("with an empty graveyard and no Food the may question is never asked") {
        // Forage has no "even if you can't" clause, so with neither mode feasible the prompt is
        // skipped outright rather than offered and refused.
        val driver = newDriver()
        val active = driver.activePlayer!!
        val cultivator = driver.putCreatureOnBattlefield(active, "Corpseberry Cultivator")

        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.bothPass()

        driver.pendingDecision.shouldBeNull()
        driver.plusOneCounters(cultivator) shouldBe 0
        driver.state.projectedState.getPower(cultivator) shouldBe 2
        driver.state.projectedState.getToughness(cultivator) shouldBe 3
    }
})
