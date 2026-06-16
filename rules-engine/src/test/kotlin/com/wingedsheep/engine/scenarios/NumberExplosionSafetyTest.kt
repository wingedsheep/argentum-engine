package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.GameLimits
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.RepeatCondition
import com.wingedsheep.sdk.scripting.effects.RepeatDynamicTimesEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe

/**
 * End-to-end proof that "explosion" cards — runaway token creation, runaway counter doubling, and
 * unbounded effect-iteration loops — are contained by the [GameLimits] safety net instead of
 * hanging or crashing the engine. Each test drives a real game through the full [GameTestDriver]
 * stack (priority, stack resolution, SBAs).
 *
 * See `backlog/number-explosion-safety.md` (Option A).
 */
class NumberExplosionSafetyTest : FunSpec({

    // Creates far more tokens than MAX_TOKENS_PER_EFFECT in one resolution.
    val tokenFlood = card("Token Flood") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        oracleText = "Create 50000 1/1 green Soldier creature tokens."
        spell {
            effect = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Soldier"),
                count = 50_000
            )
        }
    }

    // Doubles +1/+1 counters — used against a creature pre-loaded near Int overflow.
    val counterDoubler = card("Counter Doubler") {
        manaCost = "{G}"
        typeLine = "Instant"
        oracleText = "Double the number of +1/+1 counters on target creature."
        spell {
            val target = target("target creature", Targets.Creature)
            effect = Effects.DoubleCounters(Counters.PLUS_ONE_PLUS_ONE, target)
        }
    }

    // Repeats a body an absurd number of times (the executor materializes one body per iteration).
    val repeatFlood = card("Repeat Flood") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        oracleText = "You gain 1 life, repeated 50000 times."
        spell {
            effect = RepeatDynamicTimesEffect(DynamicAmount.Fixed(50_000), Effects.GainLife(1))
        }
    }

    // A do-while loop whose condition is *always* true — i.e. an unbounded loop (1 > 0 forever).
    val infiniteLoop = card("Infinite Loop") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        oracleText = "You gain 1 life. Repeat this process. (Unbounded.)"
        spell {
            effect = Effects.RepeatWhile(
                body = Effects.GainLife(1),
                repeatCondition = RepeatCondition.WhileCondition(
                    Compare(DynamicAmount.Fixed(1), ComparisonOperator.GT, DynamicAmount.Fixed(0))
                )
            )
        }
    }

    fun driverWith(vararg cards: com.wingedsheep.sdk.model.CardDefinition): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + cards.toList())
        return driver
    }

    fun tokensControlledBy(driver: GameTestDriver, playerId: EntityId): Int =
        driver.state.getBattlefield().count { id ->
            val e = driver.state.getEntity(id) ?: return@count false
            e.has<TokenComponent>() && e.get<ControllerComponent>()?.playerId == playerId
        }

    test("runaway token creation is capped at MAX_TOKENS_PER_EFFECT, not OOM") {
        val driver = driverWith(tokenFlood)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20), startingLife = 20)
        val player1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val spell = driver.putCardInHand(player1, "Token Flood")
        driver.giveMana(player1, Color.GREEN, 1)

        driver.castSpell(player1, spell)
        driver.bothPass()

        // Clamped to exactly the per-effect cap — the engine survives a "create 50000" combo.
        tokensControlledBy(driver, player1) shouldBe GameLimits.MAX_TOKENS_PER_EFFECT
    }

    test("doubling a near-overflow counter count clamps positive instead of wrapping negative") {
        val driver = driverWith(counterDoubler)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20), startingLife = 20)
        val player1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val spell = driver.putCardInHand(player1, "Counter Doubler")
        val creature = driver.putCreatureOnBattlefield(player1, "Savannah Lions")
        // Pre-load close to Int overflow: 1.5e9 doubled is 3e9, which wraps negative without the guard.
        driver.addComponent(creature, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1_500_000_000)))
        driver.giveMana(player1, Color.GREEN, 1)

        driver.castSpell(player1, spell, targets = listOf(creature))
        driver.bothPass()

        val counters = driver.state.getEntity(creature)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        counters shouldBe GameLimits.MAX_QUANTITY
        counters shouldBeGreaterThan 0
    }

    test("RepeatDynamicTimes is capped at MAX_REPEAT_ITERATIONS, not OOM") {
        val driver = driverWith(repeatFlood)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20), startingLife = 20)
        val player1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val spell = driver.putCardInHand(player1, "Repeat Flood")
        driver.giveMana(player1, Color.GREEN, 1)
        val lifeBefore = driver.getLifeTotal(player1)

        driver.castSpell(player1, spell)
        driver.bothPass()

        // Body ran exactly the capped number of times — 50000 requested clamps to the cap.
        driver.getLifeTotal(player1) shouldBe lifeBefore + GameLimits.MAX_REPEAT_ITERATIONS
    }

    test("an unbounded RepeatWhile loop aborts via the depth guard instead of StackOverflowError") {
        val driver = driverWith(infiniteLoop)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20), startingLife = 20)
        val player1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val spell = driver.putCardInHand(player1, "Infinite Loop")
        driver.giveMana(player1, Color.GREEN, 1)
        val lifeBefore = driver.getLifeTotal(player1)

        // The key assertion: this returns at all (no StackOverflowError / hang). The loop body
        // runs up to the depth cap, then the EffectExecutorRegistry aborts the branch.
        driver.castSpell(player1, spell)
        driver.bothPass()

        val gained = driver.getLifeTotal(player1) - lifeBefore
        // It ran a bounded number of times (depth-capped), not zero and not unbounded.
        gained shouldBeGreaterThan 0
        gained shouldBeLessThan GameLimits.MAX_RESOLUTION_DEPTH + 10
    }
})
