package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AttackedDefenders
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `CreaturesAttackYouEvent(defenders = EACH_PLANESWALKER_YOU_CONTROL)` — "whenever an opponent
 * attacks a planeswalker you control with one or more creatures, … that player" (Oath of Kaya).
 *
 * Pins the card's ruling (one firing per attacked planeswalker, not per attacker and not per
 * combat), that attackers aimed at the player never count, and that "that player" is the attacker.
 */
class CreaturesAttackPlaneswalkerYouControlTriggerTest : FunSpec({

    val watcher = card("Test Planeswalker Watch") {
        manaCost = "{2}"; typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.you.isAttacked(defenders = AttackedDefenders.EACH_PLANESWALKER_YOU_CONTROL)
            effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.TriggeringPlayer)) then
                Effects.GainLife(2)
        }
    }
    val walker = card("Test Watched Walker") {
        manaCost = "{3}"; typeLine = "Planeswalker — Test"; startingLoyalty = 5; oracleText = ""
    }
    val bear = card("Test Walker Bear") {
        manaCost = "{1}{G}"; typeLine = "Creature — Bear"; power = 2; toughness = 2
    }

    // player1 (active) attacks; player2 controls the watcher and the planeswalkers.
    fun createDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(watcher, walker, bear))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        d.putPermanentOnBattlefield(d.player2, "Test Planeswalker Watch")
        return d
    }

    fun GameTestDriver.walker(): EntityId =
        putPermanentOnBattlefield(player2, "Test Watched Walker").also { pw ->
            replaceState(state.updateEntity(pw) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 5)) })
        }

    fun GameTestDriver.bear(): EntityId =
        putCreatureOnBattlefield(player1, "Test Walker Bear").also { removeSummoningSickness(it) }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 20) bothPass()
    }

    test("one attacker at a planeswalker you control — the attacking player takes 2, you gain 2") {
        val d = createDriver()
        val pw = d.walker()
        val attacker = d.bear()

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, mapOf(attacker to pw)).error shouldBe null
        d.state.stack.size shouldBe 1
        d.resolveAll()

        d.assertLifeTotal(d.player1, 18)
        d.assertLifeTotal(d.player2, 22)
    }

    test("attacking two of your planeswalkers triggers once for each (the card's ruling)") {
        val d = createDriver()
        val first = d.walker()
        val second = d.walker()
        val a = d.bear()
        val b = d.bear()

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, mapOf(a to first, b to second)).error shouldBe null
        d.state.stack.size shouldBe 2
        d.resolveAll()

        d.assertLifeTotal(d.player1, 16)
        d.assertLifeTotal(d.player2, 24)
    }

    test("several attackers at the same planeswalker trigger it only once") {
        val d = createDriver()
        val pw = d.walker()
        val a = d.bear()
        val b = d.bear()

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, mapOf(a to pw, b to pw)).error shouldBe null
        d.state.stack.size shouldBe 1
        d.resolveAll()

        d.assertLifeTotal(d.player1, 18)
    }

    test("attackers aimed at you don't count — only planeswalkers do") {
        val d = createDriver()
        d.walker()
        val attacker = d.bear()

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(attacker), d.player2).error shouldBe null

        withClue("no planeswalker was attacked, so nothing triggers") {
            d.state.stack.size shouldBe 0
        }
        d.assertLifeTotal(d.player1, 20)
    }

    test("a split attack fires once, for the planeswalker only") {
        val d = createDriver()
        val pw = d.walker()
        val a = d.bear()
        val b = d.bear()

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, mapOf(a to d.player2, b to pw)).error shouldBe null
        d.state.stack.size shouldBe 1
        d.resolveAll()

        d.assertLifeTotal(d.player1, 18)
        d.assertLifeTotal(d.player2, 22)
    }
})
