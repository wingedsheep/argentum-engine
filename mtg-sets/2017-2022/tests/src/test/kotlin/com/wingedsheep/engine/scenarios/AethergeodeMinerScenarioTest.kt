package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.aer.cards.AethergeodeMiner
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Aethergeode Miner (AER) — attacking gets {E}{E}; pay {E}{E} to blink itself. The returned Miner
 * is a new object (CR 400.7), so blinking an attacking Miner takes it out of combat.
 */
class AethergeodeMinerScenarioTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + AethergeodeMiner)
        it.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.energy(): Int =
        state.getEntity(player1)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0
    fun GameTestDriver.giveEnergy(n: Int) = replaceState(state.updateEntity(player1) {
        it.with(CountersComponent(mapOf(CounterType.ENERGY to n)))
    })
    val blink = AethergeodeMiner.activatedAbilities.single().id
    fun GameTestDriver.activateBlink(miner: EntityId) = submit(ActivateAbility(player1, miner, blink))

    test("attacking gets two energy, and paying it blinks the Miner out of combat") {
        val d = driver()
        val miner = d.putCreatureOnBattlefield(d.player1, "Aethergeode Miner")
        d.removeSummoningSickness(miner)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(miner), d.player2).error shouldBe null
        d.bothPass()
        withClue("the attack trigger gives {E}{E}") { d.energy() shouldBe 2 }
        d.state.getEntity(miner)?.has<AttackingComponent>() shouldBe true

        d.activateBlink(miner).error shouldBe null
        withClue("the cost is paid on activation") { d.energy() shouldBe 0 }
        d.bothPass()

        val returned = d.findPermanent(d.player1, "Aethergeode Miner")
        withClue("the Miner is back on the battlefield") { returned shouldNotBe null }
        withClue("it is a new object, no longer attacking") {
            d.state.getEntity(returned!!)?.has<AttackingComponent>() shouldBe false
        }
        d.getExileCardNames(d.player1).contains("Aethergeode Miner") shouldBe false
    }

    test("the blink can't be activated without two energy") {
        val d = driver()
        val miner = d.putCreatureOnBattlefield(d.player1, "Aethergeode Miner")
        d.giveEnergy(1)

        withClue("one energy can't pay {E}{E}") { d.activateBlink(miner).error shouldNotBe null }
        d.energy() shouldBe 1
        d.findPermanent(d.player1, "Aethergeode Miner") shouldBe miner
    }
})
