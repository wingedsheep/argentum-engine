package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.tap
import com.wingedsheep.engine.core.untapCount
import com.wingedsheep.engine.core.untapOrConsumeStun
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The untap clock behind "for as long as <source> remains tapped" (CR 611.2b). An activated
 * ability records its source's untap count as it goes on the stack, and the effects it creates with
 * a source-tapped duration hold only while the source is tapped *and* hasn't untapped since. So a
 * source that untaps before the ability resolves — even one tapped again by then — has already
 * ended the duration: the effect does nothing (Entrancing Lyre ruling), and the expiry SBA reaps it.
 */
class SourceTappedUntapClockTest : FunSpec({

    val tappedPump = card("Test Tapped Pump") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            val t = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(2, 0, t, Duration.WhileSourceTapped("this artifact"))
        }
    }
    val abilityId = tappedPump.activatedAbilities.first().id

    data class Board(val d: GameTestDriver, val me: EntityId, val pump: EntityId, val bears: EntityId)

    fun board(): Board {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(tappedPump))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val pump = d.putPermanentOnBattlefield(me, "Test Tapped Pump")
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        return Board(d, me, pump, bears)
    }

    fun Board.activate() {
        d.submit(ActivateAbility(me, pump, abilityId, targets = listOf(ChosenTarget.Permanent(bears)))).error shouldBe null
    }

    fun Board.power(): Int? = d.state.projectedState.getPower(bears)
    fun Board.untapPump() = d.replaceState(untapOrConsumeStun(d.state, pump).first)
    fun Board.tapPump() = d.replaceState(tap(d.state, pump).state)
    fun Board.liveEffects() = d.state.floatingEffects.count { it.sourceId == pump }

    test("the untap atom advances the clock; a stun counter that replaces the untap doesn't") {
        val b = board()
        untapCount(b.d.state.getEntity(b.pump)) shouldBe 0
        b.tapPump()
        b.untapPump()
        untapCount(b.d.state.getEntity(b.pump)) shouldBe 1

        b.tapPump()
        b.d.replaceState(b.d.state.updateEntity(b.pump) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.STUN, 1))
        })
        b.untapPump()
        untapCount(b.d.state.getEntity(b.pump)) shouldBe 1
    }

    test("source stays tapped through resolution: the effect applies until it untaps, for good") {
        val b = board()
        b.activate()
        b.d.bothPass()
        b.power() shouldBe 4

        b.untapPump()
        b.power() shouldBe 2
        b.tapPump()
        b.d.bothPass()
        b.power() shouldBe 2
        b.liveEffects() shouldBe 0
    }

    test("source untapped before resolution: the effect does nothing") {
        val b = board()
        b.activate()
        b.untapPump()
        b.d.bothPass()
        b.power() shouldBe 2
        b.liveEffects() shouldBe 0
    }

    test("source untapped and tapped again before resolution: the effect still does nothing") {
        val b = board()
        b.activate()
        b.untapPump()
        b.tapPump()
        b.d.bothPass()
        b.power() shouldBe 2
        b.liveEffects() shouldBe 0
    }

    test("a later activation counts from its own activation") {
        val b = board()
        b.activate()
        b.untapPump()
        b.d.bothPass()
        b.power() shouldBe 2

        b.activate()
        b.d.bothPass()
        b.power() shouldBe 4
    }
})
