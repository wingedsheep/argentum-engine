package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.EncroachingMycosynth
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Encroaching Mycosynth (ONE #47) — {3}{U} Artifact.
 *
 * "Nonland permanents you control are artifacts in addition to their other types. The same is true
 *  for permanent spells you control and nonland permanent cards you own that aren't on the
 *  battlefield."
 *
 * The zone-by-zone matrix lives in the engine's `CrossZoneGrantCardTypeTest`; this pins the card.
 */
class EncroachingMycosynthScenarioTest : FunSpec({

    val predicates = PredicateEvaluator(cardRegistry = null)

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(EncroachingMycosynth)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.isArtifact(id: EntityId): Boolean =
        predicates.matches(state, state.projectedState, id, GameObjectFilter.Artifact, PredicateContext(controllerId = player1))

    test("your creatures, creature spells and creature cards are artifacts; lands and opponents' aren't") {
        val driver = newDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "Encroaching Mycosynth")

        val creature = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val land = driver.putLandOnBattlefield(me, "Forest")
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")
        val inGraveyard = driver.putCardInGraveyard(me, "Centaur Courser")
        val theirGraveyard = driver.putCardInGraveyard(driver.player2, "Centaur Courser")

        driver.isArtifact(creature) shouldBe true
        driver.isArtifact(land) shouldBe false
        driver.isArtifact(theirs) shouldBe false
        driver.isArtifact(inGraveyard) shouldBe true
        driver.isArtifact(theirGraveyard) shouldBe false

        val spell = driver.putCardInHand(me, "Centaur Courser")
        driver.isArtifact(spell) shouldBe true
        driver.giveMana(me, Color.GREEN, 4)
        driver.castSpell(me, spell)
        driver.isArtifact(spell) shouldBe true
    }
})
