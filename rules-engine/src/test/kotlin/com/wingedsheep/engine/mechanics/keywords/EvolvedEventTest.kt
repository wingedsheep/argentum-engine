package com.wingedsheep.engine.mechanics.keywords

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.evolve
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ModifyCounterPlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `EventPattern.EvolvedEvent` / `Triggers.self.evolves()` (CR 702.100b): a creature "evolves" when
 * one or more +1/+1 counters are put on it as a result of its evolve ability resolving. The
 * `evolve()` ability's tail emits the event only when its own counter lands — a +1/+1 counter from
 * any other source fires nothing, an extra counter from a placement replacement still fires once,
 * and an entering creature that isn't bigger never triggers evolve at all.
 */
class EvolvedEventTest : FunSpec({

    val evolver = card("Evolved Event Test Evolver") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Elk"
        power = 2
        toughness = 2
        oracleText = "Evolve\nWhenever this creature evolves, you gain 1 life."
        evolve()
        triggeredAbility {
            trigger = Triggers.self.evolves()
            effect = Effects.GainLife(1)
        }
    }

    val growth = card("Evolved Event Test Growth") {
        manaCost = "{G}"
        typeLine = "Instant"
        oracleText = "Put a +1/+1 counter on target creature."
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.AddCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, count = 1, target = t)
        }
    }

    val scales = card("Evolved Event Test Scales") {
        manaCost = "{G}"
        typeLine = "Enchantment"
        oracleText = "If one or more +1/+1 counters would be put on a creature you control, that many plus one +1/+1 counters are put on it instead."
        replacementEffect(ModifyCounterPlacement(modifier = 1))
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(evolver, growth, scales))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20, skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.drain() {
        var guard = 0
        while ((state.stack.isNotEmpty() || pendingDecision != null) && guard++ < 40) {
            if (pendingDecision != null) autoResolveDecision() else bothPass()
        }
    }

    fun GameTestDriver.cast(name: String, generic: Int, green: Int, targets: List<EntityId> = emptyList()) {
        val me = player1
        val id = putCardInHand(me, name)
        giveMana(me, Color.GREEN, green)
        if (generic > 0) giveColorlessMana(me, generic)
        castSpell(me, id, targets).error shouldBe null
        drain()
    }

    test("a bigger creature entering evolves the creature and fires its evolves trigger") {
        val d = createDriver()
        val evo = d.putCreatureOnBattlefield(d.player1, evolver.name)

        d.cast("Centaur Courser", generic = 2, green = 1)

        d.plusOnes(evo) shouldBe 1
        d.getLifeTotal(d.player1) shouldBe 21
    }

    test("a creature that isn't bigger doesn't trigger evolve, so nothing evolves") {
        val d = createDriver()
        val evo = d.putCreatureOnBattlefield(d.player1, evolver.name)

        d.cast("Forest Walker", generic = 1, green = 1) // 2/2 vs 2/2: neither stat greater

        d.plusOnes(evo) shouldBe 0
        d.getLifeTotal(d.player1) shouldBe 20
    }

    test("a +1/+1 counter from any other source is not an evolve") {
        val d = createDriver()
        val evo = d.putCreatureOnBattlefield(d.player1, evolver.name)

        d.cast(growth.name, generic = 0, green = 1, targets = listOf(evo))

        d.plusOnes(evo) shouldBe 1
        d.getLifeTotal(d.player1) shouldBe 20
    }

    test("a counter-placement replacement adding extra counters still evolves it once") {
        val d = createDriver()
        val evo = d.putCreatureOnBattlefield(d.player1, evolver.name)
        d.putPermanentOnBattlefield(d.player1, scales.name)

        d.cast("Centaur Courser", generic = 2, green = 1)

        d.plusOnes(evo) shouldBe 2
        d.getLifeTotal(d.player1) shouldBe 21
    }
})
