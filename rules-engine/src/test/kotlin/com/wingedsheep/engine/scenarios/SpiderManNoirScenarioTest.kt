package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.SpiderManNoir
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Spider-Man Noir (SPM #67) — {4}{B} Legendary Creature — Spider Human Hero 4/4.
 *
 * "Menace
 *  Whenever a creature you control attacks alone, put a +1/+1 counter on it. Then surveil X, where
 *  X is the number of counters on it."
 *
 * The trigger is an ANY-bound `attacks(filter = creature you control, requires = {Alone})`, so "it"
 * is the lone attacker. The counter lands first, then "surveil X" reads the counters on that same
 * creature *after* the counter is added — proven dynamic below: a lone attacker with a pre-existing
 * counter surveils 2, a freshly-attacking one surveils 1, and attacking two-wide fires nothing.
 */
class SpiderManNoirScenarioTest : FunSpec({

    // A plain second attacker so we can exercise the "not alone" case.
    val bear = card("Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(SpiderManNoir, bear))
        initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.plusCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.seedPlusCounters(id: EntityId, n: Int) {
        replaceState(state.updateEntity(id) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(CounterType.PLUS_ONE_PLUS_ONE, n))
        })
    }

    /** bothPass until the attacks-alone trigger's surveil selection pauses (or the stack empties). */
    fun GameTestDriver.resolveUntilSurveilOrDone() {
        var guard = 0
        while (guard++ < 50 && !(isPaused && pendingDecision is SelectCardsDecision)) {
            if (state.stack.isNotEmpty() && !isPaused) bothPass() else break
        }
    }

    test("a creature attacking alone gets a +1/+1 counter and surveils X = 1") {
        val d = driver()
        val you = d.player1
        val opp = d.getOpponent(you)

        val noir = d.putPermanentOnBattlefield(you, "Spider-Man Noir")
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(you, listOf(noir), opp).error shouldBe null

        d.resolveUntilSurveilOrDone()

        // Counter is placed before the surveil pauses mid-resolution.
        d.plusCounters(noir) shouldBe 1
        val select = d.pendingDecision as SelectCardsDecision
        // X = 1 counter → exactly one card looked at.
        select.options.size shouldBe 1

        val gyBefore = d.getGraveyard(you).size
        d.submitCardSelection(you, select.options) // put the looked-at card into the graveyard
        d.getGraveyard(you).size shouldBe gyBefore + 1
    }

    test("surveil X scales with the counters already on the attacker (X = 2)") {
        val d = driver()
        val you = d.player1
        val opp = d.getOpponent(you)

        val noir = d.putPermanentOnBattlefield(you, "Spider-Man Noir")
        d.seedPlusCounters(noir, 1) // already a 5/5 with one +1/+1 counter
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(you, listOf(noir), opp).error shouldBe null

        d.resolveUntilSurveilOrDone()

        // 1 seeded + 1 from the trigger = 2 counters, so X = 2.
        d.plusCounters(noir) shouldBe 2
        val select = d.pendingDecision as SelectCardsDecision
        select.options.size shouldBe 2
    }

    test("attacking alongside another creature does not trigger — no counter, no surveil") {
        val d = driver()
        val you = d.player1
        val opp = d.getOpponent(you)

        val noir = d.putPermanentOnBattlefield(you, "Spider-Man Noir")
        val other = d.putPermanentOnBattlefield(you, "Test Bear")
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(you, listOf(noir, other), opp).error shouldBe null

        d.resolveUntilSurveilOrDone()

        d.plusCounters(noir) shouldBe 0
        (d.pendingDecision as? SelectCardsDecision) shouldBe null
    }

    test("Spider-Man Noir has menace") {
        SpiderManNoir.keywords.contains(Keyword.MENACE) shouldBe true
    }
})
