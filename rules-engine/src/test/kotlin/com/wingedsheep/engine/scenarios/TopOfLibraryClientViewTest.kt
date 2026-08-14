package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * The client-view contract behind rendering the top library card face up on the Deck pile.
 *
 * The web client has no rules knowledge: it shows the top card face up exactly when the server
 * sent details for the *first* entry of that library's `cardIds`. Two properties have to hold for
 * that to be both correct and leak-free, and this test pins them:
 *
 *  - **Position 0 is the top of the library.** The library zone is always sent in full (opaque ids
 *    for unknown cards), so the client identifies the top card purely by index.
 *  - **[LookAtTopOfLibrary] is a private peek.** Unlike
 *    [com.wingedsheep.sdk.scripting.RevealTopOfLibrary] (Goblin Spy, covered by [GoblinSpyTest]),
 *    only the controller gets the card's details — an opponent's view must still be opaque.
 */
class TopOfLibraryClientViewTest : FunSpec({

    val LensOfClarity = card("Lens of Clarity") {
        manaCost = "{0}"
        typeLine = "Artifact"

        staticAbility {
            ability = LookAtTopOfLibrary
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(LensOfClarity)
        return driver
    }

    fun transformer(d: GameTestDriver): ClientStateTransformer =
        ClientStateTransformer(cardRegistry = d.cardRegistry)

    test("controller sees the top card of their own library, at position 0 of the library zone") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Plains" to 20), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(player, "Lens of Clarity")
        val topCard = driver.putCardOnTopOfLibrary(player, "Lightning Bolt")

        val view = transformer(driver).transform(driver.state, viewingPlayerId = player)

        view.cards.keys shouldContain topCard
        view.cards[topCard]!!.name shouldBe "Lightning Bolt"

        val libraryZone = view.zones.first {
            it.zoneId.ownerId == player && it.zoneId.zoneType == Zone.LIBRARY
        }
        libraryZone.cardIds.first() shouldBe topCard
    }

    test("the peek is private — an opponent's view of that library stays opaque") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Plains" to 20), startingLife = 20)

        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(player, "Lens of Clarity")
        val topCard = driver.putCardOnTopOfLibrary(player, "Lightning Bolt")

        val view = transformer(driver).transform(driver.state, viewingPlayerId = opponent)

        view.cards.keys shouldNotContain topCard
    }

    test("without the ability the controller's own top card is hidden") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Plains" to 20), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val topCard = driver.putCardOnTopOfLibrary(player, "Lightning Bolt")

        val view = transformer(driver).transform(driver.state, viewingPlayerId = player)

        view.cards.keys shouldNotContain topCard
    }

    test("losing the source hides the top card again") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Plains" to 20), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val lens = driver.putPermanentOnBattlefield(player, "Lens of Clarity")
        val topCard = driver.putCardOnTopOfLibrary(player, "Lightning Bolt")

        transformer(driver).transform(driver.state, viewingPlayerId = player)
            .cards.keys shouldContain topCard

        driver.moveToGraveyard(lens)

        transformer(driver).transform(driver.state, viewingPlayerId = player)
            .cards.keys shouldNotContain topCard
    }
})
