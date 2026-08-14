package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.woe.cards.TheApprenticesFolly
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * The Apprentice's Folly (WOE #200).
 *
 * Proves the state-dependent chapter target restriction: a nontoken creature is excluded when its
 * name is already represented by a token the Saga's controller controls. It also pins the printed
 * copy exceptions on a legal target (nonlegendary Reflection with haste).
 */
class TheApprenticesFollyScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TheApprenticesFolly))
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Mountain" to 20),
            startingPlayer = 0,
            skipMulligans = true,
        )
        return driver
    }

    test("chapter excludes a creature whose name is shared with your token and creates the Reflection copy") {
        val driver = driver()
        val controller = driver.activePlayer!!

        val excludedBear = driver.putCreatureOnBattlefield(controller, "Grizzly Bears")
        driver.putCreatureOnBattlefield(controller, "Grizzly Bears").also { token ->
            driver.replaceState(driver.state.updateEntity(token) { it.with(TokenComponent) })
        }
        val legalCreature = driver.putCreatureOnBattlefield(controller, "Hill Giant")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val saga = driver.putCardInHand(controller, TheApprenticesFolly.name)
        driver.giveMana(controller, Color.BLUE, 3)
        driver.giveMana(controller, Color.RED, 1)
        driver.castSpell(controller, saga).error shouldBe null
        driver.bothPass()

        val decision = driver.pendingDecision as ChooseTargetsDecision
        val legalTargets = decision.legalTargets[0].orEmpty()
        legalTargets shouldNotContain excludedBear
        legalTargets shouldContain legalCreature

        driver.submitTargetSelection(controller, listOf(legalCreature)).error shouldBe null
        while (driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()

        val reflection = driver.getPermanents(controller).single { id ->
            driver.state.getEntity(id)?.get<CardComponent>()?.name == "Hill Giant" &&
                driver.state.getEntity(id)?.has<TokenComponent>() == true
        }
        driver.state.projectedState.hasSubtype(reflection, "Reflection") shouldBe true
        driver.state.projectedState.hasKeyword(reflection, Keyword.HASTE) shouldBe true
        driver.state.projectedState.isLegendary(reflection) shouldBe false
    }
})
