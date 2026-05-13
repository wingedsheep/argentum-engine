package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.UUID

/**
 * Surveil N: look at the top N cards; send any chosen subset to the graveyard,
 * then put the rest back on top in controller-chosen order.
 *
 * Covered scenario: Surveil 2 — player puts the top card (A) into the graveyard
 * and keeps the second card (B) on top; the cards below (C, D) must remain in
 * their original relative positions immediately beneath B.
 */
class SurveilNTest : FunSpec({

    val surveilAbilityId = AbilityId(UUID.randomUUID().toString())

    val SurveilArchmage = CardDefinition(
        name = "Surveil Archmage",
        manaCost = ManaCost.parse("{3}{U}"),
        typeLine = TypeLine.creature(setOf(Subtype("Human"), Subtype("Wizard"))),
        oracleText = "{2}{U}: Surveil 2.",
        creatureStats = CreatureStats(2, 2),
        script = CardScript.permanent(
            ActivatedAbility(
                id = surveilAbilityId,
                cost = AbilityCost.Mana(ManaCost.parse("{2}{U}")),
                effect = EffectPatterns.surveil(2)
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SurveilArchmage))
        return driver
    }

    test("surveil 2 - chosen card goes to graveyard, kept card is new library top, cards below are undisturbed") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 30, "Mountain" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Build library top [A, B, C, D] from top by inserting bottom-first
        val cardD = driver.putCardOnTopOfLibrary(activePlayer, "Mountain")
        val cardC = driver.putCardOnTopOfLibrary(activePlayer, "Island")
        val cardB = driver.putCardOnTopOfLibrary(activePlayer, "Mountain")
        val cardA = driver.putCardOnTopOfLibrary(activePlayer, "Grizzly Bears")

        // Verify initial library order before the effect
        val libraryZone = ZoneKey(activePlayer, Zone.LIBRARY)
        val libraryBefore = driver.state.getZone(libraryZone)
        libraryBefore[0] shouldBe cardA
        libraryBefore[1] shouldBe cardB
        libraryBefore[2] shouldBe cardC
        libraryBefore[3] shouldBe cardD

        val surveilSource = driver.putCreatureOnBattlefield(activePlayer, "Surveil Archmage")
        driver.removeSummoningSickness(surveilSource)
        driver.giveMana(activePlayer, Color.BLUE, 3)

        // Activate Surveil 2
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = surveilSource,
                abilityId = surveilAbilityId
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        // Engine pauses: player chooses which of the top 2 cards go to the graveyard
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()

        val selectDecision = driver.pendingDecision as SelectCardsDecision
        selectDecision.options.size shouldBe 2

        // Player sends card A to the graveyard; card B stays on top
        driver.submitDecision(
            activePlayer,
            CardsSelectedResponse(
                decisionId = selectDecision.id,
                selectedCards = listOf(cardA)
            )
        )

        // Engine pauses: controller orders the remaining card(s) back on top
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
        val reorderDecision = driver.pendingDecision as ReorderLibraryDecision
        reorderDecision.cards.size shouldBe 1
        reorderDecision.cards[0] shouldBe cardB
        driver.submitOrderedResponse(activePlayer, reorderDecision.cards)

        driver.isPaused shouldBe false

        // Card A is now in the graveyard
        val graveyardZone = ZoneKey(activePlayer, Zone.GRAVEYARD)
        val graveyard = driver.state.getZone(graveyardZone)
        graveyard.contains(cardA) shouldBe true

        // Card A is no longer in the library
        val libraryAfter = driver.state.getZone(libraryZone)
        libraryAfter.contains(cardA) shouldBe false

        // Card B is the new top of library; C and D are immediately beneath it, unchanged
        libraryAfter[0] shouldBe cardB
        libraryAfter[1] shouldBe cardC
        libraryAfter[2] shouldBe cardD
    }
})
