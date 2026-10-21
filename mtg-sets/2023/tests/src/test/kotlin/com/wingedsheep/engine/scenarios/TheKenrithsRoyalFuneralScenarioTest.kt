package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * The Kenriths' Royal Funeral (MAT #34) — {2}{W}{B} Legendary Enchantment.
 *
 * "When The Kenriths' Royal Funeral enters, exile up to two target legendary creature cards from
 *  your graveyard. You draw X cards and you lose X life, where X is the greatest mana value among
 *  cards exiled this way.
 *  Legendary spells you cast cost {1} less to cast for each card exiled with The Kenriths' Royal
 *  Funeral."
 *
 * Covers the two things the script leaves to the engine: X is the *greatest* mana value among the
 * exiled cards (not their sum or count), and the exiled cards land in the Funeral's linked-exile
 * pile so the discount counts them — for legendary spells only.
 */
class TheKenrithsRoyalFuneralScenarioTest : ScenarioTestBase() {

    private fun castFuneral(): TestGame {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "The Kenriths' Royal Funeral")
            .withLandsOnBattlefield(1, "Plains", 2)
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withCardInGraveyard(1, "Squee, Goblin Nabob")
            .withCardInGraveyard(1, "Sun Quan, Lord of Wu")
            .withCardInGraveyard(1, "Grizzly Bears")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(8) { builder = builder.withCardInLibrary(1, "Swamp") }
        val game = builder.build()
        game.castSpell(1, "The Kenriths' Royal Funeral").error shouldBe null
        game.resolveStack()
        return game
    }

    private fun genericCost(game: TestGame, spellName: String): Int =
        CostCalculator(cardRegistry, predicateEvaluator = services.predicateEvaluator).calculateEffectiveCost(
            game.state,
            cardRegistry.requireCard(spellName),
            game.player1Id,
        ).genericAmount

    init {
        context("The Kenriths' Royal Funeral") {
            test("exiles two legends, draws and loses the greatest mana value, discounts legendary spells") {
                val game = castFuneral()
                val squee = game.findCardsInGraveyard(1, "Squee, Goblin Nabob").single()
                val sunQuan = game.findCardsInGraveyard(1, "Sun Quan, Lord of Wu").single()
                val handBefore = game.handSize(1)

                game.selectTargets(listOf(squee, sunQuan)).error shouldBe null
                game.resolveStack()

                game.isInExile(1, "Squee, Goblin Nabob") shouldBe true
                game.isInExile(1, "Sun Quan, Lord of Wu") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true

                // X = 6 (Sun Quan), not 3 (Squee) and not 9 (the sum).
                game.handSize(1) shouldBe handBefore + 6
                game.getLifeTotal(1) shouldBe 14

                val funeral = game.findPermanent("The Kenriths' Royal Funeral")!!
                game.state.getEntity(funeral)?.get<LinkedExileComponent>()?.exiledIds.orEmpty() shouldContainExactlyInAnyOrder
                    listOf(squee, sunQuan)

                // Kenrith, the Returned King is {4}{W}: two cards exiled with the Funeral take {2} off.
                genericCost(game, "Kenrith, the Returned King") shouldBe 2
                // Grizzly Bears is not legendary — no discount.
                genericCost(game, "Grizzly Bears") shouldBe 1
            }

            test("choosing no targets exiles nothing: X is 0 and there is no discount") {
                val game = castFuneral()
                val handBefore = game.handSize(1)

                game.skipTargets().error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Squee, Goblin Nabob") shouldBe true
                game.isInGraveyard(1, "Sun Quan, Lord of Wu") shouldBe true
                game.handSize(1) shouldBe handBefore
                game.getLifeTotal(1) shouldBe 20
                genericCost(game, "Kenrith, the Returned King") shouldBe 4
            }
        }
    }
}
