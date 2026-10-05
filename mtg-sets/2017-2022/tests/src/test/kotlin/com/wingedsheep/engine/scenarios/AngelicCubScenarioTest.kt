package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.gpt.cards.GhostWarden
import com.wingedsheep.mtg.sets.definitions.j22.cards.AngelicCub
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Angelic Cub — {1}{W} Creature — Cat Angel, 1/1 (J22).
 *
 *   Whenever this creature becomes the target of a spell or ability for the first time each turn,
 *   put a +1/+1 counter on it.
 *   As long as this creature has three or more +1/+1 counters on it, it has flying.
 *
 * Targeting is driven by Ghost Warden ("{T}: Target creature gets +1/+1 until end of turn"), one
 * per targeting, so the counter count is read apart from the pump.
 */
class AngelicCubScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AngelicCub, GhostWarden))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.readyWarden(owner: EntityId): EntityId =
        putCreatureOnBattlefield(owner, "Ghost Warden").also { removeSummoningSickness(it) }

    fun GameTestDriver.wardenTargets(player: EntityId, warden: EntityId, target: EntityId) = submit(
        ActivateAbility(
            playerId = player,
            sourceId = warden,
            abilityId = GhostWarden.activatedAbilities.first().id,
            targets = listOf(ChosenTarget.Permanent(target))
        )
    )

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 50) {
            if (state.pendingDecision != null) autoResolveDecision() else bothPass()
        }
    }

    test("the first targeting this turn puts a +1/+1 counter on it, resolving before the ability") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val cub = driver.putCreatureOnBattlefield(me, "Angelic Cub")
        val warden = driver.readyWarden(me)

        driver.wardenTargets(me, warden, cub).error shouldBe null
        // The trigger goes on the stack above Ghost Warden's ability and resolves first.
        driver.bothPass()
        driver.plusOneCounters(cub) shouldBe 1
        driver.state.stack.size shouldBe 1
        driver.resolveStack()

        driver.plusOneCounters(cub) shouldBe 1
        driver.state.projectedState.getPower(cub) shouldBe 3 // 1 base + counter + Warden's +1/+1
    }

    test("a second targeting the same turn doesn't trigger again") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val cub = driver.putCreatureOnBattlefield(me, "Angelic Cub")
        val first = driver.readyWarden(me)
        val second = driver.readyWarden(me)

        driver.wardenTargets(me, first, cub).error shouldBe null
        driver.resolveStack()
        driver.wardenTargets(me, second, cub).error shouldBe null
        driver.resolveStack()

        driver.plusOneCounters(cub) shouldBe 1
    }

    test("an opponent's targeting counts as the first time — unlike valiant, it isn't scoped to you") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val cub = driver.putCreatureOnBattlefield(me, "Angelic Cub")
        val mine = driver.readyWarden(me)
        val theirs = driver.readyWarden(opponent)

        driver.passPriority(me)
        driver.wardenTargets(opponent, theirs, cub).error shouldBe null
        driver.resolveStack()
        driver.plusOneCounters(cub) shouldBe 1

        // Priority can land on the resolved ability's controller; hand it back to the active player.
        driver.state.priorityPlayerId?.takeIf { it != me }?.let { driver.passPriority(it) }
        driver.wardenTargets(me, mine, cub).error shouldBe null
        driver.resolveStack()
        driver.plusOneCounters(cub) shouldBe 1
    }

    test("it triggers again on a later turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val cub = driver.putCreatureOnBattlefield(me, "Angelic Cub")
        val theirs = driver.readyWarden(opponent)

        driver.passPriority(me)
        driver.wardenTargets(opponent, theirs, cub).error shouldBe null
        driver.resolveStack()
        driver.plusOneCounters(cub) shouldBe 1

        driver.passPriorityUntil(Step.UPKEEP)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.activePlayer shouldBe opponent
        driver.wardenTargets(opponent, theirs, cub).error shouldBe null
        driver.resolveStack()
        driver.plusOneCounters(cub) shouldBe 2
    }

    test("it has flying only with three or more +1/+1 counters") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val cub = driver.putCreatureOnBattlefield(me, "Angelic Cub")
        fun withCounters(n: Int) = driver.replaceState(
            driver.state.updateEntity(cub) { it.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, n)) }
        )

        driver.state.projectedState.hasKeyword(cub, Keyword.FLYING) shouldBe false
        withCounters(2)
        driver.state.projectedState.hasKeyword(cub, Keyword.FLYING) shouldBe false
        withCounters(3)
        driver.state.projectedState.hasKeyword(cub, Keyword.FLYING) shouldBe true
    }
})
