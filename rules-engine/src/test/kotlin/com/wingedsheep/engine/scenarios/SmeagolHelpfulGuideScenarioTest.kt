package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.BirthdayEscape
import com.wingedsheep.mtg.sets.definitions.ltr.cards.SmeagolHelpfulGuide
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Sméagol, Helpful Guide — "Whenever the Ring tempts you, target opponent reveals cards from
 * the top of their library until they reveal a land card. Put that card onto the battlefield
 * tapped under your control and the rest into their graveyard."
 *
 * Triggered by a Ring tempt (Birthday Escape). Exercises GatherUntilMatch from the target
 * opponent's library + filtered MoveCollections (land → your battlefield tapped, rest → their
 * graveyard).
 */
class SmeagolHelpfulGuideScenarioTest : FunSpec({

    val TestOgre = CardDefinition.creature("Test Ogre", ManaCost.parse("{3}"), emptySet(), 3, 3)

    test("a Ring tempt steals the opponent's revealed land and mills the rest") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SmeagolHelpfulGuide, BirthdayEscape, TestOgre))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        val smeagol = driver.putCreatureOnBattlefield(active, "Sméagol, Helpful Guide")

        // Opponent's library top: a nonland Ogre, then Mountains. Reveal until a land reveals the
        // Ogre then a Mountain.
        driver.putCardOnTopOfLibrary(opponent, "Test Ogre")

        val spell = driver.putCardInHand(active, "Birthday Escape")
        driver.giveMana(active, Color.BLUE, 1)
        driver.castSpell(active, spell)
        driver.bothPass()

        // Drain: ring-bearer choice (pick Sméagol), then Sméagol's "target opponent".
        var guard = 0
        while (guard++ < 12) {
            when (driver.pendingDecision) {
                is SelectCardsDecision -> driver.submitCardSelection(active, listOf(smeagol))
                is ChooseTargetsDecision -> driver.submitTargetSelection(active, listOf(opponent))
                else -> if (driver.state.stack.isNotEmpty()) driver.bothPass() else break
            }
        }

        // A Mountain is now on the battlefield under the active player's control, tapped.
        val stolen = driver.state.getBattlefield().firstOrNull { id ->
            val c = driver.state.getEntity(id)
            c?.get<CardComponent>()?.name == "Mountain" &&
                c.get<ControllerComponent>()?.playerId == active
        }
        (stolen != null) shouldBe true
        driver.state.getEntity(stolen!!)?.has<TappedComponent>() shouldBe true

        // The non-land Ogre went into the opponent's graveyard.
        driver.state.getGraveyard(opponent).any {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Test Ogre"
        } shouldBe true
    }
})
