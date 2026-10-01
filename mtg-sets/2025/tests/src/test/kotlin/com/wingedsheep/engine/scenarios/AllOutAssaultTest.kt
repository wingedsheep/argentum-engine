package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.AdditionalPhasesComponent
import com.wingedsheep.engine.state.components.player.ExtraPhaseKind
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tdm.cards.AllOutAssault
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Tests for All-Out Assault (Tarkir: Dragonstorm) and, through it, the one-shot
 * event-based "when you next attack this turn" delayed trigger (item 15 of
 * backlog/tdm-engine-gaps.md).
 *
 * "Creatures you control get +1/+1 and have deathtouch. When this enchantment enters, if it's
 * your main phase, there is an additional combat phase after this phase followed by an
 * additional main phase. When you next attack this turn, untap each creature you control."
 *
 * The delayed trigger is modeled with `CreateDelayedTriggerEffect(trigger = Triggers.you.attacks(),
 * fireOnce = true)`: it fires the first time you declare attackers this turn, then removes
 * itself — so a later attack the same turn (here, the bonus combat) won't untap again.
 */
class AllOutAssaultTest : FunSpec({

    // A plain vanilla creature to attack with / be buffed by the static.
    val bear = card("Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AllOutAssault, bear))
        return driver
    }

    /** Fully resolve the stack, resolving every triggered ability that lands on it. */
    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard < 50) {
            bothPass()
            guard++
        }
    }

    /** Cast All-Out Assault from hand during the active player's precombat main and resolve its ETB. */
    fun GameTestDriver.resolveAllOutAssault(controller: EntityId) {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        // {2}{R}{W}{B}: hand it the colored pips plus two extra red for the generic {2}.
        giveMana(controller, com.wingedsheep.sdk.core.Color.RED, 3)
        giveMana(controller, com.wingedsheep.sdk.core.Color.WHITE, 1)
        giveMana(controller, com.wingedsheep.sdk.core.Color.BLACK, 1)
        val assault = putCardInHand(controller, "All-Out Assault")
        castSpell(controller, assault)
        resolveStack() // resolve the spell, then its ETB triggered ability
    }

    test("ETB during your main phase grants an extra combat and creates a one-shot next-attack delayed trigger") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val attacker = driver.activePlayer!!

        driver.resolveAllOutAssault(attacker)

        // The extra combat phase followed by an extra main phase is queued on the controller.
        driver.state.getEntity(attacker)?.get<AdditionalPhasesComponent>()?.phases.orEmpty().map { it.kind } shouldBe
            listOf(ExtraPhaseKind.COMBAT, ExtraPhaseKind.MAIN)

        // Exactly one event-based, one-shot delayed trigger ("when you next attack this turn").
        val delayed = driver.state.delayedTriggers
        delayed.size shouldBe 1
        delayed.first().trigger shouldBe Triggers.you.attacks()
        delayed.first().fireOnce shouldBe true
    }

    test("Creatures you control get +1/+1 and have deathtouch") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val attacker = driver.activePlayer!!

        val myBear = driver.putCreatureOnBattlefield(attacker, "Test Bear")
        driver.resolveAllOutAssault(attacker)

        val projected = driver.state.projectedState
        projected.getPower(myBear) shouldBe 3
        projected.getToughness(myBear) shouldBe 3
        projected.hasKeyword(myBear, Keyword.DEATHTOUCH) shouldBe true
    }

    test("Delayed trigger fires on your next attack, untapping your creatures, then is consumed and does not fire on the bonus combat") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val myBear = driver.putCreatureOnBattlefield(attacker, "Test Bear")
        driver.removeSummoningSickness(myBear)

        driver.resolveAllOutAssault(attacker)
        driver.state.delayedTriggers.size shouldBe 1

        // First combat: declare the bear as an attacker (which taps it), then the delayed
        // trigger fires and untaps it.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(myBear), defender)
        driver.resolveStack()

        driver.state.getEntity(myBear)?.has<TappedComponent>() shouldBe false
        // The one-shot trigger has been consumed.
        driver.state.delayedTriggers.size shouldBe 0

        // Move past this combat, then into the bonus combat (granted by the ETB) the same turn.
        driver.passPriorityUntil(Step.END_COMBAT)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.state.turnNumber shouldBe 1 // still the same turn — this is the extra combat

        // Attack again: the bear taps, and because the delayed trigger was one-shot it does
        // NOT untap a second time.
        driver.declareAttackers(attacker, listOf(myBear), defender)
        driver.resolveStack()

        driver.state.getEntity(myBear)?.has<TappedComponent>() shouldBe true
    }

    test("Delayed trigger expires at end of turn if you never attack") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val attacker = driver.activePlayer!!

        driver.resolveAllOutAssault(attacker)
        driver.state.delayedTriggers.size shouldBe 1

        // Advance into the opponent's turn without ever declaring attackers.
        val startTurn = driver.state.turnNumber
        var guard = 0
        while (driver.state.turnNumber == startTurn && guard < 200) {
            driver.bothPass()
            guard++
        }

        driver.state.delayedTriggers.size shouldBe 0
    }
})
