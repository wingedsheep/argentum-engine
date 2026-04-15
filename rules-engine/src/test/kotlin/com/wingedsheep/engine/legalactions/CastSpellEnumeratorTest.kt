package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.support.EnumerationFixtures
import com.wingedsheep.engine.legalactions.support.EnumerationTestDriver
import com.wingedsheep.engine.legalactions.support.shouldContainAffordableCastOf
import com.wingedsheep.engine.legalactions.support.shouldContainCastOf
import com.wingedsheep.engine.legalactions.support.shouldNotContainCastOf
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.CantCastSpellsComponent
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for [enumerators.CastSpellEnumerator] over the spells-from-hand path.
 * Alternative-zone casts (graveyard, exile, top of library) live in
 * CastFromZoneEnumerator and have their own test file in phase 4.
 *
 * Note on "unaffordable" spells: the enumerator drops a spell entirely when
 * no payment path is reachable (line 250 of the enumerator). So a spell with
 * no mana available does not appear at all — it does NOT appear with
 * `affordable=false`. These tests assert that contract.
 */
class CastSpellEnumeratorTest : FunSpec({

    /**
     * Per-test helper: construct a state where Player 1's hand and battlefield
     * are exactly the supplied cards. Goes through GameTestDriver init for
     * realistic player/turn structure, then surgically moves named cards from
     * library to the requested zones.
     */
    fun setupP1Board(
        handCards: List<String>,
        battlefieldCards: List<String>,
        atStep: Step = Step.PRECOMBAT_MAIN
    ): EnumerationTestDriver {
        val driver = EnumerationTestDriver()
        driver.registerCards(TestCards.all)
        // Deck contains everything we'll need plus filler. Filler ensures the
        // game doesn't end via empty-library before we get to enumerate.
        val deckSpec = (handCards + battlefieldCards)
            .groupingBy { it }.eachCount()
            .map { (name, count) -> name to (count + 4) }   // copies for surgery + buffer
            .toMutableList()
        deckSpec.add("Forest" to 30) // filler for both libraries
        driver.game.initMirrorMatch(
            deck = Deck.of(*deckSpec.toTypedArray()),
            skipMulligans = true
        )
        driver.game.passPriorityUntil(atStep)

        var state: GameState = driver.game.state
        // Clear P1's hand back to empty so we can deterministically populate it.
        for (handId in state.getHand(driver.player1).toList()) {
            state = state.moveToZone(
                handId,
                ZoneKey(driver.player1, Zone.HAND),
                ZoneKey(driver.player1, Zone.LIBRARY)
            )
        }
        // Move requested cards from library to hand.
        for (cardName in handCards) {
            val cardId = state.getLibrary(driver.player1).first { id ->
                state.getEntity(id)?.get<CardComponent>()?.name == cardName
            }
            state = state.moveToZone(
                cardId,
                ZoneKey(driver.player1, Zone.LIBRARY),
                ZoneKey(driver.player1, Zone.HAND)
            )
        }
        // Move requested cards from library to battlefield.
        for (cardName in battlefieldCards) {
            val cardId = state.getLibrary(driver.player1).first { id ->
                state.getEntity(id)?.get<CardComponent>()?.name == cardName
            }
            state = state.moveToZone(
                cardId,
                ZoneKey(driver.player1, Zone.LIBRARY),
                ZoneKey(driver.player1, Zone.BATTLEFIELD)
            )
        }
        driver.game.replaceState(state)
        return driver
    }

    // -------------------------------------------------------------------------
    // Filtering
    // -------------------------------------------------------------------------

    test("lands in hand are not enumerated as CastSpell actions") {
        val driver = EnumerationFixtures.allForestsMainPhase()

        val casts = driver.enumerateFor(driver.player1).castActions()

        casts.shouldBeEmpty()
    }

    test("creature in hand with sufficient mana surfaces as an affordable CastSpell") {
        val driver = setupP1Board(
            handCards = listOf("Grizzly Bears"),
            battlefieldCards = listOf("Forest", "Forest")
        )

        driver.enumerateFor(driver.player1) shouldContainAffordableCastOf "Grizzly Bears"
    }

    test("creature in hand with no mana available is not enumerated at all") {
        val driver = setupP1Board(
            handCards = listOf("Grizzly Bears"),
            battlefieldCards = emptyList()
        )

        driver.enumerateFor(driver.player1) shouldNotContainCastOf "Grizzly Bears"
    }

    test("opponent does not see CastSpell actions for cards in P1's hand") {
        val driver = setupP1Board(
            handCards = listOf("Lightning Bolt"),
            battlefieldCards = listOf("Forest")  // P1 has mana but Bolt is {R}, irrelevant for P2
        )

        driver.enumerateFor(driver.player2) shouldNotContainCastOf "Lightning Bolt"
    }

    // -------------------------------------------------------------------------
    // Timing
    // -------------------------------------------------------------------------

    test("sorcery-speed creature is not castable on the upkeep step") {
        val driver = setupP1Board(
            handCards = listOf("Grizzly Bears"),
            battlefieldCards = listOf("Forest", "Forest"),
            atStep = Step.UPKEEP
        )

        driver.enumerateFor(driver.player1) shouldNotContainCastOf "Grizzly Bears"
    }

    test("instants ARE castable on the upkeep step") {
        val driver = setupP1Board(
            handCards = listOf("Lightning Bolt", "Grizzly Bears"),
            battlefieldCards = listOf("Mountain", "Mountain"),
            atStep = Step.UPKEEP
        )

        val view = driver.enumerateFor(driver.player1)
        view shouldContainCastOf "Lightning Bolt"     // instant — OK
        view shouldNotContainCastOf "Grizzly Bears"   // sorcery-speed — blocked
    }

    // -------------------------------------------------------------------------
    // Costs and X
    // -------------------------------------------------------------------------

    test("manaCostString is populated from the card's cost") {
        val driver = setupP1Board(
            handCards = listOf("Lightning Bolt"),
            battlefieldCards = listOf("Mountain")
        )

        val cast = driver.enumerateFor(driver.player1).castActionsFor("Lightning Bolt").first()

        cast.manaCostString shouldBe "{R}"
        cast.affordable shouldBe true
        cast.hasXCost shouldBe false
    }

    test("description starts with 'Cast' and includes the card name") {
        val driver = setupP1Board(
            handCards = listOf("Grizzly Bears"),
            battlefieldCards = listOf("Forest", "Forest")
        )

        val cast = driver.enumerateFor(driver.player1).castActionsFor("Grizzly Bears").first()

        cast.actionType shouldBe "CastSpell"
        cast.description shouldBe "Cast Grizzly Bears"
    }

    // -------------------------------------------------------------------------
    // Targeting
    // -------------------------------------------------------------------------

    test("Lightning Bolt reports valid targets and requiresTargets=true") {
        val driver = setupP1Board(
            handCards = listOf("Lightning Bolt"),
            battlefieldCards = listOf("Mountain")
        )

        val cast = driver.enumerateFor(driver.player1).castActionsFor("Lightning Bolt").first()

        cast.requiresTargets shouldBe true
        cast.targetCount shouldBe 1
        cast.validTargets shouldNotBe null
        // AnyTarget allows both players; both should be in the valid set.
        cast.validTargets!! shouldContain driver.player1
        cast.validTargets!! shouldContain driver.player2
    }

    test("CastSpell action carries the card's entity id") {
        val driver = setupP1Board(
            handCards = listOf("Grizzly Bears"),
            battlefieldCards = listOf("Forest", "Forest")
        )
        val expectedHandId = driver.game.state.getHand(driver.player1).first { id ->
            driver.game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
        }

        val cast = driver.enumerateFor(driver.player1).castActionsFor("Grizzly Bears").first()
        val action = cast.action as CastSpell

        action.cardId shouldBe expectedHandId
        action.playerId shouldBe driver.player1
    }

    // -------------------------------------------------------------------------
    // Player-level restrictions
    // -------------------------------------------------------------------------

    test("CantCastSpellsComponent on player suppresses every spell cast") {
        val driver = setupP1Board(
            handCards = listOf("Grizzly Bears", "Lightning Bolt"),
            battlefieldCards = listOf("Forest", "Mountain")
        )
        val playerContainer = driver.game.state.getEntity(driver.player1)!!
            .with(CantCastSpellsComponent())
        driver.game.replaceState(driver.game.state.withEntity(driver.player1, playerContainer))

        val view = driver.enumerateFor(driver.player1)

        view shouldNotContainCastOf "Grizzly Bears"
        view shouldNotContainCastOf "Lightning Bolt"
    }

})
