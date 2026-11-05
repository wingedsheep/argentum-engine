package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.xln.cards.GrimCaptainsCall
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Grim Captain's Call: "Return a Pirate card from your graveyard to your hand, then do the same for
 * Vampire, Dinosaur, and Merfolk."
 *
 * Pins the sequencing the four rulings depend on: each type is a separate resolution-time pick, a
 * missing type is skipped, and a card with two of the types is returned once while another card
 * can still be returned for its other type.
 */
class GrimCaptainsCallScenarioTest : FunSpec({

    fun creature(name: String, subtypes: String) = card(name) {
        manaCost = "{1}"
        typeLine = "Creature — $subtypes"
        power = 1
        toughness = 1
    }

    val pirateA = creature("Test Pirate A", "Human Pirate")
    val pirateB = creature("Test Pirate B", "Human Pirate")
    val vampireDinosaur = creature("Test Vampire Dinosaur", "Vampire Dinosaur")
    val dinosaur = creature("Test Dinosaur", "Dinosaur")
    val merfolk = creature("Test Merfolk", "Merfolk Wizard")

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(GrimCaptainsCall, pirateA, pirateB, vampireDinosaur, dinosaur, merfolk))
        initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20, skipMulligans = true)
    }

    fun GameTestDriver.cast(you: EntityId) {
        giveMana(you, Color.BLACK, 1)
        giveColorlessMana(you, 2)
        val spell = putCardInHand(you, "Grim Captain's Call")
        castSpell(you, spell).error shouldBe null
        bothPass()
    }

    /** Answers any forced single-candidate pick (if the engine prompts for one at all). */
    fun GameTestDriver.takeForcedPicks(you: EntityId) {
        while (true) {
            val decision = pendingDecision as? SelectCardsDecision ?: return
            decision.options.size shouldBe 1
            submitCardSelection(you, decision.options).error shouldBe null
        }
    }

    test("returns one card per type, chosen at resolution, skipping a missing type") {
        val d = setup()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val firstPirate = d.putCardInGraveyard(you, "Test Pirate A")
        val secondPirate = d.putCardInGraveyard(you, "Test Pirate B")
        val theMerfolk = d.putCardInGraveyard(you, "Test Merfolk")
        val bears = d.putCardInGraveyard(you, "Grizzly Bears")

        d.cast(you)

        // Pirate first: both Pirates are offered, exactly one must be returned.
        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        pick.playerId shouldBe you
        pick.minSelections shouldBe 1
        pick.maxSelections shouldBe 1
        pick.options shouldContainExactlyInAnyOrder listOf(firstPirate, secondPirate)
        d.submitCardSelection(you, listOf(secondPirate)).error shouldBe null

        // No Vampire or Dinosaur: those are skipped; the lone Merfolk comes back.
        d.takeForcedPicks(you)
        d.pendingDecision shouldBe null

        d.getHand(you) shouldContain secondPirate
        d.getHand(you) shouldContain theMerfolk
        d.getGraveyard(you) shouldContain firstPirate
        d.getGraveyard(you) shouldContain bears
    }

    test("a Vampire Dinosaur is returned once, and another Dinosaur still comes back for its type") {
        val d = setup()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val hybrid = d.putCardInGraveyard(you, "Test Vampire Dinosaur")
        val plainDinosaur = d.putCardInGraveyard(you, "Test Dinosaur")

        d.cast(you)

        // Vampire: only the hybrid qualifies. Dinosaur: the hybrid is already in hand, so only the
        // plain Dinosaur is left to pick.
        d.takeForcedPicks(you)
        d.pendingDecision shouldBe null

        d.getHand(you) shouldContain hybrid
        d.getHand(you) shouldContain plainDinosaur
        d.getGraveyard(you) shouldNotContain hybrid
        d.getGraveyard(you) shouldNotContain plainDinosaur
    }
})
