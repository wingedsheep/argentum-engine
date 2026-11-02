package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.permanent.counters.ProliferateExecutor
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.CantGetCountersComponent
import com.wingedsheep.engine.state.components.player.PlayerCounterLock
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.LockPlayerCountersEffect
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [LockPlayerCountersEffect] — "that player can't get counters" (CR 122.1: players have counters
 * too). Pins, per the Suncleanser rulings:
 * - no counter of any kind goes on a locked player — effect placement of poison and energy, and
 *   proliferate;
 * - a cost that would give the locked player counters ("Ward—Get five poison counters") can't be
 *   paid, so the spell is countered without a prompt;
 * - a source-keyed lock ("for as long as this creature remains on the battlefield") ends when the
 *   source leaves, and doesn't come back when the card returns as a new object (CR 611.2b);
 * - a source-keyed lock whose source isn't on the battlefield as it resolves does nothing (CR 611.2b);
 * - an end-of-turn lock expires at cleanup; locks from separate sources stack;
 * - "loses all counters" (`RemoveAllCounters`) clears a player's counters.
 */
class LockPlayerCountersTest : FunSpec({

    fun instant(name: String, script: CardScript) =
        CardDefinition.instant(name = name, manaCost = ManaCost.parse("{W}"), oracleText = name, script = script)

    val whileHere = Duration.WhileSourceOnBattlefield("Counter Warden")

    // "When this creature enters, you can't get counters for as long as it remains on the battlefield."
    val warden = card("Counter Warden") {
        manaCost = "{W}"
        typeLine = "Creature — Human Cleric"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.LockPlayerCounters(EffectTarget.Controller, whileHere)
        }
    }

    val poisonMe = instant("Poison Me", CardScript.spell(Effects.AddCounters(CounterType.POISON, 2, EffectTarget.Controller)))
    val energyMe = instant("Energy Me", CardScript.spell(Effects.AddCounters(CounterType.ENERGY, 3, EffectTarget.Controller)))
    val lockThisTurn = instant("Lock This Turn", CardScript.spell(Effects.LockPlayerCounters(EffectTarget.Controller, Duration.EndOfTurn)))
    // A spell is never on the battlefield, so its source-keyed lock never starts.
    val lockWhileSpellHere = instant("Lock While Spell Here", CardScript.spell(Effects.LockPlayerCounters(EffectTarget.Controller, whileHere)))
    val cleanseMe = instant("Cleanse Me", CardScript.spell(Effects.RemoveAllCounters(EffectTarget.Controller)))

    val poisonWardedBear = card("Poison-Warded Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(Keyword.WARD)
        keywordAbility(KeywordAbility.Ward(WardCost.PlayerCounters(CounterType.POISON, 5)))
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(warden, poisonMe, energyMe, lockThisTurn, lockWhileSpellHere, cleanseMe, poisonWardedBear)
        )
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun counters(driver: GameTestDriver, id: EntityId, type: CounterType): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    fun cast(driver: GameTestDriver, player: EntityId, name: String) =
        driver.putCardInHand(player, name).let { card ->
            driver.giveMana(player, Color.WHITE, 1)
            driver.castSpell(player, card).also { if (it.error == null) driver.bothPass() }
        }

    /** Cast the Warden and resolve both it and its enters trigger. */
    fun castWarden(driver: GameTestDriver, player: EntityId): EntityId {
        cast(driver, player, "Counter Warden").error shouldBe null
        if (driver.state.stack.isNotEmpty()) driver.bothPass()
        return driver.findPermanent(player, "Counter Warden")!!
    }

    test("a locked player gets no poison or energy counters, the other player still does") {
        val driver = newDriver()
        val me = driver.player1
        castWarden(driver, me)
        driver.state.cantGetCounters(me) shouldBe true
        driver.state.projectedState.canReceiveCounters(me) shouldBe false

        cast(driver, me, "Poison Me").error shouldBe null
        cast(driver, me, "Energy Me").error shouldBe null
        counters(driver, me, CounterType.POISON) shouldBe 0
        counters(driver, me, CounterType.ENERGY) shouldBe 0

        withClue("the lock is on its target only") {
            driver.state.cantGetCounters(driver.player2) shouldBe false
        }
    }

    test("proliferate skips a locked player") {
        val driver = newDriver()
        val me = driver.player1
        driver.addComponent(me, CountersComponent().withAdded(CounterType.POISON, 1))
        driver.addComponent(me, CantGetCountersComponent(listOf(PlayerCounterLock(PlayerEffectRemoval.Permanent))))

        val (after, _) = ProliferateExecutor.addOneOfEachKind(
            driver.state, listOf(me), driver.player2, PredicateEvaluator(cardRegistry = null)
        )
        after.getEntity(me)?.get<CountersComponent>()?.getCount(CounterType.POISON) shouldBe 1
    }

    test("a ward cost that gives the locked payer counters can't be paid — the spell is countered") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2
        castWarden(driver, me)
        val bear = driver.putCreatureOnBattlefield(opponent, "Poison-Warded Bear")

        driver.giveMana(me, Color.RED, 1)
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.castSpellWithTargets(me, bolt, listOf(ChosenTarget.Permanent(bear)))
        driver.bothPass()

        withClue("no pay-or-counter prompt is offered") {
            (driver.pendingDecision is YesNoDecision) shouldBe false
        }
        repeat(2) { if (driver.state.priorityPlayerId != null && driver.state.stack.isNotEmpty()) driver.bothPass() }
        counters(driver, me, CounterType.POISON) shouldBe 0
        driver.findPermanent(opponent, "Poison-Warded Bear") shouldNotBe null
    }

    test("the lock ends when its source leaves, and a returning card doesn't revive it") {
        val driver = newDriver()
        val me = driver.player1
        val wardenId = castWarden(driver, me)
        driver.state.cantGetCounters(me) shouldBe true

        driver.moveToGraveyard(wardenId)
        driver.state.cantGetCounters(me) shouldBe false
        cast(driver, me, "Poison Me").error shouldBe null
        counters(driver, me, CounterType.POISON) shouldBe 2

        withClue("the same card back on the battlefield is a new object (CR 400.7) — the lock stays off") {
            driver.replaceState(
                driver.state.moveToZone(wardenId, ZoneKey(me, Zone.GRAVEYARD), ZoneKey(me, Zone.BATTLEFIELD))
            )
            driver.findPermanent(me, "Counter Warden") shouldBe wardenId
            driver.state.cantGetCounters(me) shouldBe false
        }
    }

    test("locks from separate sources stack: one leaving keeps the other") {
        val driver = newDriver()
        val me = driver.player1
        val first = castWarden(driver, me)
        castWarden(driver, me)
        driver.state.getEntity(me)?.get<CantGetCountersComponent>()?.locks?.size shouldBe 2

        driver.moveToGraveyard(first)
        driver.state.cantGetCounters(me) shouldBe true
    }

    test("a source-keyed lock whose source isn't on the battlefield does nothing (CR 611.2b)") {
        val driver = newDriver()
        val me = driver.player1
        cast(driver, me, "Lock While Spell Here").error shouldBe null
        driver.state.cantGetCounters(me) shouldBe false
        cast(driver, me, "Poison Me").error shouldBe null
        counters(driver, me, CounterType.POISON) shouldBe 2
    }

    test("an end-of-turn lock expires in cleanup") {
        val driver = newDriver()
        val me = driver.player1
        cast(driver, me, "Lock This Turn").error shouldBe null
        cast(driver, me, "Poison Me").error shouldBe null
        counters(driver, me, CounterType.POISON) shouldBe 0

        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.cantGetCounters(me) shouldBe false
    }

    test("a player loses all counters — every kind is removed") {
        val driver = newDriver()
        val me = driver.player1
        driver.addComponent(
            me,
            CountersComponent().withAdded(CounterType.POISON, 3).withAdded(CounterType.ENERGY, 2)
        )
        cast(driver, me, "Cleanse Me").error shouldBe null
        counters(driver, me, CounterType.POISON) shouldBe 0
        counters(driver, me, CounterType.ENERGY) shouldBe 0
    }

    test("only durations the lock can honor are accepted") {
        LockPlayerCountersEffect(duration = Duration.EndOfTurn).description shouldBe "Target player can't get counters this turn"
        LockPlayerCountersEffect(duration = whileHere).description shouldBe
            "Target player can't get counters for as long as Counter Warden remains on the battlefield"
        shouldThrow<IllegalArgumentException> { LockPlayerCountersEffect(duration = Duration.UntilYourNextTurn) }
    }
})
