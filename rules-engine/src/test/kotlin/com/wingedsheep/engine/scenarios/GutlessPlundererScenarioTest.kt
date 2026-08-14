package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fdn.cards.GutlessPlunderer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Gutless Plunderer {2}{B} — Creature — Skeleton Pirate 2/2
 *   Deathtouch
 *   Raid — When this creature enters, if you attacked this turn, look at the top three cards of
 *   your library. You may put one of those cards back on top of your library. Put the rest into
 *   your graveyard.
 *
 * Proves the Raid intervening-"if" gates the look-at-top-three pipeline: when the controller
 * attacked this turn the two unselected cards are milled and one is kept on top; with no attack
 * the ETB does nothing.
 */
class GutlessPlundererScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GutlessPlunderer))
        return driver
    }

    test("raid active: keeps one on top and mills the other two") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        // Attack this turn to satisfy Raid.
        val goblin = driver.putCreatureOnBattlefield(you, "Goblin Guide")
        driver.removeSummoningSickness(goblin)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(goblin), defendingPlayer = opponent).error shouldBe null
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Seed the top three cards of the library.
        val bottomOfThree = driver.putCardOnTopOfLibrary(you, "Grizzly Bears")
        val midOfThree = driver.putCardOnTopOfLibrary(you, "Hill Giant")
        val topOfThree = driver.putCardOnTopOfLibrary(you, "Centaur Courser")
        val seeded = setOf(bottomOfThree, midOfThree, topOfThree)

        driver.giveMana(you, Color.BLACK, 3)
        val plunderer = driver.putCardInHand(you, "Gutless Plunderer")
        driver.castSpell(you, plunderer).error shouldBe null

        val graveyardBefore = driver.getGraveyard(you).size

        var kept: EntityId? = null
        var safety = 0
        while (safety < 40) {
            val pending = driver.state.pendingDecision
            when {
                pending is SelectCardsDecision -> {
                    kept = pending.options.first()
                    driver.submitCardSelection(pending.playerId, listOf(kept))
                }
                driver.stackSize > 0 -> driver.bothPass()
                else -> break
            }
            safety++
        }

        val keptCard = kept
        (keptCard != null) shouldBe true // a selection decision was presented
        (keptCard in seeded) shouldBe true

        val graveyard = driver.getGraveyard(you)
        (graveyard.size - graveyardBefore) shouldBe 2
        seeded.filter { it != keptCard }.forEach { milled ->
            graveyard.contains(milled) shouldBe true
        }
        graveyard.contains(keptCard) shouldBe false
    }

    test("no attack this turn: raid ETB does nothing") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val you = driver.activePlayer!!

        driver.putCardOnTopOfLibrary(you, "Grizzly Bears")
        driver.putCardOnTopOfLibrary(you, "Hill Giant")
        driver.putCardOnTopOfLibrary(you, "Centaur Courser")

        driver.giveMana(you, Color.BLACK, 3)
        val plunderer = driver.putCardInHand(you, "Gutless Plunderer")
        driver.castSpell(you, plunderer).error shouldBe null

        val graveyardBefore = driver.getGraveyard(you).size

        var safety = 0
        while (driver.stackSize > 0 && safety < 20) {
            (driver.state.pendingDecision is SelectCardsDecision) shouldBe false
            driver.bothPass()
            safety++
        }

        driver.getGraveyard(you).size shouldBe graveyardBefore
    }
})
