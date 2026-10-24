package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.stx.cards.IngeniousMastery
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ingenious Mastery (STX #44): "You may pay {2}{U} rather than pay this spell's mana cost. If the
 * {2}{U} cost was paid, you draw three cards, then an opponent creates two Treasure tokens and
 * they scry 2. If that cost wasn't paid, you draw X cards."
 */
class IngeniousMasteryScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + IngeniousMastery + PredefinedTokens.Treasure)
        it.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.treasures(player: com.wingedsheep.sdk.model.EntityId): Int =
        state.getBattlefield(player).count { state.getEntity(it)?.get<CardComponent>()?.name == "Treasure" }

    /** Resolves everything, returning who was asked to choose cards (the scry's top/bottom split). */
    fun GameTestDriver.resolveAll(): List<com.wingedsheep.sdk.model.EntityId> {
        val selectors = mutableListOf<com.wingedsheep.sdk.model.EntityId>()
        repeat(20) {
            val decision = state.pendingDecision
            if (decision != null) {
                if (decision is SelectCardsDecision) selectors += decision.playerId
                autoResolveDecision()
            } else if (stackSize > 0) bothPass()
            else return selectors
        }
        error("stack did not finish resolving")
    }

    test("cast for {2}{U}: you draw three, the opponent gets two Treasures and scries 2") {
        val d = createDriver()
        val me = d.player1
        val opponent = d.player2
        val mastery = d.putCardInHand(me, "Ingenious Mastery")
        d.giveMana(me, Color.BLUE, 3)
        val myHand = d.getHand(me).size

        d.submit(CastSpell(me, mastery, useAlternativeCost = true,
            alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE)).error shouldBe null
        val selectors = d.resolveAll()

        selectors shouldBe listOf(opponent)
        d.getHand(me).size shouldBe myHand - 1 + 3
        d.treasures(opponent) shouldBe 2
        d.treasures(me) shouldBe 0
    }

    test("cast for {X}{2}{U}: you draw X cards and the opponent gets nothing") {
        val d = createDriver()
        val me = d.player1
        val opponent = d.player2
        val mastery = d.putCardInHand(me, "Ingenious Mastery")
        d.giveMana(me, Color.BLUE, 5)
        val myHand = d.getHand(me).size

        d.castXSpell(me, mastery, 2).error shouldBe null
        d.resolveAll()

        d.getHand(me).size shouldBe myHand - 1 + 2
        d.treasures(opponent) shouldBe 0
    }
})
