package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ProliferatedEvent
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onc.cards.ContaminantGrafter
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Contaminant Grafter (ONC #17) — {4}{G} Creature — Phyrexian Druid, 5/5.
 *
 *   Trample, toxic 1
 *   Whenever one or more creatures you control deal combat damage to one or more players,
 *   proliferate.
 *   Corrupted — At the beginning of your end step, if an opponent has three or more poison
 *   counters, draw a card, then you may put a land card from your hand onto the battlefield.
 */
class ContaminantGrafterScenarioTest : FunSpec({

    fun driverWith(players: Int): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(ContaminantGrafter))
        val ids = d.initMultiplayer(List(players) { Deck.of("Grizzly Bears" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe ids[0]
        return d to ids
    }

    fun GameTestDriver.ready(player: EntityId, name: String): EntityId =
        putCreatureOnBattlefield(player, name).also { removeSummoningSickness(it) }

    fun GameTestDriver.poison(player: EntityId, count: Int) {
        replaceState(state.updateEntity(player) { it.with(CountersComponent(mapOf(CounterType.POISON to count))) })
    }

    fun GameTestDriver.poisonOf(player: EntityId): Int =
        state.getEntity(player)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0

    test("creatures hitting two opponents proliferate exactly once") {
        val (d, p) = driverWith(3)
        val grafter = d.ready(p[0], "Contaminant Grafter")
        val bears = d.ready(p[0], "Grizzly Bears")
        d.poison(p[1], 1)
        d.poison(p[2], 1)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p[0], mapOf(grafter to p[1], bears to p[2])).error shouldBe null

        // Walk combat until the proliferate choice, then pick both opponents.
        var guard = 0
        var answered = 0
        while (d.state.step != Step.POSTCOMBAT_MAIN && guard++ < 60) {
            val decision = d.pendingDecision
            when {
                decision is SelectCardsDecision && p[1] in decision.options -> {
                    d.submitCardSelection(decision.playerId, listOf(p[1], p[2])).error shouldBe null
                    answered++
                }
                decision != null -> d.autoResolveDecision()
                else -> d.bothPass()
            }
        }

        withClue("one proliferate for the whole batch") {
            d.events.filterIsInstance<ProliferatedEvent>().size shouldBe 1
            answered shouldBe 1
        }
        withClue("toxic 1 then proliferate") { d.poisonOf(p[1]) shouldBe 3 }
        withClue("proliferate only") { d.poisonOf(p[2]) shouldBe 2 }
        d.getLifeTotal(p[1]) shouldBe 15
        d.getLifeTotal(p[2]) shouldBe 18
    }

    test("corrupted end step: draw a card, then put a land card from hand onto the battlefield") {
        val (d, p) = driverWith(2)
        d.putCreatureOnBattlefield(p[0], "Contaminant Grafter")
        d.poison(p[1], 3)
        val forest = d.putCardInHand(p[0], "Forest")
        val hand = d.getHandSize(p[0])

        d.passPriorityUntil(Step.END)
        d.stackSize shouldBe 1
        d.bothPass()

        val decision = d.pendingDecision
        (decision is SelectCardsDecision) shouldBe true
        d.submitCardSelection(p[0], listOf(forest)).error shouldBe null

        d.findPermanent(p[0], "Forest") shouldNotBe null
        withClue("drew one, put the Forest down") { d.getHandSize(p[0]) shouldBe hand }
    }

    test("corrupted end step: the land drop may be declined") {
        val (d, p) = driverWith(2)
        d.putCreatureOnBattlefield(p[0], "Contaminant Grafter")
        d.poison(p[1], 3)
        d.putCardInHand(p[0], "Forest")
        val hand = d.getHandSize(p[0])

        d.passPriorityUntil(Step.END)
        d.bothPass()
        d.submitCardSelection(p[0], emptyList()).error shouldBe null

        d.findPermanent(p[0], "Forest") shouldBe null
        d.getHandSize(p[0]) shouldBe hand + 1
    }

    test("no end-step trigger while no opponent has three poison counters") {
        val (d, p) = driverWith(2)
        d.putCreatureOnBattlefield(p[0], "Contaminant Grafter")
        d.poison(p[1], 2)

        d.passPriorityUntil(Step.END)
        d.stackSize shouldBe 0
    }
})
