package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mom.cards.SandstalkerMoloch
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sandstalker Moloch — "Flash. When this creature enters, if an opponent cast a blue and/or
 * black spell this turn, look at the top four cards of your library. You may reveal a permanent
 * card from among them and put it into your hand. Put the rest on the bottom of your library in
 * a random order."
 *
 * The Moloch's controller flashes it in on the opponent's turn, after the opponent has cast a
 * spell of a given colour.
 */
class SandstalkerMolochScenarioTest : FunSpec({

    val BlueBear = CardDefinition.creature("Blue Bear", ManaCost.parse("{U}"), setOf(Subtype("Bear")), 1, 1)
    val BlackBear = CardDefinition.creature("Black Bear", ManaCost.parse("{B}"), setOf(Subtype("Bear")), 1, 1)
    val GreenBear = CardDefinition.creature("Green Bear", ManaCost.parse("{G}"), setOf(Subtype("Bear")), 1, 1)
    val LibraryCreature = CardDefinition.creature("Library Creature", ManaCost.parse("{1}{G}"), setOf(Subtype("Elf")), 1, 1)
    val LibraryInstant = CardDefinition.instant("Library Instant", ManaCost.parse("{U}"), "Nothing.")

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(SandstalkerMoloch, BlueBear, BlackBear, GreenBear, LibraryCreature, LibraryInstant)
        )
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun libraryNames(driver: GameTestDriver, player: EntityId): List<String> =
        driver.state.getZone(ZoneKey(player, Zone.LIBRARY)).mapNotNull {
            driver.state.getEntity(it)?.get<CardComponent>()?.name
        }

    /** The active player casts [spellName]; it resolves; then the other player flashes in the Moloch. */
    fun opponentCastsThenFlashMoloch(driver: GameTestDriver, spellName: String, color: Color): EntityId {
        val active = driver.activePlayer!!
        val molochOwner = driver.getOpponent(active)
        val spell = driver.putCardInHand(active, spellName)
        driver.giveMana(active, color, 1)
        driver.castSpell(active, spell)
        driver.bothPass() // resolve the opponent's spell

        driver.passPriority(active) // hand priority to the Moloch's controller, empty stack
        val moloch = driver.putCardInHand(molochOwner, "Sandstalker Moloch")
        driver.giveMana(molochOwner, Color.GREEN, 3)
        driver.castSpell(molochOwner, moloch)
        driver.bothPass() // resolve the Moloch
        return molochOwner
    }

    test("after an opponent cast a blue spell, reveal a permanent card from the top four") {
        val driver = createDriver()
        val molochOwner = driver.getOpponent(driver.activePlayer!!)
        // Top four from the top: Instant, Creature, Forest, Forest; a fifth card sits below.
        driver.putCardOnTopOfLibrary(molochOwner, "Library Instant") // becomes 5th
        driver.putCardOnTopOfLibrary(molochOwner, "Forest")
        driver.putCardOnTopOfLibrary(molochOwner, "Forest")
        val creatureId = driver.putCardOnTopOfLibrary(molochOwner, "Library Creature")
        val instantId = driver.putCardOnTopOfLibrary(molochOwner, "Library Instant")

        opponentCastsThenFlashMoloch(driver, "Blue Bear", Color.BLUE)
        driver.assertPermanentExists(molochOwner, "Sandstalker Moloch")
        driver.bothPass() // resolve the ETB trigger

        val select = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        select.minSelections shouldBe 0
        select.maxSelections shouldBe 1
        select.options.contains(creatureId) shouldBe true
        select.options.contains(instantId) shouldBe false
        select.nonSelectableOptions.contains(instantId) shouldBe true

        driver.submitCardSelection(molochOwner, listOf(creatureId))

        driver.getHand(molochOwner).contains(creatureId) shouldBe true
        val library = libraryNames(driver, molochOwner)
        // The 5th card is now on top; the three unchosen looked-at cards went to the bottom.
        library.first() shouldBe "Library Instant"
        library.takeLast(3) shouldContainExactlyInAnyOrder listOf("Library Instant", "Forest", "Forest")
    }

    test("a black spell also satisfies the condition, and the reveal is optional") {
        val driver = createDriver()
        val molochOwner = driver.getOpponent(driver.activePlayer!!)
        val creatureId = driver.putCardOnTopOfLibrary(molochOwner, "Library Creature")
        val handBefore = driver.getHand(molochOwner).size

        opponentCastsThenFlashMoloch(driver, "Black Bear", Color.BLACK)
        driver.bothPass() // resolve the ETB trigger

        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(molochOwner, emptyList())

        driver.getHand(molochOwner).size shouldBe handBefore
        driver.state.getZone(ZoneKey(molochOwner, Zone.LIBRARY)).contains(creatureId) shouldBe true
        driver.state.getZone(ZoneKey(molochOwner, Zone.LIBRARY)).takeLast(4).contains(creatureId) shouldBe true
    }

    test("a green spell alone does not satisfy the intervening if — no trigger") {
        val driver = createDriver()
        val molochOwner = driver.getOpponent(driver.activePlayer!!)
        val creatureId = driver.putCardOnTopOfLibrary(molochOwner, "Library Creature")

        opponentCastsThenFlashMoloch(driver, "Green Bear", Color.GREEN)
        driver.assertPermanentExists(molochOwner, "Sandstalker Moloch")

        driver.pendingDecision shouldBe null
        driver.state.stack.isEmpty() shouldBe true
        driver.state.getZone(ZoneKey(molochOwner, Zone.LIBRARY)).first() shouldBe creatureId
    }
})
