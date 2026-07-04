package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tla.cards.FirebenderAscension
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Firebender Ascension — {1}{R} Enchantment
 *
 * "When this enchantment enters, create a 2/2 red Soldier creature token with firebending 1.
 *  Whenever a creature you control attacking causes a triggered ability of that creature to trigger,
 *  put a quest counter on this enchantment. Then if it has four or more quest counters on it, you may
 *  copy that ability. You may choose new targets for the copy."
 *
 * Exercises the new `Triggers.AttackCausesYourCreaturesTriggeredAbility` trigger + matcher and the
 * `causedByAttack` stamp on `AbilityTriggeredEvent`:
 *  - the ETB builds the firebending Soldier token;
 *  - a creature's own attack trigger firing adds a quest counter;
 *  - a NON-attack trigger (an ETB) never adds a counter;
 *  - at four quest counters the optional copy fires, doubling the attack trigger's effect, and
 *    declining leaves it single.
 */
class FirebenderAscensionScenarioTest : FunSpec({

    // A creature whose OWN attack trigger is a plain, observable, non-targeting effect: gain 1 life.
    // SELF-bound `Triggers.Attacks` (a per-attacker AttackEvent) is exactly the "whenever this
    // creature attacks" shape Firebender Ascension keys on.
    val lifeAttacker = card("Life Attacker") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Warrior"
        power = 2
        toughness = 2
        oracleText = "Whenever this creature attacks, you gain 1 life."
        triggeredAbility {
            trigger = Triggers.Attacks
            effect = Effects.GainLife(1)
            description = "Whenever this creature attacks, you gain 1 life."
        }
    }

    // A creature with a NON-attack trigger (ETB gain life). Its triggered ability firing must NOT
    // fire Firebender Ascension's meta-trigger — it isn't caused by an attack.
    val etbGuy = card("Welcomer") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Cleric"
        power = 1
        toughness = 1
        oracleText = "When this creature enters, you gain 1 life."
        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            effect = Effects.GainLife(1)
            description = "When this creature enters, you gain 1 life."
        }
    }

    // A plain attacker with no triggers at all — attacking causes no triggered ability, so no counter.
    val vanilla = card("Vanilla Ox") {
        manaCost = "{2}"
        typeLine = "Creature — Ox"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FirebenderAscension, lifeAttacker, etbGuy, vanilla))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.resolveStack(guardMax: Int = 30) {
        var guard = 0
        while (state.stack.isNotEmpty() && guard < guardMax) {
            bothPass()
            guard++
        }
    }

    fun GameTestDriver.questCount(entityId: EntityId): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.QUEST) ?: 0

    fun GameTestDriver.seedQuestCounters(entityId: EntityId, count: Int) {
        replaceState(state.updateEntity(entityId) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(CounterType.QUEST, count))
        })
    }

    test("ETB creates a 2/2 red Soldier token with firebending") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val asc = driver.putCardInHand(me, "Firebender Ascension")
        driver.giveColorlessMana(me, 1)
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, asc)
        driver.bothPass() // resolve enchantment -> ETB trigger on stack
        driver.resolveStack()

        val soldiers = driver.getCreatures(me).filter { driver.getCardName(it) == "Soldier Token" }
        soldiers.size shouldBe 1
        val token = soldiers.single()
        driver.state.projectedState.getPower(token) shouldBe 2
        driver.state.projectedState.getToughness(token) shouldBe 2
        driver.state.projectedState.hasKeyword(token, Keyword.FIREBENDING) shouldBe true
    }

    test("a creature's own attack trigger firing adds a quest counter") {
        val driver = createDriver()
        val me = driver.player1
        val opp = driver.getOpponent(me)

        val asc = driver.putPermanentOnBattlefield(me, "Firebender Ascension")
        val attacker = driver.putCreatureOnBattlefield(me, "Life Attacker")
        driver.removeSummoningSickness(attacker)

        val lifeBefore = driver.getLifeTotal(me)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(attacker), opp)
        driver.resolveStack()

        // Attack trigger resolved once (+1 life); meta-trigger added one quest counter; below the
        // four-counter threshold, so no copy.
        driver.questCount(asc) shouldBe 1
        driver.getLifeTotal(me) shouldBe lifeBefore + 1
    }

    test("a non-attack trigger (ETB) does NOT add a quest counter") {
        val driver = createDriver()
        val me = driver.player1

        val asc = driver.putPermanentOnBattlefield(me, "Firebender Ascension")

        // Cast Welcomer so its ETB trigger fires — a triggered ability you control that was NOT
        // caused by an attack.
        val welcomer = driver.putCardInHand(me, "Welcomer")
        driver.giveColorlessMana(me, 1)
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, welcomer)
        driver.resolveStack()

        driver.questCount(asc) shouldBe 0
    }

    test("attacking with a trigger-less creature adds no quest counter") {
        val driver = createDriver()
        val me = driver.player1
        val opp = driver.getOpponent(me)

        val asc = driver.putPermanentOnBattlefield(me, "Firebender Ascension")
        val ox = driver.putCreatureOnBattlefield(me, "Vanilla Ox")
        driver.removeSummoningSickness(ox)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(ox), opp)
        driver.resolveStack()

        driver.questCount(asc) shouldBe 0
    }

    test("at four quest counters, saying yes copies the attack trigger (life gained twice)") {
        val driver = createDriver()
        val me = driver.player1
        val opp = driver.getOpponent(me)

        val asc = driver.putPermanentOnBattlefield(me, "Firebender Ascension")
        driver.seedQuestCounters(asc, 3) // one attack away from the four-counter threshold
        val attacker = driver.putCreatureOnBattlefield(me, "Life Attacker")
        driver.removeSummoningSickness(attacker)

        val lifeBefore = driver.getLifeTotal(me)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(attacker), opp)

        // Firebender's meta-ability resolves first (on top): 4th counter, then the "you may copy"
        // prompt. Say yes to copy the gain-life ability.
        var guard = 0
        while (driver.state.pendingDecision !is YesNoDecision && guard < 20) {
            driver.bothPass()
            guard++
        }
        (driver.state.pendingDecision is YesNoDecision) shouldBe true
        driver.submitYesNo(me, true)

        driver.resolveStack()

        // Copy resolves (+1) and the original attack trigger resolves (+1) → +2 life total.
        driver.questCount(asc) shouldBe 4
        driver.getLifeTotal(me) shouldBe lifeBefore + 2
    }

    test("at four quest counters, declining the copy leaves the attack trigger single") {
        val driver = createDriver()
        val me = driver.player1
        val opp = driver.getOpponent(me)

        val asc = driver.putPermanentOnBattlefield(me, "Firebender Ascension")
        driver.seedQuestCounters(asc, 3)
        val attacker = driver.putCreatureOnBattlefield(me, "Life Attacker")
        driver.removeSummoningSickness(attacker)

        val lifeBefore = driver.getLifeTotal(me)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(attacker), opp)

        var guard = 0
        while (driver.state.pendingDecision !is YesNoDecision && guard < 20) {
            driver.bothPass()
            guard++
        }
        (driver.state.pendingDecision is YesNoDecision) shouldBe true
        driver.submitYesNo(me, false)

        driver.resolveStack()

        // Only the original attack trigger resolved: +1 life, no copy.
        driver.questCount(asc) shouldBe 4
        driver.getLifeTotal(me) shouldBe lifeBefore + 1
    }
})
