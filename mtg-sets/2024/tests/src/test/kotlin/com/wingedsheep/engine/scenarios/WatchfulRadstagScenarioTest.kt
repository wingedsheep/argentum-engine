package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.Battlegrowth
import com.wingedsheep.mtg.sets.definitions.pip.cards.WatchfulRadstag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Watchful Radstag — evolving creates a token copy of it (without the counter); a +1/+1 counter
 * from another source, or a creature entering that isn't bigger, copies nothing.
 */
class WatchfulRadstagScenarioTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + WatchfulRadstag + Battlegrowth)
        initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.drain() {
        var guard = 0
        while ((state.stack.isNotEmpty() || pendingDecision != null) && guard++ < 40) {
            if (pendingDecision != null) autoResolveDecision() else bothPass()
        }
    }

    fun GameTestDriver.plusOnes(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.radstags() =
        getPermanents(player1).filter { getCardName(it) == WatchfulRadstag.name }

    test("evolving creates a token copy of Watchful Radstag without its counter") {
        val d = setup()
        val radstag = d.putCreatureOnBattlefield(d.player1, WatchfulRadstag.name)

        val courser = d.putCardInHand(d.player1, "Centaur Courser") // 3/3 is bigger than 2/2
        d.giveMana(d.player1, Color.GREEN, 1)
        d.giveColorlessMana(d.player1, 2)
        d.castSpell(d.player1, courser).error shouldBe null
        d.drain()

        d.plusOnes(radstag) shouldBe 1
        val copies = d.radstags().filter { it != radstag }
        copies.size shouldBe 1
        val copy = copies.single()
        d.state.getEntity(copy)?.has<TokenComponent>() shouldBe true
        d.plusOnes(copy) shouldBe 0
        d.state.projectedState.getPower(copy) shouldBe 2
    }

    test("a +1/+1 counter from another source isn't an evolve — no copy") {
        val d = setup()
        val radstag = d.putCreatureOnBattlefield(d.player1, WatchfulRadstag.name)

        val growth = d.putCardInHand(d.player1, Battlegrowth.name)
        d.giveMana(d.player1, Color.GREEN, 1)
        d.castSpell(d.player1, growth, targets = listOf(radstag)).error shouldBe null
        d.drain()

        d.plusOnes(radstag) shouldBe 1
        d.radstags().size shouldBe 1
    }

    test("a creature that isn't bigger doesn't evolve it — no copy") {
        val d = setup()
        val radstag = d.putCreatureOnBattlefield(d.player1, WatchfulRadstag.name)

        val lions = d.putCardInHand(d.player1, "Savannah Lions") // 1/1
        d.giveMana(d.player1, Color.WHITE, 1)
        d.castSpell(d.player1, lions).error shouldBe null
        d.drain()

        d.plusOnes(radstag) shouldBe 0
        d.radstags().size shouldBe 1
    }
})
