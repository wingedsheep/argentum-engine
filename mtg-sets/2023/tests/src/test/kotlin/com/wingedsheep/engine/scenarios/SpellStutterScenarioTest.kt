package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.woe.cards.SpellStutter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Spell Stutter (WOE) — "Counter target spell unless its controller pays {2} plus an additional {1}
 * for each Faerie you control."
 *
 * The payment prompts name Spell Stutter as their source, so the player being taxed can tell which
 * card is asking (they used to read a generic "Counter unless pays").
 */
class SpellStutterScenarioTest : FunSpec({

    /** Player 1 casts Grizzly Bears; player 2 answers with Spell Stutter and it resolves. */
    fun stutterTheBears(): Pair<GameTestDriver, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + SpellStutter)
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Forest" to 20), startingLife = 20)
        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        repeat(2) { driver.putPermanentOnBattlefield(player1, "Forest") }
        val bears = driver.putCardInHand(player1, "Grizzly Bears")
        driver.giveMana(player1, Color.GREEN, 2)
        driver.castSpell(player1, bears).error shouldBe null
        val bearsOnStack = driver.getTopOfStack()!!
        driver.passPriority(player1)

        val stutter = driver.putCardInHand(player2, "Spell Stutter")
        driver.giveMana(player2, Color.BLUE, 2)
        driver.castSpellWithTargets(player2, stutter, listOf(ChosenTarget.Spell(bearsOnStack))).error shouldBe null
        driver.bothPass()
        return driver to player1
    }

    test("the pay-or-be-countered question and the mana payment both name Spell Stutter") {
        val (driver, player1) = stutterTheBears()

        val question = driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        question.playerId shouldBe player1
        question.context.sourceName shouldBe "Spell Stutter"
        question.prompt shouldBe "Pay {2} to prevent your spell from being countered?"

        driver.submitYesNo(player1, true)
        val payment = driver.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        payment.context.sourceName shouldBe "Spell Stutter"
        payment.requiredCost shouldBe "{2}"
    }

    test("declining lets Spell Stutter counter the spell") {
        val (driver, player1) = stutterTheBears()
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(player1, false)
        driver.getGraveyardCardNames(player1) shouldContain "Grizzly Bears"
    }
})
