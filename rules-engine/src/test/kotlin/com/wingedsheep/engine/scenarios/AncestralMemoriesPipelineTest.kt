package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for the Gather → Select → Move pipeline via Ancestral Memories:
 * "Look at the top 7 cards of your library. Put 2 of them into your hand
 * and the rest into your graveyard."
 *
 * Uses the pipeline-based implementation (EffectPatterns.lookAtTopAndKeep)
 * instead of the deprecated LookAtTopCardsEffect.
 */
class AncestralMemoriesPipelineTest : FunSpec({

    val AncestralMemories = CardDefinition(
        name = "Ancestral Memories",
        manaCost = ManaCost.parse("{2}{U}{U}{U}"),
        typeLine = TypeLine.sorcery(),
        oracleText = "Look at the top seven cards of your library. Put two of them into your hand and the rest into your graveyard.",
        script = CardScript.spell(
            EffectPatterns.lookAtTopAndKeep(count = 7, keepCount = 2)
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(AncestralMemories)
        return driver
    }

    test("Ancestral Memories pipeline: look at 7, keep 2, rest to graveyard") {
        val driver = createDriver()
        // Use non-land deck so library cards are predictable
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put specific cards on top of library so we know what's there
        val card1 = driver.putCardOnTopOfLibrary(activePlayer, "Island")
        val card2 = driver.putCardOnTopOfLibrary(activePlayer, "Forest")
        val card3 = driver.putCardOnTopOfLibrary(activePlayer, "Mountain")
        val card4 = driver.putCardOnTopOfLibrary(activePlayer, "Plains")
        val card5 = driver.putCardOnTopOfLibrary(activePlayer, "Swamp")
        val card6 = driver.putCardOnTopOfLibrary(activePlayer, "Island")
        val card7 = driver.putCardOnTopOfLibrary(activePlayer, "Forest")
        // Library from top: card7, card6, card5, card4, card3, card2, card1, <deck cards...>

        // Put Ancestral Memories in hand and give enough mana
        val spellCard = driver.putCardInHand(activePlayer, "Ancestral Memories")
        driver.giveMana(activePlayer, Color.BLUE, 5)

        val initialHandSize = driver.getHandSize(activePlayer)
        val initialGraveyardSize = driver.state.getZone(ZoneKey(activePlayer, Zone.GRAVEYARD)).size

        // Cast Ancestral Memories
        val castResult = driver.castSpell(activePlayer, spellCard)
        castResult.isSuccess shouldBe true

        // Let the spell resolve (both players pass priority)
        driver.bothPass()

        // GatherCards runs (stores top 7 in "looked") then SelectFromCollection pauses for a decision
        driver.isPaused shouldBe true
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()

        // The decision should show 7 cards to choose from
        val selectDecision = decision as SelectCardsDecision
        selectDecision.options.size shouldBe 7
        selectDecision.minSelections shouldBe 2
        selectDecision.maxSelections shouldBe 2

        // The decision should include card info for these hidden library cards
        selectDecision.cardInfo!!.size shouldBe 7

        // Player selects card7 and card5 to keep
        val selectedCards = listOf(card7, card5)
        driver.submitCardSelection(activePlayer, selectedCards)

        // After selection, the MoveCollection steps complete:
        // - card7 and card5 go to hand
        // - card6, card4, card3, card2, card1 go to graveyard
        driver.isPaused shouldBe false

        val handZone = driver.state.getZone(ZoneKey(activePlayer, Zone.HAND))
        handZone.contains(card7) shouldBe true
        handZone.contains(card5) shouldBe true

        val graveyardZone = driver.state.getZone(ZoneKey(activePlayer, Zone.GRAVEYARD))
        graveyardZone.contains(card6) shouldBe true
        graveyardZone.contains(card4) shouldBe true
        graveyardZone.contains(card3) shouldBe true
        graveyardZone.contains(card2) shouldBe true
        graveyardZone.contains(card1) shouldBe true

        // Hand: initial - 1 (spell cast) + 2 (kept cards)
        driver.getHandSize(activePlayer) shouldBe initialHandSize - 1 + 2

        // Graveyard: 5 non-selected cards + the Ancestral Memories spell itself
        val graveyardGrowth = driver.state.getZone(ZoneKey(activePlayer, Zone.GRAVEYARD)).size - initialGraveyardSize
        graveyardGrowth shouldBe 5 + 1
    }

    test("pipeline card info includes names and mana costs for library cards") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Place a recognizable card on top
        driver.putCardOnTopOfLibrary(activePlayer, "Island")
        driver.putCardOnTopOfLibrary(activePlayer, "Grizzly Bears")
        driver.putCardOnTopOfLibrary(activePlayer, "Forest")
        driver.putCardOnTopOfLibrary(activePlayer, "Mountain")
        driver.putCardOnTopOfLibrary(activePlayer, "Plains")
        driver.putCardOnTopOfLibrary(activePlayer, "Swamp")
        driver.putCardOnTopOfLibrary(activePlayer, "Island")

        val spellCard = driver.putCardInHand(activePlayer, "Ancestral Memories")
        driver.giveMana(activePlayer, Color.BLUE, 5)

        driver.castSpell(activePlayer, spellCard)
        driver.bothPass()

        driver.isPaused shouldBe true
        val selectDecision = driver.pendingDecision as SelectCardsDecision

        // Card info should expose names for all 7 cards
        val names = selectDecision.cardInfo!!.values.map { it.name }.toSet()
        names.contains("Island") shouldBe true
        names.contains("Grizzly Bears") shouldBe true
        names.contains("Forest") shouldBe true
    }
})
