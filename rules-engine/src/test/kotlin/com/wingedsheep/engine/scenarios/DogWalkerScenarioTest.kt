package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.handlers.effects.FaceDownTurnUp
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.DogWalker
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Dog Walker — {R}{W} 3/1 with vigilance, Disguise {R/W}{R/W}, and "when this creature is turned
 * face up, create two tapped 1/1 white Dog creature tokens."
 *
 * The Dogs are the disguise payoff, so the two lines are not equivalent: hard-casting for {R}{W}
 * deliberately gets nothing.
 */
class DogWalkerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DogWalker))
        return driver
    }

    fun GameTestDriver.dogs(playerId: com.wingedsheep.sdk.model.EntityId) =
        getCreatures(playerId).filter { getCardName(it) == "Dog Token" }

    test("hard-cast: a 3/1 with vigilance and no Dogs") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(player, "Dog Walker")
        driver.giveMana(player, Color.RED, 1)
        driver.giveMana(player, Color.WHITE, 1)

        driver.castSpell(player, card).error shouldBe null
        driver.bothPass()

        val walker = driver.findPermanent(player, "Dog Walker")
        walker.shouldNotBeNull()
        driver.state.projectedState.getPower(walker) shouldBe 3
        driver.state.projectedState.getToughness(walker) shouldBe 1
        driver.state.projectedState.hasKeyword(walker, Keyword.VIGILANCE) shouldBe true
        // The trigger is "is turned face up", not "enters" — entering face up produces nothing.
        driver.dogs(player).size shouldBe 0
    }

    test("turning it face up creates two tapped 1/1 Dogs") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val walker = driver.putCreatureOnBattlefield(player, "Dog Walker")
        val cardDef = driver.cardRegistry.requireCard("Dog Walker")
        driver.replaceState(
            driver.state.updateEntity(walker) { container ->
                var c = container.with(FaceDownComponent)
                    .with(FaceDownModeComponent(FaceDownMode.DISGUISE))
                FaceDownTurnUp.dataFor(cardDef, "Dog Walker", FaceDownMode.DISGUISE)
                    ?.let { c = c.with(it) }
                c
            }
        )
        driver.removeSummoningSickness(walker)

        // Disguise {R/W}{R/W} — two red pays both hybrid halves.
        driver.giveMana(player, Color.RED, 2)
        driver.submit(
            TurnFaceUp(
                playerId = player,
                sourceId = walker,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        repeat(2) { if (driver.state.priorityPlayerId != null && !driver.isPaused) driver.bothPass() }

        val dogs = driver.dogs(player)
        dogs.size shouldBe 2
        dogs.all { driver.isTapped(it) } shouldBe true
        driver.state.projectedState.getPower(dogs.first()) shouldBe 1
        driver.state.projectedState.getToughness(dogs.first()) shouldBe 1
    }
})
