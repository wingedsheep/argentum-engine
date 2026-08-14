package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.AdditionalPhasesComponent
import com.wingedsheep.engine.state.components.player.ExtraPhaseKind
import com.wingedsheep.engine.state.components.player.QueuedPhase
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dft.cards.FullThrottle
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Full Throttle and its repeating beginning-of-combat delayed trigger. */
class FullThrottleScenarioTest : FunSpec({

    val testDriver = card("Full Throttle Test Driver") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Human Pilot"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FullThrottle, testDriver))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard < 50) {
            bothPass()
            guard++
        }
    }

    test("adds two combats and untaps prior attackers at each additional combat") {
        val driver = createDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)
        val creature = driver.putCreatureOnBattlefield(attacker, "Full Throttle Test Driver")
        driver.removeSummoningSickness(creature)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(attacker, Color.RED, 6)
        val fullThrottle = driver.putCardInHand(attacker, "Full Throttle")
        driver.castSpell(attacker, fullThrottle).isSuccess shouldBe true
        driver.resolveStack()

        driver.state.getEntity(attacker)?.get<AdditionalPhasesComponent>() shouldBe
            AdditionalPhasesComponent(
                listOf(QueuedPhase(ExtraPhaseKind.COMBAT), QueuedPhase(ExtraPhaseKind.COMBAT))
            )
        driver.state.delayedTriggers.single().repeatAtEachMatchingStep shouldBe true

        // Natural combat: attack and remain tapped through the rest of this combat.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(creature), defender)
        driver.resolveStack()
        driver.state.getEntity(creature)?.has<TappedComponent>() shouldBe true

        // First additional combat: the delayed trigger untaps the creature at beginning of combat.
        driver.passPriorityUntil(Step.END_COMBAT)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.state.getEntity(creature)?.has<TappedComponent>() shouldBe false
        driver.state.delayedTriggers.size shouldBe 1

        driver.declareAttackers(attacker, listOf(creature), defender)
        driver.resolveStack()
        driver.state.getEntity(creature)?.has<TappedComponent>() shouldBe true

        // Second additional combat: the same resident delayed trigger fires again.
        driver.passPriorityUntil(Step.END_COMBAT)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.state.getEntity(creature)?.has<TappedComponent>() shouldBe false
    }

    test("repeating combat trigger expires at end of turn") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(caster, Color.RED, 6)
        driver.castSpell(caster, driver.putCardInHand(caster, "Full Throttle"))
        driver.resolveStack()
        driver.state.delayedTriggers.size shouldBe 1

        val startTurn = driver.state.turnNumber
        var guard = 0
        while (driver.state.turnNumber == startTurn && guard < 400) {
            driver.bothPass()
            guard++
        }

        driver.state.delayedTriggers.size shouldBe 0
    }
})
