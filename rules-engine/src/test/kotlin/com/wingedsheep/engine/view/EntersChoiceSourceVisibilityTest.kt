package com.wingedsheep.engine.view

import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A permanent spell's "as this enters, choose …" (Sorcerous Spyglass, the Thriving lands) is asked
 * after the spell has left the stack and before it reaches the battlefield, so the card is in no
 * zone. [ClientStateTransformer] still projects it while it is the pending decision's source, so the
 * decider's prompt and every other seat's "X is making a choice" banner can show its card.
 */
class EntersChoiceSourceVisibilityTest : FunSpec({

    val chooser = card("Test Entering Chooser") {
        manaCost = "{U}"
        typeLine = "Artifact"
        replacementEffect(EntersWithChoice(ChoiceType.COLOR))
    }

    test("the resolving spell behind an as-enters choice is visible to both players") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + chooser)
        d.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val spellId = d.putCardInHand(d.player1, chooser.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpell(d.player1, spellId).error shouldBe null
        d.bothPass()

        val decision = d.state.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()
        decision.context.sourceId shouldBe spellId
        d.state.stack.contains(spellId) shouldBe false

        val transformer = ClientStateTransformer(cardRegistry = d.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
        for (viewer in listOf(d.player1, d.player2)) {
            transformer.transform(d.state, viewingPlayerId = viewer)
                .cards[spellId].shouldNotBeNull().name shouldBe chooser.name
        }
    }
})
