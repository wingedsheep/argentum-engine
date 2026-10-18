package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for AttacksWithoutTappingGroupEffect — "attacking doesn't cause [group] to tap".
 *
 * It's a rule modification over a dynamic group (CR 611.2c), not vigilance: creatures that arrive
 * after it resolved are covered, the group is fixed to the resolving player ("you control"), nothing
 * gains a keyword, and an attached condition is re-asked when attackers are declared.
 */
class AttacksWithoutTappingGroupTest : FunSpec({

    val UntappedCharge = card("Untapped Charge") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.AttacksWithoutTappingGroup(GroupFilter.AllCreaturesYouControl, Duration.EndOfTurn)
        }
    }

    val NeverCharge = card("Never Charge") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.AttacksWithoutTappingGroup(
                GroupFilter.AllCreaturesYouControl, Duration.EndOfTurn, condition = Conditions.Not(Conditions.IsYourTurn)
            )
        }
    }

    val Warrior = CardDefinition.creature(
        name = "Test Warrior",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype("Warrior")),
        power = 2,
        toughness = 2,
        oracleText = ""
    )

    fun driverAtMain(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(UntappedCharge, NeverCharge, Warrior))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 10, "Mountain" to 10), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.resolve(name: String) {
        giveMana(player1, Color.WHITE, 1)
        castSpell(player1, putCardInHand(player1, name)).error shouldBe null
        bothPass()
    }

    test("a creature that arrived after the effect resolved attacks without tapping, without vigilance") {
        val driver = driverAtMain()
        driver.resolve("Untapped Charge")
        val warrior = driver.putCreatureOnBattlefield(driver.player1, "Test Warrior")
        driver.removeSummoningSickness(warrior)

        withClue("rule modification, not a granted keyword") {
            driver.state.projectedState.attacksWithoutTapping(warrior) shouldBe true
            driver.state.projectedState.hasKeyword(warrior, "VIGILANCE") shouldBe false
        }
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, mapOf(warrior to driver.player2)).error shouldBe null
        withClue("attacking didn't tap it (CR 611.2c covers late arrivals)") { driver.isTapped(warrior) shouldBe false }
    }

    test("\"creatures you control\" is the resolving player's: an opponent's creatures are unaffected") {
        val driver = driverAtMain()
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Test Warrior")
        driver.resolve("Untapped Charge")
        driver.state.projectedState.attacksWithoutTapping(theirs) shouldBe false
    }

    test("while its condition is false the effect is dark and attackers tap") {
        val driver = driverAtMain()
        driver.resolve("Never Charge")
        val warrior = driver.putCreatureOnBattlefield(driver.player1, "Test Warrior")
        driver.removeSummoningSickness(warrior)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, mapOf(warrior to driver.player2)).error shouldBe null
        driver.isTapped(warrior) shouldBe true
    }
})
