package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GroupFilter
import com.wingedsheep.sdk.scripting.TapAllCreaturesEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for TapAllCreaturesEffect (Blinding Light and similar cards).
 */
class TapAllCreaturesTest : FunSpec({

    // Test card: Blinding Light - Tap all nonwhite creatures
    val BlindingLight = CardDefinition.sorcery(
        name = "Blinding Light",
        manaCost = ManaCost.parse("{2}{W}"),
        oracleText = "Tap all nonwhite creatures.",
        script = CardScript.spell(
            effect = TapAllCreaturesEffect(filter = GroupFilter.AllCreatures.notColor(Color.WHITE))
        )
    )

    // Test card: Tap all creatures (no filter)
    val MassiveStun = CardDefinition.sorcery(
        name = "Massive Stun",
        manaCost = ManaCost.parse("{2}{U}"),
        oracleText = "Tap all creatures.",
        script = CardScript.spell(
            effect = TapAllCreaturesEffect(filter = GroupFilter.AllCreatures)
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BlindingLight, MassiveStun))
        return driver
    }

    test("Blinding Light taps nonwhite creatures") {
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

        // Put creatures on battlefield:
        // - Grizzly Bears (green, should be tapped)
        // - Savannah Lions (white, should NOT be tapped)
        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        val lions = driver.putCreatureOnBattlefield(activePlayer, "Savannah Lions")

        // Verify neither is tapped initially
        driver.isTapped(bears) shouldBe false
        driver.isTapped(lions) shouldBe false

        // Give mana and cast Blinding Light
        val blindingLight = driver.putCardInHand(activePlayer, "Blinding Light")
        driver.giveMana(activePlayer, Color.WHITE, 3)

        driver.castSpell(activePlayer, blindingLight)
        driver.bothPass()

        // Verify: Bears (green) is tapped, Lions (white) is not
        driver.isTapped(bears) shouldBe true
        driver.isTapped(lions) shouldBe false
    }

    test("Blinding Light does not tap already tapped creatures") {
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

        // Put a green creature that's already tapped
        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.tapPermanent(bears)

        driver.isTapped(bears) shouldBe true

        // Cast Blinding Light
        val blindingLight = driver.putCardInHand(activePlayer, "Blinding Light")
        driver.giveMana(activePlayer, Color.WHITE, 3)

        driver.castSpell(activePlayer, blindingLight)
        driver.bothPass()

        // Bears should still be tapped (no double-tap event needed)
        driver.isTapped(bears) shouldBe true
    }

    test("Tap all creatures affects both players' creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Island" to 20,
                "Forest" to 20
            ),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creatures on both sides
        val myBears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        val theirBears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        driver.isTapped(myBears) shouldBe false
        driver.isTapped(theirBears) shouldBe false

        // Cast Massive Stun (tap all creatures)
        val stun = driver.putCardInHand(activePlayer, "Massive Stun")
        driver.giveMana(activePlayer, Color.BLUE, 3)

        driver.castSpell(activePlayer, stun)
        driver.bothPass()

        // Both creatures should be tapped
        driver.isTapped(myBears) shouldBe true
        driver.isTapped(theirBears) shouldBe true
    }
})
