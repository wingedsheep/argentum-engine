package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m21.cards.TemperedVeteran
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tempered Veteran (M21) —
 * {W}, {T}: Put a +1/+1 counter on target creature with a +1/+1 counter on it.
 * {4}{W}{W}, {T}: Put a +1/+1 counter on target creature.
 */
class TemperedVeteranScenarioTest : FunSpec({

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + TemperedVeteran)
        initMirrorMatch(deck = Deck.of("Plains" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    val cheap = TemperedVeteran.activatedAbilities[0].id
    val expensive = TemperedVeteran.activatedAbilities[1].id

    test("{W}, {T} only targets a creature that already has a +1/+1 counter") {
        val d = setup()
        val you = d.activePlayer!!
        val veteran = d.putCreatureOnBattlefield(you, "Tempered Veteran")
        d.removeSummoningSickness(veteran)
        val courser = d.putCreatureOnBattlefield(you, "Centaur Courser")

        d.giveMana(you, Color.WHITE, 1)
        d.submit(
            ActivateAbility(playerId = you, sourceId = veteran, abilityId = cheap, targets = listOf(ChosenTarget.Permanent(courser)))
        ).error shouldNotBe null
        d.isTapped(veteran) shouldBe false

        d.addComponent(courser, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
        d.submit(
            ActivateAbility(playerId = you, sourceId = veteran, abilityId = cheap, targets = listOf(ChosenTarget.Permanent(courser)))
        ).error shouldBe null
        d.bothPass()

        d.isTapped(veteran) shouldBe true
        d.plusOnes(courser) shouldBe 2
    }

    test("{4}{W}{W}, {T} puts a counter on any creature") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        val veteran = d.putCreatureOnBattlefield(you, "Tempered Veteran")
        d.removeSummoningSickness(veteran)
        val enemy = d.putCreatureOnBattlefield(opponent, "Centaur Courser")

        d.giveMana(you, Color.WHITE, 2)
        d.giveColorlessMana(you, 4)
        d.submit(
            ActivateAbility(playerId = you, sourceId = veteran, abilityId = expensive, targets = listOf(ChosenTarget.Permanent(enemy)))
        ).error shouldBe null
        d.bothPass()

        d.plusOnes(enemy) shouldBe 1
    }
})
