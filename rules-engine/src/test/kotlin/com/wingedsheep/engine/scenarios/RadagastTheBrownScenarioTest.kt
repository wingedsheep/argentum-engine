package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.RadagastTheBrown
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
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Radagast the Brown — "Whenever Radagast or another nontoken creature you control enters,
 * look at the top X cards of your library, where X is that creature's mana value. You may
 * reveal a creature card that doesn't share a creature type with a creature you control from
 * among those cards and put it into your hand. Put the rest on the bottom in a random order."
 *
 * Exercises the new `notSharingCreatureTypeWithPermanentYouControl` filter + the
 * `Patterns.Library.lookAtTopRevealMatchingToHand` pipeline, with X driven by the *triggering*
 * creature's mana value.
 */
class RadagastTheBrownScenarioTest : FunSpec({

    // A 3-mana Soldier we cast to trigger Radagast (X = 3).
    val TestSoldier = CardDefinition.creature("Test Soldier", ManaCost.parse("{2}{G}"), setOf(Subtype("Soldier")), 2, 2)
    // A Bear we already control — gives our creatures the "Bear" creature type.
    val TestBear = CardDefinition.creature("Test Bear", ManaCost.parse("{1}{G}"), setOf(Subtype("Bear")), 2, 2)
    // Library candidates.
    val LibraryBear = CardDefinition.creature("Library Bear", ManaCost.parse("{1}{G}"), setOf(Subtype("Bear")), 1, 1)
    val LibrarySoldier = CardDefinition.creature("Library Soldier", ManaCost.parse("{1}{W}"), setOf(Subtype("Soldier")), 1, 1)
    val LibrarySpirit = CardDefinition.creature("Library Spirit", ManaCost.parse("{1}{U}"), setOf(Subtype("Spirit")), 1, 1)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(
                RadagastTheBrown, TestSoldier, TestBear, LibraryBear, LibrarySoldier, LibrarySpirit
            )
        )
        return driver
    }

    fun libraryNames(driver: GameTestDriver, player: EntityId): List<String> =
        driver.state.getZone(ZoneKey(player, Zone.LIBRARY)).mapNotNull {
            driver.state.getEntity(it)?.get<CardComponent>()?.name
        }

    test("only a creature sharing no creature type with your creatures is revealable; rest go to bottom") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!

        // Radagast (Avatar Wizard) and a Bear are already on the battlefield under our control,
        // so our creatures' types include Avatar, Wizard, Bear.
        driver.putCreatureOnBattlefield(active, "Radagast the Brown")
        driver.putCreatureOnBattlefield(active, "Test Bear")

        // Stack the top three of our library: a Bear (shares Bear), a Soldier (shares Soldier
        // once the cast creature enters), and a Spirit (shares nothing). putCardOnTopOfLibrary
        // prepends, so push in reverse to get [Bear, Soldier, Spirit] from the top.
        val spiritId = driver.putCardOnTopOfLibrary(active, "Library Spirit")
        val soldierId = driver.putCardOnTopOfLibrary(active, "Library Soldier")
        val bearId = driver.putCardOnTopOfLibrary(active, "Library Bear")

        // Cast a 3-mana Soldier; X = 3 → look at exactly those three cards.
        val caster = driver.putCardInHand(active, "Test Soldier")
        driver.giveMana(active, Color.GREEN, 3)
        driver.castSpell(active, caster)
        driver.bothPass() // resolve the creature spell — it enters and fires Radagast's trigger
        driver.bothPass() // resolve the triggered ability → look at top 3

        val decision = driver.pendingDecision
        decision shouldNotBe null
        val select = decision.shouldBeInstanceOf<SelectCardsDecision>()

        // "May" reveal → 0..1 selections.
        select.minSelections shouldBe 0
        select.maxSelections shouldBe 1

        // Only the Spirit (shares no creature type with our creatures) is selectable.
        // The Bear and the Soldier share a type, so they're shown but not selectable.
        select.options shouldContainExactlyInAnyOrder listOf(spiritId)
        select.nonSelectableOptions shouldContainExactlyInAnyOrder listOf(bearId, soldierId)

        // Reveal the Spirit and put it into hand.
        driver.submitCardSelection(active, listOf(spiritId))

        // Spirit is in hand; the other two looked-at cards are on the bottom of the library.
        driver.getHand(active).any {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Library Spirit"
        } shouldBe true

        val library = libraryNames(driver, active)
        library.contains("Library Spirit") shouldBe false
        // Bear and Soldier are at the bottom of the library.
        library.takeLast(2) shouldContainExactlyInAnyOrder listOf("Library Bear", "Library Soldier")
    }

    test("the reveal is optional — declining leaves all looked-at cards on the bottom") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!

        driver.putCreatureOnBattlefield(active, "Radagast the Brown")

        val spiritId = driver.putCardOnTopOfLibrary(active, "Library Spirit")
        val soldierId = driver.putCardOnTopOfLibrary(active, "Library Soldier")
        val bearId = driver.putCardOnTopOfLibrary(active, "Library Bear")

        val caster = driver.putCardInHand(active, "Test Soldier")
        driver.giveMana(active, Color.GREEN, 3)
        driver.castSpell(active, caster)
        driver.bothPass()
        driver.bothPass()

        val select = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        // No Bear in play this time, so the Spirit and the Bear are both revealable, but the
        // Soldier shares a type with the just-cast Test Soldier.
        select.options shouldContainExactlyInAnyOrder listOf(spiritId, bearId)
        select.nonSelectableOptions shouldContainExactlyInAnyOrder listOf(soldierId)
        // Decline the optional reveal.
        driver.submitCardSelection(active, emptyList())

        // Nothing went to hand from the look; all three are on the bottom of the library.
        driver.getHand(active).none {
            val n = driver.state.getEntity(it)?.get<CardComponent>()?.name
            n == "Library Spirit" || n == "Library Bear" || n == "Library Soldier"
        } shouldBe true

        val library = libraryNames(driver, active)
        library.takeLast(3) shouldContainExactlyInAnyOrder
            listOf("Library Bear", "Library Soldier", "Library Spirit")
    }
})
