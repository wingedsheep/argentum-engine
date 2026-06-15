package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.HewTheEntwood
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Hew the Entwood — "Sacrifice any number of lands. Reveal the top X cards of your library, where
 * X is the number of lands sacrificed this way. Choose any number of artifact and/or land cards
 * revealed this way. Put all nonland cards chosen this way onto the battlefield, then put all land
 * cards chosen this way onto the battlefield tapped, then put the rest on the bottom of your
 * library in a random order."
 *
 * Exercises SacrificeAnyNumber (records X) → GatherCards(TopOfLibrary(X)) → reveal →
 * SelectFromCollection(any number, filter = Artifact OR Land) → partitioned MoveCollections:
 * chosen nonland → battlefield untapped, chosen land → battlefield tapped, rest → bottom random.
 */
class HewTheEntwoodScenarioTest : FunSpec({

    val TestArtifact = CardDefinition.artifact("Test Artifact", ManaCost.parse("{2}"))
    val TestOgre = CardDefinition.creature("Test Ogre", ManaCost.parse("{3}"), emptySet(), 3, 3)

    test("sacrifice 3 lands, reveal top 3, choose the artifact + land; nonland goes to bottom") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(HewTheEntwood, TestArtifact, TestOgre))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!

        // Three lands on the battlefield to sacrifice → X = 3.
        val land1 = driver.putLandOnBattlefield(active, "Mountain")
        val land2 = driver.putLandOnBattlefield(active, "Mountain")
        val land3 = driver.putLandOnBattlefield(active, "Mountain")

        // Top of library (top-most last): Ogre (non-artifact, non-land), Forest (land),
        // Test Artifact (artifact). Reveal top 3 surfaces all three.
        driver.putCardOnTopOfLibrary(active, "Test Ogre")
        driver.putCardOnTopOfLibrary(active, "Forest")
        val artifactId = driver.putCardOnTopOfLibrary(active, "Test Artifact")

        val spell = driver.putCardInHand(active, "Hew the Entwood")
        driver.giveMana(active, Color.RED, 5)
        driver.castSpell(active, spell)
        driver.bothPass()

        val sacrificeOptions = setOf(land1, land2, land3)
        var guard = 0
        while (guard++ < 12) {
            val pd = driver.pendingDecision
            when {
                pd is SelectCardsDecision && pd.options.toSet() == sacrificeOptions -> {
                    // Sacrifice all three lands.
                    driver.submitCardSelection(active, listOf(land1, land2, land3))
                }
                pd is SelectCardsDecision -> {
                    // Choose the artifact and the revealed land; the Ogre is not selectable.
                    val forestId = pd.options.first { opt ->
                        driver.state.getEntity(opt)?.get<CardComponent>()?.name == "Forest"
                    }
                    pd.options.any {
                        driver.state.getEntity(it)?.get<CardComponent>()?.name == "Test Ogre"
                    } shouldBe false
                    driver.submitCardSelection(active, listOf(artifactId, forestId))
                }
                driver.state.stack.isNotEmpty() -> driver.bothPass()
                else -> break
            }
        }

        fun onBattlefield(name: String): com.wingedsheep.sdk.model.EntityId? =
            driver.state.getBattlefield().firstOrNull {
                driver.state.getEntity(it)?.get<CardComponent>()?.name == name
            }

        // Chosen artifact (nonland) entered the battlefield untapped.
        val artifactOnBf = onBattlefield("Test Artifact")
        (artifactOnBf != null) shouldBe true
        driver.state.getEntity(artifactOnBf!!)?.has<TappedComponent>() shouldBe false

        // Chosen land entered the battlefield tapped.
        val forestOnBf = onBattlefield("Forest")
        (forestOnBf != null) shouldBe true
        driver.state.getEntity(forestOnBf!!)?.has<TappedComponent>() shouldBe true

        // The non-artifact, non-land Ogre was not choosable and went to the bottom of the library.
        onBattlefield("Test Ogre") shouldBe null
        val library = driver.state.getLibrary(active)
        library.any {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Test Ogre"
        } shouldBe true
    }
})
