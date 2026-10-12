package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
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
import com.wingedsheep.sdk.scripting.effects.ForEachInCollectionEffect
import com.wingedsheep.sdk.scripting.effects.IterationSpace
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "Whenever you cast a spell that targets **only a single** creature you control, … that creature"
 * — `SpellCastPredicate.TargetsMatching(filter, only = true)` (Ivy, Gleeful Spellthief; Vesuvan
 * Duplimancy). Every instance of "target" on the spell must be one and the same object, no player
 * among them, and that object must match the filter. The trigger-time capture is unchanged, so the
 * payoff acts on exactly that object.
 */
class SpellTargetsOnlySingleTest : FunSpec({

    val pump = card("Test Single Pump") {
        manaCost = "{G}"
        typeLine = "Instant"
        spell {
            val a = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(1, 1, a)
        }
    }

    val doublePump = card("Test Double Pump") {
        manaCost = "{G}"
        typeLine = "Instant"
        spell {
            val a = target(TargetFilter.Creature)
            val b = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(1, 1, a) then Effects.ModifyStats(1, 1, b)
        }
    }

    val splitPump = card("Test Split Pump") {
        manaCost = "{G}"
        typeLine = "Instant"
        spell {
            val c = target(TargetFilter.Creature)
            val p = target(Targets.Player)
            effect = Effects.ModifyStats(1, 1, c) then Effects.GainLife(1, p)
        }
    }

    val observer = card("Test Single Target Observer") {
        manaCost = "{2}{W}"
        typeLine = "Creature — Human Scout"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.you.casts(
                requires = setOf(SpellCastPredicate.TargetsMatching(GameObjectFilter.Creature.youControl(), only = true))
            )
            effect = ForEachInCollectionEffect(
                collection = IterationSpace.TRIGGER_CAPTURED_COLLECTION,
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
            )
            description = "Whenever you cast a spell that targets only a single creature you control, " +
                "put a +1/+1 counter on that creature."
        }
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(pump, doublePump, splitPump, observer))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.resolveStack() {
        repeat(8) { if (pendingDecision != null) autoResolveDecision() else bothPass() }
    }

    fun GameTestDriver.cast(spell: String, vararg targets: ChosenTarget) {
        val me = activePlayer!!
        val id = putCardInHand(me, spell)
        giveMana(me, Color.GREEN, 1)
        castSpellWithTargets(me, id, targets.toList()).error shouldBe null
        resolveStack()
    }

    test("a spell with one target on a matching creature triggers and captures it") {
        val d = driver()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, observer.name)
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears")

        d.cast(pump.name, ChosenTarget.Permanent(bear))

        d.plusOneCounters(bear) shouldBe 1
    }

    test("several instances of target all on the same creature still count as a single creature") {
        val d = driver()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, observer.name)
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears")

        d.cast(doublePump.name, ChosenTarget.Permanent(bear), ChosenTarget.Permanent(bear))

        d.plusOneCounters(bear) shouldBe 1
    }

    test("two different creatures, both matching, are not a single creature") {
        val d = driver()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, observer.name)
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val courser = d.putCreatureOnBattlefield(me, "Centaur Courser")

        d.cast(doublePump.name, ChosenTarget.Permanent(bear), ChosenTarget.Permanent(courser))

        d.plusOneCounters(bear) shouldBe 0
        d.plusOneCounters(courser) shouldBe 0
    }

    test("a player among the targets disqualifies the spell") {
        val d = driver()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, observer.name)
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears")

        d.cast(splitPump.name, ChosenTarget.Permanent(bear), ChosenTarget.Player(d.getOpponent(me)))

        d.plusOneCounters(bear) shouldBe 0
    }

    test("a single target that fails the filter does not trigger") {
        val d = driver()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, observer.name)
        val theirs = d.putCreatureOnBattlefield(d.getOpponent(me), "Grizzly Bears")

        d.cast(pump.name, ChosenTarget.Permanent(theirs))

        d.plusOneCounters(theirs) shouldBe 0
    }
})
