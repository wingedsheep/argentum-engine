package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.VisageBandit
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Visage Bandit — {3}{U} 2/2 Creature — Shapeshifter Rogue.
 *
 * "You may have this creature enter as a copy of a creature you control, except it's a
 * Shapeshifter Rogue in addition to its other types."
 * Plot {2}{U}.
 *
 * Verifies the restricted EntersAsCopy: only creatures *you control* are offered, the copy
 * picks up the source's P/T, and Shapeshifter + Rogue are added to its subtypes. Declining
 * leaves it a plain 2/2 Visage Bandit.
 */
class VisageBanditScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(VisageBandit)
        return driver
    }

    test("enters as a copy of a creature you control, gaining Shapeshifter and Rogue") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val giant = driver.putCreatureOnBattlefield(me, "Hill Giant") // 3/3 I control
        val theirGiant = driver.putCreatureOnBattlefield(opp, "Hill Giant") // not copyable

        val bandit = driver.putCardInHand(me, "Visage Bandit")
        driver.giveMana(me, Color.BLUE, 4)
        driver.castSpell(me, bandit)
        driver.bothPass()

        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        // Only my creature is offered — the opponent's is excluded by "you control".
        decision.options shouldContain giant
        decision.options shouldNotContain theirGiant

        driver.submitCardSelection(me, listOf(giant))

        // Now a 3/3 Hill Giant copy.
        projector.getProjectedPower(driver.state, bandit) shouldBe 3
        projector.getProjectedToughness(driver.state, bandit) shouldBe 3
        val card = driver.state.getEntity(bandit)?.get<CardComponent>()!!
        card.name shouldBe "Hill Giant"

        // Plus Shapeshifter and Rogue in addition to its copied types.
        val projected = projector.project(driver.state)
        val subtypes = projected.getSubtypes(bandit)
        subtypes shouldContain "Shapeshifter"
        subtypes shouldContain "Rogue"
        subtypes shouldContain "Giant"
    }

    test("declining leaves it a plain 2/2 Visage Bandit") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(me, "Hill Giant")

        val bandit = driver.putCardInHand(me, "Visage Bandit")
        driver.giveMana(me, Color.BLUE, 4)
        driver.castSpell(me, bandit)
        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(me, emptyList())

        driver.state.getBattlefield() shouldContain bandit
        projector.getProjectedPower(driver.state, bandit) shouldBe 2
        projector.getProjectedToughness(driver.state, bandit) shouldBe 2
        driver.state.getEntity(bandit)?.get<CardComponent>()!!.name shouldBe "Visage Bandit"
    }
})
