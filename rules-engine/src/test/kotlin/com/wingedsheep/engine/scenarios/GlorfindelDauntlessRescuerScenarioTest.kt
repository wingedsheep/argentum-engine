package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blc.cards.TempleOfMystery
import com.wingedsheep.mtg.sets.definitions.ltr.cards.GlorfindelDauntlessRescuer
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Glorfindel, Dauntless Rescuer — "Whenever you scry, choose one and Glorfindel gets +1/+1 until
 * end of turn. • must be blocked if able • can't be blocked by more than one creature each combat."
 *
 * Exercises Gap 39: choosing mode 2 grants the floating CANT_BE_BLOCKED_BY_MORE_THAN_ONE flag,
 * which BlockPhaseManager now honors (caps blockers at one).
 */
class GlorfindelDauntlessRescuerScenarioTest : FunSpec({

    test("scry mode 2 grants +1/+1 and caps Glorfindel's blockers at one") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GlorfindelDauntlessRescuer, TempleOfMystery))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != active }

        val glorfindel = driver.putCreatureOnBattlefield(active, "Glorfindel, Dauntless Rescuer")
        // Two would-be blockers for the opponent.
        val blockerA = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val blockerB = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        // Trigger "you scry" via Temple of Mystery's ETB scry 1.
        val temple = driver.putCardInHand(active, "Temple of Mystery")
        driver.playLand(active, temple)

        // Drain the scry decisions, then choose Glorfindel's mode 2 (option index 1 = can't be
        // blocked by more than one creature).
        var guard = 0
        while (guard++ < 16) {
            when (val pd = driver.pendingDecision) {
                is SelectCardsDecision -> driver.submitCardSelection(active, emptyList())
                is ReorderLibraryDecision -> driver.submitOrderedResponse(active, pd.cards)
                is YesNoDecision -> driver.submitYesNo(active, true)
                is ChooseOptionDecision -> driver.submitDecision(active, OptionChosenResponse(pd.id, 1))
                else -> if (driver.state.stack.isNotEmpty()) driver.bothPass() else break
            }
        }

        // +1/+1 applied (base 3/2 -> 4/3) and the can't-be-blocked-by-more-than-one flag granted.
        driver.state.projectedState.getPower(glorfindel) shouldBe 4
        driver.state.projectedState.getToughness(glorfindel) shouldBe 3
        driver.state.projectedState
            .hasKeyword(glorfindel, AbilityFlag.CANT_BE_BLOCKED_BY_MORE_THAN_ONE) shouldBe true

        // Attack with Glorfindel this turn; the opponent may not assign two blockers to it.
        driver.removeSummoningSickness(glorfindel)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(glorfindel), opponent).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(
            opponent,
            mapOf(blockerA to listOf(glorfindel), blockerB to listOf(glorfindel))
        ).error shouldNotBe null
    }
})
