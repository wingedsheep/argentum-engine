package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Nethergoyf (MH3 #103).
 *
 * "Nethergoyf's power is equal to the number of card types among cards in your graveyard and its
 * toughness is equal to that number plus 1. / Escape—{2}{B}, Exile any number of other cards from
 * your graveyard with four or more card types among them."
 */
class NethergoyfScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to player
    }

    fun escape(driver: GameTestDriver, player: EntityId, goyf: EntityId, exiled: List<EntityId>) =
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = goyf,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.ESCAPE,
                additionalCostPayment = AdditionalCostPayment(exiledCards = exiled),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )

    fun escapeAction(driver: GameTestDriver, player: EntityId) =
        driver.legalActions(player).firstOrNull { it.actionType == "CastWithEscape" }

    test("escapes by exiling cards showing four card types between them, and counts only your graveyard") {
        val (driver, player) = setup()
        val opponent = driver.getOpponent(player)
        val goyf = driver.putCardInGraveyard(player, "Nethergoyf")
        // Artifact creature + instant + land: four card types among three cards.
        val ornithopter = driver.putCardInGraveyard(player, "Ornithopter")
        val bolt = driver.putCardInGraveyard(player, "Lightning Bolt")
        val swamp = driver.putCardInGraveyard(player, "Swamp")
        val divination = driver.putCardInGraveyard(player, "Divination")
        driver.putCardInGraveyard(opponent, "Pacifism")
        driver.giveMana(player, Color.BLACK, 3)

        val action = escapeAction(driver, player).shouldNotBeNull()
        action.affordable shouldBe true
        val info = action.additionalCostInfo.shouldNotBeNull()
        info.costType shouldBe "ExileForTotal"
        info.exileMinTotalWeight shouldBe 4
        info.exileWeightUnit shouldBe "card types"
        info.validExileTargets shouldContainExactlyInAnyOrder listOf(ornithopter, bolt, swamp, divination)
        info.exileCardTypes[ornithopter] shouldBe listOf("ARTIFACT", "CREATURE")

        val result = escape(driver, player, goyf, listOf(ornithopter, bolt, swamp))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.getExile(player) shouldContainAll listOf(ornithopter, bolt, swamp)
        driver.bothPass()

        val perm = driver.findPermanent(player, "Nethergoyf").shouldNotBeNull()
        // Only Divination (sorcery) is left in its owner's graveyard; the opponent's Pacifism doesn't count.
        driver.state.projectedState.getPower(perm) shouldBe 1
        driver.state.projectedState.getToughness(perm) shouldBe 2
    }

    test("an artifact creature and a creature show two card types, not three — that doesn't pay") {
        val (driver, player) = setup()
        val goyf = driver.putCardInGraveyard(player, "Nethergoyf")
        val ornithopter = driver.putCardInGraveyard(player, "Ornithopter")
        val bears = driver.putCardInGraveyard(player, "Grizzly Bears")
        val memnite = driver.putCardInGraveyard(player, "Memnite")
        val bolt = driver.putCardInGraveyard(player, "Lightning Bolt")
        driver.giveMana(player, Color.BLACK, 3)

        // The graveyard can only ever show three types (artifact, creature, instant): the escape is
        // shown but not castable.
        escapeAction(driver, player).shouldNotBeNull().affordable shouldBe false
        driver.submitExpectFailure(
            CastSpell(
                playerId = player,
                cardId = goyf,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.ESCAPE,
                additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(ornithopter, bears, memnite, bolt)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        driver.getGraveyard(player) shouldContain goyf
    }

    test("a submitted selection short of four types is refused even when the graveyard could pay") {
        val (driver, player) = setup()
        val goyf = driver.putCardInGraveyard(player, "Nethergoyf")
        val ornithopter = driver.putCardInGraveyard(player, "Ornithopter")
        val bolt = driver.putCardInGraveyard(player, "Lightning Bolt")
        driver.putCardInGraveyard(player, "Swamp")
        driver.giveMana(player, Color.BLACK, 3)

        escapeAction(driver, player).shouldNotBeNull().affordable shouldBe true
        driver.submitExpectFailure(
            CastSpell(
                playerId = player,
                cardId = goyf,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.ESCAPE,
                additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(ornithopter, bolt)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        // Naming Nethergoyf itself doesn't help: it's on the stack, not an "other" card.
        driver.submitExpectFailure(
            CastSpell(
                playerId = player,
                cardId = goyf,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.ESCAPE,
                additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(ornithopter, bolt, goyf)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        driver.getGraveyard(player) shouldContain goyf
    }

    test("Nethergoyf can't supply the fourth type for its own escape") {
        val (driver, player) = setup()
        // Sorcery, instant, land — Nethergoyf itself would be the fourth type (creature).
        val goyf = driver.putCardInGraveyard(player, "Nethergoyf")
        val divination = driver.putCardInGraveyard(player, "Divination")
        val bolt = driver.putCardInGraveyard(player, "Lightning Bolt")
        val swamp = driver.putCardInGraveyard(player, "Swamp")
        driver.giveMana(player, Color.BLACK, 3)

        escapeAction(driver, player).shouldNotBeNull().affordable shouldBe false
        driver.submitExpectFailure(
            CastSpell(
                playerId = player,
                cardId = goyf,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.ESCAPE,
                additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(divination, bolt, swamp, goyf)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        driver.getGraveyard(player) shouldContain goyf
    }

    test("counts card types, not cards, on the battlefield") {
        val (driver, player) = setup()
        driver.putCardInGraveyard(player, "Ornithopter")
        driver.putCardInGraveyard(player, "Memnite")
        driver.putCardInGraveyard(player, "Grizzly Bears")
        val goyf = driver.putCreatureOnBattlefield(player, "Nethergoyf")
        // Artifact + creature only: a 2/3, however many such cards there are.
        driver.state.projectedState.getPower(goyf) shouldBe 2
        driver.state.projectedState.getToughness(goyf) shouldBe 3
    }
})
