package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bro.cards.TheFallOfKroog
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.wingedsheep.engine.state.components.stack.ChosenTarget

class DependentTargetFillAiTest : FunSpec({
    test("heuristic targeting chooses the opponent's land even if caster owns alternatives") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + TheFallOfKroog)
        d.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.giveMana(d.player1, Color.RED, 6)
        val spell = d.putCardInHand(d.player1, "The Fall of Kroog")
        d.putLandOnBattlefield(d.player1, "Mountain")
        val theirs = d.putLandOnBattlefield(d.player2, "Forest")
        val action = d.legalActions(d.player1).single { (it.action as? CastSpell)?.cardId == spell }
        val filled = TargetSelection.fillHeuristically(d.state, action, d.player1, fillPartialRequirements = false) as CastSpell
        filled.targets shouldBe listOf(ChosenTarget.Player(d.player2), ChosenTarget.Permanent(theirs))
        d.submit(filled).outcome shouldBe Outcome.Done
    }
})
