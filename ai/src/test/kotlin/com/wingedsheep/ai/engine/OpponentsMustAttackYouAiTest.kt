package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.xln.cards.TroveOfTemptation
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * The AI under Trove of Temptation ("each opponent must attack you … with at least one creature
 * each combat if able"). The requirement names no creature, so it never reaches the AI as
 * `mandatoryAttackers`; facing an 8/8 the combat advisor's plan is "stay home", which the engine
 * rejects. [AttackRequirementRepair] sends the cheapest creature in instead.
 */
class OpponentsMustAttackYouAiTest : FunSpec({

    test("attacks the Trove's controller with one creature even when attacking is a bad trade") {
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all + listOf(TroveOfTemptation, PredefinedTokens.Treasure))
            initMirrorMatch(Deck.of("Forest" to 40))
        }
        val p1 = driver.player1
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val lions = driver.putCreatureOnBattlefield(p1, "Savannah Lions")
        val courser = driver.putCreatureOnBattlefield(p1, "Centaur Courser")
        driver.removeSummoningSickness(lions)
        driver.removeSummoningSickness(courser)
        driver.putCreatureOnBattlefield(p2, "Force of Nature")
        driver.putPermanentOnBattlefield(p2, "Trove of Temptation")
        val turn = driver.state.turnNumber
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.state.turnNumber shouldBe turn

        val attackAction = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, p1)
            .single { it.actionType == "DeclareAttackers" }
        val ai = AIPlayer.create(driver.cardRegistry, p1)
        val chosen = ai.chooseFrom(driver.state, listOf(attackAction)).action as DeclareAttackers

        chosen.attackers.values shouldContain p2
        driver.submit(chosen).error shouldBe null
    }
})
