package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.TheRingGoesSouth
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The Ring Goes South — "The Ring tempts you. Then reveal cards from the top of your library
 * until you reveal X land cards, where X is the number of legendary creatures you control. Put
 * those land cards onto the battlefield tapped and the rest on the bottom of your library in a
 * random order."
 *
 * Exercises GatherUntilMatch (dynamic X) + the new MoveCollection.filter (lands → battlefield
 * tapped, the rest → bottom of library).
 */
class TheRingGoesSouthScenarioTest : FunSpec({

    val TestLegend = CardDefinition.creature(
        "Test Legend", ManaCost.parse("{2}"), emptySet(), 2, 2,
        supertypes = setOf(Supertype.LEGENDARY)
    )
    val TestOgre = CardDefinition.creature("Test Ogre", ManaCost.parse("{3}"), emptySet(), 3, 3)

    test("reveals until X lands; lands enter tapped, the rest go to the bottom") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TheRingGoesSouth, TestLegend, TestOgre))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!

        // One legendary creature → X = 1.
        val legend = driver.putCreatureOnBattlefield(active, "Test Legend")

        // Library top (top-most last): Test Ogre (nonland), then Forest (land). Reveal until 1
        // land reveals the Ogre then the Forest.
        driver.putCardOnTopOfLibrary(active, "Forest")
        driver.putCardOnTopOfLibrary(active, "Test Ogre")

        val spell = driver.putCardInHand(active, "The Ring Goes South")
        driver.giveMana(active, Color.GREEN, 4)
        driver.castSpell(active, spell)
        driver.bothPass()

        // Drain decisions: the Ring tempt prompts a Ring-bearer choice (pick the legend); the
        // reveal/move steps need no input. Loop until the spell fully resolves.
        var guard = 0
        while (guard++ < 10) {
            val pd = driver.pendingDecision
            if (pd is SelectCardsDecision) {
                driver.submitCardSelection(active, listOf(legend))
            } else if (driver.state.stack.isNotEmpty()) {
                driver.bothPass()
            } else break
        }

        // The revealed Forest is on the battlefield, tapped.
        val forestOnBf = driver.state.getBattlefield().firstOrNull {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Forest"
        }
        (forestOnBf != null) shouldBe true
        driver.state.getEntity(forestOnBf!!)?.has<TappedComponent>() shouldBe true

        // The non-land Ogre did not enter the battlefield (it went to the bottom of the library).
        driver.state.getBattlefield().any {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Test Ogre"
        } shouldBe false
    }
})
