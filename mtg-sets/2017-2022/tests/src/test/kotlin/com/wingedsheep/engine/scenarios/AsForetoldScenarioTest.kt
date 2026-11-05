package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.MayCastWithoutPayingCostUsedThisTurnComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.akh.cards.AsForetold
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * As Foretold (AKH 42): "At the beginning of your upkeep, put a time counter on this enchantment.
 * Once each turn, you may pay {0} rather than pay the mana cost for a spell you cast with mana value
 * X or less, where X is the number of time counters on this enchantment."
 *
 * "Once each turn" is any player's turn — the free cast works on an opponent's turn too, which is
 * what separates it from Zaffai's "once during each of your turns".
 */
class AsForetoldScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(AsForetold)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        return d
    }

    fun GameTestDriver.timeCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.TIME) ?: 0

    fun GameTestDriver.setTimeCounters(id: EntityId, n: Int) =
        replaceState(state.updateEntity(id) { it.with(CountersComponent().withAdded(CounterType.TIME, n)) })

    fun GameTestDriver.freeCastOffered(player: EntityId, cardId: EntityId): Boolean =
        legalActions(player).any { legal ->
            val a = legal.action
            a is CastSpell && a.cardId == cardId && a.useWithoutPayingManaCost
        }

    test("gains a time counter at the beginning of its controller's upkeep") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val foretold = d.putPermanentOnBattlefield(p1, "As Foretold")
        d.timeCounters(foretold) shouldBe 0

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.getOpponent(p1))
        d.timeCounters(foretold) shouldBe 0
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, p1)
        d.timeCounters(foretold) shouldBe 1
    }

    test("free-casts a spell with mana value up to X on an opponent's turn, once that turn") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val foretold = d.putPermanentOnBattlefield(p1, "As Foretold")
        d.setTimeCounters(foretold, 1)

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, p2)
        d.timeCounters(foretold) shouldBe 1 // p2's upkeep adds nothing
        val bolt1 = d.putCardInHand(p1, "Lightning Bolt")
        val bolt2 = d.putCardInHand(p1, "Lightning Bolt")
        d.passPriority(p2) // p1 now holds priority on p2's turn, with no mana

        d.freeCastOffered(p1, bolt1) shouldBe true
        d.submitSuccess(
            CastSpell(p1, bolt1, targets = listOf(ChosenTarget.Player(p2)), useWithoutPayingManaCost = true)
        )
        d.state.getEntity(foretold)?.get<MayCastWithoutPayingCostUsedThisTurnComponent>() shouldNotBe null
        d.bothPass()
        d.getLifeTotal(p2) shouldBe 17

        // Once each turn: the second Bolt is not free this turn.
        d.passPriority(p2)
        d.freeCastOffered(p1, bolt2) shouldBe false
        d.submitExpectFailure(
            CastSpell(p1, bolt2, targets = listOf(ChosenTarget.Player(p2)), useWithoutPayingManaCost = true)
        )

        // The use resets for the next turn (p1's own).
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, p1)
        d.state.getEntity(foretold)?.get<MayCastWithoutPayingCostUsedThisTurnComponent>() shouldBe null
        d.freeCastOffered(p1, bolt2) shouldBe true
    }

    test("a spell whose mana value exceeds the time counters is not free") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val foretold = d.putPermanentOnBattlefield(p1, "As Foretold")
        val bears = d.putCardInHand(p1, "Grizzly Bears")

        d.setTimeCounters(foretold, 1)
        d.freeCastOffered(p1, bears) shouldBe false
        d.submitExpectFailure(CastSpell(p1, bears, useWithoutPayingManaCost = true))

        d.setTimeCounters(foretold, 2)
        d.freeCastOffered(p1, bears) shouldBe true
        d.submitSuccess(CastSpell(p1, bears, useWithoutPayingManaCost = true))
        d.bothPass()
        d.getCreatures(p1).size shouldBe 1
    }
})
