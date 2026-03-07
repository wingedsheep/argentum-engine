package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.conditions.SourceHasKeyword
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for SourceHasKeyword condition.
 *
 * Uses a test creature that gets +2/+2 as long as it has flying.
 */
class SourceHasKeywordTest : FunSpec({

    // A 2/2 creature with flying that gets +2/+2 as long as it has flying
    val FlyingBrute = CardDefinition(
        name = "Flying Brute",
        manaCost = ManaCost.parse("{2}{U}"),
        typeLine = TypeLine.creature(setOf(Subtype("Human"))),
        oracleText = "Flying\nThis creature gets +2/+2 as long as it has flying.",
        creatureStats = CreatureStats(2, 2),
        keywords = setOf(Keyword.FLYING),
        script = CardScript.permanent(
            staticAbilities = listOf(
                ConditionalStaticAbility(
                    ability = ModifyStats(2, 2, StaticTarget.SourceCreature),
                    condition = SourceHasKeyword(Keyword.FLYING)
                )
            )
        )
    )

    // A 2/2 creature without flying that also has the same conditional ability
    val GroundBrute = CardDefinition(
        name = "Ground Brute",
        manaCost = ManaCost.parse("{2}{G}"),
        typeLine = TypeLine.creature(setOf(Subtype("Human"))),
        oracleText = "This creature gets +2/+2 as long as it has flying.",
        creatureStats = CreatureStats(2, 2),
        script = CardScript.permanent(
            staticAbilities = listOf(
                ConditionalStaticAbility(
                    ability = ModifyStats(2, 2, StaticTarget.SourceCreature),
                    condition = SourceHasKeyword(Keyword.FLYING)
                )
            )
        )
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FlyingBrute, GroundBrute))
        return driver
    }

    test("creature with flying gets +2/+2 from SourceHasKeyword condition") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val brute = driver.putCreatureOnBattlefield(activePlayer, "Flying Brute")
        val projected = projector.project(driver.state)

        projected.hasKeyword(brute, Keyword.FLYING) shouldBe true
        projected.getPower(brute) shouldBe 4
        projected.getToughness(brute) shouldBe 4
    }

    test("creature without flying does not get +2/+2 from SourceHasKeyword condition") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val brute = driver.putCreatureOnBattlefield(activePlayer, "Ground Brute")
        val projected = projector.project(driver.state)

        projected.hasKeyword(brute, Keyword.FLYING) shouldBe false
        projected.getPower(brute) shouldBe 2
        projected.getToughness(brute) shouldBe 2
    }

    test("creature gains bonus when granted flying by another effect") {
        val driver = createDriver()
        driver.registerCards(listOf(
            CardDefinition(
                name = "Flight Granter",
                manaCost = ManaCost.parse("{U}"),
                typeLine = TypeLine.parse("Enchantment"),
                oracleText = "All creatures you control have flying.",
                script = CardScript.permanent(
                    staticAbilities = listOf(
                        GrantKeyword(Keyword.FLYING, StaticTarget.AllControlledCreatures)
                    )
                )
            )
        ))

        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val brute = driver.putCreatureOnBattlefield(activePlayer, "Ground Brute")

        // Without Flight Granter, no bonus
        var projected = projector.project(driver.state)
        projected.getPower(brute) shouldBe 2
        projected.getToughness(brute) shouldBe 2

        // Put Flight Granter on the battlefield
        driver.putPermanentOnBattlefield(activePlayer, "Flight Granter")

        // Now Ground Brute has flying from the enchantment, so it should get +2/+2
        projected = projector.project(driver.state)
        projected.hasKeyword(brute, Keyword.FLYING) shouldBe true
        projected.getPower(brute) shouldBe 4
        projected.getToughness(brute) shouldBe 4
    }
})
