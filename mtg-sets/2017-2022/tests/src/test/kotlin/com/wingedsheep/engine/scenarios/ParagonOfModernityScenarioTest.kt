package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.snc.cards.ParagonOfModernity
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Paragon of Modernity (SNC #242) — {4} Artifact Creature — Angel Warrior 2/2, flying.
 *
 *   {3}: This creature gets +1/+1 until end of turn. If exactly three colors of mana were spent
 *   to activate this ability, put a +1/+1 counter on it instead.
 *
 * Ruling (2022-04-29): colorless mana isn't counted among the colors spent on the ability.
 */
class ParagonOfModernityScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(ParagonOfModernity))
        d.initMirrorMatch(Deck.of("Plains" to 40))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d to d.putPermanentOnBattlefield(d.activePlayer!!, "Paragon of Modernity")
    }

    fun GameTestDriver.activate(paragon: EntityId) {
        val ability = ParagonOfModernity.script.activatedAbilities.single().id
        submit(ActivateAbility(activePlayer!!, paragon, ability)).error shouldBe null
        bothPass()
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("three colors spent puts a +1/+1 counter instead of the pump") {
        val (d, paragon) = setup()
        val p = d.activePlayer!!
        d.giveMana(p, Color.WHITE); d.giveMana(p, Color.BLUE); d.giveMana(p, Color.BLACK)
        d.activate(paragon)

        d.plusOneCounters(paragon) shouldBe 1
        d.state.projectedState.getPower(paragon) shouldBe 3
        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.state.projectedState.getPower(paragon) shouldBe 3
    }

    test("two colors spent only pumps until end of turn") {
        val (d, paragon) = setup()
        val p = d.activePlayer!!
        d.giveMana(p, Color.WHITE, 2); d.giveMana(p, Color.BLUE)
        d.activate(paragon)

        d.plusOneCounters(paragon) shouldBe 0
        d.state.projectedState.getPower(paragon) shouldBe 3
        d.state.projectedState.getToughness(paragon) shouldBe 3
    }

    test("colorless mana is not a color: two colors plus colorless only pumps") {
        val (d, paragon) = setup()
        val p = d.activePlayer!!
        d.giveMana(p, Color.RED); d.giveMana(p, Color.GREEN); d.giveColorlessMana(p, 1)
        d.activate(paragon)

        d.plusOneCounters(paragon) shouldBe 0
        d.state.projectedState.getPower(paragon) shouldBe 3
    }
})
