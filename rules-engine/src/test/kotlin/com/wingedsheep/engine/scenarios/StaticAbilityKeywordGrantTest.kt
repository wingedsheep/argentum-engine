package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToCreatureGroup
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for static abilities that grant keywords to creature groups.
 *
 * Tests the GrantKeywordToCreatureGroup static ability, which is used by cards like
 * Adept Watershaper ("Other tapped creatures you control have indestructible").
 */
class StaticAbilityKeywordGrantTest : FunSpec({

    // Test card: "Other tapped creatures you control have indestructible"
    val IndestructibleGranter = CardDefinition.creature(
        name = "Indestructible Granter",
        manaCost = ManaCost.parse("{2}{W}"),
        subtypes = setOf(Subtype("Test")),
        power = 3,
        toughness = 4,
        script = CardScript(
            staticAbilities = listOf(
                GrantKeywordToCreatureGroup(
                    keyword = Keyword.INDESTRUCTIBLE,
                    filter = GroupFilter.OtherTappedCreaturesYouControl
                )
            )
        )
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(IndestructibleGranter))
        return driver
    }

    test("GrantKeywordToCreatureGroup grants indestructible to other tapped creatures you control") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Plains" to 20,
                "Forest" to 20
            ),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Indestructible Granter and another creature on the battlefield
        val granter = driver.putCreatureOnBattlefield(activePlayer, "Indestructible Granter")
        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        // Verify bears does NOT have indestructible (not tapped yet)
        var projected = projector.project(driver.state)
        projected.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe false

        // Tap the bears
        driver.tapPermanent(bears)

        // Now bears should have indestructible (it's tapped and controlled by same player)
        projected = projector.project(driver.state)
        projected.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe true

        // Granter itself should NOT have indestructible (it says "other")
        projected.hasKeyword(granter, Keyword.INDESTRUCTIBLE) shouldBe false
    }

    test("GrantKeywordToCreatureGroup does not grant indestructible to untapped creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put both creatures on battlefield
        driver.putCreatureOnBattlefield(activePlayer, "Indestructible Granter")
        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        // Bears is not tapped, so should NOT have indestructible
        val projected = projector.project(driver.state)
        projected.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe false
    }

    test("GrantKeywordToCreatureGroup does not grant indestructible to opponent's tapped creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Player 1 controls the granter
        driver.putCreatureOnBattlefield(activePlayer, "Indestructible Granter")

        // Opponent has a creature
        val opponentCreature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Tap opponent's creature
        driver.tapPermanent(opponentCreature)

        // Opponent's creature should NOT have indestructible (different controller)
        val projected = projector.project(driver.state)
        projected.hasKeyword(opponentCreature, Keyword.INDESTRUCTIBLE) shouldBe false
    }

    test("GrantKeywordToCreatureGroup affects multiple tapped creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put granter and two creatures on battlefield
        driver.putCreatureOnBattlefield(activePlayer, "Indestructible Granter")
        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        val lions = driver.putCreatureOnBattlefield(activePlayer, "Savannah Lions")

        // Tap both creatures
        driver.tapPermanent(bears)
        driver.tapPermanent(lions)

        // Both should have indestructible
        val projected = projector.project(driver.state)
        projected.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe true
        projected.hasKeyword(lions, Keyword.INDESTRUCTIBLE) shouldBe true
    }

    test("Untapping a creature removes the granted indestructible") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creatures on battlefield
        driver.putCreatureOnBattlefield(activePlayer, "Indestructible Granter")
        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        // Tap - should have indestructible
        driver.tapPermanent(bears)
        var projected = projector.project(driver.state)
        projected.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe true

        // Untap - should lose indestructible
        driver.untapPermanent(bears)
        projected = projector.project(driver.state)
        projected.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe false
    }

    test("Tapping the granter itself does not give it indestructible") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put just the granter on battlefield
        val granter = driver.putCreatureOnBattlefield(activePlayer, "Indestructible Granter")

        // Tap the granter itself
        driver.tapPermanent(granter)

        // Granter should NOT have indestructible (it says "other")
        val projected = projector.project(driver.state)
        projected.hasKeyword(granter, Keyword.INDESTRUCTIBLE) shouldBe false
    }
})
