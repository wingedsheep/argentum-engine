package com.wingedsheep.engine

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.emerge
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * "Emerge from [quality]" (CR 702.119b): the sacrificed permanent must have the named quality
 * instead of being a creature, and its mana value still comes off the generic part of the emerge
 * cost. Card-level coverage lives in `CrabominationScenarioTest`.
 */
class EmergeFromQualityTest : FunSpec({

    val ArtifactEmerger = card("Artifact Emerger") {
        manaCost = "{6}{B}"
        typeLine = "Creature — Horror"
        power = 5
        toughness = 5
        oracleText = "Emerge from artifact {4}{B}"
        emerge("{4}{B}", from = GameObjectFilter.Artifact)
    }

    val PlainEmerger = card("Plain Emerger") {
        manaCost = "{6}{B}"
        typeLine = "Creature — Horror"
        power = 5
        toughness = 5
        oracleText = "Emerge {4}{B}"
        emerge("{4}{B}")
    }

    val Trinket = card("Emerge Trinket") {
        manaCost = "{3}"
        typeLine = "Artifact"
        oracleText = ""
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ArtifactEmerger, PlainEmerger, Trinket))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 60), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun emergeOffers(driver: GameTestDriver, player: EntityId, cardId: EntityId) =
        driver.legalActions(player).filter {
            val action = it.action
            action is CastSpell && action.cardId == cardId && action.alternativeCostType == AlternativeCostType.EMERGE
        }

    fun castEmerge(driver: GameTestDriver, player: EntityId, cardId: EntityId, sacrifice: EntityId) =
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.EMERGE,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(sacrifice))
            )
        )

    test("emerge from artifact offers only artifacts and reduces by the artifact's mana value") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val emerger = driver.putCardInHand(player, "Artifact Emerger")
        val trinket = driver.putPermanentOnBattlefield(player, "Emerge Trinket")
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.giveMana(player, Color.BLACK, 2)

        val offer = emergeOffers(driver, player, emerger).single()
        val info = offer.additionalCostInfo!!
        withClue("a non-artifact creature is no emerge-from-artifact payment") {
            info.validSacrificeTargets shouldContainExactly listOf(trinket)
        }
        info.description shouldBe "an artifact to sacrifice (its mana value reduces the emerge cost)"
        info.costAfterSacrifice!![trinket] shouldBe "{1}{B}"

        castEmerge(driver, player, emerger, trinket).outcome shouldBe Outcome.Done
        driver.getGraveyard(player) shouldContain trinket
        withClue("the creature was not sacrificed") { driver.state.getBattlefield() shouldContain bears }
        driver.bothPass()
        driver.state.getBattlefield() shouldContain emerger
    }

    test("a creature can't be submitted as the sacrifice for emerge from artifact") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val emerger = driver.putCardInHand(player, "Artifact Emerger")
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        repeat(5) { driver.giveMana(player, Color.BLACK) }

        emergeOffers(driver, player, emerger) shouldBe emptyList()
        driver.submitExpectFailure(
            CastSpell(
                playerId = player,
                cardId = emerger,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.EMERGE,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears))
            )
        )
        driver.state.getBattlefield() shouldContain bears
    }

    test("plain emerge still sacrifices a creature and ignores a noncreature artifact") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val emerger = driver.putCardInHand(player, "Plain Emerger")
        driver.putPermanentOnBattlefield(player, "Emerge Trinket")
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        repeat(3) { driver.giveMana(player, Color.BLACK) }

        val info = emergeOffers(driver, player, emerger).single().additionalCostInfo!!
        info.validSacrificeTargets shouldContainExactly listOf(bears)
        info.description shouldBe "a creature to sacrifice (its mana value reduces the emerge cost)"
    }
})
