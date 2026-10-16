package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.SentryBot
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sentry Bot (PIP) — flash; costs {1} less for each creature attacking you; enters giving {E} for
 * each creature attacking you; at the beginning of combat on your turn, may pay {E}{E}{E} to put a
 * +1/+1 counter on each creature you control.
 *
 * Pins the self-cast dynamic reduction reading "attacking you" against the caster mid-combat, the
 * same count feeding the energy trigger, and the exact-energy may-pay gating the group counters.
 */
class SentryBotScenarioTest : FunSpec({

    fun driver(startingPlayer: Int) = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(SentryBot))
        it.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = startingPlayer)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.energy(player: EntityId): Int =
        state.getEntity(player)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    fun GameTestDriver.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.seedEnergy(player: EntityId, amount: Int) {
        replaceState(
            state.updateEntity(player) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.ENERGY, amount))
            }
        )
    }

    test("two attackers make it cost {2}{W} at flash speed and give two energy") {
        val d = driver(startingPlayer = 1)
        val bears1 = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val bears2 = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.removeSummoningSickness(bears1)
        d.removeSummoningSickness(bears2)
        val spell = d.putCardInHand(d.player1, SentryBot.name)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player2, listOf(bears1, bears2), d.player1).error shouldBe null
        d.passPriority(d.player2)

        d.giveMana(d.player1, Color.WHITE, 1)
        d.giveColorlessMana(d.player1, 2)
        d.castSpell(d.player1, spell).error shouldBe null
        d.bothPass() // resolve Sentry Bot
        d.bothPass() // resolve the enters trigger
        d.energy(d.player1) shouldBe 2
    }

    test("with no creature attacking you it costs the full {4}{W}") {
        val d = driver(startingPlayer = 0)
        val spell = d.putCardInHand(d.player1, SentryBot.name)
        d.giveMana(d.player1, Color.WHITE, 1)
        d.giveColorlessMana(d.player1, 2)
        d.castSpell(d.player1, spell).error shouldNotBe null
    }

    test("paying three energy at beginning of combat puts a +1/+1 counter on each creature you control") {
        val d = driver(startingPlayer = 0)
        val bot = d.putCreatureOnBattlefield(d.player1, SentryBot.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val theirBears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.seedEnergy(d.player1, 4)

        d.passPriorityUntil(Step.BEGIN_COMBAT)
        d.bothPass() // resolve the beginning-of-combat trigger
        d.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, true).error shouldBe null

        d.energy(d.player1) shouldBe 1
        d.plusOnes(bot) shouldBe 1
        d.plusOnes(bears) shouldBe 1
        d.plusOnes(theirBears) shouldBe 0
    }

    test("with fewer than three energy the payment isn't offered") {
        val d = driver(startingPlayer = 0)
        val bot = d.putCreatureOnBattlefield(d.player1, SentryBot.name)
        d.seedEnergy(d.player1, 2)

        d.passPriorityUntil(Step.BEGIN_COMBAT)
        d.bothPass()
        (d.state.pendingDecision is YesNoDecision) shouldBe false
        d.energy(d.player1) shouldBe 2
        d.plusOnes(bot) shouldBe 0
    }
})
