package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.SolsticeZealot
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Solstice Zealot (MH3) — two energy on entry; {T}, pay {E}: tap target creature.
 */
class SolsticeZealotScenarioTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + SolsticeZealot)
        it.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.energy(): Int =
        state.getEntity(player1)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0
    fun GameTestDriver.giveEnergy(n: Int) = replaceState(state.updateEntity(player1) {
        it.with(CountersComponent(mapOf(CounterType.ENERGY to n)))
    })
    val ability = SolsticeZealot.activatedAbilities.single().id
    fun GameTestDriver.activate(zealot: EntityId, target: EntityId) =
        submit(ActivateAbility(player1, zealot, ability, targets = listOf(ChosenTarget.Permanent(target))))

    test("entering gives two energy") {
        val d = driver()
        val card = d.putCardInHand(d.player1, "Solstice Zealot")
        d.giveMana(d.player1, Color.WHITE, 3)
        d.castSpell(d.player1, card).error shouldBe null
        d.bothPass()
        d.bothPass()
        d.energy() shouldBe 2
    }

    test("tapping itself and paying one energy taps an opponent's creature") {
        val d = driver()
        val zealot = d.putCreatureOnBattlefield(d.player1, "Solstice Zealot")
        d.removeSummoningSickness(zealot)
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.giveEnergy(2)

        d.activate(zealot, bears).error shouldBe null
        d.isTapped(zealot) shouldBe true
        d.energy() shouldBe 1
        d.bothPass()

        d.isTapped(bears) shouldBe true
    }

    test("needs energy, an untapped Zealot, and no summoning sickness") {
        val d = driver()
        val zealot = d.putCreatureOnBattlefield(d.player1, "Solstice Zealot")
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")

        d.giveEnergy(1)
        withClue("summoning sick creature can't pay {T}") { d.activate(zealot, bears).error shouldNotBe null }

        d.removeSummoningSickness(zealot)
        d.giveEnergy(0)
        withClue("no energy can't pay {E}") { d.activate(zealot, bears).error shouldNotBe null }
        d.isTapped(zealot) shouldBe false
        d.isTapped(bears) shouldBe false
    }
})
