package com.wingedsheep.engine.scenarios

import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dom.cards.GhituChronicler
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ghitu Chronicler (DOM): "When this creature enters, if it was kicked, return target
 * instant or sorcery card FROM YOUR GRAVEYARD to your hand."
 *
 * Regression guard: the target used the unrestricted any-graveyard filter, letting the
 * caster pull instants/sorceries out of an opponent's graveyard.
 */
class GhituChroniclerTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GhituChronicler))
        return driver
    }

    test("kicked ETB can only target instants/sorceries in YOUR graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mine = driver.putCardInGraveyard(active, "Lightning Bolt")
        val theirs = driver.putCardInGraveyard(opponent, "Lightning Bolt")

        driver.giveMana(active, Color.RED, 2)
        driver.giveColorlessMana(active, 4)
        val chronicler = driver.putCardInHand(active, "Ghitu Chronicler")
        driver.submitSuccess(CastSpell(playerId = active, cardId = chronicler, declaredCostSlot = ChoiceSlot.KICKED))

        // Resolve the creature; the kicked ETB trigger pauses for target selection.
        repeat(4) {
            if (driver.pendingDecision == null && driver.state.priorityPlayerId != null) driver.bothPass()
        }

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseTargetsDecision>()
        val legal = decision.legalTargets[0].orEmpty()
        legal shouldContain mine
        legal shouldNotContain theirs
    }
})
