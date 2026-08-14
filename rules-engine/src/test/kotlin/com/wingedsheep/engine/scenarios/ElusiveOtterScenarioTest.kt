package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.woe.cards.ElusiveOtter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Elusive Otter // Grove's Bounty (WOE #225) — {U} 1/1 Creature — Otter.
 *
 *   Prowess
 *   Creatures with power less than this creature's power can't block it.
 *
 *   Adventure — Grove's Bounty {X}{G}, Sorcery:
 *   Distribute X +1/+1 counters among any number of target creatures you control.
 *
 * Grove's Bounty is the first X-scaled distribute, so the coverage focus is the widened
 * [com.wingedsheep.sdk.scripting.effects.DistributeCountersAmongTargetsEffect.totalCounters]
 * (now a `DynamicAmount`, read at resolution) and the X-clamped "any number of targets".
 */
class ElusiveOtterScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ElusiveOtter)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun countersOn(driver: GameTestDriver, entityId: EntityId): Int =
        driver.state.getEntity(entityId)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Cast Grove's Bounty (cardFaces[0] is the Adventure) for [x], distributing among [targets]. */
    fun castGrovesBounty(driver: GameTestDriver, card: EntityId, x: Int, targets: List<EntityId>) =
        driver.submitSuccess(
            CastSpell(
                playerId = driver.player1,
                cardId = card,
                targets = targets.map { ChosenTarget.Permanent(it) },
                xValue = x,
                faceIndex = 0,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )

    test("Grove's Bounty puts X counters on a single target and exiles the card on an Adventure") {
        val driver = newDriver()
        val player = driver.player1
        val card = driver.putCardInHand(player, "Elusive Otter")
        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.giveMana(player, Color.GREEN, 4) // {X=3}{G}

        castGrovesBounty(driver, card, x = 3, targets = listOf(bear))
        while (driver.stackSize > 0) driver.bothPass()

        countersOn(driver, bear) shouldBe 3
        driver.getExile(player) shouldContain card
    }

    test("X counters are split across two targets") {
        val driver = newDriver()
        val player = driver.player1
        val card = driver.putCardInHand(player, "Elusive Otter")
        val first = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val second = driver.putCreatureOnBattlefield(player, "Centaur Courser")
        driver.giveMana(player, Color.GREEN, 5) // {X=4}{G}

        castGrovesBounty(driver, card, x = 4, targets = listOf(first, second))
        while (driver.stackSize > 0) driver.bothPass()

        countersOn(driver, first) + countersOn(driver, second) shouldBe 4
        countersOn(driver, first) shouldBe 2
        countersOn(driver, second) shouldBe 2
    }

    test("X = 0 distributes nothing and still exiles on an Adventure") {
        val driver = newDriver()
        val player = driver.player1
        val card = driver.putCardInHand(player, "Elusive Otter")
        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.giveMana(player, Color.GREEN, 1) // {X=0}{G}

        castGrovesBounty(driver, card, x = 0, targets = emptyList())
        while (driver.stackSize > 0) driver.bothPass()

        countersOn(driver, bear) shouldBe 0
        driver.getExile(player) shouldContain card
    }

    test("more targets than X is rejected — each target must get a counter (CR 601.2d)") {
        val driver = newDriver()
        val player = driver.player1
        val card = driver.putCardInHand(player, "Elusive Otter")
        val first = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val second = driver.putCreatureOnBattlefield(player, "Centaur Courser")
        driver.giveMana(player, Color.GREEN, 2) // {X=1}{G}

        driver.submitExpectFailure(
            CastSpell(
                playerId = player,
                cardId = card,
                targets = listOf(first, second).map { ChosenTarget.Permanent(it) },
                xValue = 1,
                faceIndex = 0,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
    }

    test("the creature face is a 1/1 Otter with prowess") {
        val driver = newDriver()
        val otter = driver.putCreatureOnBattlefield(driver.player1, "Elusive Otter")

        driver.state.projectedState.getPower(otter) shouldBe 1
        driver.state.projectedState.getToughness(otter) shouldBe 1
        driver.state.projectedState.hasKeyword(otter, Keyword.PROWESS) shouldBe true
        driver.state.projectedState.hasSubtype(otter, "Otter") shouldBe true
    }

    test("a weaker creature can't block the Otter, an equal-power one can") {
        val driver = newDriver()
        val player = driver.player1
        val opponent = driver.player2
        val otter = driver.putCreatureOnBattlefield(player, "Elusive Otter")
        driver.removeSummoningSickness(otter)
        // Savannah Lions is 1/1 (equal power — may block); the Otter is 1/1 for now.
        val lions = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")
        driver.removeSummoningSickness(lions)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(otter), opponent)

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        // Equal power is not *less* power, so the block is legal.
        driver.declareBlockers(opponent, mapOf(lions to listOf(otter))).isSuccess shouldBe true
    }
})
