package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khm.cards.StalwartValkyrie
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Stalwart Valkyrie {3}{W} — Creature — Angel Warrior 3/2.
 *
 * "Flying
 *  You may pay {1}{W} and exile a creature card from your graveyard rather than pay this spell's
 *  mana cost."
 */
class StalwartValkyrieScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(StalwartValkyrie))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun altCast(driver: GameTestDriver, player: EntityId, cardId: EntityId): LegalAction? =
        driver.legalActions(player).firstOrNull { legal ->
            val action = legal.action
            action is CastSpell && action.cardId == cardId &&
                action.useAlternativeCost &&
                action.alternativeCostType == AlternativeCostType.SELF_ALTERNATIVE
        }

    test("paying {1}{W} and exiling a creature card from the graveyard casts it as a 3/2 flier") {
        val driver = createDriver()
        val you = driver.activePlayer!!

        val elves = driver.putCardInGraveyard(you, "Llanowar Elves")
        driver.putCardInGraveyard(you, "Lightning Bolt")
        val valkyrie = driver.putCardInHand(you, "Stalwart Valkyrie")
        driver.giveMana(you, Color.WHITE, 2)

        val offered = altCast(driver, you, valkyrie)!!
        val cost = offered.additionalCostInfo!!
        cost.costType shouldBe "ExileFromGraveyard"
        // Only the creature card is a legal exile choice — the instant isn't.
        cost.validExileTargets shouldBe listOf(elves)

        driver.submit(
            CastSpell(
                playerId = you,
                cardId = valkyrie,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
                additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(elves))
            )
        ).error shouldBe null
        driver.bothPass()

        val permanent = driver.findPermanent(you, "Stalwart Valkyrie")!!
        driver.getExileCardNames(you) shouldBe listOf("Llanowar Elves")
        driver.getGraveyardCardNames(you) shouldBe listOf("Lightning Bolt")
        val projected = driver.state.projectedState
        projected.getPower(permanent) shouldBe 3
        projected.getToughness(permanent) shouldBe 2
        projected.hasKeyword(permanent, Keyword.FLYING) shouldBe true
    }

    test("the alternative cost is not offered without a creature card in the graveyard") {
        val driver = createDriver()
        val you = driver.activePlayer!!

        driver.putCardInGraveyard(you, "Lightning Bolt")
        val valkyrie = driver.putCardInHand(you, "Stalwart Valkyrie")
        driver.giveMana(you, Color.WHITE, 2)

        altCast(driver, you, valkyrie) shouldBe null
    }

    test("it can still be cast normally for {3}{W}, exiling nothing") {
        val driver = createDriver()
        val you = driver.activePlayer!!

        driver.putCardInGraveyard(you, "Llanowar Elves")
        val valkyrie = driver.putCardInHand(you, "Stalwart Valkyrie")
        driver.giveMana(you, Color.WHITE, 4)

        driver.castSpell(you, valkyrie).error shouldBe null
        driver.bothPass()

        (driver.findPermanent(you, "Stalwart Valkyrie") != null) shouldBe true
        driver.getGraveyardCardNames(you) shouldBe listOf("Llanowar Elves")
        driver.getExileCardNames(you) shouldBe emptyList()
    }
})
