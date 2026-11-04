package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Abandoned Sarcophagus: cast spells with cycling from your graveyard; a card with cycling that
 * would be put into your graveyard and wasn't cycled is exiled instead.
 */
class AbandonedSarcophagusScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Plains" to 30), startingLife = 20, skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putPermanentOnBattlefield(d.player1, "Abandoned Sarcophagus")
        return d
    }

    fun GameTestDriver.graveyardActions(player: EntityId, cardId: EntityId) = legalActions(player)
        .filter { legal ->
            legal.sourceZone == "GRAVEYARD" && when (val a = legal.action) {
                is CastSpell -> a.cardId == cardId
                is PlayLand -> a.cardId == cardId
                else -> false
            }
        }

    test("a cycled card reaches the graveyard, and can then be cast from there — and is exiled as it resolves") {
        val d = driver()
        val faith = d.putCardInHand(d.player1, "Renewed Faith")
        d.giveMana(d.player1, Color.WHITE, 2)
        d.submit(CycleCard(d.player1, faith)).error shouldBe null
        // "When you cycle Renewed Faith, you may gain 2 life" — decline.
        while (d.pendingDecision != null) d.submitYesNo(d.player1, false)
        while (d.state.stack.isNotEmpty()) {
            d.bothPass()
            while (d.pendingDecision != null) d.submitYesNo(d.player1, false)
        }
        d.getGraveyard(d.player1) shouldContain faith

        d.graveyardActions(d.player1, faith).shouldNotBeEmpty()
        d.giveMana(d.player1, Color.WHITE, 3)
        d.castSpell(d.player1, faith).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 26
        d.getExile(d.player1) shouldContain faith
        d.getGraveyard(d.player1) shouldNotContain faith
    }

    test("a creature with cycling that dies is exiled; the opponent's goes to their graveyard") {
        val d = driver()
        val mine = d.putCreatureOnBattlefield(d.player1, "Angel of the God-Pharaoh")
        val theirs = d.putCreatureOnBattlefield(d.player2, "Angel of the God-Pharaoh")
        repeat(2) {
            val murder = d.putCardInHand(d.player1, "Murder")
            d.giveMana(d.player1, Color.BLACK, 3)
            d.castSpell(d.player1, murder, listOf(if (it == 0) mine else theirs)).outcome shouldBe Outcome.Done
            d.bothPass()
        }
        d.getExile(d.player1) shouldContain mine
        d.getGraveyard(d.player1) shouldNotContain mine
        d.getGraveyard(d.player2) shouldContain theirs
    }

    test("it grants casting only — a land with cycling can't be played from the graveyard") {
        val d = driver()
        val desert = d.putCardInGraveyard(d.player1, "Desert of the True")
        d.graveyardActions(d.player1, desert).shouldBeEmpty()
    }
})
