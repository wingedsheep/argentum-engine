package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m20.cards.Blightbeetle
import com.wingedsheep.mtg.sets.definitions.mrd.cards.Battlegrowth
import com.wingedsheep.mtg.sets.definitions.shm.cards.Scar
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Blightbeetle — "Creatures your opponents control can't have +1/+1 counters put on them."
 * Battlegrowth ("Put a +1/+1 counter on target creature") still resolves on an opponent's
 * creature, but no counter goes on; the Blightbeetle controller's own creatures are unaffected.
 */
class BlightbeetleScenarioTest : FunSpec({

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(Blightbeetle, Battlegrowth, Scar))
        initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
    }
    fun GameTestDriver.plusCounters(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    fun GameTestDriver.growth(you: EntityId, target: EntityId) {
        giveMana(you, Color.GREEN)
        val spell = putCardInHand(you, "Battlegrowth")
        castSpellWithTargets(you, spell, listOf(ChosenTarget.Permanent(target))).error shouldBe null
        bothPass()
    }

    test("an opponent's creature can't get a +1/+1 counter") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(you, "Blightbeetle")
        val theirBears = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        d.growth(you, theirBears)

        d.plusCounters(theirBears) shouldBe 0
        d.state.projectedState.getPower(theirBears) shouldBe 2
    }

    test("the Blightbeetle controller's own creatures still get +1/+1 counters") {
        val d = setup()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(you, "Blightbeetle")
        val ownBears = d.putCreatureOnBattlefield(you, "Grizzly Bears")

        d.growth(you, ownBears)

        d.plusCounters(ownBears) shouldBe 1
        d.state.projectedState.getPower(ownBears) shouldBe 3
    }

    test("other kinds of counter still go on an opponent's creature") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(you, "Blightbeetle")
        val theirBears = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        d.giveMana(you, Color.BLACK)
        val scar = d.putCardInHand(you, "Scar")
        d.castSpellWithTargets(you, scar, listOf(ChosenTarget.Permanent(theirBears))).error shouldBe null
        d.bothPass()

        d.state.getEntity(theirBears)?.get<CountersComponent>()?.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 1
        d.state.projectedState.getPower(theirBears) shouldBe 1
    }
})
