package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.mtg.sets.definitions.spe.cards.PumpkinBombs
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Pumpkin Bombs
 * {1}{R}
 * Artifact
 * {T}, Discard two cards: Draw three cards, then put a fuse counter on this artifact. It deals
 * damage equal to the number of fuse counters on it to target opponent. They gain control of this
 * artifact.
 */
class PumpkinBombsScenarioTest : FunSpec({

    val abilityId = PumpkinBombs.activatedAbilities.first().id
    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(PumpkinBombs)
        return driver
    }

    fun GameTestDriver.fuseCounters(bombs: EntityId): Int =
        state.getEntity(bombs)?.get<CountersComponent>()?.getCount(CounterType.FUSE) ?: 0

    fun GameTestDriver.activate(player: EntityId, bombs: EntityId, opponent: EntityId) {
        val hand = getHand(player)
        val result = submit(
            ActivateAbility(
                playerId = player,
                sourceId = bombs,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Player(opponent)),
                costPayment = AdditionalCostPayment(discardedCards = hand.take(2))
            )
        )
        result.outcome shouldBe Outcome.Done
        bothPass()
    }

    test("draws three, adds a fuse counter, deals that much damage and changes hands") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bombs = driver.putPermanentOnBattlefield(alice, "Pumpkin Bombs")
        val handBefore = driver.getHandSize(alice)

        driver.activate(alice, bombs, bob)

        driver.getHandSize(alice) shouldBe handBefore - 2 + 3
        driver.fuseCounters(bombs) shouldBe 1
        driver.getLifeTotal(bob) shouldBe 19
        projector.project(driver.state).getController(bombs) shouldBe bob
    }

    test("the fuse counters stay on the bomb, so the next activation hits harder") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bombs = driver.putPermanentOnBattlefield(alice, "Pumpkin Bombs")
        driver.activate(alice, bombs, bob)

        // Bob now controls the tapped bomb; untap it and let him fire it back while he has priority.
        driver.replaceState(driver.state.updateEntity(bombs) { it.without<TappedComponent>() })
        driver.passPriority(alice)
        driver.activate(bob, bombs, alice)

        driver.fuseCounters(bombs) shouldBe 2
        driver.getLifeTotal(alice) shouldBe 18
        projector.project(driver.state).getController(bombs) shouldBe alice
    }
})
