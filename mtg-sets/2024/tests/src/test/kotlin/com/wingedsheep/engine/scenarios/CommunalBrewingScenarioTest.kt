package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blc.cards.CommunalBrewing
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Communal Brewing — {2}{G} Enchantment (BLC).
 *
 * When this enchantment enters, any number of target opponents each draw a card. Put an ingredient
 * counter on this enchantment, then put an ingredient counter on it for each card drawn this way.
 * Whenever you cast a creature spell, that creature enters with X additional +1/+1 counters on it,
 * where X is the number of ingredient counters on this enchantment.
 *
 * Pins the "cards drawn this way" tally (read off the opponents' draw tracker across the draws) and
 * the cast trigger's counters riding the creature spell from the stack onto the battlefield.
 */
class CommunalBrewingScenarioTest : FunSpec({

    val projector = StateProjector()

    fun GameTestDriver.counters(id: EntityId, type: CounterType): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CommunalBrewing))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Resolve the stack, answering the ETB's target prompt with [targets]. */
    fun GameTestDriver.resolveAll(targets: List<EntityId> = emptyList()) {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 50) {
            when (val decision = state.pendingDecision) {
                is ChooseTargetsDecision -> submitTargetSelection(decision.playerId, targets)
                null -> bothPass()
                else -> autoResolveDecision()
            }
        }
    }

    fun GameTestDriver.castBrewing(me: EntityId, targets: List<EntityId>): EntityId {
        val spell = putCardInHand(me, "Communal Brewing")
        giveColorlessMana(me, 2)
        giveMana(me, Color.GREEN, 1)
        castSpell(me, spell)
        resolveAll(targets)
        return findPermanent(me, "Communal Brewing")!!
    }

    fun GameTestDriver.castCourser(me: EntityId): EntityId {
        val courser = putCardInHand(me, "Centaur Courser")
        giveColorlessMana(me, 2)
        giveMana(me, Color.GREEN, 1)
        castSpell(me, courser)
        resolveAll()
        return findPermanent(me, "Centaur Courser")!!
    }

    test("targeting an opponent: they draw, two ingredient counters, creature enters with two +1/+1 counters") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val oppHandBefore = driver.getHandSize(opponent)

        val brewing = driver.castBrewing(me, listOf(opponent))

        driver.getHandSize(opponent) shouldBe oppHandBefore + 1
        driver.counters(brewing, CounterType.INGREDIENT) shouldBe 2

        val courser = driver.castCourser(me)
        driver.counters(courser, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        // 3/3 base + two +1/+1 counters.
        projector.project(driver.state).getPower(courser) shouldBe 5
        projector.project(driver.state).getToughness(courser) shouldBe 5
    }

    test("no targets: nobody draws, one ingredient counter, creature enters with one +1/+1 counter") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val oppHandBefore = driver.getHandSize(opponent)

        val brewing = driver.castBrewing(me, emptyList())

        driver.getHandSize(opponent) shouldBe oppHandBefore
        driver.counters(brewing, CounterType.INGREDIENT) shouldBe 1

        val courser = driver.castCourser(me)
        driver.counters(courser, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
    }

    test("a target with an empty library draws nothing: only the base ingredient counter") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.replaceState(
            driver.state.copy(zones = driver.state.zones + (ZoneKey(opponent, Zone.LIBRARY) to emptyList()))
        )

        val brewing = driver.castBrewing(me, listOf(opponent))

        driver.counters(brewing, CounterType.INGREDIENT) shouldBe 1
    }
})
