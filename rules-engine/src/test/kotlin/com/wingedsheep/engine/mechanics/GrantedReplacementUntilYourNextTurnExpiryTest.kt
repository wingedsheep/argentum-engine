package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.CleanupPhaseManager
import com.wingedsheep.engine.event.GrantedReplacementEffect
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A replacement effect granted "until your next turn" (Arlinn, the Pack's Hope's +1) wears off on
 * the post-untap hook of its controller's next turn — not the opponent's, and not at cleanup.
 */
class GrantedReplacementUntilYourNextTurnExpiryTest : FunSpec({

    test("an UntilYourNextTurn grant expires on its controller's next turn only") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val anchor = driver.putCreatureOnBattlefield(me, "Centaur Courser")

        fun grant(duration: Duration) = GrantedReplacementEffect(
            entityId = anchor,
            controllerId = me,
            replacement = EntersWithCounters(
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                count = 1,
                appliesTo = EventPattern.ZoneChangeEvent(
                    filter = GameObjectFilter.Creature.youControl(),
                    to = Zone.BATTLEFIELD
                )
            ),
            duration = duration
        )
        val untilNextTurn = grant(Duration.UntilYourNextTurn)
        val permanent = grant(Duration.Permanent)
        val state = driver.state.copy(grantedReplacementEffects = listOf(untilNextTurn, permanent))
        val cleanup = CleanupPhaseManager(
            driver.cardRegistry, DecisionHandler(), conditionEvaluator = driver.services.conditionEvaluator
        )

        cleanup.cleanupEndOfTurn(state).grantedReplacementEffects shouldBe listOf(untilNextTurn, permanent)
        cleanup.expireUntilYourNextTurnEffects(state, opponent).grantedReplacementEffects shouldBe
            listOf(untilNextTurn, permanent)
        cleanup.expireUntilYourNextTurnEffects(state, me).grantedReplacementEffects shouldBe listOf(permanent)
    }
})
