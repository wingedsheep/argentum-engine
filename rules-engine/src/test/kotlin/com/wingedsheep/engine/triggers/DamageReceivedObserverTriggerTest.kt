package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The observer form of "is dealt damage": `Triggers.a(filter).isDealtDamage()` /
 * `Triggers.another(filter).isDealtDamage()`. The subject filter is matched against the damaged
 * permanent, which becomes the triggering entity; a damaged player never matches.
 */
class DamageReceivedObserverTriggerTest : FunSpec({

    // "Whenever another creature you control is dealt damage, you gain 1 life."
    val medic = card("Test Damage Medic") {
        manaCost = "{1}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 4
        triggeredAbility {
            trigger = Triggers.another(GameObjectFilter.Creature.youControl()).isDealtDamage()
            effect = Effects.GainLife(1)
        }
    }

    // "Whenever a creature an opponent controls is dealt damage, put a +1/+1 counter on it."
    val marker = card("Test Damage Marker") {
        manaCost = "{1}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.opponentControls()).isDealtDamage()
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity)
        }
    }

    val ping = card("Test Ping") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }

    fun createDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(medic, marker, ping))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.pingAndResolve(caster: EntityId, target: EntityId) {
        val id = putCardInHand(caster, "Test Ping")
        giveMana(caster, Color.RED, 1)
        castSpell(caster, id, listOf(target)).outcome shouldBe Outcome.Done
        var guard = 0
        while (guard++ < 10 && state.stack.isNotEmpty()) bothPass()
    }

    fun GameTestDriver.plusCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("another creature you control being dealt damage triggers") {
        val d = createDriver()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, "Test Damage Medic")
        val courser = d.putCreatureOnBattlefield(me, "Centaur Courser")

        d.pingAndResolve(me, courser)

        d.getLifeTotal(me) shouldBe 21
    }

    test("another excludes the source itself") {
        val d = createDriver()
        val me = d.activePlayer!!
        val medic = d.putCreatureOnBattlefield(me, "Test Damage Medic")

        d.pingAndResolve(me, medic)

        d.getLifeTotal(me) shouldBe 20
    }

    test("the subject filter is read against the damaged permanent") {
        val d = createDriver()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, "Test Damage Medic")
        val theirs = d.putCreatureOnBattlefield(d.getOpponent(me), "Centaur Courser")

        d.pingAndResolve(me, theirs)

        d.getLifeTotal(me) shouldBe 20
    }

    test("the damaged permanent is the triggering entity") {
        val d = createDriver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Test Damage Marker")
        val theirs = d.putCreatureOnBattlefield(d.getOpponent(me), "Centaur Courser")
        val mine = d.putCreatureOnBattlefield(me, "Centaur Courser")

        d.pingAndResolve(me, theirs)
        d.pingAndResolve(me, mine)

        d.plusCounters(theirs) shouldBe 1
        d.plusCounters(mine) shouldBe 0
    }

    test("a damaged player never matches an object filter") {
        val d = createDriver()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, "Test Damage Medic")
        d.putPermanentOnBattlefield(me, "Test Damage Marker")

        d.pingAndResolve(me, d.getOpponent(me))
        d.pingAndResolve(me, me)

        d.getLifeTotal(me) shouldBe 19
    }

    test("the observer form takes no damage-source filter") {
        shouldThrow<IllegalArgumentException> {
            Triggers.a(GameObjectFilter.Creature).isDealtDamage(by = GameObjectFilter.Creature)
        }
    }
})
