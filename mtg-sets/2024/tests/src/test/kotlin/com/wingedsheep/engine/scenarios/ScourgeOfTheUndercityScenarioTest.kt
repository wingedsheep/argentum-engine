package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.j25.cards.ScourgeOfTheUndercity
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scourge of the Undercity (J25 #11) — "Lifelink. When this creature enters, another target creature
 * you control gains lifelink until end of turn."
 */
class ScourgeOfTheUndercityScenarioTest : FunSpec({

    test("the enters trigger grants lifelink to another creature you control") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + ScourgeOfTheUndercity)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.putCreatureOnBattlefield(d.player2, "Centaur Courser")

        val card = d.putCardInHand(d.player1, "Scourge of the Undercity")
        d.giveMana(d.player1, Color.BLACK, 2)
        d.castSpell(d.player1, card).outcome shouldBe Outcome.Done
        d.bothPass() // Scourge resolves; enters trigger announces its target

        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        withClue("neither the Scourge itself nor an opponent's creature is legal") {
            decision.legalTargets.getValue(0) shouldContainExactly listOf(bears)
        }
        d.submitTargetSelection(d.player1, listOf(bears)).error shouldBe null
        d.bothPass()

        d.state.projectedState.hasKeyword(bears, Keyword.LIFELINK) shouldBe true
        d.state.projectedState.hasKeyword(card, Keyword.LIFELINK) shouldBe true
    }
})
