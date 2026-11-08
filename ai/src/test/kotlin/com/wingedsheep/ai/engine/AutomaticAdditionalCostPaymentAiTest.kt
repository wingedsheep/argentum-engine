package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AutomaticAdditionalCostPaymentAiTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Forest" to 40))
    }

    test("AI materializes each enumerated spell additional-cost selection") {
        val driver = driver()
        val player = driver.activePlayer!!
        val spell = driver.putCardInHand(player, "Grizzly Bears")
        val candidates = List(8) { driver.putCardInHand(player, "Forest") }
        val ai = AIPlayer.create(driver.cardRegistry, player)

        fun choose(info: AdditionalCostData): CastSpell {
            val legal = LegalAction(
                action = CastSpell(player, spell),
                actionType = "CastSpell",
                description = "test",
                additionalCostInfo = info,
            )
            return ai.chooseFrom(driver.state, listOf(legal)).action as CastSpell
        }

        choose(AdditionalCostData("behold", "Behold", validBeholdTargets = candidates, beholdCount = 2))
            .additionalCostPayment?.beheldCards shouldBe candidates.take(2)
        choose(AdditionalCostData("reveal", "RevealCard", validRevealTargets = candidates, revealCount = 2))
            .additionalCostPayment?.revealedCards shouldBe candidates.take(2)
        choose(AdditionalCostData("tap", "TapPermanents", validTapTargets = candidates, tapCount = 2))
            .additionalCostPayment?.tappedPermanents shouldBe candidates.take(2)
        choose(AdditionalCostData("discard", "DiscardCard", validDiscardTargets = candidates, discardCount = 2))
            .additionalCostPayment?.discardedCards shouldBe candidates.take(2)
        choose(AdditionalCostData("sacrifice", "SacrificePermanent", validSacrificeTargets = candidates, sacrificeCount = 2))
            .additionalCostPayment?.sacrificedPermanents shouldBe candidates.take(2)
        choose(AdditionalCostData("bounce", "BouncePermanent", validBounceTargets = candidates, bounceCount = 2))
            .additionalCostPayment?.bouncedPermanents shouldBe candidates.take(2)
        choose(AdditionalCostData("exile", "ExileFromGraveyard", validExileTargets = candidates, exileMinCount = 2))
            .additionalCostPayment?.exiledCards shouldBe candidates.take(2)
    }

    test("AI binds X and sacrifices exactly X for a sacrifice-X spell cost") {
        val driver = driver()
        val player = driver.activePlayer!!
        val spell = driver.putCardInHand(player, "Grizzly Bears")
        val fodder = List(3) { driver.putCreatureOnBattlefield(player, "Grizzly Bears") }
        val legal = LegalAction(
            action = CastSpell(player, spell),
            actionType = "CastSpell",
            description = "test",
            hasXCost = true,
            maxAffordableX = 2,
            additionalCostInfo = AdditionalCostData(
                description = "Sacrifice X creatures you control",
                costType = "SacrificeVariable",
                validSacrificeTargets = fodder,
                sacrificeCount = 0,
                countIsX = true,
            ),
        )

        val chosen = AIPlayer.create(driver.cardRegistry, player)
            .chooseFrom(driver.state, listOf(legal)).action as CastSpell
        val x = chosen.xValue ?: 0
        x shouldBe 2
        chosen.additionalCostPayment?.variableCostPermanents?.size shouldBe x
    }

    test("AI binds X and discards exactly X for a discard-X spell cost") {
        val driver = driver()
        val player = driver.activePlayer!!
        val spell = driver.putCardInHand(player, "Grizzly Bears")
        val fodder = List(3) { driver.putCardInHand(player, "Forest") }
        val legal = LegalAction(
            action = CastSpell(player, spell),
            actionType = "CastSpell",
            description = "test",
            hasXCost = true,
            maxAffordableX = 2,
            additionalCostInfo = AdditionalCostData(
                description = "Discard X cards",
                costType = "DiscardCard",
                validDiscardTargets = fodder,
                discardCount = 0,
                discardMaxCount = 3,
                countIsX = true,
            ),
        )

        val chosen = AIPlayer.create(driver.cardRegistry, player)
            .chooseFrom(driver.state, listOf(legal)).action as CastSpell
        val x = chosen.xValue ?: 0
        x shouldBe 2
        chosen.additionalCostPayment?.discardedCards?.size shouldBe x
    }

    test("AI materializes enumerated costs for activated abilities") {
        val driver = driver()
        val player = driver.activePlayer!!
        val source = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val fodder = driver.putCreatureOnBattlefield(player, "Hill Giant")
        val legal = LegalAction(
            action = ActivateAbility(player, source, AbilityId("test")),
            actionType = "ActivateAbility",
            description = "test",
            additionalCostInfo = AdditionalCostData(
                description = "tap a permanent",
                costType = "TapPermanents",
                validTapTargets = listOf(fodder),
                tapCount = 1,
            ),
        )

        val chosen = AIPlayer.create(driver.cardRegistry, player)
            .chooseFrom(driver.state, listOf(legal)).action as ActivateAbility
        chosen.costPayment?.tappedPermanents shouldBe listOf(fodder)
    }

    context("a \"costs {N} less for each\" discount picker") {
        // Gorex, the Tombshell's shape: exile any number of creature cards from your graveyard,
        // {2} off for each. The offer is priced at the largest discount; the AI used to submit no
        // picks at all, so a cast the discount made affordable was refused.
        val tombshell = card("Test Tombshell") {
            manaCost = "{6}{B}{B}"
            typeLine = "Creature — Zombie Turtle"
            power = 4
            toughness = 4
            additionalCost(Costs.additional.ExileAnyNumberFromGraveyardForCostReduction(GameObjectFilter.Creature, 2))
        }

        fun cast(swamps: Int): Pair<GameTestDriver, CastSpell> {
            val driver = GameTestDriver().apply {
                registerCards(TestCards.all + tombshell)
                initMirrorMatch(Deck.of("Swamp" to 40))
                passPriorityUntil(Step.PRECOMBAT_MAIN)
            }
            val player = driver.activePlayer!!
            val spell = driver.putCardInHand(player, "Test Tombshell")
            repeat(swamps) { driver.putLandOnBattlefield(player, "Swamp") }
            repeat(4) { driver.putCardInGraveyard(player, "Grizzly Bears") }
            val offer = driver.legalActions(player).first { (it.action as? CastSpell)?.cardId == spell }
            val chosen = AIPlayer.create(driver.cardRegistry, player)
                .chooseFrom(driver.state, listOf(offer)).action as CastSpell
            return driver to chosen
        }

        test("exiles only as many cards as the mana it has needs, and the cast goes through") {
            val (driver, chosen) = cast(swamps = 4)
            // {6}{B}{B} from four Swamps: {4} must come off, two cards' worth.
            chosen.additionalCostPayment?.exiledCards?.size shouldBe 2
            driver.submit(chosen).outcome shouldBe Outcome.Done
        }

        test("pays full price, exiling nothing, when its mana already covers the spell") {
            val (_, chosen) = cast(swamps = 8)
            chosen.additionalCostPayment?.exiledCards.orEmpty().size shouldBe 0
        }
    }
})
