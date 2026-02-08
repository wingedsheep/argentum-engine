package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.targeting.TargetCreature
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Mage's Guile.
 *
 * Mage's Guile: {1}{U}
 * Instant
 * Target creature gains shroud until end of turn.
 * Cycling {U}
 */
class MagesGuileTest : FunSpec({

    val MagesGuile = CardDefinition.instant(
        name = "Mage's Guile",
        manaCost = ManaCost.parse("{1}{U}"),
        oracleText = "Target creature gains shroud until end of turn.\nCycling {U}",
        script = CardScript.spell(
            effect = GrantKeywordUntilEndOfTurnEffect(Keyword.SHROUD, EffectTarget.ContextTarget(0)),
            TargetCreature()
        )
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MagesGuile))
        return driver
    }

    test("Mage's Guile grants shroud to target creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on the battlefield
        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        // Creature should not have shroud initially
        projector.getProjectedKeywords(driver.state, bears).contains(Keyword.SHROUD) shouldBe false

        // Cast Mage's Guile targeting the creature
        val guile = driver.putCardInHand(activePlayer, "Mage's Guile")
        driver.giveMana(activePlayer, Color.BLUE, 2)

        val castResult = driver.castSpell(activePlayer, guile, listOf(bears))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // Creature should now have shroud
        projector.getProjectedKeywords(driver.state, bears).contains(Keyword.SHROUD) shouldBe true
    }

    test("Mage's Guile also grants shroud to opponent's creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on the battlefield for the opponent
        val opponentBears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Cast Mage's Guile targeting the opponent's creature
        val guile = driver.putCardInHand(activePlayer, "Mage's Guile")
        driver.giveMana(activePlayer, Color.BLUE, 2)

        val castResult = driver.castSpell(activePlayer, guile, listOf(opponentBears))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // Opponent's creature should now have shroud
        projector.getProjectedKeywords(driver.state, opponentBears).contains(Keyword.SHROUD) shouldBe true
    }
})
