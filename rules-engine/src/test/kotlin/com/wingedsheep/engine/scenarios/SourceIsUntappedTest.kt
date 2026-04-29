package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.conditions.SourceIsUntapped
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for the SourceIsUntapped projection condition.
 *
 * Models Illusion Spinners' "has hexproof as long as it's untapped" pattern.
 */
class SourceIsUntappedTest : FunSpec({

    val UntappedHexproofBeast = CardDefinition(
        name = "Untapped Hexproof Beast",
        manaCost = ManaCost.parse("{2}{U}"),
        typeLine = TypeLine.creature(setOf(Subtype("Beast"))),
        oracleText = "This creature has hexproof as long as it's untapped.",
        creatureStats = CreatureStats(2, 2),
        script = CardScript.permanent(
            staticAbilities = listOf(
                ConditionalStaticAbility(
                    ability = GrantKeyword(Keyword.HEXPROOF, StaticTarget.SourceCreature),
                    condition = SourceIsUntapped
                )
            )
        )
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + UntappedHexproofBeast)
        return driver
    }

    test("untapped creature has hexproof granted by SourceIsUntapped condition") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val beast = driver.putCreatureOnBattlefield(activePlayer, "Untapped Hexproof Beast")
        val projected = projector.project(driver.state)

        projected.hasKeyword(beast, Keyword.HEXPROOF) shouldBe true
    }

    test("tapped creature loses hexproof granted by SourceIsUntapped condition") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val beast = driver.putCreatureOnBattlefield(activePlayer, "Untapped Hexproof Beast")

        var projected = projector.project(driver.state)
        projected.hasKeyword(beast, Keyword.HEXPROOF) shouldBe true

        driver.tapPermanent(beast)
        projected = projector.project(driver.state)
        projected.hasKeyword(beast, Keyword.HEXPROOF) shouldBe false

        driver.untapPermanent(beast)
        projected = projector.project(driver.state)
        projected.hasKeyword(beast, Keyword.HEXPROOF) shouldBe true
    }
})
